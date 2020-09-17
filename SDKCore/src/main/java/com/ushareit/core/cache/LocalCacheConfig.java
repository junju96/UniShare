package com.ushareit.core.cache;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Pair;

import com.lenovo.anyshare.settings.SettingOperate;
import com.ushareit.core.os.AndroidHelper;
import com.ushareit.core.utils.permission.PermissionsUtils;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.StorageVolumeHelper;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.utils.WWUtils;
import com.ushareit.sdk_core.R;

import java.io.File;

import androidx.documentfile.provider.DocumentFile;

public class LocalCacheConfig {
    public static final String KEY_STORAGE_PATH_SETTING = "storage_path_setting";
    public static final String KEY_AUTH_EXTRA_SDCARD_URI = "AUTH_EXTRA_SDCARD_URI";

    public static String getPersistPath(Context context) {
        String persistPath = SettingOperate.getString(KEY_STORAGE_PATH_SETTING);
        // compatible patch
        if (TextUtils.isEmpty(persistPath))
            persistPath = SettingOperate.getString(KEY_AUTH_EXTRA_SDCARD_URI);

        if (TextUtils.isEmpty(persistPath)) {
            StorageVolumeHelper.Volume volume = StorageVolumeHelper.getVolume(context);
            persistPath = new File(volume.mPath, isUsePrivateDir(context, volume) ? getPrivateRootName(context, volume.mPath) : WWUtils.getAppRootDirName(context)).getAbsolutePath();
        }
        return persistPath;
    }

    /**
     * get volume path description and absolute folder path description
     * @param context
     * @return volume path and folder path
     */
    public static Pair<String, String> getDisplayName(Context context, StorageVolumeHelper.Volume volume, String path) {
        String rootDescription = volume.mDescription;
        if (TextUtils.isEmpty(rootDescription) || rootDescription.equals("sdcard0"))
            rootDescription = context.getString(R.string.setting_storage_default);
        else if (rootDescription.equals("sdcard1"))
            rootDescription = context.getString(R.string.setting_storage_sdcard);
        else if (!volume.mWritable && !volume.mIsPrimary && !volume.mPrivateDirWritable && !volume.mSupportAuth)
            rootDescription = rootDescription + " " + context.getString(R.string.setting_storage_no_permission);

        String subDescription = WWUtils.getAppRootDirName(context);
        if (volume.isAuth())
            subDescription += (" " + context.getString(R.string.setting_storage_auth));
        if (TextUtils.isEmpty(path))
            return new Pair<>(rootDescription, new File(rootDescription, subDescription).getAbsolutePath());

        if (SFile.isDocumentUri(path)) {
            String uriPath = Uri.parse(path).getPath();
            String[] pathArry = uriPath.split(":");
            subDescription = (pathArry.length > 1) ? new File(pathArry[1], subDescription).getAbsolutePath() : subDescription;
        } else if (path.contains(volume.mPath))
            subDescription = path.substring(volume.mPath.length());

        return new Pair<>(rootDescription, new File(rootDescription, subDescription).getAbsolutePath());
    }

    public static SFile getAppRoot(Context context) {
        String persistPath = getPersistPath(context);
        return SFile.isDocumentUri(persistPath) ? SFile.create(SFile.create(DocumentFile.fromTreeUri(context, Uri.parse(persistPath))), WWUtils.getAppRootDirName(context)) : SFile.create(persistPath);
    }

    public static String getPrivateRootName(Context context, String path) {
        File file = FileUtils.getPrivateExtAppDir(context, path);
        if (file != null && file.exists()) {
            file = new File(file, WWUtils.getAppRootDirName(context));
            return file.getAbsolutePath().substring(path.length());
        }
        return "/Android/data/" + context.getPackageName() + "/" + WWUtils.getAppRootDirName(context);
    }

    public static boolean isOverMarshmallow() {
        return Build.VERSION.SDK_INT >= AndroidHelper.ANDROID_VERSION_CODE.MARSHMALLOW;
    }

    /***
     * Generates the path to an application's cache.
     * These code is from AOSP, because this method getExternalStorageAppCacheDirectory is not existed on Android 4.4
     */
    public static File getExternalStorageAppCacheDirectory(String packageName) {
        File EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY = new File(new File(getDirectory(
                "EXTERNAL_STORAGE", Environment.getExternalStorageDirectory().getPath()), "Android"), "data");
        return new File(new File(EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY, packageName), "cache");
    }

    private static File getDirectory(String variableName, String defaultPath) {
        String path = System.getenv(variableName);
        return path == null ? new File(defaultPath) : new File(path);
    }

    private static boolean isUsePrivateDir(Context context, StorageVolumeHelper.Volume volume) {
        if (Build.VERSION.SDK_INT < 23)
            return !volume.mWritable;
        else
            return PermissionsUtils.hasStoragePermission(context) && !volume.mWritable;
    }
}
