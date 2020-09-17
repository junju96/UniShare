package com.ushareit.core.utils;

import com.ushareit.core.utils.i18n.LocaleUtils;

import java.util.HashMap;
import java.util.Map;

public enum BuildType {
    DEBUG("debug"), DEV("dev"), WTEST("wtest"), ALPHA("alpha"), RELEASE("release");
    private String mValue;

    BuildType(String value) {
        mValue = value;
    }
    private final static Map<String, BuildType> VALUES = new HashMap<>();

    static {
        for (BuildType item : BuildType.values())
            VALUES.put(item.mValue, item);
    }

    public static BuildType fromString(String value) {
        String iValue = LocaleUtils.toLowerCaseIgnoreLocale(value);
        return VALUES.containsKey(iValue) ? VALUES.get(iValue) : null;
    }

    @Override
    public String toString() {
        return mValue;
    }

}
