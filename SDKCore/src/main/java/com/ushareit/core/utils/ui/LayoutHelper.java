package com.ushareit.core.utils.ui;

import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ushareit.core.os.AndroidHelper;

public class LayoutHelper {

    public static void setGravityStart(FrameLayout.LayoutParams params) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.gravity |= Gravity.START;
        else
            params.gravity |= Gravity.LEFT;
    }

    public static void setGravityStart(LinearLayout.LayoutParams params) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.gravity |= Gravity.START;
        else
            params.gravity |= Gravity.LEFT;
    }

    public static void setGravityStart(WindowManager.LayoutParams params) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.gravity |= Gravity.START;
        else
            params.gravity |= Gravity.LEFT;
    }

    public static void setGravityEnd(FrameLayout.LayoutParams params) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.gravity |= Gravity.END;
        else
            params.gravity |= Gravity.RIGHT;
    }

    public static void setGravityEnd(LinearLayout.LayoutParams params) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.gravity |= Gravity.END;
        else
            params.gravity |= Gravity.RIGHT;
    }

    public static void setGravityEnd(WindowManager.LayoutParams params) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.gravity |= Gravity.END;
        else
            params.gravity |= Gravity.RIGHT;
    }

    public static void setEndMargin(FrameLayout.LayoutParams params, int rightMargin) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.setMarginEnd(rightMargin);
        else
            params.rightMargin = rightMargin;
    }

    public static void setEndMargin(LinearLayout.LayoutParams params, int rightMargin) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.setMarginEnd(rightMargin);
        else
            params.rightMargin = rightMargin;
    }

    public static void setStartMargin(FrameLayout.LayoutParams params, int leftMargin) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.setMarginStart(leftMargin);
        else
            params.leftMargin = leftMargin;
    }

    public static void setStartMargin(LinearLayout.LayoutParams params, int leftMargin) {
        if (Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.JELLY_BEAN_MR1)
            params.setMarginStart(leftMargin);
        else
            params.leftMargin = leftMargin;
    }
}
