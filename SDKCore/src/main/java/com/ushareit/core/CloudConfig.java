package com.ushareit.core;

import android.content.Context;

import java.util.Map;

public class CloudConfig {

    /*
     * Check whether a special configuration exist in local configurations cache.
     */
    public static boolean hasConfig(Context context, String key) {
        return getImpl().hasConfig(context, key);
    }

    /*
     * get string type configuration from local configurations cache.
     */
    public static String getStringConfig(Context context, String key) {
        return getStringConfig(context, key, "");
    }

    /*
     * get string type configuration with default value from local configurations cache.
     */
    public static String getStringConfig(Context context, String key, String defaultValue) {
        return getImpl().getStringConfig(context, key, defaultValue);
    }

    /*
     * get integer type configuration from local configurations cache.
     */
    public static int getIntConfig(Context context, String key, int defaultValue) {
        return getImpl().getIntConfig(context, key, defaultValue);
    }

    /*
     * get long type configuration from local configurations cache.
     */
    public static long getLongConfig(Context context, String key, long defaultValue) {
        return getImpl().getLongConfig(context, key, defaultValue);
    }

    /*
     * get boolean type configuration from local configurations cache.
     */
    public static boolean getBooleanConfig(Context context, String key, boolean defaultValue) {
        return getImpl().getBooleanConfig(context, key, defaultValue);
    }

    public static void addListener(String bizKey, IConfigListener listener) {
        getImpl().addListener(bizKey, listener);
    }

    public static void removeListener(String bizKey) {
        getImpl().removeListener(bizKey);
    }

    public static void setImpl(ICloudConfig impl){
        mCloudConfigImpl = impl;
    }

    private static ICloudConfig getImpl(){
        if(mCloudConfigImpl == null){
            mCloudConfigImpl = new CloudConfigEmptyImpl();
        }
        return mCloudConfigImpl;
    }

    private volatile static ICloudConfig mCloudConfigImpl;

    /**
     * CloudConfig的Api接口
     */
    public interface ICloudConfig {

        boolean hasConfig(Context context, String key);

        String getStringConfig(Context context, String key, String defaultValue);

        int getIntConfig(Context context, String key, int defaultValue);

        long getLongConfig(Context context, String key, long defaultValue);

        boolean getBooleanConfig(Context context, String key, boolean defaultValue);

        void addListener(String bizKey, IConfigListener listener);

        void removeListener(String bizKey);
    }

    /**
     * ICloudConfigListener的默认空实现
     */
    static class CloudConfigEmptyImpl implements ICloudConfig {

        @Override
        public boolean hasConfig(Context context, String key) {
            return false;
        }

        @Override
        public String getStringConfig(Context context, String key, String defaultValue) {
            return defaultValue;
        }

        @Override
        public int getIntConfig(Context context, String key, int defaultValue) {
            return defaultValue;
        }

        @Override
        public long getLongConfig(Context context, String key, long defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getBooleanConfig(Context context, String key, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public void addListener(String bizKey, IConfigListener listener) {}

        @Override
        public void removeListener(String bizKey) {}
    }

    public interface IConfigListener {
        void onConfigUpdated(String biz, Map<String, Object> keyValueMap);
    }
}