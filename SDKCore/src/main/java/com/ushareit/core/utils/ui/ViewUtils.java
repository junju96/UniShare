package com.ushareit.core.utils.ui;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.sdk_core.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ViewUtils {
    private static final String TAG = "ViewUtils";
    private static final int BUTTON_CLICK_INTERVAL = 1000; // ms

    public static void setImageResource(ImageView imageView, int resId) {
        try {
            imageView.setImageResource(resId);
        } catch (OutOfMemoryError e) {
            collectViewOperationOOM(imageView.getContext(), imageView, resId);
            Logger.d(TAG, "Caught OutOfMemoryError while attempting to setImageResource");
            imageView.setImageDrawable(null);
        } catch (Throwable t) {
            imageView.setImageDrawable(null);
        }
    }

    public static void setBackgroundResource(View view, int resId) {
        try {
            view.setBackgroundResource(resId);
        } catch (OutOfMemoryError e) {
            collectViewOperationOOM(view.getContext(), view, resId);
            Logger.d(TAG, "Caught OutOfMemoryError while attempting to setBackgroundResource");
        } catch (Throwable t) {}
    }

    public static void setBackgroundBitmap(View view, Bitmap bitmap) {
        try {
            setBackgroundDrawable(view, new BitmapDrawable(bitmap));
        } catch (Throwable t) {}
    }

    public static void setBackgroundDrawable(View view, Drawable drawable) {
        try {
            ViewCompat.setBackground(view, drawable);
        } catch (Throwable t) {}
    }

    public static void setViewSize(View target, int width, int height) {
        ViewGroup.LayoutParams targetLP = target.getLayoutParams();
        targetLP.width = width;
        targetLP.height = height;
        target.setLayoutParams(targetLP);
    }

    public static void setViewWidth(View target, int width) {
        ViewGroup.LayoutParams targetLP = target.getLayoutParams();
        targetLP.width = width;
        target.setLayoutParams(targetLP);
    }

    public static void setViewHeight(View target, int height) {
        ViewGroup.LayoutParams targetLP = target.getLayoutParams();
        targetLP.height = height;
        target.setLayoutParams(targetLP);
    }

    public static void setViewMargin(View target, Rect margin) {
        ViewGroup.MarginLayoutParams targetLP = (ViewGroup.MarginLayoutParams)target.getLayoutParams();
        int left = margin.left != Integer.MAX_VALUE ? margin.left : targetLP.leftMargin;
        int top = margin.top != Integer.MAX_VALUE ? margin.top : targetLP.topMargin;
        int right = margin.right != Integer.MAX_VALUE ? margin.right : targetLP.rightMargin;
        int bottom = margin.bottom != Integer.MAX_VALUE ? margin.bottom : targetLP.bottomMargin;
        targetLP.setMargins(left, top, right, bottom);
        target.setLayoutParams(targetLP);
    }

    public static void setViewLeftMargin(View target, int left) {
        ViewGroup.MarginLayoutParams targetLP = (ViewGroup.MarginLayoutParams)target.getLayoutParams();
        targetLP.setMargins(left, targetLP.topMargin, targetLP.rightMargin, targetLP.bottomMargin);
        target.setLayoutParams(targetLP);
    }

    public static void setViewTopMargin(View target, int top) {
        ViewGroup.MarginLayoutParams targetLP = (ViewGroup.MarginLayoutParams)target.getLayoutParams();
        targetLP.setMargins(targetLP.leftMargin, top, targetLP.rightMargin, targetLP.bottomMargin);
        target.setLayoutParams(targetLP);
    }

    public static void setViewRightMargin(View target, int right) {
        ViewGroup.MarginLayoutParams targetLP = (ViewGroup.MarginLayoutParams)target.getLayoutParams();
        targetLP.setMargins(targetLP.leftMargin, targetLP.topMargin, right, targetLP.bottomMargin);
        target.setLayoutParams(targetLP);
    }

    public static void setViewBottomMargin(View target, int bottom) {
        ViewGroup.MarginLayoutParams targetLP = (ViewGroup.MarginLayoutParams)target.getLayoutParams();
        targetLP.setMargins(targetLP.leftMargin, targetLP.topMargin, targetLP.rightMargin, bottom);
        target.setLayoutParams(targetLP);
    }

    private static final String ANALYTICS_EVENT_ERR_VIEW_OPERATION_OOM = "ERR_ViewOperOOM";

    private static void collectViewOperationOOM(Context context, View view, int resId) {
        try {
            HashMap<String, String> params = new LinkedHashMap<String, String>();

            String viewName = view.getClass().getSimpleName();
            if (context instanceof Activity)
                viewName = viewName + "_" + ((Activity)context).getClass().getSimpleName();
            params.put("view", viewName);
            params.put("resId", String.valueOf(resId));
//            Stats.onEvent(context, ANALYTICS_EVENT_ERR_VIEW_OPERATION_OOM, params);
        } catch (Throwable t) {}
    }

    // To avoid click too frequently
    // NOTE: input parameter v should not be set other tags
    public static boolean isClickTooFrequently(View v) {
        return isClickTooFrequently(v, BUTTON_CLICK_INTERVAL);
    }

    public static boolean isClickTooFrequently(View v, long intervalTime) {
        try {
            Object tag = v.getTag(R.id.b_click_frequently_tag);
            long past = tag == null ? 0 : (Long)tag;
            long now = System.currentTimeMillis();
            long interval = now - past;

            if (Math.abs(interval) < intervalTime)
                return true;

            v.setTag(R.id.b_click_frequently_tag, now);
        } catch (Exception e) {}

        return false;
    }

    public static boolean activityIsDead(Context context) {
        if (context == null)
            return true;

        try {
            if (context instanceof Activity) {
                if (((Activity) context).isFinishing())
                    return true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && ((Activity) context).isDestroyed())
                    return true;
            }
        } catch (Exception e) {
            return true;
        }

        return false;
    }


    /**
     * https://stackoverflow.com/questions/10095196/whered-padding-go-when-setting-background-drawable/20873849
     * https://github.com/android/android-ktx/issues/208
     * @param target
     * @param drawable
     */
    public static void setBackgroundKeepPadding(View target, Drawable drawable) {
        if (target == null) {
            return;
        }
        int oldPaddingLeft = ViewCompat.getLayoutDirection(target) == View.LAYOUT_DIRECTION_RTL ? ViewCompat.getPaddingStart(target) : target.getPaddingLeft();
        int oldPaddingRight = ViewCompat.getLayoutDirection(target) == View.LAYOUT_DIRECTION_RTL ? ViewCompat.getPaddingEnd(target) : target.getPaddingRight();
        int oldPaddingTop = target.getPaddingTop();
        int oldPaddingBottom = target.getPaddingBottom();
        ViewCompat.setBackground(target, drawable);

        int newPaddingLeft = ViewCompat.getLayoutDirection(target) == View.LAYOUT_DIRECTION_RTL ? ViewCompat.getPaddingStart(target) : target.getPaddingLeft();
        int newPaddingRight = ViewCompat.getLayoutDirection(target) == View.LAYOUT_DIRECTION_RTL ? ViewCompat.getPaddingEnd(target) : target.getPaddingRight();
        int newPaddingTop = target.getPaddingTop();
        int newPaddingBottom = target.getPaddingBottom();
        if (newPaddingLeft != oldPaddingLeft || newPaddingTop != oldPaddingTop || newPaddingRight != oldPaddingRight || newPaddingBottom != oldPaddingBottom) {
            target.setPadding(oldPaddingLeft, oldPaddingTop, oldPaddingRight, oldPaddingBottom);
        }
    }

    public static void setBackgroundKeepPadding(View target, @DrawableRes int resId) {
        setBackgroundKeepPadding(target, ContextCompat.getDrawable(ObjectStore.getContext(), resId));
    }


    /**
     * enlarge click area
     */
    public static void setTouchDelegate(final View view, final int left, final int top, final int right, final int bottom) {
        if (view == null || !(view.getParent() instanceof View)) {
            return;
        }
        final View parent = (View) view.getParent();
        parent.post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                view.setEnabled(true);
                view.getHitRect(bounds);
                bounds.left -= left;
                bounds.top -= top;
                bounds.right += right;
                bounds.bottom += bottom;
                parent.setTouchDelegate(new TouchDelegate(bounds, view));
            }
        });
    }
}
