package com.ushareit.core.utils.device;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.WindowManager;

import com.ushareit.core.os.AndroidHelper;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.lang.ObjectStore;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

public final class SysCaps {
    private SysCaps() {}

    private static long sTotalMemory = -1; // in MB
    
    public static long getFreeMem() {
        MemoryInfo info = new MemoryInfo();
        ActivityManager am = (ActivityManager) ObjectStore.getContext().getSystemService(Activity.ACTIVITY_SERVICE);
        am.getMemoryInfo(info);
        return info.availMem;
    }

    public static long getTotalMem() {
        if (sTotalMemory == -1)
            sTotalMemory = readProcMeminfo();
        return sTotalMemory;
    }
    
    private static long readProcMeminfo() {
        long total = 0;
        FileReader fReader = null;
        BufferedReader bReader = null;
        try {
            fReader = new FileReader("/proc/meminfo");
            bReader = new BufferedReader(fReader);
            String text = bReader.readLine();
            if (!TextUtils.isEmpty(text)) {
                String[] array = text.split("\\s+");
                total = Long.valueOf(array[1]) / 1024;
            }
        } catch (Exception e) {
        } finally {
            Utils.close(bReader);
            Utils.close(fReader);
        }
        return total;
    }
    
    private static Pair<Integer, Integer> sResolution = null;

    public static Pair<Integer,Integer> getResolution(Context context) {
        if (sResolution == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(metrics);
                sResolution = new Pair<Integer, Integer>(metrics.widthPixels, metrics.heightPixels);
            }
        }
        return sResolution;
    }

    public static class CameraUtils {
        private static boolean checkCameraFacing(final int facing) {
            if (getSdkVersion() < AndroidHelper.ANDROID_VERSION_CODE.GINGERBREAD)
                return false;

            try {
                final int cameraCount = Camera.getNumberOfCameras();
                Camera.CameraInfo info = new Camera.CameraInfo();
                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, info);
                    if (facing == info.facing) {
                        return true;
                    }
                }
            } catch (Throwable e) {}
            return false;
        }

        public static boolean hasBackFacingCamera() {
            return checkCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        public static boolean hasFrontFacingCamera() {
            return checkCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        public static int getSdkVersion() {
            return Build.VERSION.SDK_INT;
        }
    }

    public static class SensorUtils {
        public static List<Sensor> getSensorList() {
            try {
                SensorManager sensor = (SensorManager) ObjectStore.getContext().getSystemService(Context.SENSOR_SERVICE);
                if (sensor != null)
                    return sensor.getSensorList(Sensor.TYPE_ALL);
            } catch (Throwable e){}
            return null;
        }

        public static boolean hasGravitySensor() {
            List<Sensor> sensorList = getSensorList();
            if (sensorList == null)
                return false;
            for (Sensor sensor : sensorList) {
                if (sensor.getType() == Sensor.TYPE_GRAVITY)
                    return true;
            }
            return false;
        }

        public static boolean hasGyroscopeSensor() {
            List<Sensor> sensorList = getSensorList();
            if (sensorList == null)
                return false;
            for (Sensor sensor : sensorList) {
                if (sensor.getType() == Sensor.TYPE_GYROSCOPE)
                    return true;
            }
            return false;
        }
    }
}
