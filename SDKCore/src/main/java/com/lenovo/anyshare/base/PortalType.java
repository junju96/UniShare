package com.lenovo.anyshare.base;

import android.content.Intent;
import android.text.TextUtils;

// records how this app was launched for (what's supposed to do when this app get started).
public class PortalType {
    public static final String TAG = "PortalType";
    public static final String RUNTIME_GAME_ID = "runtime_game_id";
    public static final String UNKNOWN = "unknown_portal";


    private String mValue;
    private static PortalType mPortal = null;

    public static PortalType getInstance() {
        if (mPortal == null)
            mPortal = new PortalType(UNKNOWN);
        return mPortal;
    }

    public static PortalType createInstance(Intent intent) {
        if (intent.hasExtra(TAG))
            mPortal = new PortalType(intent.getStringExtra(TAG));
        else
            mPortal = new PortalType(UNKNOWN);

        return mPortal;
    }

    public static PortalType createInstance(String portal) {
        if (!TextUtils.isEmpty(portal))
            mPortal = new PortalType(portal);
        else
            mPortal = new PortalType(UNKNOWN);

        return mPortal;
    }

    private PortalType(String value) {
        mValue = value;
    }

    public String toString() {
        if (!TextUtils.isEmpty(mValue))
            return mValue;
        return UNKNOWN;
    }

    public boolean isEqual(String type) {
        return (!TextUtils.isEmpty(type) && type.equalsIgnoreCase(mPortal.mValue));
    }
}
