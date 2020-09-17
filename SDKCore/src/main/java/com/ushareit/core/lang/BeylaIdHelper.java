package com.ushareit.core.lang;

import android.os.Environment;
import android.text.TextUtils;


import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.utils.device.DeviceHelper;
import com.ushareit.core.utils.permission.PermissionsUtils;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.algo.HashUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class BeylaIdHelper {
    private static final String TAG = "BeylaIdHelper";

    private final static String BEYLA_EXTERNAL_PATH = ".SHAREit";
    private final static String BEYLA_CFG_NAME = ".shareit_beyla_ids.cfg";
    private final static String KEY_BEYLA_ID = "beyla_id";
    private final static String KEY_ND_ID = "beyla_nd_id";

    private static String mBeylaId;
    private static String mNDId;
    private static String mExternalBeylaIdPath = null;
    private static String mDCIMBeylaIdPath = null;

    private static void init() {
        try {
            if (mExternalBeylaIdPath == null)
                mExternalBeylaIdPath = new File(Environment.getExternalStorageDirectory(), BEYLA_EXTERNAL_PATH + File.separator + BEYLA_CFG_NAME).getAbsolutePath();
            if (mDCIMBeylaIdPath == null)
                mDCIMBeylaIdPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), BEYLA_CFG_NAME).getAbsolutePath();
        } catch (Exception e) {
            Logger.w(TAG, "init beyla id file path", e);
        }
    }

    public static String getBeylaId() {
        if (mBeylaId != null)
            return mBeylaId;

        if (isNoPermission()) {
            Logger.w(TAG, "get beyla id without storage permission!");
            return "";
        }

        synchronized (BeylaIdHelper.class) {
            init();
            mBeylaId = getId(KEY_BEYLA_ID);
            // beyla id is not exist, create beyla id and save it
            if (TextUtils.isEmpty(mBeylaId)) {
                String id = UUID.randomUUID().toString().replaceAll("-", "");
                putIdToPref(KEY_BEYLA_ID, id);
                putIdToFile(KEY_BEYLA_ID, id, mExternalBeylaIdPath);
                putIdToFile(KEY_BEYLA_ID, id, mDCIMBeylaIdPath);
                mBeylaId = id;
            }
        }
        Logger.v(TAG, "get beyla id:" + mBeylaId);
        return mBeylaId;
    }

    public static String getNDId() {
        if (mNDId != null)
            return mNDId;

        if (isNoPermission()) {
            Logger.w(TAG, "get beyla id without storage permission!");
            return "";
        }

        synchronized (BeylaIdHelper.class) {
            init();
            mNDId = getId(KEY_ND_ID);
            // beyla id is not exist, create beyla id and save it
            if (TextUtils.isEmpty(mNDId)) {
                String id = createNDId();
                putIdToPref(KEY_ND_ID, id);
                putIdToFile(KEY_ND_ID, id, mExternalBeylaIdPath);
                putIdToFile(KEY_ND_ID, id, mDCIMBeylaIdPath);
                mNDId = id;
            }
        }
        Logger.v(TAG, "get ND id:" + mNDId);
        return mBeylaId;
    }

    public static void resetBeylaId() {
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        putIdToPref(KEY_BEYLA_ID, id);
        putIdToFile(KEY_BEYLA_ID, id, mExternalBeylaIdPath);
        putIdToFile(KEY_BEYLA_ID, id, mDCIMBeylaIdPath);
        mBeylaId = id;
    }

    private static String getId(String key) {
        //1. get stored id from preference and external
        String idInPref = getIdFromPref(key);
        String idInExternal = getIdFromFile(key, mExternalBeylaIdPath);
        String idInDCIM = getIdFromFile(key, mDCIMBeylaIdPath);

        //2. check the id in preference priority and sync external id with preference
        if (!TextUtils.isEmpty(idInPref)) {
            if (!TextUtils.equals(idInPref, idInExternal))
                putIdToFile(key, idInPref, mExternalBeylaIdPath);
            if (!TextUtils.equals(idInPref, idInDCIM))
                putIdToFile(key, idInPref, mDCIMBeylaIdPath);
            return idInPref;
        }

        //3. id is not exist in preference, check the external store and sync to preference with external. like install APP again
        if (!TextUtils.isEmpty(idInExternal)) {
            if (!TextUtils.equals(idInExternal, idInPref))
                putIdToPref(key, idInExternal);
            if (!TextUtils.equals(idInExternal, idInDCIM))
                putIdToFile(key, idInExternal, mDCIMBeylaIdPath);
            return idInExternal;
        }

        //4. id is not exist in preference, check the external store and sync to preference with external. like install APP again
        if (!TextUtils.isEmpty(idInDCIM)) {
            if (!TextUtils.equals(idInDCIM, idInPref))
                putIdToPref(key, idInDCIM);
            if (!TextUtils.equals(idInDCIM, idInExternal))
                putIdToFile(key, idInDCIM, mExternalBeylaIdPath);
            return idInDCIM;
        }

        return getPatchId(key);
    }

    private static String getPatchId(String key) {
        String id;

        String compatibleExtPath  = new File(Environment.getExternalStorageDirectory(), BEYLA_EXTERNAL_PATH + File.separator + getCompatibleConfigName()).getAbsolutePath();
        id = getIdFromFile(key, compatibleExtPath);
        if (TextUtils.isEmpty(id)) {
            String compatibleDCIMPath  = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), getCompatibleConfigName()).getAbsolutePath();
            id = getIdFromFile(key, compatibleDCIMPath);
        }
        if (TextUtils.isEmpty(id)) {
            Logger.d(TAG, "there is not " + key + " in patch!");
            return null;
        }

        putIdToPref(key, id);
        putIdToFile(key, id, mExternalBeylaIdPath);
        putIdToFile(key, id, mDCIMBeylaIdPath);
        Logger.v(TAG, "get " + key + " from patch, id:" + id);
        return id;
    }

    private static String getIdFromPref(String key) {
        if (isNoPermission())
            return "";
        Settings settings = new Settings(ObjectStore.getContext(),"beyla_settings");
        return settings.get(key);
    }

    private static void putIdToPref(String key, String id) {
        Settings settings = new Settings(ObjectStore.getContext(),"beyla_settings");
        settings.set(key, id);
    }

    private static String getIdFromFile(String key, String filePath) {
        if (isNoPermission())
            return "";
        if (filePath == null) {
            Logger.d(TAG, "getIdFromFile filepath is empty");
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.d(TAG, "getIdFromFile file is not exist");
            return null;
        }

        try {
            Properties prop = getProperties(file);
            String id = prop.getProperty(key);
            if (TextUtils.isEmpty(id)) {
                Logger.d(TAG, "getIdFromFile id is empty!");
                return null;
            }
            return id;
        } catch (Throwable e) {
            Logger.w(TAG, "getIdFromFile failed, file path:" + filePath, e);
        }
        return null;
    }

    private static void putIdToFile(String key, String id, String filePath) {
        if (isNoPermission())
            return;
        Assert.notNull(id);
        if (filePath == null) {
            Logger.d(TAG, "putIdToFile filepath is empty");
            return;
        }

        OutputStream os = null;
        try {
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                Logger.d(TAG, "putIdToFile file is not exist");
                file.getParentFile().mkdirs();
                if (file.isDirectory())
                    file.delete();
                file.createNewFile();
            }

            Properties prop = getProperties(file);
            prop.put(key, id);
            os = new FileOutputStream(filePath);
            prop.store(os, "beyla_ids");
        } catch (Throwable e) {
            Logger.w(TAG, "putIdToFile failed, file path:" + filePath, e);
        } finally {
            Utils.close(os);
        }
    }

    private static Properties getProperties(File file) {
        FileInputStream in = null;
        try {
            Properties prop = new Properties();
            in = new FileInputStream(file);
            prop.load(in);
            return prop;
        } catch (Throwable e) {
            Logger.w(TAG, "getProperty failed, file path:" + file.getAbsolutePath(), e);
        } finally {
            Utils.close(in);
        }
        return new Properties();
    }

    //!!! can not change the add id sequence!!!
    private static String createNDId() {
        List<String> ids = new ArrayList<String>();
        String id = DeviceHelper.getMacAddress(ObjectStore.getContext());
        if (!TextUtils.isEmpty(id) && !DeviceHelper.isBadMacId(id))
            ids.add(id);
        id = DeviceHelper.getIMEI(ObjectStore.getContext());
        if (!TextUtils.isEmpty(id))
            ids.add(id);
        id = DeviceHelper.getAndroidID(ObjectStore.getContext());
        if (!TextUtils.isEmpty(id) && !DeviceHelper.isBadAndroid(id))
            ids.add(id);

        if (ids.size() < 2)
            return getBeylaId();

        String src = "";
        for (String i : ids)
            src = src.concat(i);
        return HashUtils.hash(src);
    }

    private static boolean isNoPermission() {
        return !PermissionsUtils.hasStoragePermission(ObjectStore.getContext());
    }

    /**
     * long long ago, india pepole share one SD card with many phones, beyla id is same in some devices.
     * we resolve this to named beyla config file with android id.
     * NOW, this case is less.
     * we should share beyla id in all family app.
     */
    private static String getCompatibleConfigName() {
        String configName = DeviceHelper.getAndroidID(ObjectStore.getContext());
        if (TextUtils.isEmpty(configName))
            configName = DeviceHelper.getBuildSN();
        return "." + (TextUtils.isEmpty(configName) ? "beyla" : configName) + ".cfg";
    }
}