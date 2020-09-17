package com.ushareit.core.utils.device;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.i18n.LocaleUtils;

import org.json.JSONObject;

import java.io.File;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeviceHelper {
    private static final String TAG = "DEVICEHelper";

    private static final String KEY_DEVICE_ID = "DEVICE_ID";

    public static enum IDType {
        IMEI('i'), SOC('s'), MAC('m'), UUID('u'), ANDROID('a'), BUILD('b'), UNKNOWN('u');

        private char mTag;

        private final static Map<Character, IDType> VALUES = new HashMap<Character, IDType>();
        static {
            for (IDType item : IDType.values())
                VALUES.put(item.mTag, item);
        }

        public static IDType fromChar(char value) {
            IDType type = VALUES.get(value);
            return (type == null) ? IDType.UNKNOWN : type;
        }

        private IDType(char tag) {
            mTag = tag;
        }

        public char getTag() {
            return mTag;
        }

        public String getName() {
            switch (this) {
                case IMEI:
                    return "imei";
                case SOC:
                    return "soc";
                case MAC:
                    return "mac";
                case UUID:
                    return "uuid";
                case ANDROID:
                    return "android_id";
                case BUILD:
                    return "build";
                default:
                    return "unknown";
            }
        }
    }

    private static String mDeviceId;
    public static String getDeviceId(Context ctx) {
        if (TextUtils.isEmpty(mDeviceId)) {
            mDeviceId = getOrCreateDeviceId(ctx);
        }
        return mDeviceId;
    }

    public static String getOrCreateDeviceId(Context ctx) {
        // try use cached device id
        Settings settings = new Settings(ctx);
        String id = settings.get(KEY_DEVICE_ID);

        if (!TextUtils.isEmpty(id) && !isBadMacId(id) && !isBadAndroid(id))
            return id;

        IDType type = IDType.MAC;
        try {
            id = getMacAddress(ctx);
            if (TextUtils.isEmpty(id)) {
                type = IDType.ANDROID;
                id = getAndroidID(ctx);

                if (isBadAndroid(id))
                    id = null;
            }
            if (TextUtils.isEmpty(id)) {
                type = IDType.UUID;
                id = getUUID();
            }
        } catch (Exception e) {
            Logger.w("Helper", "can't get real device id, generate one by random instead");
            type = IDType.UUID;
            id = getUUID();
        }
        id = type.getTag() + "." + id;
        // save id and return
        settings.set(KEY_DEVICE_ID, id);
        return id;
    }
    
    private static String mMacAddress = null;
    public static String getMacAddress(Context context) {
        if (!TextUtils.isEmpty(mMacAddress))
            return mMacAddress;
        mMacAddress = DeviceSettings.getMacAddress();
        if (!TextUtils.isEmpty(mMacAddress))
            return mMacAddress;

        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null)
            return null;

        String id = wifiInfo.getMacAddress();
        if (!TextUtils.isEmpty(id))
            id = id.replace(":", "");

        // some little AndroidM devices get bad address "02:00:00:00:00:00" cause by privacy.
        if (!TextUtils.isEmpty(id) && isBadMacId(IDType.MAC.getTag() + "." + id)) {
            id = getMacAddressByNetInterface();
            if (!TextUtils.isEmpty(id))
                id = id.replace(":", "");
        }

        mMacAddress = id;
        if (!TextUtils.isEmpty(mMacAddress))
            DeviceSettings.setMacAddress(mMacAddress);
        return mMacAddress;
    }

    private static String mAndroidId = null;
    public static String getAndroidID(Context context) {
        if (!TextUtils.isEmpty(mAndroidId))
            return mAndroidId;
        mAndroidId = DeviceSettings.getAndroidId();
        if (!TextUtils.isEmpty(mAndroidId))
            return mAndroidId;

        String id = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(id.trim()))
            return null;

        mAndroidId = id;
        if (!TextUtils.isEmpty(mAndroidId))
            DeviceSettings.setAndroidId(mAndroidId);
        return mAndroidId;
    }

    private static String mImei = null;
    public static String getIMEI(Context context) {
        if (!TextUtils.isEmpty(mImei))
            return mImei;
        mImei = DeviceSettings.getIMEI();
        if (!TextUtils.isEmpty(mImei))
            return mImei;

        IMSUtils.IMSInfo info = IMSUtils.getIMSInfo(context);
        if (info == null || !info.isAvailable())
            return null;
        mImei = info.getBetterIMEI();
        if (!TextUtils.isEmpty(mImei))
            DeviceSettings.setIMEI(mImei);
        return mImei;
    }

    private static IMSUtils.IMSInfo mImsInfo = null;
    public static int supportSimCount(Context context) {
        if (mImsInfo == null)
            mImsInfo = IMSUtils.getIMSInfo(context);
        if (mImsInfo == null) {
            Logger.d(TAG, "load ims info failed!");
            return -2;
        }
        return mImsInfo.mSimType == IMSUtils.SimType.DUAL_SIM ? 2 : (mImsInfo.mSimType == IMSUtils.SimType.SINGLE_SIM ? 1 : (mImsInfo.mSimType == IMSUtils.SimType.NO_SIM ? 0 : -1));
    }

    public static int activeSimCount(Context context) {
        if (mImsInfo == null)
            mImsInfo = IMSUtils.getIMSInfo(context);
        if (mImsInfo == null) {
            Logger.d(TAG, "load ims info failed!");
            return -2;
        }
        return mImsInfo.mActiveState == IMSUtils.ActiveState.DOUBLE_ACTIVE ? 2 : (mImsInfo.mActiveState == IMSUtils.ActiveState.SINGLE_ACTIVE ? 1 : (mImsInfo.mActiveState == IMSUtils.ActiveState.NO_ACTIVE ? 0 : -1));
    }

    public static List<String> getIMSIs(Context context) {
        if (mImsInfo == null)
            mImsInfo = IMSUtils.getIMSInfo(context);
        if (mImsInfo == null) {
            Logger.d(TAG, "load ims info failed!");
            return new ArrayList<String>();
        }
        return mImsInfo.getIMSIList();
    }

    public static String getIMSI(Context context) {
        List<String> imsiList = getIMSIs(context);
        if (imsiList != null && !imsiList.isEmpty()) {
            return imsiList.get(0);
        }
        return "";
    }

    private static String mStorageCID = null;
    public static String getStorageCID() {
        if (!TextUtils.isEmpty(mStorageCID))
            return mStorageCID;
        mStorageCID = DeviceSettings.getStorageCID();
        if (!TextUtils.isEmpty(mStorageCID))
            return mStorageCID;

        File file = getCIDSerialFile();
        if (file == null)
            return null;

        java.io.FileInputStream fis = null;
        try {
            fis = new java.io.FileInputStream(file);
            byte[] buffer = new byte[128];
            int length = fis.read(buffer, 0, 128);
            String sn = new String(buffer, 0, length);
            if (sn.length() >= 32 && !sn.contains("00000000000000000000")) {
                char[] arr = LocaleUtils.toUpperCaseIgnoreLocale(sn.trim()).toCharArray();
                StringBuilder sb = new StringBuilder();
                sb.append(arr, 0, 6);
                sb.append(arr, 16, 10);
                mStorageCID = sb.toString();
                if (!TextUtils.isEmpty(mStorageCID))
                    DeviceSettings.setStorageCID(mStorageCID);
                return mStorageCID;
            }
        } catch (Exception e) {} finally {
            Utils.close(fis);
        }
        return null;
    }

    private static String mBuildSN = null;
    public static String getBuildSN() {
        if (!TextUtils.isEmpty(mBuildSN))
            return mBuildSN;
        mBuildSN = DeviceSettings.getBuildSN();
        if (!TextUtils.isEmpty(mBuildSN))
            return mBuildSN;

        Class<Build> c = Build.class;
        try {
            java.lang.reflect.Field f = c.getDeclaredField("SERIAL");
            mBuildSN = (String)f.get(c);
            if (!TextUtils.isEmpty(mBuildSN))
                DeviceSettings.setBuildSN(mBuildSN);
            return mBuildSN;
        } catch (Exception e) {}
        return null;
    }

    public static String getUUID() {
        long r = (long)(Math.random() * Long.MAX_VALUE);
        return new UUID(r, Build.FINGERPRINT.hashCode()).toString();
    }

    public static IDType parseIDType(String deviceId) {
        if (TextUtils.isEmpty(deviceId) || deviceId.indexOf(".") != 1)
            return IDType.UNKNOWN;

        char value = deviceId.charAt(0);
        return IDType.fromChar(value);
    }

    public static boolean isBadMacId(String id) {
        if (TextUtils.isEmpty(id))
            return false;
        return (IDType.MAC.getTag() + "." + "020000000000").equals(id);
    }

    public static boolean isBadAndroid(String id) {
        if (TextUtils.isEmpty(id))
            return false;
        return (IDType.ANDROID.getTag() + "." + "9774d56d682e549c").equalsIgnoreCase(id);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static String getMacAddressByNetInterface() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
            return "";
        try {
            Enumeration<NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();
            if (enu == null)
                return "";

            while (enu.hasMoreElements()) {
                NetworkInterface networkInterface = enu.nextElement();
                String name = networkInterface.getName();
                if (TextUtils.isEmpty(name) || !LocaleUtils.toLowerCaseIgnoreLocale(name).contains("wlan"))
                    continue;

                byte[] addr = networkInterface.getHardwareAddress();
                StringBuilder buf = new StringBuilder();
                for (byte b : addr)
                    buf.append(String.format("%02X:", b));
                if (buf.length() > 0)
                    buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Throwable e) {}

        return "";
    }

    private static String SOC_HOST = "mmc_host";
    private static String SOC_SERIAL_PATH = "/mmc0/mmc0:0001/cid";

    private static File findCIDSerialFile(File dir) {
        if (dir.getName().equals(SOC_HOST)) {
            File f = new File(dir.getAbsolutePath() + SOC_SERIAL_PATH);
            if (f.exists() && f.canRead())
                return f;
        }
        return null;
    }

    private static File getCIDSerialFile() {
        File[] dirs = new File("/sys/devices").listFiles();
        if (dirs == null)
            return null;

        for (File dir : dirs) {
            if (dir.isFile())
                continue;
            File ret = findCIDSerialFile(dir);
            if (ret != null)
                return ret;

            File[] ds1 = dir.listFiles();
            if (ds1 == null)
                continue;

            for (File d1 : ds1) {
                if (d1.isFile())
                    continue;
                if ((ret = findCIDSerialFile(d1)) != null)
                    return ret;

                File[] ds2 = d1.listFiles();
                if (ds2 == null)
                    continue;

                for (File d2 : ds2)
                    if ((ret = findCIDSerialFile(d2)) != null)
                        return ret;
            }
        }
        return null;
    }

    public static JSONObject serializeDeviceInfo(Context context) {
        JSONObject json = new JSONObject();
        try {
            json.put(IDType.MAC.getName(), getMacAddress(context));
            json.put(IDType.IMEI.getName(), getIMEI(context));
            json.put(IDType.ANDROID.getName(), getAndroidID(context));
            json.put(IDType.BUILD.getName(), getBuildSN());
            json.put(IDType.SOC.getName(), getStorageCID());
        } catch (Exception e) {}
        return json;
    }

    public static String getDeviceInfo(String deviceInfo, IDType idType) {
        if (TextUtils.isEmpty(deviceInfo))
            return null;
        try {
            JSONObject jsonObject = new JSONObject(deviceInfo);
            return jsonObject.optString(idType.getName());
        } catch (Exception e) {
        }
        return null;
    }

    /*
    this method can't be executed in main thread
     */
    private static String GAID = null;
    public static String getGAID (Context context) {
        if (!TextUtils.isEmpty(GAID))
            return GAID;

        try {
            GAID = AdvertisingIdClient.getAdvertisingIdInfo(context).getId();
            Logger.v("GAID", "the google adversting id: " + GAID);
        } catch (Throwable e) {}
        return GAID;
    }

    static class DeviceSettings extends Settings {
        static final String KEY_MAC_ADDRESS_ID = "mac_address";
        static final String KEY_ANDROID_ID = "android_id";
        static final String KEY_IMEI = "imei";
        static final String KEY_STORAGE_CID = "storage_cid";
        static final String KEY_BUILD_SN = "build_sn";

        public DeviceSettings(Context ctx) {
            super(ctx, "device_settings");
        }

        static void setMacAddress(String macAddress) {
            new DeviceSettings(ObjectStore.getContext()).set(KEY_MAC_ADDRESS_ID, macAddress);
        }

        static String getMacAddress() {
            return new DeviceSettings(ObjectStore.getContext()).get(KEY_MAC_ADDRESS_ID);
        }

        static void setAndroidId(String androidId) {
            new DeviceSettings(ObjectStore.getContext()).set(KEY_ANDROID_ID, androidId);
        }

        static String getAndroidId() {
            return new DeviceSettings(ObjectStore.getContext()).get(KEY_ANDROID_ID);
        }

        static void setIMEI(String imei) {
            new DeviceSettings(ObjectStore.getContext()).set(KEY_IMEI, imei);
        }

        public static String getIMEI() {
            return new DeviceSettings(ObjectStore.getContext()).get(KEY_IMEI);
        }

        static void setStorageCID(String cid) {
            new DeviceSettings(ObjectStore.getContext()).set(KEY_STORAGE_CID, cid);
        }

        public static String getStorageCID() {
            return new DeviceSettings(ObjectStore.getContext()).get(KEY_STORAGE_CID);
        }

        static void setBuildSN(String sn) {
            new DeviceSettings(ObjectStore.getContext()).set(KEY_BUILD_SN, sn);
        }

        public static String getBuildSN() {
            return new DeviceSettings(ObjectStore.getContext()).get(KEY_BUILD_SN);
        }
    }

}
