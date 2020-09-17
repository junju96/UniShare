package com.ushareit.core.utils.device;

import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.ushareit.core.os.AndroidHelper;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.lang.ObjectStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DevBrandUtils {

    public static class MIUI {
        public static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
        public static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
        public static final String KEY_MIUI_VERSION_INCREMENTAL = "ro.build.version.incremental"; // V6.5.3.0.KXECNCD 带字母组合的是稳定版; 6.8.3 如果是这种日期（意思是2016.8.3），再判断一下mod_devices
        public static final String KEY_MIUI_MOD_DEVICE = "ro.product.mod_device";  //virgo_alpha alpha是体验版，dev是开发版

        private static Boolean mIsMIUI;

        public static synchronized boolean isMIUI() {
            if (mIsMIUI == null)
                mIsMIUI = isPropertyExist(KEY_MIUI_VERSION_CODE);
            return mIsMIUI;
        }

        public static synchronized String getMIUICode(){
            return getProperty(KEY_MIUI_VERSION_CODE);
        }
        public static synchronized String getMIUIVersion() {
            String miVersion = getProperty(KEY_MIUI_VERSION_NAME);
            return TextUtils.isEmpty(miVersion) ? "" : miVersion;
        }

        public static synchronized String getMIUIVersionValue() {
            String version = getProperty(KEY_MIUI_VERSION_INCREMENTAL);
            return TextUtils.isEmpty(version) ? "" : version;
        }
        public static synchronized boolean isWLANAssistantOn() {
            try {
                return Settings.System.getInt(ObjectStore.getContext().getContentResolver(), "wifi_assistant", 1) == 1;
            } catch (Exception e) {
            }
            return false;
        }

        public static boolean isFullScreen() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    return Settings.Global.getInt(ObjectStore.getContext().getContentResolver(), "force_fsg_nav_bar", 0) != 0;
                }
            } catch (Exception e) {

            }
            return false;
        }
    }

    public static class EMUIUtils {
        public static final String KEY_EMUI_VERSION_CODE = "ro.build.version.emui";

        private static Boolean mIsEMUI;
        public static boolean isEMUI() {
            if (mIsEMUI == null)
                mIsEMUI = isPropertyExist(KEY_EMUI_VERSION_CODE);
            return mIsEMUI;
        }

        public static boolean isEMUI5() {
            try {
                return isEMUI() && (Build.VERSION.SDK_INT == AndroidHelper.ANDROID_VERSION_CODE.NOUGAT);
            } catch (Exception e) {}
            return false;
        }

    }

    public static class FlymeUtils {
        public static final String KEY_FLYME_VERSION_NAME = "ro.build.display.id";
        private static Boolean mIsFlyme;
        public static boolean isFlyme() {
            if (mIsFlyme != null)
                return mIsFlyme;

            String meizuFlymeOSFlag = getProperty(KEY_FLYME_VERSION_NAME);
            if (TextUtils.isEmpty(meizuFlymeOSFlag)){
                mIsFlyme = false;
            }else if (meizuFlymeOSFlag.contains("flyme") || meizuFlymeOSFlag.toLowerCase().contains("flyme")){
                mIsFlyme =  true;
            }else {
                mIsFlyme = false;
            }
            return mIsFlyme;
        }
    }

    private static synchronized boolean isPropertyExist(String prop) {
        return !TextUtils.isEmpty(getProperty(prop));
    }

    private static synchronized String getProperty(String prop) {
        String line = "";
        BufferedReader reader = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + prop);
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = reader.readLine();
        } catch (Throwable e) {} finally {
            Utils.close(reader);
        }

        return line;
    }
}
