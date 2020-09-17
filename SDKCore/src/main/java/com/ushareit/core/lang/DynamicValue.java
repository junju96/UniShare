package com.ushareit.core.lang;

import android.util.Pair;

public class DynamicValue {
    public static final int STATUS_UNLOAD = 0;
    public static final int STATUS_LOADING = 1;
    public static final int STATUS_LOADED = 2;
    public static final int STATUS_ERROR = 3;

    public int mStatus = STATUS_UNLOAD;
    private long mUpdateDuration = 0L;
    private long mLastSetTime = 0L;
    private Object mValue = null;
    private Object mDefaultValue = null;

    public DynamicValue(Object value, boolean isDefault, long updateDuration) {
        if (isDefault) {
            mDefaultValue = value;
            mStatus = STATUS_UNLOAD;
        } else {
            mValue = value;
            mStatus = STATUS_LOADED;
            mLastSetTime = System.currentTimeMillis();
        }
        mUpdateDuration = updateDuration;
    }

    public boolean hasValue() {
        return (mValue != null);
    }

    public boolean isNeedUpdate() {
        return (Math.abs(System.currentTimeMillis() - mLastSetTime) > mUpdateDuration && mStatus != STATUS_LOADING);
    }

    public void startLoad() {
        mStatus = STATUS_LOADING;
    }

    public void endLoad(int status) {
        mStatus = status;
    }

    public boolean isLoading() {
        return (mStatus == STATUS_LOADING);
    }

    public void updateValue(Object value) {
        updateValue(value, mUpdateDuration);
    }

    public void updateValue(Object value, long updateDuration) {
        mValue = value;
        mStatus = STATUS_LOADED;
        mLastSetTime = System.currentTimeMillis();

        mUpdateDuration = updateDuration;
    }

    public void updateDuration(long updateDuration) {
        mUpdateDuration = updateDuration;
    }

    public Integer getIntValue() {
        return (mValue != null) ? (Integer)mValue : (Integer)mDefaultValue;
    }

    public Long getLongValue() {
        return (mValue != null) ? (Long)mValue : (Long)mDefaultValue;
    }

    public Boolean getBooleanValue() {
        return (mValue != null) ? (Boolean)mValue : (Boolean)mDefaultValue;
    }

    public String getStringValue() {
        return (mValue != null) ? (String)mValue : (String)mDefaultValue;
    }

    public Object getObjectValue() {
        return (mValue != null) ? (Object)mValue : (Object)mDefaultValue;
    }

    @SuppressWarnings("unchecked")
    public Pair<Boolean, Boolean> getPairBooleanValue() {
        return (mValue != null) ? (Pair<Boolean, Boolean>)mValue : (Pair<Boolean, Boolean>)mDefaultValue;
    }
}

