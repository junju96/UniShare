package com.ushareit.core.utils;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.StreamUtils;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.lang.DynamicValue;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.os.AndroidHelper;
import com.ushareit.core.stats.Stats;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//TODO liufs: remove quietInstall methods, and make some changes to PackageUtils
public final class PackageUtils {
    private static final String TAG = "PackageUtils";

    private static DynamicValue sLastInstalledPkgs = new DynamicValue(new ArrayList<PackageInfo>(), true, 15*60*1000);
    private static StringBuilder sGetInstalledHistory = new StringBuilder();
    private static boolean sCollectedGetInstalledErr = false;
    private static long sLastGetInstalledHistoryTime = 0;

    private PackageUtils() {}

    public static class Classifier {

        private static final String TAG = "PackageClassifier";
        private static final String[] CLONE_ABLED_VERSIONS = { "5.", "6.", "7.", "8.", "9." };
        private static int[] mGames = null;
        private static int[] mExtGames = null;
        private static PackageManager mPackageManager = null;
        private static String[] mNativeApps = {
                // Google android system applications
                "com.android.browser",
                "com.android.calculator",
                "com.android.calculator2",
                "com.android.calendar",
                "com.android.contacts",
                "com.android.email",
                "com.android.gallery3d",
                "com.android.mms",
                "com.android.music",
                "com.android.settings",
                "com.android.soundrecorder",
                "com.android.videoeditor",
                "com.android.quicksearchbox",
                "com.android.task",
                "com.android.stk",
                "com.android.camera",
                "com.android.deskclock",
                "com.android.development",
                "com.cooliris.media",

                // Lenovo or 3rd developed system applications
                "com.mediatek.FMRadio",
                "com.mediatek.bluetooth",
                "com.mtk.telephony",
                "com.lenovo.fm",
                "com.mediatek.StkSelection",
                "com.lenovo.email",
                "com.lenovo.music",
                "com.lenovo.videoplayer",
                "com.lenovo.app.Calendar",
                "com.lenovo.ideafriend",
                "com.lenovo.launcher",
                "com.lenovo.wo3g",
                "com.mediatek.wo3g",
                "com.lenovo.android.settings.tether",
                "com.lenovomobile.deskclock",
        };
        // If these applications are system app, still list them when filter system app
        private static String[] mWhiteSystemApps = {
                "com.tencent.",
                "com.sina.",
                "com.baidu.",
                "com.sohu.",
                "com.lenovo.launcher",
                "com.lenovo.safecenter",
        };

        public static void init(final Context context) {
            TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("TS.PackageClassifier.init") {
                @Override
                public void execute() {
                    if (mPackageManager == null)
                        mPackageManager = context.getPackageManager();
                }
            });
        }

        public static AppCategoryType getCategoryType(Context ctx, PackageInfo pkgInfo) {
            if (mPackageManager == null)
                mPackageManager = ctx.getPackageManager();

            String packageName = pkgInfo.packageName;
            boolean isWidget = false;
            if (mPackageManager.getLaunchIntentForPackage(packageName) == null) {
                List<String> widgetIds = listWidgetIds(ctx);
                isWidget = widgetIds.contains(packageName);
            }

            return getPackageCategoryType(ctx, packageName, isWidget);
        }

        public static AppCategoryType getPackageCategoryType(Context ctx, String packageName, boolean isWidget) {
            for (String systemApp : mNativeApps) {
                if (systemApp.equals(packageName)) {
                    if (packageName.equals("com.lenovo.launcher")) {
                        return localLauncherSupportClone(ctx) ? AppCategoryType.APP : AppCategoryType.NATIVE_APP;
                    }

                    return AppCategoryType.NATIVE_APP;
                }
            }

            int hash = packageName.hashCode();
            if ((mGames != null && Arrays.binarySearch(mGames, hash) >= 0) || (mExtGames != null && Arrays.binarySearch(mExtGames, hash) >= 0))
                return AppCategoryType.GAME;
            else if (isWidget)
                return AppCategoryType.WIDGET;
            else
                return AppCategoryType.APP;
        }

        public static boolean isWhiteSystemApp(String packageName) {
            for (String app : mWhiteSystemApps) {
                if (packageName.contains(app))
                    return true;
            }
            return false;
        }

        public static boolean launcherVersionNameSupportClone(String versionName) {
            for (String cloneAbledVersion : CLONE_ABLED_VERSIONS) {
                if (versionName.startsWith(cloneAbledVersion))
                    return true;
            }
            return false;
        }

        public static boolean localLauncherSupportClone(Context ctx) {
            PackageInfo packageInfo = Extractor.getPackageInfo(ctx, "com.lenovo.launcher");
            if (packageInfo == null)
                return false;

            return launcherVersionNameSupportClone(packageInfo.versionName);
        }

        public static enum AppCategoryType {
            GAME(0), NATIVE_APP(1), APP(2), WIDGET(3);

            private int mValue;

            AppCategoryType(int value) {
                mValue = value;
            }

            private static SparseArray<AppCategoryType> mValues = new SparseArray<AppCategoryType>();

            static {
                for (AppCategoryType item : AppCategoryType.values())
                    mValues.put(item.mValue, item);
            }

            public static AppCategoryType fromInt(int value) {
                return mValues.get(Integer.valueOf(value));
            }

            public int toInt() {
                return mValue;
            }
        }
    }

    public synchronized static List<PackageInfo> getInstalledPackages(Context context, int flags, String from) {
        return getInstalledPackages(context, flags, from, false);
    }
    // Add synchronized to avoid occur TransactionTooLargeException when several threads call this API at the same time
    public synchronized static List<PackageInfo> getInstalledPackages(Context context, int flags, String from, boolean isForced) {
        try {
            long originLastTime = sLastGetInstalledHistoryTime;
            sLastGetInstalledHistoryTime = System.currentTimeMillis();
            long interval = originLastTime == 0 ? 0L : (sLastGetInstalledHistoryTime - originLastTime) / 1000;
            sGetInstalledHistory.append(from + "-" + interval + " ");
            if (flags != 0 || sLastInstalledPkgs.isNeedUpdate() || isForced) {
                List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(flags);
                if (flags == 0 && packageInfos != null)
                    sLastInstalledPkgs.updateValue(packageInfos);
                return packageInfos;
            } else
                return (List<PackageInfo>)sLastInstalledPkgs.getObjectValue();
        } catch (Throwable th) {
            statsPackageManagerError(th.getMessage());
            sGetInstalledHistory = new StringBuilder();
            if (flags == 0)
                return (List<PackageInfo>)sLastInstalledPkgs.getObjectValue();
            else
                return new ArrayList<PackageInfo>();
        }
    }

    public static List<String> listWidgetIds(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        List<AppWidgetProviderInfo> widgets = wm.getInstalledProviders();
        List<String> widgetIds = new ArrayList<String>();
        for (AppWidgetProviderInfo widget : widgets)
            widgetIds.add(widget.provider.getPackageName());
        return widgetIds;
    }

    public static boolean odexFileExist(String path) {
        SFile odexFile = SFile.create(path.replace(".apk", ".odex"));
        if (odexFile != null && odexFile.exists())
            return true;
        String[] androidLPath = { "/arm/", "/arm64/", "/x86/", "/x86_64/" };
        SFile apk = SFile.create(path);
        SFile parent = apk.getParent();
        // if the path is not parent/xxx/, return false
        if (!parent.getAbsolutePath().contains(FileUtils.getBaseName(path)))
            return false;

        // for Android L odex, the odex file is /system/app/xxx/arm/xxx.odex
        if (parent != null && parent.exists()) {
            for (String lpath : androidLPath) {
                SFile file = SFile.create(parent.getAbsolutePath() + lpath + apk.getName().replace(".apk", ".odex"));
                if (file != null && file.exists())
                    return true;
            }
        }
        return false;
    }

    public static boolean isForeground() {
        boolean foreground = getAppRunningStatus(ObjectStore.getContext()) == RUN_STATUS_FOREGROUND;
        Logger.d("tangbin", "isForeGround : " + foreground);
        return foreground;
    }

    public static final int RUN_STATUS_FOREGROUND = 1;
    public static final int RUN_STATUS_BACKGROUND = 0;
    public static final int RUN_STATUS_FAILED = -1;
    /**
     *
     * @param context
     * @return 1 means app is foreground; 0 means app is running background; -1 means exception failed.
     */
    public static int getAppRunningStatus(Context context) {
        try {
            Assert.notNull(context);
            String packageName = context.getPackageName();

            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT <= AndroidHelper.ANDROID_VERSION_CODE.KITKAT) {
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                return (tasks != null && !tasks.isEmpty() && packageName.equalsIgnoreCase(tasks.get(0).topActivity.getPackageName())) ? RUN_STATUS_FOREGROUND : RUN_STATUS_BACKGROUND;
            }

            Field field = null;
            try {
                field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
            } catch (Exception e) {
                Logger.e(TAG, "getField processState exception", e);
            }
            if (field == null)
                return RUN_STATUS_FAILED;
            List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
            if (appList == null || appList.isEmpty())
                return RUN_STATUS_BACKGROUND;
            // this value get by ActivityManager.PROCESS_STATE_TOP;
            final int PROCESS_STATE_TOP = 2;
            for (ActivityManager.RunningAppProcessInfo app : appList) {
                if (app.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
                    continue;
                Integer state = null;
                try {
                    state = field.getInt(app);
                } catch (Exception e) {}
                if (state == null || state != PROCESS_STATE_TOP)
                    continue;

                return TextUtils.equals(app.processName, packageName) ? RUN_STATUS_FOREGROUND : RUN_STATUS_BACKGROUND;
            }
            return RUN_STATUS_BACKGROUND;
        } catch (Exception e) {
            Logger.e(TAG, "getAppRunningStatus failed!", e);
            return RUN_STATUS_FAILED;
        }

    }

    /**
     *
     * @param context
     * @return top activity class name of current app
     */
    public static String getCurrentTopActivity(Context context) {
        try {
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null)
                return null;

            String topActivityName = null;
            List<ActivityManager.RunningTaskInfo> runningTask = am.getRunningTasks(1);
            if (runningTask != null && runningTask.size() > 0) {
                ActivityManager.RunningTaskInfo taskTop = runningTask.get(0);
                ComponentName componentTop = taskTop.topActivity;
                if (!TextUtils.equals(context.getPackageName(), componentTop.getPackageName()))
                    return null;

                topActivityName = componentTop.getClassName();
            }
            return topActivityName;
        } catch (Exception e) {
            Logger.e(TAG, "getCurrentTopActivity failed!", e);
            return null;
        }

    }

    private static final String SEN_ERR_ABOUT_PCK_MANAGER = "ERR_AboutPackageManager";

    private static void statsPackageManagerError(final String error) {
        try {
            HashMap<String, String> params = new LinkedHashMap<String, String>();
            params.put("error", error);
            if (!sCollectedGetInstalledErr) {
                params.put("history", sGetInstalledHistory.toString().trim());
                sCollectedGetInstalledErr = true;
            } else
                params.put("history", null);
            Stats.onEvent(ObjectStore.getContext(), SEN_ERR_ABOUT_PCK_MANAGER, params);
        } catch (Throwable t) {
        }
    }

    public static List<ResolveInfo> getAppListByType(Context context, String data, String type) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(data), type);
            PackageManager pManager = context.getPackageManager();
            return pManager.queryIntentActivities(intent, 0);

        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isDefaultOpenApp(Context context, String packageName, String action, String type) {
        final IntentFilter filter = new IntentFilter(action);
        try {
            filter.addDataType(type);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        List<ComponentName> preferredActivities = new ArrayList<ComponentName>();
        final PackageManager packageManager = context.getPackageManager();
        packageManager.getPreferredActivities(filters, preferredActivities, packageName);

        try {
            if (preferredActivities != null && filters != null) {
                for (int i = 0; i < preferredActivities.size(); i++) {
                    if (packageName != null &&
                            packageName.equals(preferredActivities.get(i).getPackageName()) &&
                            filters.get(i).getDataType(i).contains(type)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {

        }
        return false;
    }

    public final static int AAB_TYPE_A64 = 2;
    public final static int AAB_TYPE_A32 = 1;
    public final static int AAB_TYPE_NONE = 0;
    // get lib so abi feature type from splitSourceDirs, if not match any, return AAB_TYPE_A32;
    public static int getDynamicAppType(String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return AAB_TYPE_NONE;

        String pkgName = TextUtils.isEmpty(packageName) ? ObjectStore.getContext().getPackageName() : packageName;
        PackageManager pm = ObjectStore.getContext().getPackageManager();
        PackageInfo pkgInfo;
        try {
            pkgInfo = pm.getPackageInfo(pkgName, 0);
        } catch (NameNotFoundException e) {
            Logger.d(TAG, "package name:" + pkgName + " is not found!");
            return AAB_TYPE_NONE;
        }

        return getDynamicAppType(Arrays.asList(pkgInfo.applicationInfo.splitSourceDirs));
    }

    // get lib so abi feature type from splitSourceDirs, if not match any, return AAB_TYPE_A32;
    public static int getDynamicAppType(List<String> splitSourceDirs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return AAB_TYPE_NONE;

        if (splitSourceDirs == null || splitSourceDirs.isEmpty())
            return AAB_TYPE_NONE;

        for (String path : splitSourceDirs) {
            if (path != null && path.contains("arm64"))
                return AAB_TYPE_A64;
        }

        return AAB_TYPE_A32;
    }

    public static List<String> listDynamicAppDirs(String packageName) {
        List<String> dirs = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return dirs;

        String pkgName = TextUtils.isEmpty(packageName) ? ObjectStore.getContext().getPackageName() : packageName;
        PackageManager pm = ObjectStore.getContext().getPackageManager();
        PackageInfo pkgInfo;
        try {
            pkgInfo = pm.getPackageInfo(pkgName, 0);
        } catch (NameNotFoundException e) {
            Logger.d(TAG, "package name:" + pkgName + " is not found!");
            return dirs;
        }
        dirs.add(pkgInfo.applicationInfo.sourceDir);
        dirs.addAll(Arrays.asList(pkgInfo.applicationInfo.splitSourceDirs));
        return dirs;
    }

    public static boolean isSupportDynamicApp() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static class Extractor {

        private static final String TAG = "PackageExtractor";

        public static PackageInfo getPackageInfo(Context context, String packageName) {
            try {
                return context.getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                return null;
            }
        }

        // Request uninstall apk file package information through apk file path.
        // But this PackageInfo doesn't contain icon and label information as they are need loaded from resources.
        public static PackageInfo getPackageInfoByPath(Context context, String path) {
            try {
                PackageManager pm = context.getPackageManager();
                return pm.getPackageArchiveInfo(path, PackageManager.GET_CONFIGURATIONS);
            } catch (Throwable e) {
                return null;
            }
        }

        // Request uninstall apk file icon through apk file path.
        public static Drawable getPackageIconByPath(Context context, String path) {
            PackageInfo info = getPackageInfoByPath(context, path);
            if (info == null || info.applicationInfo == null || info.applicationInfo.icon <= 0)
                return null;

            try {
                Resources resource = getPackageResourcesByPath(context, path);
                if (resource == null)
                    return null;

                Drawable icon = resource.getDrawable(info.applicationInfo.icon);
                if (icon == null)
                    icon = info.applicationInfo.loadIcon(context.getPackageManager());
                return icon;
            } catch (Exception e) {
            }
            return null;
        }

        // Request uninstall apk label through apk file path.
        public static String getPackageLableByPath(Context context, String path, PackageInfo info) {
            if (info == null)
                return null;

            if (info.applicationInfo == null)
                return info.packageName;

            if (info.applicationInfo.nonLocalizedLabel != null)
                return info.applicationInfo.nonLocalizedLabel.toString();

            try {
                if (info.applicationInfo.labelRes != 0) {
                    Resources resource = getPackageResourcesByPath(context, path);
                    if (resource != null)
                        return resource.getText(info.applicationInfo.labelRes).toString().trim();
                }
            } catch (Exception e) {}

            if (info.applicationInfo.name != null)
                return info.applicationInfo.name.toString();

            return info.packageName;
        }

        // Request uninstall apk file icon through apk file path.
        private static Resources getPackageResourcesByPath(Context context, String path) {
            try {
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.setToDefaults();

                Class<?> amClass = Class.forName("android.content.res.AssetManager");
                Object am = amClass.newInstance();
                Class<?>[] paramTypes = new Class[1];
                paramTypes[0] = String.class;
                Method addAssetPathMethod = amClass.getDeclaredMethod("addAssetPath", paramTypes);
                Object[] paramValues = new Object[1];
                paramValues[0] = path;
                addAssetPathMethod.invoke(am, paramValues);

                Resources res = context.getResources();
                paramTypes = new Class[3];
                paramTypes[0] = am.getClass();
                paramTypes[1] = res.getDisplayMetrics().getClass();
                paramTypes[2] = res.getConfiguration().getClass();
                Constructor<Resources> resourcesConstructor = Resources.class.getConstructor(paramTypes);
                paramValues = new Object[3];
                paramValues[0] = am;
                paramValues[1] = res.getDisplayMetrics();
                paramValues[2] = res.getConfiguration();

                return (Resources)resourcesConstructor.newInstance(paramValues);
            } catch (Exception e) {
                Logger.d(TAG, e.getMessage());
                return null;
            }
        }
    }

    /**
     * inject extra file to signed android apk without corrupt its signature.
     */
    public static class Injector {

        public static final String REFERER_FILE = "META-INF/REFERER.TXT";
        public static final String EMBEDDED_APK = "META-INF/EMBEDDED.APK";
        private static final String TAG = "PackageInjector";

        /**
         * check if specified apk contains referer info.
         * @param apkFilePath
         * @return
         */
        public static boolean hasRefererInfo(String apkFilePath) {
            return (getJarEntry(apkFilePath, REFERER_FILE) != null);
        }

        /**
         * inject referer info to specified apk file.
         * @param oldApkFilePath
         * @param newApkFilePath
         * @param referer the referer info as string
         * @throws IOException
         */
        public static void injectRefererInfo(SFile oldApkFilePath, SFile newApkFilePath, String referer) throws IOException {
            append(oldApkFilePath, newApkFilePath, REFERER_FILE, referer.getBytes("UTF-8"));
        }

        /**
         * extract referer info from specified apk file.
         * @param apkFilePath
         * @return return empty if no referer info exists, otherwise the referer info as string.
         * @throws IOException
         */
        public static String extractRefererInfo(String apkFilePath) throws IOException {
            // this is slow because using only ZipFile
            // return new String(extract(apkFilePath,REFERER_FILE));

            // this is quick because using JarFile
            return new String(extractFromJar(apkFilePath, REFERER_FILE));
        }

        /**
         * check if specified apk contains embeded apk.
         * @param apkFilePath
         * @return
         */
        public static boolean hasEmbededApk(String apkFilePath) {
            return (getJarEntry(apkFilePath, EMBEDDED_APK) != null);
        }

        /**
         * get embedded apk info from specified apk.
         * @param apkFilePath
         * @return the ZipEntry object that contains info about embedded apk, or null if not exists or error occurred.
         */
        public static ZipEntry getEmbededApk(String apkFilePath) {
            return getJarEntry(apkFilePath, EMBEDDED_APK);
        }

        /**
         * extract embedded apk from specified apk file.
         * @param apkFilePath
         * @param targetFile the target file path to save this embedded apk
         * @return return false if no embedded apk exists, true if successfully saved to target file.
         * @throws IOException
         */
        public static boolean extractEmbededApk(String apkFilePath, String targetFile) throws IOException {
            return extractFromJar(apkFilePath, EMBEDDED_APK, targetFile);
        }

        /**
         * make an copy of existing zip file, and append an new file to new the copied zip file.
         * @param oldZipFilePath existing zip file full path
         * @param newZipFilePath new zip file full path, will overwriting if file already exists
         * @param newFileName the injected file name, note this is path relative to zip's root folder.
         * @param newFileContent the injected file's content, as a byte array.
         * @throws IOException
         */
        public static void append(SFile oldZipFilePath, SFile newZipFilePath, String newFileName, byte[] newFileContent) throws IOException {
            ZipInputStream zin = new ZipInputStream(oldZipFilePath.getInputStream());
            ZipOutputStream out = new ZipOutputStream(newZipFilePath.getOutputStream());

            byte[] buf = new byte[1024 * 16];

            // copy all old entries to new zip file, except the injected one (if already exists)
            ZipEntry oldEntry = zin.getNextEntry();
            while (oldEntry != null) {
                String name = oldEntry.getName();
                if (!name.equals(newFileName)) {
                    ZipEntry newEntry = (ZipEntry)oldEntry.clone();
                    out.putNextEntry(newEntry);
                    int len;
                    while ((len = zin.read(buf)) > 0)
                        out.write(buf, 0, len);
                    out.closeEntry();
                }
                oldEntry = zin.getNextEntry();
            }

            zin.close();

            // add injected file to new zip file
            ZipEntry injectEntry = new ZipEntry(newFileName);
            out.putNextEntry(injectEntry);
            out.write(newFileContent, 0, newFileContent.length);
            out.closeEntry();

            // complete new zip file
            out.close();
        }

        /**
         * extract an file's content as byte array from specified zip file.
         * to extract from jar file (apk,jar,war), use extractFromJar instead which is quicker.
         * @param zipFilePath the zip file full path
         * @param fileName the file name its contents should be extracted.
         * @return file's content as byte array.
         * @throws IOException
         */
        public static byte[] extract(String zipFilePath, String fileName) throws IOException {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFilePath));
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buf = new byte[1024 * 16];

            // copy all old entries to new zip file, except the injected one (if already exists)
            ZipEntry oldEntry = zin.getNextEntry();
            while (oldEntry != null) {
                String name = oldEntry.getName();
                if (name.equals(fileName)) {
                    int len;
                    while ((len = zin.read(buf)) > 0)
                        out.write(buf, 0, len);
                }
                oldEntry = zin.getNextEntry();
            }

            zin.close();
            return out.toByteArray();
        }

        /**
         * get jar entry info.
         * @param jarFilePath
         * @param entryName entry name, is the relative file path.
         * @return the jar entry info, null if entry not exists or error occured.
         */
        public static ZipEntry getJarEntry(String jarFilePath, String entryName) {
            try {
                JarFile jar = new JarFile(jarFilePath);
                try {
                    return jar.getEntry(entryName);
                } finally {
                    jar.close();
                }
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * extract an file's content as byte array from specified jar file.
         * @param jarFilePath the jar file full path
         * @param fileName the file name its contents should be extracted.
         * @return file's content as byte array.
         * @throws IOException
         */
        public static byte[] extractFromJar(String jarFilePath, String fileName) throws IOException {
            JarFile jar = new JarFile(jarFilePath);
            try {
                ZipEntry entry = jar.getEntry(fileName);
                if (entry != null) {
                    InputStream in = jar.getInputStream(entry);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024 * 16];

                    // copy all old entries to new zip file, except the injected one (if already exists)
                    int len;
                    while ((len = in.read(buf)) > 0)
                        out.write(buf, 0, len);
                    in.close();
                    return out.toByteArray();
                }
            } finally {
                jar.close();
            }
            return new byte[0];
        }

        /**
         * extract an file's content from jar file to specified target file.
         * @param jarFilePath the jar file full path
         * @param fileName the file name its contents should be extracted.
         * @param targetFile the target file path to save extracted file.
         * @return true if successfully extracted, false if specified file doesn't exists in jar.
         * @throws IOException
         */
        public static boolean extractFromJar(String jarFilePath, String fileName, String targetFile) throws IOException {
            JarFile jar = new JarFile(jarFilePath);
            try {
                ZipEntry entry = jar.getEntry(fileName);
                if (entry == null)
                    return false;

                InputStream in = jar.getInputStream(entry);
                SFile target = SFile.create(targetFile);
                StreamUtils.writeStreamToFile(in, target);
                return true;
            } finally {
                jar.close();
            }
        }

        /**
         * dump zip file entries to system logger.
         * @param zipFile
         */
        public static void dump(SFile zipFile) {
            try {
                ZipInputStream zin = new ZipInputStream(zipFile.getInputStream());
                ZipEntry entry = zin.getNextEntry();
                while (entry != null) {
                    Logger.d(TAG, "entry: dir = " + entry.isDirectory() + ", name = " + entry.getName() + ", size = " + entry.getSize() + " / " + entry.getCompressedSize());
                    entry = zin.getNextEntry();
                }
                zin.close();
            } catch (Exception e) {
                Logger.w(TAG, e);
            }
        }

        // NOTE: not fully tested
        public static void addFiles(SFile oldZipFile, SFile[] files, SFile newZipFile) throws Exception {
            byte[] buf = new byte[1024];
            ZipInputStream zin = new ZipInputStream(oldZipFile.getInputStream());
            ZipOutputStream out = new ZipOutputStream(newZipFile.getOutputStream());

            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                boolean notInFiles = true;
                for (SFile f : files) {
                    if (f.getName().equals(name)) {
                        notInFiles = false;
                        break;
                    }
                }

                if (notInFiles) {
                    // Add ZIP entry to output stream.
                    out.putNextEntry((ZipEntry)entry.clone());
                    // Transfer bytes from the ZIP file to the output file
                    int len;
                    while ((len = zin.read(buf)) > 0)
                        out.write(buf, 0, len);
                    out.closeEntry();
                }

                entry = zin.getNextEntry();
            }

            // Close the streams
            zin.close();

            // Compress the files
            for (int i = 0; i < files.length; i++) {
                InputStream in = files[i].getInputStream();
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry("META-INF/" + files[i].getName()));
                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                // Complete the entry
                out.closeEntry();
                in.close();
            }
            // Complete the ZIP file
            out.close();
        }
    }

}
