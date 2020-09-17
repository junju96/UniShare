package com.ushareit.core.utils.ui;

import android.view.View;

import com.ushareit.sdk_core.R;

public class ViewClickUtil {

    private static long sLastClickVideoTimeMillis = 0;

    public static boolean isClickTooFrequent(View view) {
       return isClickTooFrequent(view, 500);
    }

    public static boolean isClickTooFrequent(View view, int duration) {
        try {
            Object tag = view.getTag(R.id.tag_click_time);
            long past = tag == null ? 0 : (Long)tag;
            long now = System.currentTimeMillis();
            if ( Math.abs(now - past) < duration)
                return true;

            view.setTag(R.id.tag_click_time, now);
        } catch (Exception e) {}

        return false;
    }

    public static boolean isInterceptVideoClickEvent(View view, int duration) {
        if (view == null)
            return false;
        Object tag = view.getTag(R.id.tag_video_label);
        if (tag != null) {
            long now = System.currentTimeMillis();
            long interval = Math.abs(now - sLastClickVideoTimeMillis);
            if (interval < duration)
                return true;
            sLastClickVideoTimeMillis = now;
        }
        return false;
    }
}
