package com.ushareit.core.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * 系统工具类
 */
public class SystemUtils {

    //启用组建
    public static void enableComponent(Context context, Class<?> cls){
        ComponentName componentName = new ComponentName(context, cls);
        enableComponent(context, componentName);
    }

    //禁用组建
    public static void disableComponent(Context context, Class<?> cls){
        ComponentName componentName = new ComponentName(context, cls);
        disableComponent(context, componentName);
    }

    public static void enableComponent(Context context, ComponentName componentName){
        setComponentEnabledSetting(context, componentName, true, false);
    }

    public static void disableComponent(Context context, ComponentName componentName){
        setComponentEnabledSetting(context, componentName, false, false);
    }

    public static void enableComponentAndExit(Context context, ComponentName componentName){
        setComponentEnabledSetting(context, componentName, true, true);
    }

    public static void disableComponentAndExit(Context context, ComponentName componentName){
        setComponentEnabledSetting(context, componentName, false, true);
    }

    public static void setComponentEnabledSetting(Context context, ComponentName componentName, boolean isEnable, boolean isKill){
        try {
            PackageManager pm = context.getPackageManager();
            int newState = isEnable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            int flags = isKill ? 0 : PackageManager.DONT_KILL_APP;
            pm.setComponentEnabledSetting(componentName, newState, flags);
        }catch (Throwable throwable){}
    }
}
