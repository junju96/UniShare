package com.ushareit.core.lang;

import com.ushareit.core.Assert;
import com.ushareit.core.utils.i18n.LocaleUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * content object's extra properties.
 */
public abstract class ObjectExtras {
    private static final String CAST_ERROR = "%s's content extras is not %s type.";

    private Map<String, Object> mExtras;

    public ObjectExtras() {}

    public void readExtras(JSONObject jo) {
        try {
            Iterator<?> keys = jo.keys();
            if (!keys.hasNext())
                return;

            mExtras = new HashMap<String, Object>(2);
            while (keys.hasNext()) {
                String key = (String)keys.next();
                String value = jo.getString(key);
                mExtras.put(key, value);
            }
        } catch (Exception e) {}
    }

    public void copyExtras(ObjectExtras extras) {
        try {
            Set<Entry<String, Object>> entraSet = extras.mExtras.entrySet();
            for (Entry<String, Object> entraEntry : entraSet)
                doPutExtra(entraEntry.getKey(), entraEntry.getValue());
        } catch (Exception e) {}
    }

    /**
     * check if specified extra property exists
     * @param name the property name
     * @return
     */
    public boolean hasExtra(String name) {
        return (mExtras != null && mExtras.containsKey(name));
    }

    /**
     * remove extra property.
     * @param name property name, can't be null.
     * @return return the removed property value, if has no, return null.
     */
    public Object removeExtra(String name) {
        if (mExtras == null)
            return null;

        return mExtras.remove(name);
    }

    public void putExtra(String name, boolean value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, byte value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, char value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, short value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, int value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, long value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, float value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, double value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, String value) {
        doPutExtra(name, value);
    }

    public void putExtra(String name, Object value, boolean saveNull) {
        doPutExtra(name, value, saveNull);
    }

    public void putExtras(Map<String, Object> extras) {
        if (extras == null)
            return ;

        if (mExtras == null)
            mExtras = new HashMap<String, Object>(extras.size());
        mExtras.putAll(extras);
    }

    /**
     * put extra property, will overwrite existing property if already exists.
     * @param name property name, can't be null.
     * @param value property value
     */
    public void putExtra(String name, Object value) {
        doPutExtra(name, value);
    }

    private void doPutExtra(String name, Object value) {
        doPutExtra(name, value, false);
    }

    private void doPutExtra(String name, Object value, boolean saveNull) {
        Assert.notNull(name);
        if (value == null && !saveNull)
            return;
        if (mExtras == null)
            mExtras = new HashMap<String, Object>(2);
        mExtras.put(name, value);
    }

    /**
     * get extra property, return null if not exists.
     * @param name property name
     * @return property value, or null if property doesn't exist.
     */
    public Object getExtra(String name) {
        return getExtra(name, null);
    }

    /**
     * get extra property, return specified defaultValue if not exists.
     * @param name property name
     * @param defaultValue the default value to be returned if property doesn't exist.
     * @return return property value, or defaultValue if property doesn't exist.
     */
    public Object getExtra(String name, Object defaultValue) {
        Object obj = (mExtras == null) ? defaultValue : mExtras.get(name);
        if (obj != null)
            return obj;
        return defaultValue;
    }

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Boolean)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "boolean"));
            return defaultValue;
        }
    }

    public byte getByteExtra(String name, byte defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Byte)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "byte"));
            return defaultValue;
        }
    }

    public short getShortExtra(String name, short defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Short)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "short"));
            return defaultValue;
        }
    }

    public char getCharExtra(String name, char defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Character)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "char"));
            return defaultValue;
        }
    }

    public int getIntExtra(String name, int defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Integer)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "int"));
            return defaultValue;
        }
    }

    public long getLongExtra(String name, long defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Long)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "long"));
            return defaultValue;
        }
    }

    public float getFloatExtra(String name, float defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Float)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "float"));
            return defaultValue;
        }
    }

    public double getDoubleExtra(String name, double defaultValue) {
        Object obj = getExtra(name, defaultValue);

        try {
            return (Double)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "double"));
            return defaultValue;
        }
    }

    public String getStringExtra(String name) {
        Object obj = getExtra(name, null);

        try {
            return (String)obj;
        } catch (ClassCastException e) {
            Assert.fail(LocaleUtils.formatStringIgnoreLocale(CAST_ERROR, name, "String"));
            return null;
        }
    }

    public Object getObjectExtra(String name) {
        return getExtra(name, null);
    }

    public Map<String, Object> getExtras() {
        if (mExtras == null)
            return null;
        return new HashMap<String, Object>(mExtras);
    }
}
