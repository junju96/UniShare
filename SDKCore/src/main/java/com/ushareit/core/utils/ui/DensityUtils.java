package com.ushareit.core.utils.ui;

import android.content.res.Resources;
import android.util.TypedValue;

public class DensityUtils {

    /**
     * 获取设置密度
     * @return 密度
     */
    public static float getDensity() {
        return Resources.getSystem().getDisplayMetrics().density;
    }

    /**
     * dp转px
     * @param dip
     * @return 整型px
     */
    public static int dip2px(float dip) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int)(dip * scale + 0.5f);
    }

    /**
     * px转dp
     * @param pix
     * @return 转换后的dp
     */
    public static int px2dip(float pix) {
        float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (pix / scale + 0.5f);
    }

    /**
     * dp转px
     * @param dip
     * @return 转换后的px
     */
    public static float dipToPix(float dip) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * sp转px
     * @param sp
     * @return 转换后的px
     */
    public static float spToPix(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * px转sp
     * @param pix
     * @return 转换后的sp
     */
    public static int pixToSp(float pix) {
        float fontScale = Resources.getSystem().getDisplayMetrics().scaledDensity;
        return (int) (pix / fontScale + 0.5f);
    }
}
