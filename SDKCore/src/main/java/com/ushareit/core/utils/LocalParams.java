package com.ushareit.core.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Pair;

import com.ushareit.core.lang.BeylaIdHelper;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.net.NetworkStatus;
import com.ushareit.core.utils.device.DeviceHelper;
import com.ushareit.core.net.NetUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class LocalParams {
    // Device client information related keys
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_APP_ID = "app_id";
    public static final String KEY_APP_VER = "app_ver";
    public static final String KEY_APP_VER_NAME = "app_ver_name";
    public static final String KEY_OS_VER = "os_ver";
    public static final String KEY_OS_TYPE = "os_type";
    public static final String KEY_SCREEN_WIDTH = "screen_width";
    public static final String KEY_SCREEN_HEIGHT = "screen_height";
    public static final String KEY_DEVICE_CATEGORY = "device_category";
    public static final String KEY_DEVICE_MODEL = "device_model";
    public static final String KEY_RELEASE_CHANNEL = "release_channel";
    public static final String KEY_LANG = "lang";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_MANUFACTURER = "manufacturer";
    public static final String KEY_DPI = "dpi";

    public static final String KEY_NET = "net";
    public static final String KEY_ANDROID_ID = "android_id";
    public static final String KEY_MAC = "mac";
    public static final String KEY_IMEI = "imei";

    public static final String KEY_LAST_MANUAL_ACTIVE_TIME = "last_manual_act_t";
    public static final String KEY_LAST_SHOW_NOTIFY_TIME = "last_show_notify_t";

    public static final String KEY_MOBILENETTYPE = "mobile_net_type";
    public static final String KEY_IMSI = "imsi";
    public static final String KEY_GAID = "gaid";
    public static final String KEY_BEYLA_ID = "beyla_id";
    public static final String KEY_TIME_ZONE = "time_zone";
    public static final String KEY_CARRIER = "carrier";

    public static final String KEY_SIM_COUNT = "sim_count";
    public static final String KEY_SIM_ACTIVE_COUNT = "sim_active_cnt";
    public static final String KEY_IMSI_MINOR = "imsi_minor";

    public static final String KEY_FORCED_LANG = "forced_lang";

    public static final String KEY_LOCATION_COUNTRY = "location_country";
    public static final String KEY_LOCATION_PROVINCE = "location_province";

    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";

    public String deviceId;          // device's identifier
    public String userId;          // user's identifier
    public String appId;          // app's identifier, for android, is package name
    public int appVer;            // user's AnyShare application version code
    public String appVerName;     // user's AnyShare application version name
    public int osVer;             // for andriod is api level
    public String osType;         // "android" or "windows"
    public int screenWidth;       // screen width, in pixels
    public int screenHeight;      // screen height, in pixels
    public String deviceCategory; // device type: like "phone", "pc", "pad"
    public String deviceModel;    // user's device model (K860, etc)
    public String releaseChannel; // user's app's release channel
    public String lang;
    public String country;
    public String manufacturer;
    public int dpi;

    // full params
    public String net;              // network type
    public String androidId;        // android id
    public String mac;              // mac address
    public String imei;             // imei

    // command special params
    public long lastManualActTime;      // last user manual active SHAREit time
    public long lastShowNotifyTime;     // last show notification time

    // operator params
    public String mobileNetType;
    public String imsi;

    public String gaid;
    public String carrier;

    public String beylaId;
    public int timeZone = Integer.MIN_VALUE;

    public int simCount = Integer.MIN_VALUE;
    public int simActiveCount = Integer.MIN_VALUE;
    public String imsiMinor;

    // hidden menu test use
    public String forced_lang;

    public String lat;
    public String lng;

    public String location_country;
    public String location_province;


    public LocalParams() {}

    public LocalParams(JSONObject jo) throws JSONException {
        if (jo.has(KEY_DEVICE_ID))
            deviceId = jo.getString(KEY_DEVICE_ID);
        else
            deviceId = "";
        if (jo.has(KEY_USER_ID))
            userId = jo.getString(KEY_USER_ID);
        else
            userId = "";
        if (jo.has(KEY_APP_ID))
            appId = jo.getString(KEY_APP_ID);
        else
            appId = "";
        if (jo.has(KEY_APP_VER))
            appVer = jo.getInt(KEY_APP_VER);
        else
            appVer = 0;
        if (jo.has(KEY_APP_VER_NAME))
            appVerName = jo.getString(KEY_APP_VER_NAME);
        else
            appVerName = "";
        if (jo.has(KEY_OS_VER))
            osVer = jo.getInt(KEY_OS_VER);
        else
            osVer = 0;
        if (jo.has(KEY_OS_TYPE))
            osType = jo.getString(KEY_OS_TYPE);
        else
            osType = "";
        if (jo.has(KEY_SCREEN_WIDTH))
            screenWidth = jo.getInt(KEY_SCREEN_WIDTH);
        else
            screenWidth = 0;
        if (jo.has(KEY_SCREEN_HEIGHT))
            screenHeight = jo.getInt(KEY_SCREEN_HEIGHT);
        else
            screenHeight = 0;
        if (jo.has(KEY_DEVICE_CATEGORY))
            deviceCategory = jo.getString(KEY_DEVICE_CATEGORY);
        else
            deviceCategory = "";
        if (jo.has(KEY_DEVICE_MODEL))
            deviceModel = jo.getString(KEY_DEVICE_MODEL);
        else
            deviceModel = "";
        if (jo.has(KEY_RELEASE_CHANNEL))
            releaseChannel = jo.getString(KEY_RELEASE_CHANNEL);
        else
            releaseChannel = "";
        if (jo.has(KEY_LANG))
            lang = jo.getString(KEY_LANG);
        else
            lang = "";
        if (jo.has(KEY_COUNTRY))
            country = jo.getString(KEY_COUNTRY);
        else
            country = "";
        if (jo.has(KEY_MANUFACTURER))
            manufacturer = jo.getString(KEY_MANUFACTURER);
        else
            manufacturer = "";
        if (jo.has(KEY_DPI))
            dpi = jo.getInt(KEY_DPI);
        else
            dpi = 0;

        if (jo.has(KEY_NET))
            net = jo.getString(KEY_NET);
        else
            net = "";
        if (jo.has(KEY_ANDROID_ID))
            androidId = jo.getString(KEY_ANDROID_ID);
        else
            androidId = "";
        if (jo.has(KEY_MAC))
            mac = jo.getString(KEY_MAC);
        else
            mac = "";
        if (jo.has(KEY_IMEI))
            imei = jo.getString(KEY_IMEI);
        else
            imei = "";

        if (jo.has(KEY_LAST_MANUAL_ACTIVE_TIME))
            lastManualActTime = jo.getLong(KEY_LAST_MANUAL_ACTIVE_TIME);
        else
            lastManualActTime = 0;
        if (jo.has(KEY_LAST_SHOW_NOTIFY_TIME))
            lastShowNotifyTime = jo.getLong(KEY_LAST_SHOW_NOTIFY_TIME);
        else
            lastShowNotifyTime = 0;

        if (jo.has(KEY_MOBILENETTYPE))
            mobileNetType = jo.getString(KEY_MOBILENETTYPE);
        else
            mobileNetType = "";
        if (jo.has(KEY_IMSI))
            imsi = jo.getString(KEY_IMSI);
        else
            imsi = "";

        if (jo.has(KEY_GAID))
            gaid = jo.getString(KEY_GAID);
        else
            gaid = "";

        if (jo.has(KEY_CARRIER))
            carrier = jo.getString(KEY_CARRIER);
        else
            carrier = "";

        if (jo.has(KEY_BEYLA_ID))
            beylaId = jo.getString(KEY_BEYLA_ID);
        else
            beylaId = "";
        if (jo.has(KEY_FORCED_LANG))
            forced_lang = jo.getString(KEY_FORCED_LANG);
        else
            forced_lang = "";
        if(jo.has(KEY_LAT))
            lat = jo.getString(KEY_LAT);
        else
            lat = "";
        if(jo.has(KEY_LNG))
            lng = jo.getString(KEY_LNG);
        else
            lng = "";

    }

    public JSONObject toJSONObject() {
        JSONObject jo = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : toMap().entrySet())
                jo.put(entry.getKey(), entry.getValue());
        } catch (JSONException e) {
        }
        return jo;
    }

    public JSONObject toJSONObject(JSONObject jo) {
        try {
            for (Map.Entry<String, Object> entry : toMap().entrySet())
                jo.put(entry.getKey(), entry.getValue());
        } catch (JSONException e) {
        }
        return jo;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        if (StringUtils.isNotEmpty(deviceId))
            map.put(KEY_DEVICE_ID, deviceId);
        if (StringUtils.isNotEmpty(userId))
            map.put(KEY_USER_ID, userId);
        if (StringUtils.isNotEmpty(appId))
            map.put(KEY_APP_ID, appId);
        if (appVer != 0)
            map.put(KEY_APP_VER, appVer);
        if (StringUtils.isNotEmpty(appVerName))
            map.put(KEY_APP_VER_NAME, appVerName);
        if (osVer != 0)
            map.put(KEY_OS_VER, osVer);
        if (StringUtils.isNotEmpty(osType))
            map.put(KEY_OS_TYPE, osType);
        if (screenWidth != 0)
            map.put(KEY_SCREEN_WIDTH, screenWidth);
        if (screenHeight != 0)
            map.put(KEY_SCREEN_HEIGHT, screenHeight);
        if (StringUtils.isNotEmpty(deviceCategory))
            map.put(KEY_DEVICE_CATEGORY, deviceCategory);
        if (StringUtils.isNotEmpty(deviceModel))
            map.put(KEY_DEVICE_MODEL, deviceModel);
        if (StringUtils.isNotEmpty(releaseChannel))
            map.put(KEY_RELEASE_CHANNEL, releaseChannel);
        if (StringUtils.isNotEmpty(lang))
            map.put(KEY_LANG, lang);
        if (StringUtils.isNotEmpty(country))
            map.put(KEY_COUNTRY, country);
        if (StringUtils.isNotEmpty(manufacturer))
            map.put(KEY_MANUFACTURER, manufacturer);
        if (dpi != 0)
            map.put(KEY_DPI, dpi);

        if (StringUtils.isNotEmpty(net))
            map.put(KEY_NET, net);
        if (StringUtils.isNotEmpty(androidId))
            map.put(KEY_ANDROID_ID, androidId);
        if (StringUtils.isNotEmpty(mac))
            map.put(KEY_MAC, mac);
        if (StringUtils.isNotEmpty(imei))
            map.put(KEY_IMEI, imei);

        if (lastManualActTime != 0)
            map.put(KEY_LAST_MANUAL_ACTIVE_TIME, lastManualActTime);
        if (lastShowNotifyTime != 0)
            map.put(KEY_LAST_SHOW_NOTIFY_TIME, lastShowNotifyTime);

        if (StringUtils.isNotEmpty(mobileNetType))
            map.put(KEY_MOBILENETTYPE, mobileNetType);
        if (StringUtils.isNotEmpty(imsi))
            map.put(KEY_IMSI, imsi);
        if (StringUtils.isNotEmpty(gaid))
            map.put(KEY_GAID, gaid);
        if (StringUtils.isNotEmpty(carrier))
            map.put(KEY_CARRIER, carrier);
        if (StringUtils.isNotEmpty(beylaId))
            map.put(KEY_BEYLA_ID, beylaId);
        if(timeZone != Integer.MIN_VALUE)
            map.put(KEY_TIME_ZONE,timeZone);
        if (simCount != Integer.MIN_VALUE)
            map.put(KEY_SIM_COUNT, simCount);
        if (simActiveCount != Integer.MIN_VALUE)
            map.put(KEY_SIM_ACTIVE_COUNT, simActiveCount);
        if (StringUtils.isNotEmpty(imsiMinor))
            map.put(KEY_IMSI_MINOR, imsiMinor);

        if (StringUtils.isNotEmpty(forced_lang))
            map.put(KEY_FORCED_LANG, forced_lang);

        if(StringUtils.isNotEmpty(lat))
            map.put(KEY_LAT,lat);
        if(StringUtils.isNotEmpty(lng))
            map.put(KEY_LNG,lng);

        if(StringUtils.isNotEmpty(location_country))
            map.put(KEY_LOCATION_COUNTRY, location_country);
        if(StringUtils.isNotEmpty(location_province))
            map.put(KEY_LOCATION_PROVINCE, location_province);


        return map;
    }

    public String toString() {
        JSONObject jo = toJSONObject();
        return jo.toString();
    }

    public static LocalParams fromJSON(String json) {
        try {
            JSONObject jo = new JSONObject(json);
            return new LocalParams(jo);
        } catch (JSONException e) {
            return null;
        }
    }

    public static LocalParams createLocalParams(Context context) {
        LocalParams params = new LocalParams();

        params.deviceId = DeviceHelper.getOrCreateDeviceId(context);
        Resources resources = context.getResources();
        params.appId = AppDist.getAppId(context);
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            params.appVer = info.versionCode;
            params.appVerName = info.versionName;
        } catch (Exception e) {
            params.appVer = 0;
            params.appVerName = "";
        }
        params.osVer = android.os.Build.VERSION.SDK_INT;
        params.osType = "android";
        params.screenWidth = resources.getDisplayMetrics().widthPixels;
        params.screenHeight = resources.getDisplayMetrics().heightPixels;
        params.deviceCategory = Utils.detectDeviceType(context).toString();
        params.deviceModel = android.os.Build.MODEL;
        params.releaseChannel = AppDist.getChannel();
        params.lang = resources.getConfiguration().locale.getLanguage();
        params.country = resources.getConfiguration().locale.getCountry();
        params.manufacturer = android.os.Build.MANUFACTURER;
        params.dpi = resources.getDisplayMetrics().densityDpi;
        params.beylaId = BeylaIdHelper.getBeylaId();

        return params;
    }

    public static LocalParams createFullLocalParams(Context context) {
        LocalParams localParams = createLocalParams(context);
        localParams.net =  NetUtils.getNetworkTypeName(context);
        localParams.androidId = DeviceHelper.getAndroidID(context);
        localParams.mac = DeviceHelper.getMacAddress(context);
        localParams.imei = DeviceHelper.getIMEI(context);
        return localParams;
    }

    public static LocalParams createCommandLocalParams(Context context) {
        LocalParams params = createLocalParams(context);
        createSIMInfoParams(context, params);

        NetworkStatus status = NetworkStatus.getNetworkStatus(context);
        params.mobileNetType = status.getNetTypeDetail();
        params.imsi = status.getNumeric();
        params.gaid = DeviceHelper.getGAID(context);
        params.carrier = NetworkStatus.getNetworkStatus(context).getCarrier();
        addForcedInfoParams(context, params);
        return params;
    }

    public static LocalParams createGcmLocalParams(Context context, Pair<String, String> location){
        LocalParams localParams = createFullLocalParams(context);
        localParams.gaid = DeviceHelper.getGAID(context);
        localParams.carrier = NetworkStatus.getNetworkStatus(context).getCarrier();
        createSIMInfoParams(context, localParams);
        localParams.timeZone = TimeZone.getDefault().getRawOffset();
        addForcedInfoParams(context, localParams);
        if (location != null) {
            localParams.lat = location.first;
            localParams.lng = location.second;
        }
        return localParams;
    }

    public static LocalParams createCcfLocalParams(Context context, Pair<String, String> location) {
        LocalParams localParams = createLocalParams(context);
        localParams.carrier = NetworkStatus.getNetworkStatus(context).getCarrier();
        createSIMInfoParams(context, localParams);
        addForcedInfoParams(context, localParams);
        if (location != null) {
            localParams.lat = location.first;
            localParams.lng = location.second;
        }
        return localParams;
    }

    public static LocalParams createNotifyLocalParams(Context context, Pair<String, String> location) {
        return createCcfLocalParams(context, location);
    }

    private static void createSIMInfoParams(Context context, LocalParams localParams) {
        localParams.simCount = DeviceHelper.supportSimCount(context);
        localParams.simActiveCount = DeviceHelper.activeSimCount(context);
        List<String> imsis = DeviceHelper.getIMSIs(context);
        if (imsis.size() > 0)
            localParams.imsi = imsis.get(0);
        if (imsis.size() > 1)
            localParams.imsiMinor = imsis.get(1);

    }

    private static String getLanguageSelectValue(Context context) {
        return new Settings(context, "content_preference").get("language_select_value_v3", "");
    }

    private static void addForcedInfoParams(Context context, LocalParams localParams) {
        localParams.forced_lang =getLanguageSelectValue(context);
    }
}
