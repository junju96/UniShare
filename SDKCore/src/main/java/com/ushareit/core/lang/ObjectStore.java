package com.ushareit.core.lang;

import android.content.Context;

import com.ushareit.core.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * an global singleton object store, every object is associated with a unique String key.
 */
public final class ObjectStore {
    private static Map<String, Object> mObjects = new HashMap<String, Object>();

    private ObjectStore() {}

    /**
     * add object to store, return an unique key that can be used to query this object later.
     * @param obj storage object
     * @return key match object in object collections
     */
    public static String add(Object obj) {
        String key = UUID.randomUUID().toString();
        synchronized (mObjects) {
            mObjects.put(key, obj);
        }
        return key;
    }

    /**
     * add object to store, with specified unique key.
     * caller must assure the key is unique across all objects in this store. eg. add a custom unique prefix when generate key.
     * @param obj storage object
     * @return key the unique key used to get this object
     */
    public static void add(String key, Object obj) {
        synchronized (mObjects) {
            mObjects.put(key, obj);
        }
    }
    
    /**
     * query object by key.
     * @param key the key returned when add this object
     * @return the object associated with specified key, or null if not found
     */
    public static Object get(String key) {
        Assert.notNull(key);
        Object obj = null;
        synchronized (mObjects) {
            obj = mObjects.get(key);
        }
        return obj;
    }

    /**
     * remove object from store and return it.
     * @param key the key returned when add this object
     * @return the object associated with specified key, or null if not found
     */
    public static Object remove(String key) {
        Assert.notNull(key);
        Object obj = null;
        synchronized (mObjects) {
            obj = mObjects.remove(key);
        }
        return obj;
    }

    // following is special handy getter/setter for the global Context object.
    // we can't find better location than here to write these two methods.

    // the global application context.
    private static Context mContext = null;

    /**
     * set the global Context object, to be used by getContext() later.
     */
    public static void setContext(Context context) {
        mContext = context;
    }

    /**
     * get the global Context object.
     * this is an even quicker way to get an global Context object.
     */
    public static Context getContext() {
        return mContext;
    }
}
