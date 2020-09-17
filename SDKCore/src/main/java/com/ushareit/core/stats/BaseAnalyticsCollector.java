package com.ushareit.core.stats;

import android.content.Context;

import java.util.HashMap;

public abstract class BaseAnalyticsCollector {
    private boolean mCollectPageView;
    private boolean mCollectEvent;

    public BaseAnalyticsCollector(boolean collectPageView, boolean collectEvent) {
        mCollectPageView = collectPageView;
        mCollectEvent = collectEvent;
    }

    abstract public String getCollectorName();

    public boolean isCollectPageView() {
        return mCollectPageView;
    }

    public boolean isCollectEvent() {
        return mCollectEvent;
    }

    public void dispatch(Context context, String portal) {}

    public boolean syncDispatch(Context context, String portal) {
        return false;
    }

    public int countEvent(Context context) {
        return 0;
    }

    public abstract void onResume(String contextName, String extra);

    public abstract void onPause(String contextName, String extra);

    public abstract void onEvent(Context context, String eventId);

    public abstract void onEvent(Context context, String eventId, String label);

    public abstract void onEvent(Context context, String eventId, HashMap<String, String> map);

    public abstract void onEvent(Context context, String eventId, HashMap<String, String> map, int value);

    public abstract void onError(Context context, String error);

    public abstract void onError(Context context, Throwable e);

    public abstract void onAppDestroy();

    public void onAppBackend() {};

    public void onPause(Context context, HashMap<String, String> map) {
        this.onPause(context.getClass().getName(), "");
    }
}
