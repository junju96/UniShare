package com.ushareit.core.utils.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.ushareit.core.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PermissionsUtils {
    private static final String TAG = "PermissionsUtils";

    //申请权限时的requestCode
    public static final int PERMISSIONS_REQUEST_CODE = 1;
    public static final int PERMISSIONS_REQUEST_UNKNOWN_SOURCE_CODE = 69;
    //初始化需要的权限组
    public static String[] INIT_PERMISSION = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE};

    public static String[] STORAGE_PERMISSION = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    public static int NOTIFY_ENABLE = 1;
    public static int NOTIFY_UNABLE = 2;
    public static int NOTIFY_UNKNOWN = 0;

    public static boolean isNotificationEnable(Context context) {
        return isNotificationEnableDetail(context) == NOTIFY_ENABLE;
    }

    public static boolean isNotificationChannelEnable(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =NotificationManagerCompat.from(context).getNotificationChannel(channelId);
            if(channel == null){
                return true;
            } else {
                return channel.getImportance() > 0;
            }
        }
        return isNotificationEnable(context);
    }

    public static int isNotificationEnableDetail(Context context) {
        // no way to block notification before android 4.1
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return NOTIFY_ENABLE;
        // TODO there is no method to check between Jellybean and Kitkat, maybe need investigation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return NOTIFY_ENABLE;
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                return NotificationManagerCompat.from(context).areNotificationsEnabled() ? NOTIFY_ENABLE : NOTIFY_UNABLE;
            }
            Method methodCheckOpNoThrow = AppOpsManager.class.getMethod("checkOpNoThrow", int.class, int.class, String.class);
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int OP_POST_NOTIFICATION = 11;
            int mode = (Integer) methodCheckOpNoThrow.invoke(appOpsManager, OP_POST_NOTIFICATION, applicationInfo.uid, applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED ? NOTIFY_ENABLE : NOTIFY_UNABLE;
        }catch(Exception e) {
            return NOTIFY_UNKNOWN;
        }
    }


    /**
     * 权限管理回调监听器，在发起权限请求的Activity实现这个接口
     */
    public interface IPermissionRequestListener {
        void setPermissionRequestListener(PermissionRequestCallback callback);
    }
    /**
     * 权限请求回调类，由外部集成实现各自的业务逻辑
     */
    public static abstract class PermissionRequestCallback {
        /**
         * 权限被允许
         */
        @MainThread
        public abstract void onGranted();
        /**
         * 权限被拒绝
         * @param permissions 被拒绝的权限
         */
        @MainThread
        public abstract void onDenied(@Nullable String[] permissions);
    }
    /**
     * 通过activity请求权限组
     * @param activity activity实例
     * @param permissions 权限组
     * @param callback 权限回调函数
     */
    public static void requestPermissionsIfNecessaryForResult(
                                                    @Nullable Activity activity,
                                                    @NonNull String[] permissions,
                                                    @Nullable PermissionRequestCallback callback) {
        requestPermissionsIfNecessaryForResult(activity, permissions, callback, PERMISSIONS_REQUEST_CODE);
    }
    /**
     * 通过fragment请求权限组
     * @param fragment fragment实例
     * @param permissions 权限组
     * @param callback 权限回调函数
     */
    public static void requestPermissionsIfNecessaryForResult(
                                                    @NonNull Fragment fragment,
                                                    @NonNull String[] permissions,
                                                    @Nullable PermissionRequestCallback callback) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        requestPermissionsIfNecessaryForResult(activity, permissions, callback);
    }
    /**
     * 通过activity请求权限组
     * @param activity activity实例
     * @param permissions 权限组
     * @param callback 权限回调函数
     * @param requestCode 请求值
     */
    public static void requestPermissionsIfNecessaryForResult(
                                                    @Nullable Activity activity,
                                                    @NonNull String[] permissions,
                                                    @Nullable PermissionRequestCallback callback,
                                                    int requestCode) {
        if (isBeforeM()) {
            if (callback != null)
                callback.onGranted();
        } else {
            if (activity == null) {
                return;
            }
            final List<String> permissionsList = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (!hasPermission(activity, permission)) {
                    permissionsList.add(permission);
                }
            }
            if (permissionsList.size() > 0) {
                if (activity instanceof IPermissionRequestListener) {
                    ((IPermissionRequestListener) activity).setPermissionRequestListener(callback);
                }
                String[] permsToRequest = permissionsList.toArray(new String[permissionsList.size()]);
                try {
                    ActivityCompat.requestPermissions(activity, permsToRequest, requestCode);
                } catch (ActivityNotFoundException e) {
                    Logger.e(TAG, "request permissions", e);
                }
            } else {
                if (callback != null)
                    callback.onGranted();
            }
        }
    }
    /**
     * 是否有权限
     * @param context 上下文
     * @param permission 需要判断的权限
     * @return
     */
    public static boolean hasPermission(@Nullable Context context, @NonNull String permission) {
        return isBeforeM() || (context != null && ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
    }
    /**
     * 是否有权限组
     * @param context
     * @param permissions 需要判断的权限组
     * @return
     */
    public static boolean hasPermission(@Nullable Context context, @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }
    /**
     * 是否有存储权限
     * @param context
     * @return
     */
    public static boolean hasStoragePermission(Context context) {
        return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    /**
     * 是否有定位权限
     * @param context
     * @return
     */
    public static boolean hasLocationPermission(Context context) {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }
    /**
     * 是否有读取手机状态权限
     * @param context
     * @return
     */
    public static boolean hasReadPhonePermission(Context context) {
        return hasPermission(context, Manifest.permission.READ_PHONE_STATE);
    }
    /**
     * 是否拥有修改系统设置的权限
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasWriteSettingPermission(Context context) {
        return isBeforeM() || Settings.System.canWrite(context);
    }
    /**
     * 进入权限设置界面
     * @param context
     * @return
     */
    public static boolean launchWriteSettings(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            launchAppSettings(context);
            return false;
        }
        return false;
    }
    /**
     * 进入设置页
     * @param context
     * @return
     */
    public static boolean launchAppSettings(Context context) {
        return launchAppSettings(context, true, 0);
    }
    /**
     * 进入设置页
     * @param context
     * @param isNewTask
     * @param requestCode
     * @return
     */
    public static boolean launchAppSettings(Context context, boolean isNewTask, int requestCode) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (isNewTask)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (requestCode > 0 && context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, requestCode);
            } else {
                context.startActivity(intent);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 进入未知安装应用设置页
     * @param context
     */
    public static void launchUnknownAppSources(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, PERMISSIONS_REQUEST_UNKNOWN_SOURCE_CODE);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, "launch unknown app failed: " + e);
        }
    }
    /**
     * 检查是否拥有修改系统设置的权限, 如果没有会跳转到相关权限设置页面要求设置
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean checkWritingPermission(Context context) {
        if (!hasWriteSettingPermission(context)) {
            return launchWriteSettings(context);
        }
        return true;
    }
    /**
     * 通知权限改变
     * @param permissions
     * @param grantResults
     * @param callback
     */
    public static void notifyPermissionsChange(@NonNull String[] permissions, @NonNull int[] grantResults, @Nullable PermissionRequestCallback callback) {
        if (callback != null && permissions != null && grantResults != null && permissions.length != 0 && grantResults.length != 0) {
            List<String> permissionsNeeded = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(permissions[i]);
                }
            }

            if (permissionsNeeded.size() > 0) {
                callback.onDenied(permissionsNeeded.toArray(new String[permissionsNeeded.size()]));
            } else {
                callback.onGranted();
            }
        }
    }
    /**
     * 是否是Android M之前的版本
     * @return
     */
    private static boolean isBeforeM() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }

}
