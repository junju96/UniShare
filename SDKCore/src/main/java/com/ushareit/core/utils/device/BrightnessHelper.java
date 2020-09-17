package com.ushareit.core.utils.device;

import android.content.Context;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

public class BrightnessHelper {

    public static int getScreenMode(Context context) {
        int screenMode = 0;
        try {
            screenMode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Exception e) {}
        return screenMode;
    }

    /**
     * 获取屏幕亮度
     * @param context
     * @return 返回亮度值
     */
    public static int getScreenBrightness(Context context) {
        int screenBrightness = 255;
        try {
            screenBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {}
        return screenBrightness;
    }

    public static void setScreenMode(Context context, int paramInt) {
        try {
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, paramInt);
        } catch (Exception e) {}
    }

    public static void saveScreenBrightness(Context context, int paramInt) {
        try {
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, paramInt);
        } catch (Exception e) {}
    }

    /**
     * 设置屏幕亮度
     * @param localWindow
     * @param paramInt 亮度值
     */
    public static void setScreenBrightness(Window localWindow, int paramInt) {
        try{
            WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
            float f = paramInt / 255.0F;
            localLayoutParams.screenBrightness = f;
            localWindow.setAttributes(localLayoutParams);
        } catch (Exception e) {}
    }
}
