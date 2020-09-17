package com.ushareit.core.os;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.lang.ObjectStore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

public class AndroidHelper {
    public static boolean isOverICS() {
        return Build.VERSION.SDK_INT >= ANDROID_VERSION_CODE.ICE_CREAM_SANDWICH;
    }

    /**
     * 系统为Android3.0
     *
     * @return
     */
    public static boolean isHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * 系统为Android4.1及以上
     *
     * @return
     */
    public static boolean isJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * 系统为Android4.4,增加沉浸模式
     *
     * @return
     */
    public static boolean isKitkat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * 系统为Android5.0
     *
     * @return
     */
    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * 系统为Android5.1
     *
     * @return
     */
    public static boolean isLollipopMr1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * 系统为Android6.0
     *
     * @return
     */
    public static boolean isM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 系统为Android7.0
     *
     * @return
     */
    public static boolean isN() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * 系统为Android8.0
     *
     * @return
     */
    public static boolean isO() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * 判断是否为平板设备
     *
     * @return
     */
    public static boolean isTablet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return ObjectStore.getContext().getResources().getConfiguration().smallestScreenWidthDp >= 600;
        }
        return false;
    }

    public interface ANDROID_VERSION_CODE {
        int P = 28;                 // Android 9.0
        int O_MR1 = 27;             // Android 8.1
        int OREO = 26;              // Android 8.0
        int NOUGAT_MR1 = 25;        // Android 7.1
        int NOUGAT = 24;             // Android 7.0
        int MARSHMALLOW = 23;       // Android 6.0
        int LOLLIPOP_MR1 = 22;      // Android 5.1
        int LOLLIPOP = 21;          // Android 5.0
        int KITKAT = 19;            // Android 4.4.X
        int JELLY_BEAN_MR1 = 17;    // Android 4.2
        int JELLY_BEAN = 16;    // Android 4.1, 4.1.1
        int ICE_CREAM_SANDWICH_MR1 = 15;    // Android 4.0.3, 4.0.4
        int ICE_CREAM_SANDWICH = 14;    // Android 4.0, 4.0.1, 4.0.2
        int HONEYCOMB_MR2 = 13; // Android 3.2
        int HONEYCOMB_MR1 = 12; // Android 3.1.x
        int HONEYCOMB = 11; // Android 3.0.x
        int GINGERBREAD_MR1 = 10;   // Android 2.3.4, Android 2.3.3
        int GINGERBREAD = 9;    // Android 2.3.2, Android 2.3.1, Android 2.3
        int FROYO = 8;  // Android 2.2.x
        int ECLAIR_MR1 = 7; // Android 2.1.x
        int ECLAIR_0_1 = 6; // Android 2.0.1
        int ECLAIR = 5; // Android 2.0
        int DONUT = 4;  // Android 1.6
        int CUPCAKE = 3;    // Android 1.5
        int BASE_1_1 = 2;   // Android 1.1
        int BASE = 1;   // Android 1.0
    }

    public static class CompatibilityP {

        public static final String DEFAULT_INCOMPATIBLE_WEBVIEW_DEVICE = "realme 2 Pro,RMX1801,CPH1911,CPH1969,G965F,N960F,G950F";//,CPH1969,CPH1911,RMX1807,CPH1823,CPH1859,rmx1851,vivo 1804";

        //todo: Adapt andrid p webview bug
        public static boolean isIncompatibleWebViewDevice() {
            try {
                String models = CloudConfig.getStringConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_USE_LOCAL_HYBRID_MODELS, DEFAULT_INCOMPATIBLE_WEBVIEW_DEVICE);
                String[] localModelList = models.split(",");
                for (String model : localModelList) {
                    if (!TextUtils.isEmpty(model) && model.equalsIgnoreCase(Build.MODEL)) {
                        return true;
                    }
                }
            } catch (Exception e) {
            }
            return false;
        }

        //todo: Adapt andrid p webview bug
        public static String setWebViewDir(Context context, String mProcessName) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    if (!TextUtils.isEmpty(mProcessName) && mProcessName.equals(context.getPackageName())) {
                        WebView.setDataDirectorySuffix("main");
                        return "main";
                    } else {
                        String webViewDirSuffix = mProcessName;
                        if (!TextUtils.isEmpty(mProcessName)) {
                            int index = mProcessName.lastIndexOf(":");
                            if (index > 0 && index < mProcessName.length() - 1) {
                                webViewDirSuffix = mProcessName.substring(index + 1);
                            }
                        } else {
                            webViewDirSuffix = getRandomString();
                        }
                        WebView.setDataDirectorySuffix(webViewDirSuffix);
                        return webViewDirSuffix;
                    }
                } catch (Exception e) {

                }
            }
            return null;
        }

        private static String getRandomString() {
            String webViewDirSuffix;
            int leftLimit = 97;
            int rightLimit = 122;
            int targetStringLength = 10;
            Random random = new Random();
            StringBuilder buffer = new StringBuilder(targetStringLength);
            for (int i = 0; i < targetStringLength; i++) {
                int randomLimitedInt = leftLimit + (int)
                        (random.nextFloat() * (rightLimit - leftLimit + 1));
                buffer.append((char) randomLimitedInt);
            }
            webViewDirSuffix = buffer.toString();
            return webViewDirSuffix;
        }

        public static void closeAndroidPDialog() {
            if (Build.VERSION.SDK_INT >= 28) {//Build.VERSION_CODES.P) {
                try {
                    Class aClass = Class.forName("android.content.pm.PackageParser$Package");
                    Constructor declaredConstructor = aClass.getDeclaredConstructor(String.class);
                    declaredConstructor.setAccessible(true);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                try {
                    Class cls = Class.forName("android.app.ActivityThread");
                    Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
                    declaredMethod.setAccessible(true);
                    Object activityThread = declaredMethod.invoke(null);
                    Field mHiddenApiWarningShown = cls.getDeclaredField("mHiddenApiWarningShown");
                    mHiddenApiWarningShown.setAccessible(true);
                    mHiddenApiWarningShown.setBoolean(activityThread, true);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
