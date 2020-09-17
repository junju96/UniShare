package com.ushareit.core.utils;

import com.ushareit.core.Settings;
import com.ushareit.core.lang.ObjectStore;

public class FirstStartupUtils {

    public static final String KEY_FIRST_START_TIME = "FIRST_STARTUP_TIME";
    public static final String KEY_START_COUNT = "STARTUP_COUNT";

    private static Settings sSettings = null;
    private static Settings getSettings() {
        if (sSettings == null)
            sSettings = new Settings(ObjectStore.getContext());
        return sSettings;
    }

    public static long getFirstStartupTime() {
        return getSettings().getLong(KEY_FIRST_START_TIME, 0);
    }

    private static boolean setFirstStartupTime() {
        return getSettings().setLong(KEY_FIRST_START_TIME, System.currentTimeMillis());
    }

    public static int getStartupCount(){
        return getSettings().getInt(KEY_START_COUNT, 0);
    }

    private static boolean setStartupCount(int value){
        return getSettings().setInt(KEY_START_COUNT, value);
    }

    private static int increaseStartupCount(){
        int anInt = getStartupCount();
        anInt += 1;
        setStartupCount(anInt);
        return anInt;
    }

    public static boolean isAppFirstStart(){
        return getStartupCount() <= 1;
    }

    public static void increaseStartup(){
        if (getFirstStartupTime() == 0)
            setFirstStartupTime();
        increaseStartupCount();
    }

}
