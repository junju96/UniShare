package com.ushareit.core.change;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.ushareit.core.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChangeListenerManager {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ConcurrentHashMap<String, List<ChangedListener>> mListenersMap = new ConcurrentHashMap();

    private static class InstanceHolder {
        private static final ChangeListenerManager INSTANCE = new ChangeListenerManager();
    }

    public static ChangeListenerManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static ConcurrentHashMap<String, Object> mStickyEvents = new ConcurrentHashMap<>();

    public void notifyChange(String key) {
        notifyChange(key, null);
    }

    public void notifyChange(final String key, long delaye) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyChange(key, null);
            }
        }, delaye);
    }

    public <T> void notifyChange(final String key, final T value) {
        if (TextUtils.isEmpty(key))
            return;

        try {
            final List<ChangedListener> listeners = mListenersMap.get(key);
            if (listeners != null) {
                Runnable mNotifyTask = new Runnable() {
                    @Override
                    public void run() {
                        for (final ChangedListener listener : listeners) {
                            try {
                                listener.onListenerChange(key, value);
                            } catch (Exception e) {
                                Logger.d("ChangeListenerManager", "onListenerChange : " + key + "   " + e.toString());
                            }
                        }
                    }
                };

                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mNotifyTask.run();
                } else {
                    mHandler.post(mNotifyTask);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public <T> void notifyStickyEventChange(String key, T value) {
        mStickyEvents.put(key, value);
        notifyChange(key, value);

    }
    public void registerChangedListener(String key, ChangedListener listener) {
        if (TextUtils.isEmpty(key) || listener == null)
            return;

        List<ChangedListener> list = mListenersMap.get(key);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            list.add(listener);
            mListenersMap.put(key, list);
        } else {
            if (!list.contains(listener)) {
                list.add(listener);
            }
        }
        if (mStickyEvents.containsKey(key)) {
            Object value = mStickyEvents.remove(key);
            listener.onListenerChange(key, value);
        }
    }

    public void unregisterChangedListener(String key, ChangedListener listener) {
        if (TextUtils.isEmpty(key) || listener == null)
            return;

        try {
            List<ChangedListener> list = mListenersMap.get(key);
            if (list != null) {
                list.remove(listener);
                if (list.isEmpty())
                    mListenersMap.remove(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
