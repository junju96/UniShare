package com.ushareit.core.utils.ui;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.os.AndroidHelper;

import java.lang.reflect.Field;

public class SafeToast {
    private static final String TAG="SafeToast";

    private static Toast sToast;

    private static Toast sCustomToast;

    public static void showToast(String text, int duration) {
        try {
            if (sToast != null) {
                sToast.setText(text);
                sToast.setDuration(duration);
            } else {
                sToast = Toast.makeText(ObjectStore.getContext(), text, duration);
                hook(sToast);
            }
            sToast.show();
        } catch (Exception ex) {
            Logger.e(TAG, "safe show toast exception: " + ex.getMessage());
        }
    }

    public static void showToast(int resId, int duration) {
        try {
            showToast(ObjectStore.getContext().getResources().getString(resId), duration);
        } catch (Exception ex) {
        }
    }

    public static void showToast(String text, int duration, int gravity, int xOffset, int yOffset) {
        try {
            if (sCustomToast != null) {
                sCustomToast.setText(text);
                sCustomToast.setDuration(duration);
                sCustomToast.setGravity(gravity, xOffset, yOffset);
            } else {
                sCustomToast = Toast.makeText(ObjectStore.getContext(), text, duration);
                sCustomToast.setGravity(gravity, xOffset, yOffset);
                hook(sCustomToast);
            }
            sCustomToast.show();
        } catch (Exception ex) {
            Logger.e(TAG, "safe show toast exception: " + ex.getMessage());
        }
    }

    private static void hook(Toast toast) {
        if (Build.VERSION.SDK_INT != AndroidHelper.ANDROID_VERSION_CODE.NOUGAT_MR1)
            return;
        try {
            Field field_TN = Toast.class.getDeclaredField("mTN");
            field_TN.setAccessible(true);
            Field sField_TN_Handler = field_TN.getType().getDeclaredField("mHandler");
            sField_TN_Handler.setAccessible(true);

            Object tn = field_TN.get(toast);
            Handler preHandler = (Handler) sField_TN_Handler.get(tn);
            sField_TN_Handler.set(tn, new SafeHandler(preHandler));
        } catch (Exception e) {
            Logger.e(TAG, "safe toast hook exception: " + e.getMessage());
        }
    }


    private static class SafeHandler extends Handler {
        private Handler impl;

        public SafeHandler(Handler impl) {
            this.impl = impl;
        }

        @Override
        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
            }
        }

        @Override
        public void handleMessage(Message msg) {
            impl.handleMessage(msg);
        }
    }
}
