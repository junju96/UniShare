package com.ushareit.core.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

public class ScreenUtils {

    /**
     * 判断是否锁屏
     */
    public static boolean isKeyguardLocked(Context context) {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= 16) {
            return mKeyguardManager.isKeyguardLocked();
        } else {
            return mKeyguardManager.inKeyguardRestrictedInputMode();
        }
    }

    /**
     * 判断是否熄屏
     */
    public static boolean isScreenOff(Context context) {
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return pm != null && !pm.isScreenOn();
    }
}
