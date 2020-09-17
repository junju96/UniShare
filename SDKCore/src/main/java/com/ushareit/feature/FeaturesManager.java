package com.ushareit.feature;

import android.content.Context;
import android.text.TextUtils;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.stats.Stats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FeaturesManager {
    private static final String TAG = "FeaturesManager";


    private static class FeatureSettings extends Settings {
        private static final String FEATURE_SETTINGS_NAME = "feature_settings";
        public FeatureSettings(Context ctx) {
            super(ctx, FEATURE_SETTINGS_NAME);
        }

    }

    private static final String KEY_LOCAL_VERSION = "ver";
    private static final String KEY_LOCAL_FEATURES = "features";
    private static final String KEY_LOCAL_FEATURE_ERRS = "feature_errs";

    private static AtomicBoolean mInited = new AtomicBoolean(false);
    private static Map<String, String> mFeatures = new HashMap<String, String>(); // feature name -> error id
    private static FeatureSettings mSettings = new FeatureSettings(ObjectStore.getContext());

    public static synchronized boolean isFeatureForbid(String featureName) {
        init();
        return mFeatures.containsKey(featureName);
    }

    public static synchronized void notifyErr(Throwable throwable) {
        try {
            Map<String, String> features = readFeaturesFromLocal();
            Map<String, FeatureErrEntity> localFeatureErrs = readFeatureErrsFromLocal();
            Map<String, Map<String, FeatureErrEntity>> handledErrors = prepareLocalErrors(localFeatureErrs);

            String msg = throwable.getClass().getName() + ":" + throwable.getMessage();
            Map<String, FeatureErrEntity> stackErrs = handledErrors.get(msg);
            if (stackErrs == null)
                return;

            final int MAX_LINES = 30;
            StackTraceElement[] stacks = throwable.getStackTrace();
            if (stacks == null)
                return;

            FeatureErrEntity featureErrEntity = null;
            int lines = stacks.length > MAX_LINES ? MAX_LINES : stacks.length;
            for (int i = 0; i < lines; i++) {
                StackTraceElement trace = stacks[i];
                StringBuilder sb = new StringBuilder();
                sb.append(trace.getClassName()).append(".").append(trace.getMethodName()).append("(");
                featureErrEntity = stackErrs.get(sb.toString());
                if (featureErrEntity != null)
                    break;
            }
            if (featureErrEntity == null)
                return;

            if (!featureErrEntity.mForbidden && !features.containsKey(featureErrEntity.mFeatureName)) {
                featureErrEntity.mCnt ++;
                featureErrEntity.mForbidden = featureErrEntity.mCnt > featureErrEntity.mMaxCnt;
                if (featureErrEntity.mForbidden) {
                    features.put(featureErrEntity.mFeatureName, featureErrEntity.mId);
                    updateFeatures(features);
                    collectFeatureStatus(ObjectStore.getContext(), featureErrEntity.mId, featureErrEntity.mFeatureName, true);
                }
                updateFeatureErrs(localFeatureErrs);
            }
        } catch (Exception e) {
            Logger.w(TAG, "nofify err failed!", e);
        }
    }

    private static void init() {
        if (!mInited.compareAndSet(false, true))
            return;

        try {
            // read features from local preference
            mFeatures.putAll(readFeaturesFromLocal());

            // update local features by config
            String sJson = CloudConfig.getStringConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_FEATURES_DETAIL,"");
            if (TextUtils.isEmpty(sJson))
                return;
            final JSONObject jsonFeatures = new JSONObject(sJson);
            if (needUpdate(jsonFeatures))
                TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("features.update") {
                    @Override
                    public void execute() {
                        try {
                            updateLocalPreferenceByConfig(jsonFeatures);
                        } catch (JSONException e) {
                            Logger.w(TAG, "update local preference failed!", e);
                        }
                    }
                });

        } catch (Exception e) {
            Logger.w(TAG, "init failed!", e);
        }
    }

    private static Map<String, String> readFeaturesFromLocal() {
        Map<String, String> features = new HashMap<String, String>();
        String jsonString = null;
        synchronized (mSettings) {
            jsonString = mSettings.get(KEY_LOCAL_FEATURES);
        }
        if (TextUtils.isEmpty(jsonString))
            return features;

        // local feature: feature name -> error id
        try {
            JSONObject json = new JSONObject(jsonString);
            Iterator<String> keys = json.keys();
            while(keys.hasNext()) {
                try {
                    String key = keys.next();
                    String errId = json.getString(key);
                    features.put(key, errId);
                } catch (JSONException e1) {
                    Logger.w(TAG, "read feature item from local failed!", e1);
                }
            }

        } catch (JSONException e) {
            Logger.w(TAG, "read features from local failed!", e);
        }
        return features;
    }

    // check config ver same with local version
    private static boolean needUpdate(JSONObject jsonFeatures) throws JSONException {
        int ccfVer = jsonFeatures.has("ver") ? jsonFeatures.getInt("ver") : 0;
        if (ccfVer == 0)
            return false;

        synchronized (mSettings) {
            int featureVer = mSettings.getInt(KEY_LOCAL_VERSION);
            return ccfVer != featureVer;
        }
    }

    private static void updateLocalPreferenceByConfig(JSONObject jsonFeatures) throws JSONException {
        int ccfVer = jsonFeatures.has("ver") ? jsonFeatures.getInt("ver") : 0;
        if (ccfVer == 0)
            return ;

        HashSet<String> errIds = readFeatureErrIdsFromConfig(jsonFeatures);
        Map<String, String> localFeatures = new HashMap<String, String>(mFeatures);
        Iterator<Map.Entry<String, String>> it = localFeatures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            // local error id is not exist in config, permit this feature work.
            if (!errIds.contains(entry.getValue())) {
                it.remove();
                collectFeatureStatus(ObjectStore.getContext(), entry.getValue(), entry.getKey(), false);
            }
        }
        JSONObject jFeature = new JSONObject(localFeatures);

        Map<String, FeatureErrEntity> localFeatureErrs = readFeatureErrsFromLocal();
        updateLocalFeatureErrs(jsonFeatures, localFeatureErrs);
        JSONArray jErrors = new JSONArray();
        for (FeatureErrEntity errEntity : localFeatureErrs.values())
            jErrors.put(errEntity.toJSON());

        synchronized (mSettings) {
            mSettings.set(KEY_LOCAL_FEATURE_ERRS, jErrors.toString());
            mSettings.set(KEY_LOCAL_FEATURES, jFeature.toString());
            mSettings.setInt(KEY_LOCAL_VERSION, ccfVer);
        }
    }

    private static void updateFeatures(Map<String, String> features) {
        JSONObject jFeature = new JSONObject(features);
        synchronized (mSettings) {
            mSettings.set(KEY_LOCAL_FEATURES, jFeature.toString());
        }
    }

    private static void updateFeatureErrs(Map<String, FeatureErrEntity> featureErrs) throws JSONException {
        JSONArray jErrors = new JSONArray();
        for (FeatureErrEntity errEntity : featureErrs.values())
            jErrors.put(errEntity.toJSON());

        synchronized (mSettings) {
            mSettings.set(KEY_LOCAL_FEATURE_ERRS, jErrors.toString());
        }
    }

    private static HashSet<String> readFeatureErrIdsFromConfig(JSONObject jsonFeatures) {
        HashSet<String> errIds = new HashSet<String>();
        try {
            JSONArray jarray = jsonFeatures.has("data") ? jsonFeatures.getJSONArray("data") : new JSONArray();
            for (int i = 0; i < jarray.length(); i++) {
                try {
                    String id = FeatureErrEntity.getErrIdFromJSON(jarray.getJSONObject(i));
                    errIds.add(id);
                } catch (Exception e1) {}
            }
        } catch (JSONException e) {
            Logger.w(TAG, "read feature error ids from config failed!", e);
        }
        return errIds;
    }

    private static Map<String, FeatureErrEntity> readFeatureErrsFromLocal() {
        Map<String, FeatureErrEntity> featureErrs = new HashMap<String, FeatureErrEntity>();

        String jsonString = null;
        synchronized (mSettings) {
            jsonString = mSettings.get(KEY_LOCAL_FEATURE_ERRS);
        }
        if (TextUtils.isEmpty(jsonString))
            return featureErrs;
        try {
            JSONArray jarray = new JSONArray(jsonString);
            for (int i = 0; i < jarray.length(); i++) {
                FeatureErrEntity entity = new FeatureErrEntity(jarray.getJSONObject(i));
                featureErrs.put(entity.mFatalMsg + entity.mStackDigest, entity);
            }
        } catch (JSONException e) {
            Logger.w(TAG, "read feature errors from local failed!", e);
        }
        return featureErrs;
    }

    private static void updateLocalFeatureErrs(JSONObject jsonConfigErrs, Map<String, FeatureErrEntity> localErrs) {
        Map<String, FeatureErrEntity> configErrs = readFeatureErrsFromConfig(jsonConfigErrs);
        // remove the unused errors in config
        Iterator<Map.Entry<String, FeatureErrEntity>> it = localErrs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, FeatureErrEntity> entry = it.next();
            if (!configErrs.containsKey(entry.getKey()))
                it.remove();
        }

        // add new error to local
        it = configErrs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, FeatureErrEntity> entry = it.next();
            if (!localErrs.containsKey(entry.getKey()))
                localErrs.put(entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, FeatureErrEntity> readFeatureErrsFromConfig(JSONObject jsonConfigErrs) {
        Map<String, FeatureErrEntity> featureErrs = new HashMap<String, FeatureErrEntity>();

        try {
            JSONArray jarray = jsonConfigErrs.has("data") ? jsonConfigErrs.getJSONArray("data") : new JSONArray();
            for (int i = 0; i < jarray.length(); i++) {
                FeatureErrEntity entity = new FeatureErrEntity(jarray.getJSONObject(i));
                featureErrs.put(entity.mFatalMsg + entity.mStackDigest, entity);
            }
        } catch (JSONException e) {
            Logger.w(TAG, "read feature errors from config failed!", e);
        }
        return featureErrs;
    }

    // process the errors to msg -> stack errors(stack err -> feature)
    private static Map<String, Map<String, FeatureErrEntity>> prepareLocalErrors(Map<String, FeatureErrEntity> localFeatureErrs) {
        Map<String, Map<String, FeatureErrEntity>> handledErrors = new HashMap<String, Map<String, FeatureErrEntity>>();
        for (FeatureErrEntity entity : localFeatureErrs.values()) {
            Map<String, FeatureErrEntity> stacks = handledErrors.get(entity.mFatalMsg);
            if (stacks == null) {
                stacks = new HashMap<String, FeatureErrEntity>();
                handledErrors.put(entity.mFatalMsg, stacks);
            }
            stacks.put(entity.mStackDigest, entity);
        }
        return handledErrors;
    }

    static class FeatureErrEntity {
        public final String mId;
        public boolean mForbidden;

        public final String mFeatureName;
        public final String mFatalMsg;
        public final String mStackDigest;
        public final int mMaxCnt;
        public int mCnt;

        public FeatureErrEntity(JSONObject json) throws JSONException {
            mId = json.getString("id");
            mForbidden = json.has("forbid") ? json.getBoolean("forbid") : false;

            mFeatureName = json.getString("name");
            mFatalMsg = json.getString("msg");
            mStackDigest = json.getString("stack");
            mMaxCnt = json.has("max_cnt") ? json.getInt("max_cnt") : 0;
            mCnt = json.has("cnt") ? json.getInt("cnt") : 0;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", mId);
            json.put("forbid", mForbidden);
            json.put("name", mFeatureName);
            json.put("msg", mFatalMsg);
            json.put("stack", mStackDigest);
            json.put("max_cnt", mMaxCnt);
            json.put("cnt", mCnt);
            return json;
        }

        public static String getErrIdFromJSON(JSONObject json) throws JSONException {
            return json.getString("id");
        }

        @Override
        public String toString() {
            return "FeatureErrEntity{" +
                    "Id='" + mId + '\'' +
                    ", Forbidden=" + mForbidden +
                    ", FeatureName='" + mFeatureName + '\'' +
                    ", FatalMsg='" + mFatalMsg + '\'' +
                    ", StackDigest='" + mStackDigest + '\'' +
                    ", MaxCnt=" + mMaxCnt +
                    ", Cnt=" + mCnt +
                    '}';
        }
    }

    private static final String ENV_FEATURE_FORBID = "env_feature_forbid";
    private static final String ENV_FEATURE_PERMIT = "env_feature_permit";
    private static void collectFeatureStatus(Context context, String id, String name, boolean forbid) {
        try {
            String status = id + "_" + name;
            HashMap<String, String> params = new LinkedHashMap<String, String>();
            params.put("status", status);
            Logger.v(TAG, "collect event " + (forbid ? ENV_FEATURE_FORBID : ENV_FEATURE_PERMIT) + ", params:" + params.toString());
            Stats.onEvent(context, forbid ? ENV_FEATURE_FORBID : ENV_FEATURE_PERMIT, params);
            // wait for collection
            Thread.sleep(500);
        } catch (Exception e) {}
    }
}
