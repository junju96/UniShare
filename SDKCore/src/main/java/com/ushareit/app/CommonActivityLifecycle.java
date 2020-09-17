package com.ushareit.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class CommonActivityLifecycle implements Application.ActivityLifecycleCallbacks{

    private static int sActivityCount = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        sActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        sActivityCount--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public static boolean isAppInBackground() {
        return sActivityCount <= 0;
    }
}
