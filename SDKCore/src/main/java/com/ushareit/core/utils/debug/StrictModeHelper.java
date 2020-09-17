package com.ushareit.core.utils.debug;

import android.os.Build;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;

import com.ushareit.core.Logger;

public final class StrictModeHelper {
    private static final boolean ENABLE = false;

    public static void enableStrictMode() {
        if (Logger.isDebugVersion) {
            StrictModeHelper.enableStrictModeIfNeeded();
            StrictModeHelper.enableThreadPolicyIfNeeded();
            StrictModeHelper.enableVmPolicyIfNeeded();
        }
    }

    public static void enableStrictModeIfNeeded() {
        if (Build.VERSION.SDK_INT < 11)
            return;
        if (!ENABLE)
            return;

        ThreadPolicy threadPolicy = 
                new ThreadPolicy.Builder().detectAll().permitDiskReads().permitDiskWrites().penaltyLog().penaltyDeath().build();

        VmPolicy vmPolicy  = 
                new VmPolicy.Builder().penaltyLog().penaltyDeath().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().build();
        StrictMode.setThreadPolicy(threadPolicy);
        StrictMode.setVmPolicy(vmPolicy);
    }

    public static void enableThreadPolicyIfNeeded() {
        if (Build.VERSION.SDK_INT < 11)
            return;
        if (!ENABLE)
            return;

        ThreadPolicy threadPolicy = 
                new ThreadPolicy.Builder().detectAll().permitDiskReads().permitDiskWrites().penaltyLog().penaltyDeath().build();
        setThreadPolicyIfNeeded(threadPolicy);
    }

    public static void enableVmPolicyIfNeeded() {
        if (Build.VERSION.SDK_INT < 11)
            return;
        if (!ENABLE)
            return;
        
        VmPolicy vmPolicy  = 
                new VmPolicy.Builder().penaltyLog().penaltyDeath().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().build();
        StrictMode.setVmPolicy(vmPolicy);
    }

    public static ThreadPolicy getCurrentThreadPolicy() {
        return StrictMode.getThreadPolicy();
    }

    public static void setThreadPolicyIfNeeded(ThreadPolicy threadPolicy) {
        if (Build.VERSION.SDK_INT < 11)
            return;
        if (!ENABLE)
            return;

        StrictMode.setThreadPolicy(threadPolicy);
    }

    public static void setThreadPolicyPermitAllIfNeeded() {
        if (Build.VERSION.SDK_INT < 11)
            return;
        if (!ENABLE)
            return;
        
        ThreadPolicy threadPolicy = new ThreadPolicy.Builder().permitAll().build();
        setThreadPolicyIfNeeded(threadPolicy);
    }

    private StrictModeHelper() {}
}
