package com.ushareit.hook;

import android.os.Build;
import android.os.Handler;

import com.ushareit.core.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HookUtil {
    public static final String TAG = "HookUtil";
    public static final String CLASS_NAME_ACTIVITY_THREAD = "android.app.ActivityThread";
    public static final String METHOD_CURRENT_ACTIVITY_THREAD = "currentActivityThread";
    public static final String FIELD_M_H = "mH";
    public static final String FIELD_M_CALLBACK = "mCallback";


    private static boolean hookHideApi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                Method forName = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

                Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
                Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
                Object sVmRuntime = getRuntime.invoke(null);
                setHiddenApiExemptions.invoke(sVmRuntime, new Object[]{new String[]{"L"}});
                Logger.d(TAG, "hookHideApi result success");
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    public static void hookSystemHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (hookHideApi()) {
                hookSystemHandlerByReflect();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            hookSystemHandlerByReflect();
        }
    }

    private static void hookSystemHandlerByReflect() {
        boolean hookSuccess = false;
        try {
            Class cls = Class.forName(CLASS_NAME_ACTIVITY_THREAD);
            Method declaredMethod = cls.getDeclaredMethod(METHOD_CURRENT_ACTIVITY_THREAD);
            declaredMethod.setAccessible(true);
            Object activityThread = declaredMethod.invoke(null);
            if (activityThread != null) {
                Field handlerField = cls.getDeclaredField(FIELD_M_H);
                handlerField.setAccessible(true);
                Handler handler = (Handler) handlerField.get(activityThread);
                if (handler != null) {
                    Field mCallbackField = Handler.class.getDeclaredField(FIELD_M_CALLBACK);
                    mCallbackField.setAccessible(true);
                    ActivityThreadHCallbackProxy activityThreadHandler = new ActivityThreadHCallbackProxy(handler);
                    mCallbackField.set(handler, activityThreadHandler);
                    hookSuccess = true;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        Logger.d(TAG, "hook result is " + hookSuccess);
    }

}
