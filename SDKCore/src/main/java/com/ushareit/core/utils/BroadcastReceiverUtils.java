package com.ushareit.core.utils;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ushareit.core.Logger;
import com.ushareit.core.algo.DES;
import com.ushareit.core.lang.StringUtils;

// TODO: SW move to app sub package???
public class BroadcastReceiverUtils {
    private static final String TAG = "BroadcastReceiverUtils";

    /**
     * 发送退出本App的广播
     * @param context
     * @param intent 发送的intent
     */
    public static void sendExitSelfLocalBroadcast(Context context, Intent intent) {
        sendExitSelfLocalBroadcast(context, false, intent);
    }

    /**
     * 发送退出本App的广播
     * @param context
     * @param isSync
     * @param intent 发送的intent
     */
    public static void sendExitSelfLocalBroadcast(Context context, boolean isSync, Intent intent) {
        if(intent==null){
            return;
        }
        LocalBroadcastManager lb = LocalBroadcastManager.getInstance(context);
        if (isSync)
            lb.sendBroadcastSync(intent);
        else
            lb.sendBroadcast(intent);
    }

    /**
     * 发送退出本App的广播
     * @param context
     * @param intent 接收的intent
     * @param exitIntent 发送退出广播的intent
     */
    public static void exitSelfBroadcastReceived(Context context, Intent intent, Intent exitIntent) {
        String key = intent.getStringExtra("id");
        if (StringUtils.isEmpty(key))
            return;
        // elapsed less than 10 second
        long decodeKey = Long.parseLong(decrypt(key));
        long current = SystemClock.elapsedRealtime();
        if ((decodeKey == 0) || (current <= decodeKey) || (current - decodeKey) > 10 * 1000)
            return;
        // self not in background
        RunningAppProcessInfo ap = getRunningAppProcessInfo(context, context.getPackageName());
        if (ap == null || ap.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
            return;

        sendExitSelfLocalBroadcast(context, exitIntent);
    }

    private static String decrypt(String src) {
        String code = "";
        try {
            code = DES.getInstance().decrypt(src);
        } catch (Exception e) {
            Logger.d(TAG, "exitSelf, " + e.toString());
        }
        return code;
    }

    private static String encrypt(String src) {
        String code = "";
        try {
            code = DES.getInstance().encrypt(src);
        } catch (Exception e) {
            Logger.d(TAG, "exitSelf, " + e.toString());
        }
        return code;
    }

    // BACKGROUND=400 EMPTY=500 FOREGROUND=100 GONE=1000 PERCEPTIBLE=130 SERVICE=300 ISIBLE=200
    private static RunningAppProcessInfo getRunningAppProcessInfo(Context context, String packageName) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        for (RunningAppProcessInfo ap : processes) {
            if (ap.processName.equals(packageName)) {
                Logger.i(TAG, "exitSelf, importance: " + ap.importance + ", name: " + packageName);
                return ap;
            }
        }
        return null;
    }
}
