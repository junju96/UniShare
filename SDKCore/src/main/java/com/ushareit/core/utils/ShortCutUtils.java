package com.ushareit.core.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.ushareit.core.lang.ObjectStore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class ShortCutUtils {
    private static String mBufferedValue = null;

    /**
     * 快捷方式是否存在
     * @param context
     * @param title 名称
     * @param intent
     * @return 返回结果
     */
    public static boolean isShortCutExist(Context context, String title, Intent intent) {
        boolean result = false;
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = getUriFromLauncher(context);
            Cursor c = cr.query(uri, new String[]{"title", "intent"}, "title=?  and intent=?",
                    new String[]{title, intent.toUri(0)}, null);
            if (c != null && c.getCount() > 0) {
                result = true;
            }
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private static Uri getUriFromLauncher(Context context) {
        StringBuilder uriStr = new StringBuilder();
        String authority = getAuthorityFromPermissionDefault(context);
        if (authority == null || authority.trim().equals("")) {
            authority = getAuthorityFromPermission(context, getCurrentLauncherPackageName(context) + ".permission.READ_SETTINGS");
        }
        uriStr.append("content://");
        if (TextUtils.isEmpty(authority)) {
            int sdkInt = android.os.Build.VERSION.SDK_INT;
            if (sdkInt < 19) {
                uriStr.append("com.android.launcher2.settings");
            } else {
                uriStr.append("com.android.launcher3.settings");
            }
        } else {
            uriStr.append(authority);
        }
        uriStr.append("/favorites?notify=true");
        return Uri.parse(uriStr.toString());
    }

    public static String getAuthorityFromPermissionDefault(Context context) {
        if (TextUtils.isEmpty(mBufferedValue))
            mBufferedValue = getAuthorityFromPermission(context, "com.android.launcher.permission.READ_SETTINGS");
        return mBufferedValue;
    }

    public static String getAuthorityFromPermission(Context context, String permission) {
        if (TextUtils.isEmpty(permission)) {
            return "";
        }
        try {
            List<PackageInfo> packs = PackageUtils.getInstalledPackages(context, PackageManager.GET_PROVIDERS, "shortCut");
            if (packs == null) {
                return "";
            }
            for (PackageInfo pack : packs) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        if (permission.equals(provider.readPermission) || permission.equals(provider.writePermission)) {
                            return provider.authority;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return "";
    }

    public static String getCurrentLauncherPackageName(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
            if (res == null || res.activityInfo == null) {
                return "";
            }
            if (res.activityInfo.packageName.equals("android")) {
                return "";
            } else {
                return res.activityInfo.packageName;
            }
        } catch (Exception e) {}
        return "";
    }

    /**
     * 删除快捷方式
     * @param cx
     * @param nameResId 标题资源id
     * @param action
     * @param resId 图标资源id
     * @param className
     */
    public static void delShortcut(Context cx, int nameResId, String action, int resId, String className) {
        if(cx == null||nameResId == -1)
            return;

        Intent shortcut = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");
        String title = null;
        try {
            title = cx.getResources().getString(nameResId);
            Intent intent = new Intent();
            intent.setPackage(ObjectStore.getContext().getPackageName());
            intent.setClassName(ObjectStore.getContext().getPackageName(), className);
            intent.setAction(action);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, resId);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            cx.sendBroadcast(shortcut);
        } catch (Exception e) {
        }
    }

    public static void updateShortcutInAndroidO(Context context, String action, int nameResId, int iconResId) {
        try {
            if (TextUtils.isEmpty(action))
                return;

            Object shortCutService = context.getSystemService(Context.SHORTCUT_SERVICE);

            Class<?> shortcutManagerClazz = Class.forName("android.content.pm.ShortcutManager");
            Method getPinnedShortcutsMethod = shortcutManagerClazz.getDeclaredMethod("getPinnedShortcuts");
            List pinnedShortcuts = (List) getPinnedShortcutsMethod.invoke(shortCutService);

            Class<?> shortcutInfoClazz = Class.forName("android.content.pm.ShortcutInfo");
            Method getIntent = shortcutInfoClazz.getDeclaredMethod("getIntent");

            for (Object item : pinnedShortcuts) {
                Intent shortCutIntent = (Intent) getIntent.invoke(item);
                if (shortCutIntent == null)
                    continue;


                if (action.equalsIgnoreCase(shortCutIntent.getAction())) {

                    Object shortcutInfo = updateShortCutInfo(context, nameResId, iconResId, shortcutInfoClazz, item);
                    if (shortcutInfo == null)
                        continue;
                    pinnedShortcuts.remove(item);
                    pinnedShortcuts.add(shortcutInfo);

                    Method updateShortcuts = shortcutManagerClazz.getDeclaredMethod("updateShortcuts", List.class);
                    updateShortcuts.invoke(shortCutService, pinnedShortcuts);
                }

            }

        } catch (Exception e) {
        }
    }

    private static Object updateShortCutInfo(Context context, int nameResId, int iconResId, Class<?> shortcutInfoClazz, Object shortcutInfo) {
        try {
            Method getId = shortcutInfoClazz.getDeclaredMethod("getId");

            Class<?> clazz = Class.forName("android.content.pm.ShortcutInfo$Builder");
            Constructor constructor = clazz.getConstructor(new Class[]{Context.class, String.class});
            Object builder = constructor.newInstance(context, getId.invoke(shortcutInfo));

            Method setShortLabel = clazz.getDeclaredMethod("setShortLabel", CharSequence.class);
            String shortCutName = context.getString(nameResId);
            setShortLabel.invoke(builder, shortCutName);

            Class<?> iconClazz = Class.forName("android.graphics.drawable.Icon");
            Method createWithResource = iconClazz.getDeclaredMethod("createWithResource", Context.class, int.class);
            Object icon = createWithResource.invoke(null, context, iconResId);
            Method setIcon = clazz.getDeclaredMethod("setIcon", iconClazz);
            setIcon.invoke(builder, icon);

            Method setRank = clazz.getDeclaredMethod("setRank", int.class);
            setRank.invoke(builder, 0);

            Method build = clazz.getDeclaredMethod("build");
            return build.invoke(builder);


        } catch (Exception e) {

        }
        return null;
    }

    public static boolean hasInstallShortCut(Context context, String name) {
        boolean result = false;
        Cursor cursor = null;
        try {
            String url = "";
            if (Build.VERSION.SDK_INT < 8) {
                url = "content://com.android.launcher.settings/favorites?notify=true";
            } else {
                url = "content://com.android.launcher2.settings/favorites?notify=true";
            }
            ContentResolver resolver = context.getContentResolver();
            cursor = resolver.query(Uri.parse(url), null, "title=?", new String[] { name }, null);
            result = cursor != null && cursor.moveToFirst();
        } finally {
            Utils.close(cursor);
        }

        return result;
    }

    /**
     * install a activity shortcut. Don't check whether it is installed.
     * @param context application context object
     * @param intent shortcut's intent.
     * @param nameResId shortcut's display name's resource id
     * @param iconResId shortcut's display icon's resource id
     */
    public static void doInstallShortcut(Context context, Intent intent, String nameResId, int iconResId) {
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, nameResId);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(context, iconResId);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
        shortcut.putExtra("duplicate", false);
        context.sendBroadcast(shortcut);
    }

    public static void doInstallShortcutInAndroidO(Context context, Intent intent, IntentSender intentSender, String nameResId, int iconResId, String type) {
        try{
            Class<?> clazz = Class.forName("android.content.pm.ShortcutInfo$Builder");
            Constructor constructor = clazz.getConstructor(new Class[]{Context.class, String.class});
            Object builder = constructor.newInstance(context, type + "_ShortCut." + nameResId);

            Method setShortLabel = clazz.getDeclaredMethod("setShortLabel", CharSequence.class);
            String shortCutName = nameResId;
            setShortLabel.invoke(builder, shortCutName);

            Class<?> iconClazz = Class.forName("android.graphics.drawable.Icon");
            Method createWithResource = iconClazz.getDeclaredMethod("createWithResource", Context.class, int.class);
            Object icon = createWithResource.invoke(null, context, iconResId);

            Method setIcon = clazz.getDeclaredMethod("setIcon", iconClazz);
            setIcon.invoke(builder, icon);

            Method setRank = clazz.getDeclaredMethod("setRank", int.class);
            setRank.invoke(builder, 0);

            Method setIntent = clazz.getDeclaredMethod("setIntent", Intent.class);
            setIntent.invoke(builder, intent);

            Method build = clazz.getDeclaredMethod("build");
            Object shortcutInfo = build.invoke(builder);

            @SuppressLint("WrongConstant") Object shortCutService = context.getSystemService("shortcut");

            Class<?> shortcutInfoClazz = Class.forName("android.content.pm.ShortcutInfo");
            Class<?> shortcutManagerClazz = Class.forName("android.content.pm.ShortcutManager");
            Method requestPinShortcut = shortcutManagerClazz.getDeclaredMethod("requestPinShortcut", shortcutInfoClazz, IntentSender.class);
            requestPinShortcut.invoke(shortCutService, shortcutInfo, intentSender);
        } catch (Exception e) {}
    }
}
