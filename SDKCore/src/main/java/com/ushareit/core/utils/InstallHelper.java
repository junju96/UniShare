package com.ushareit.core.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Pair;

import com.ushareit.core.change.ChangeListenerManager;
import com.ushareit.core.change.ChangedKeys;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.StreamUtils;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.stats.Stats;
import com.ushareit.core.utils.cmd.RootUtils;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.RequiresApi;

public class InstallHelper {
    private static final String TAG = "InstallHelper";

    // App install status
    public static final int APP_STATUS_UNINSTALL = 0;           // app not installed
    public static final int APP_STATUS_INSTALLED = 1;           // app exist and is newest
    public static final int APP_STATUS_NEED_UPGRADE = 2;        // app need upgrade
    public static final int APP_STATUS_INSTALLING = 3;          // app is installing
    public static final int APP_STATUS_INSTALL_FAILED = 4;      // app install failed
    public static final int APP_STATUS_PENDING_USER_ACTION = -1;      // app install need user action
    // App install result
    public static final int INSTALL_PERMISSION_INVALID = -1;
    public static final int INSTALL_SUCCESS = 0;
    public static final int INSTALL_FAILED_UNEXPECTED_EXCEPTION = 1;
    public static final int INSTALL_FAILED_CONTAINER_ERROR = 2;
    public static final int INSTALL_FAILED_PACKAGE_UPDATE_ERROR = 3;
    public static final int INSTALL_FAILED_PACKAGE_INVALID = 4;
    public static final int INSTALL_FAILED_PACKAGE_CONTENT_ERROR = 5;
    public static final int INSTALL_FAILED_PACKAGE_CERTIFICATE_ERROR = 6;
    public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = 7;
    public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = 8;
    public static final int INSTALL_FAILED_UID_CHANGED = 9;
    public static final String ACTION_DYNAMIC_APP_INSTALL = "com.ushareit.package.action.install_completed";
    public static final String KEY_EXTRA_DYNAMIC_APP_PKG_NAME = "key_dynamic_app_pkg_name";
    private static List<String> mInstallingPkgs = new ArrayList<String>();

    public static boolean canQuietInstall() {
        return (RootUtils.getMaskNoWait() > RootUtils.PERMISSION_MASK_NONE);
    }

    public static int quietInstallPackage(Context context, String path) {
        mInstallingPkgs.add(path);
        int result = RootUtils.quietInstallPackage(context, path);
        mInstallingPkgs.remove(path);
        return result;
    }

    public static boolean isInQuietInstall(String path) {
        return mInstallingPkgs.contains(path);
    }

    public static void installPackage(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static boolean quietUnInstallPackage(Context context, String packageName) {
        if (!isAppInstalled(context, packageName))
            return false;

        if (RootUtils.quietUnInstallPackage(context, packageName))
            return isAppInstalled(context, packageName);

        return false;
    }

    public static void uninstallPackage(Context context, String packageName) {
        try {
            Uri packageURI = Uri.parse("package:" + packageName);
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            context.startActivity(uninstallIntent);
        } catch (Exception e) {
            Stats.onError(context, e);
        }
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isInstallFromGP (Context context, String packageName) {
        try {
            String installer = context.getPackageManager().getInstallerPackageName(packageName);
            return (StringUtils.isNotEmpty(installer) && installer.equals("com.android.vending"));
        } catch (Exception e) {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void installDynamicApp(final Context context, final String appPackageName, final String filePath, final String targetClassName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new IllegalStateException("Can not install dynamic app below Lolipop!");
        }

        TaskHelper.exec(new TaskHelper.Task() {
            PackageInstaller.Session session = null;
            int sessionId;

            @Override
            public void execute() throws Exception {

                ChangeListenerManager.getInstance().notifyChange(ChangedKeys.KEY_DYNAMIC_APP_INSTALL_STATUS, Pair.create(APP_STATUS_INSTALLING, appPackageName));
                PackageInstaller installer = context.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(appPackageName);
                sessionId = installer.createSession(params);
                session = installer.openSession(sessionId);
                File folder = new File(filePath);
                List<File> allApks = Arrays.asList(folder.listFiles());
                Collections.sort(allApks, new Comparator<File>() {
                    @Override
                    public int compare(File file, File t1) {
                        if (file.getName().equalsIgnoreCase("base.apk"))
                            return -1;
                        return 1;
                    }
                });
                for (File file : allApks) {
                    OutputStream os = session.openWrite(FileUtils.getBaseName(file.getName()), 0, file.length());
                    StreamUtils.writeFileToStream(SFile.create(file), os);
                    session.fsync(os);
                    Utils.close(os);
                }
                session.commit(createDefaultIntent(context, sessionId, targetClassName, appPackageName));
            }

            @Override
            public void callback(Exception e) {
                if (e != null) {
                    Logger.e(TAG, "install dynamic app failed!", e);
                    ChangeListenerManager.getInstance().notifyChange(ChangedKeys.KEY_DYNAMIC_APP_INSTALL_STATUS, Pair.create(APP_STATUS_INSTALL_FAILED, appPackageName));
                    return;
                }

            }
        });
    }

    private static IntentSender createDefaultIntent(Context context, int sessionId, String targetClassName, String pkgName) {
        Intent intent = new Intent(ACTION_DYNAMIC_APP_INSTALL);
        intent.setPackage(context.getPackageName());
        intent.setComponent(new ComponentName(context.getPackageName(), targetClassName));
        intent.putExtra(KEY_EXTRA_DYNAMIC_APP_PKG_NAME, pkgName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                0);
        return pendingIntent.getIntentSender();
    }

    // get the application install status in the device
    // APP_STATUS_UNINSTALL mean the application is not installed
    // APP_STATUS_INSTALLED mean this application is installed and is newest
    // APP_STATUS_NEED_UPGRADE mean application is installed but local is not newest, need upgrade
    public static int getAppStatus(Context context, String packageName, int versionCode) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            if (info.versionCode >= versionCode)
                return APP_STATUS_INSTALLED;
            else
                return APP_STATUS_NEED_UPGRADE;
        } catch (PackageManager.NameNotFoundException e) {
            return APP_STATUS_UNINSTALL;
        }
    }

    public static class PreInstalledHelper {

        private final static String TAG = "PreInstalledHelper";

        /*
        this method can't be executed in main thread
        */
        private static String mGuardTag = null;
        public static String getGuardTag () {
            if (mGuardTag != null)
                return mGuardTag;

            try {
                mGuardTag = getGuardTagFromFile();
            } catch (Throwable e) {}
            return mGuardTag;
        }

        private static String getGuardTagFromFile() {
            try {
                String path = "/system/etc/shareit/pre_installed";
                SFile file = SFile.create(path);
                if (!file.exists())
                    return "";

                return StreamUtils.readStringFromFile(file, 50);
            } catch (Throwable e) {
                Logger.d(TAG , "getGuardTagFromFile exception : ", e);
                return "";
            }
        }

    }
}
