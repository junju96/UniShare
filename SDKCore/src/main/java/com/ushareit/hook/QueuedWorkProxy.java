package com.ushareit.hook;

import android.os.Build;

import com.ushareit.core.Logger;

import java.lang.reflect.Field;
import java.util.Collection;

class QueuedWorkProxy {
    private static final String TAG = "QueuedWorkProxy";
    private static final String CLASS_NAME = "android.app.QueuedWork";
    private static final String FILED_NAME_PENDING_WORK_FINISH_ = "sPendingWorkFinishers";
    private static final String FILED_NAME_FINISHERS = "sFinishers";

    private static Collection<Runnable> sPendingWorkFinishers = null;
    private static boolean sSupportHook = true;

    public static void cleanAll() {

            if (sPendingWorkFinishers == null && sSupportHook) {
                try {
                    Class cls = Class.forName(CLASS_NAME);
                    Field f = null;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        f = cls.getDeclaredField(FILED_NAME_PENDING_WORK_FINISH_);
                    }else{
                        f = cls.getDeclaredField(FILED_NAME_FINISHERS);
                    }
                    f.setAccessible(true);
                    sPendingWorkFinishers = (Collection<Runnable>) f.get(null);
                } catch (Throwable t) {
                    sSupportHook = false;
                }
            }
            if (sPendingWorkFinishers != null) {
                Logger.d(TAG, "cleanAll sPendingWorkFinishers size is: " + sPendingWorkFinishers.size());
                sPendingWorkFinishers.clear();
                Logger.d(TAG, "cleanAll success ");
            }

    }
}
