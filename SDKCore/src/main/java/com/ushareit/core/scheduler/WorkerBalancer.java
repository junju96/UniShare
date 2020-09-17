package com.ushareit.core.scheduler;

import android.content.Context;

import com.ushareit.core.Settings;

public class WorkerBalancer {
    private static String NAME = "background_worker";

    public static boolean canWork(Context context, String identifier, long interval) {
        long currentTime = System.currentTimeMillis();
        long lastTime = new Settings(context.getApplicationContext(), NAME).getLong(identifier, Long.MIN_VALUE);
        return (lastTime == Long.MIN_VALUE || Math.abs(currentTime - lastTime) > interval);
    }

    public static void reportResult(Context context, String identifier){
        new Settings(context.getApplicationContext(), NAME).setLong(identifier, System.currentTimeMillis());
    }
}
