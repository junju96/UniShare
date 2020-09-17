package com.ushareit.core.utils.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public final class RH {
    private static Bitmap sBitmap = null;
    private static Drawable sDrawable = null;

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        try {
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight());
        } catch (OutOfMemoryError e) {
            if (sBitmap == null) {
                try {
                    sBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
                } catch (Throwable t) {}
            }

            if (sBitmap != null)
                return sBitmap;
            throw e;
        }
    }

    public static Drawable loadIcon(ApplicationInfo _this, PackageManager pm) {
        try {
            Drawable ret = _this.loadIcon(pm);
            if (sDrawable == null)
                sDrawable = ret;
            return ret;
        } catch (OutOfMemoryError e) {
            if (sDrawable != null)
                return sDrawable;
            throw e;
        }
    }

    public static Drawable getDrawable(Context context, int id) {
        try {
            Drawable ret = context.getResources().getDrawable(id);
            if (sDrawable == null)
                sDrawable = ret;
            return ret;
        } catch (OutOfMemoryError e) {
            if (sDrawable != null)
                return sDrawable;
            throw e;
        }
    }

    public static byte[] newByteArray(int length) {
        try {
            return new byte[length];
        } catch (OutOfMemoryError e) {
            System.gc();
            try {
                return new byte[length];
            } catch (OutOfMemoryError ee) {
                System.gc();
                try {
                    Thread.sleep(1000 * 3);
                } catch (InterruptedException e1) {}
                return new byte[length];
            }
        }
    }
}
