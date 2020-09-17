package com.ushareit.core.lang;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Collections {
    public static Object safeGet(Map map, Object key) {
        if(map != null && map.containsKey(key))
            return map.get(key);
        return null;
    }

    /**
     * an hash map that will perform an internal copy on any write access.
     * this is quicker that normal hash map when most access are read access and rarely write.
     * @param <K>
     * @param <V>
     */
    public static class CopyOnWriteHashMap<K, V> implements Map<K, V> {
        protected volatile Map<K, V> mMapToRead = getNewMap();

        // you can override to create your specific map, eg. TreeMap
        protected Map<K, V> getNewMap() {
            return new HashMap<K, V>();
        }

        // copy mapToWrite to mapToRead
        protected Map<K, V> copy() {
            Map<K, V> newMap = getNewMap();
            newMap.putAll(mMapToRead);
            return newMap;
        }

        // read methods

        public int size() {
            return mMapToRead.size();
        }

        public boolean isEmpty() {
            return mMapToRead.isEmpty();
        }

        public boolean containsKey(Object key) {
            return mMapToRead.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return mMapToRead.containsValue(value);
        }

        public Collection<V> values() {
            return mMapToRead.values();
        }

        public Set<Entry<K, V>> entrySet() {
            return mMapToRead.entrySet();
        }

        public Set<K> keySet() {
            return mMapToRead.keySet();
        }

        public V get(Object key) {
            return mMapToRead.get(key);
        }

        // write methods

        public synchronized void clear() {
            mMapToRead = getNewMap();
        }

        public synchronized V remove(Object key) {
            Map<K, V> map = copy();
            V o = map.remove(key);
            mMapToRead = map;
            return o;
        }

        public synchronized V put(K key, V value) {
            Map<K, V> map = copy();
            V o = map.put(key, value);
            mMapToRead = map;
            return o;
        }

        public synchronized void putAll(Map<? extends K, ? extends V> t) {
            Map<K, V> map = copy();
            map.putAll(t);
            mMapToRead = map;
        }
    }

    /**
     * an hash map that value are stored as SoftReference which can be GCed by system.
     * @param <K>
     * @param <V>
     */
    public static class SoftReferenceHashMap<K, V> {

        private final HashMap<K, Entry<K, V>> mWeakMap = new HashMap<K, Entry<K, V>>();
        private ReferenceQueue<V> mQueue = new ReferenceQueue<V>();

        public SoftReferenceHashMap() {}

        private static class Entry<K, V> extends SoftReference<V> {
            K mKey;

            public Entry(K key, V value, ReferenceQueue<V> queue) {
                super(value, queue);
                mKey = key;
            }
        }

        @SuppressWarnings("unchecked")
        private void cleanUpWeakMap() {
            Entry<K, V> entry = (Entry<K, V>)mQueue.poll();
            while (entry != null) {
                mWeakMap.remove(entry.mKey);
                entry = (Entry<K, V>)mQueue.poll();
            }
        }

        public synchronized V put(K key, V value) {
            cleanUpWeakMap();
            Entry<K, V> entry = mWeakMap.put(key, new Entry<K, V>(key, value, mQueue));
            return entry == null ? null : entry.get();
        }

        public synchronized V get(K key) {
            cleanUpWeakMap();
            Entry<K, V> entry = mWeakMap.get(key);
            return entry == null ? null : entry.get();
        }

        public synchronized void clear() {
            mWeakMap.clear();
            mQueue = new ReferenceQueue<V>();
        }

        public synchronized ArrayList<K> keys() {
            Set<K> set = mWeakMap.keySet();
            ArrayList<K> result = new ArrayList<K>(set);
            return result;
        }
    }

    /**
     * an hash map that value are stored as WeakReference which can be GCed by system.
     */
    public static class WeakReferenceHashMap<K, V> {

        private final HashMap<K, Entry<K, V>> mWeakMap = new HashMap<K, Entry<K, V>>();
        private ReferenceQueue<V> mQueue = new ReferenceQueue<V>();

        public WeakReferenceHashMap() {}

        private static class Entry<K, V> extends WeakReference<V> {
            K mKey;

            public Entry(K key, V value, ReferenceQueue<V> queue) {
                super(value, queue);
                mKey = key;
            }
        }

        @SuppressWarnings("unchecked")
        private void cleanUpWeakMap() {
            Entry<K, V> entry = (Entry<K, V>)mQueue.poll();
            while (entry != null) {
                mWeakMap.remove(entry.mKey);
                entry = (Entry<K, V>)mQueue.poll();
            }
        }

        public synchronized V put(K key, V value) {
            cleanUpWeakMap();
            Entry<K, V> entry = mWeakMap.put(key, new Entry<K, V>(key, value, mQueue));
            return entry == null ? null : entry.get();
        }

        public synchronized V get(K key) {
            cleanUpWeakMap();
            Entry<K, V> entry = mWeakMap.get(key);
            return entry == null ? null : entry.get();
        }

        public synchronized void clear() {
            mWeakMap.clear();
            mQueue = new ReferenceQueue<V>();
        }

        public synchronized ArrayList<K> keys() {
            Set<K> set = mWeakMap.keySet();
            ArrayList<K> result = new ArrayList<K>(set);
            return result;
        }
    }
}
