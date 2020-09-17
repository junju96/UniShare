package com.ushareit.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.ushareit.core.Logger;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.stats.Stats;
import com.ushareit.core.utils.device.DevBrandUtils;
import com.ushareit.core.utils.i18n.LocaleUtils;

public final class AppStarter {
    private static final String TAG = "AppStarter";

    private static final String MARKET_URI_NATIVE_FORMAT = "market://details?id=%s";
    private static final String MARKET_URI_WEB_FORMAT = "https://play.google.com/store/apps/details?id=%s";
    private static final String MARKET_URI_NATIVE_REFERRER_FORMAT = "market://details?id=%s&%s";
    private static final String MARKET_URI_WEB_REFERRER_FORMAT = "https://play.google.com/store/apps/details?id=%s&%s";

    public static void startAppMarketDefault(Context context, String packageName, String referrer, String utmSource, String utmMedium, boolean forceUseGoogle) {
        if (!TextUtils.isEmpty(referrer))
            startAppMarketWithReferrer(context, packageName, referrer, forceUseGoogle);
        else
            startAppMarket(context, packageName, utmSource, utmMedium, forceUseGoogle);
    }

    public static void startAppMarket(Context context, String packageName, String utmSource, String utmMedium, boolean forceUseGoogle) {
        try {
            String utmMediaUri = TextUtils.isEmpty(utmMedium) ? "" : "%26utm_medium%3D" + utmMedium + "%26utm_campaign%3D" + utmMedium;
            String uri = TextUtils.isEmpty(utmSource)
                    ? LocaleUtils.formatStringIgnoreLocale(MARKET_URI_NATIVE_FORMAT, packageName)
                    : LocaleUtils.formatStringIgnoreLocale(MARKET_URI_NATIVE_REFERRER_FORMAT, packageName, "referrer=utm_source%3D" + utmSource + utmMediaUri);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            if (forceUseGoogle || hasActivity(context, intent, "com.android.vending")) {
                intent.setPackage("com.android.vending");
                if (DevBrandUtils.MIUI.isMIUI() && hasGPAppDetailActivity(context, uri, "com.android.vending"))
                    intent.setClassName("com.android.vending", "com.google.android.finsky.activities.MainActivity");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            collectStartAppMarket(context, uri, packageName, false);
        } catch (Exception e) {
            try {
                String utmMediaUri = TextUtils.isEmpty(utmMedium) ? "" : "%26utm_medium%3D" + utmMedium + "%26utm_campaign%3D" + utmMedium;
                String uri = TextUtils.isEmpty(utmSource)
                        ? LocaleUtils.formatStringIgnoreLocale(MARKET_URI_WEB_FORMAT, packageName)
                        : LocaleUtils.formatStringIgnoreLocale(MARKET_URI_WEB_REFERRER_FORMAT, packageName, "referrer=utm_source%3D" + utmSource + utmMediaUri);
                startBrowserNoChoice(context, uri, true);
                collectStartAppMarket(context, uri, packageName, true);
            } catch (Exception e1) {}
        }
    }

    public static void startAppMarketWithReferrer(Context context, String packageName, String param, boolean forceUseGoogle) {
        try {
            String uri = TextUtils.isEmpty(param)
                    ? LocaleUtils.formatStringIgnoreLocale(MARKET_URI_NATIVE_FORMAT, packageName)
                    : LocaleUtils.formatStringIgnoreLocale(MARKET_URI_NATIVE_REFERRER_FORMAT, packageName, param);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            if (forceUseGoogle || hasActivity(context, intent, "com.android.vending"))
                intent.setPackage("com.android.vending");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            collectStartAppMarket(context, uri, packageName, false);
        } catch (Exception e) {
            try {
                String uri = TextUtils.isEmpty(param)
                        ? LocaleUtils.formatStringIgnoreLocale(MARKET_URI_WEB_FORMAT, packageName)
                        : LocaleUtils.formatStringIgnoreLocale(MARKET_URI_WEB_REFERRER_FORMAT, packageName, param);
                startBrowserNoChoice(context, uri, true);
                collectStartAppMarket(context, uri, packageName, true);
            } catch (Exception e1) {}
        }
    }

    public static void startAppMarketWithUrl(Context context, String url, String packageName, boolean forceUseGoogle) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (forceUseGoogle || hasActivity(context, intent, "com.android.vending"))
                intent.setPackage("com.android.vending");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            collectStartAppMarket(context, url, packageName, false);
        } catch (Exception e) {
            try {
                Logger.d(TAG,"startAppMarketWithUrl startBrowserNoChoice");
                startBrowserNoChoice(context, url, true);
                collectStartAppMarket(context, url, packageName, true);
            } catch (Exception e1) {}
        }
    }

    public static void startFacebook(Context context, String shareId, String appServerPath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + shareId));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            startBrowserNoChoice(context, "https://www.facebook.com/" + appServerPath, true);
        }
    }

    private static List<String> mBrowserPackages = new ArrayList<String>();
    static {
        mBrowserPackages.add("com.android.chrome");
        mBrowserPackages.add("com.android.browser");
        mBrowserPackages.add("com.sec.android.app.sbrowser");
        mBrowserPackages.add("com.opera.browser");
        mBrowserPackages.add("com.opera.mini.android");
        mBrowserPackages.add("com.opera.mini.native");
        mBrowserPackages.add("com.UCMobile");
        mBrowserPackages.add("com.UCMobile.intl");
        mBrowserPackages.add("com.uc.browser.en");
        mBrowserPackages.add("com.UCMobile.internet.org");
        mBrowserPackages.add("com.uc.browser.hd");
        mBrowserPackages.add("org.mozilla.firefox");
        mBrowserPackages.add("com.tencent.mtt");
        mBrowserPackages.add("com.qihoo.browser");
        mBrowserPackages.add("com.baidu.browser.apps");
        mBrowserPackages.add("sogou.mobile.explorer");
        mBrowserPackages.add("com.zui.browser");
        mBrowserPackages.add("com.oupeng.browser");
        mBrowserPackages.add("com.oupeng.mini.android");
    }

    public static boolean startBrowserNoChoice(Context context, String url, boolean newTask) {
        return startBrowserNoChoice(context, url, newTask, 0);
    }

    public static boolean startBrowserNoChoice(Context context, String url, boolean newTask, int failedResId) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (newTask)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return startActivityNoChoice(context, intent, failedResId, mBrowserPackages);
    }

    public static boolean startBrowserNoChoice(Context context, Intent intent, int failedResId) {
        return startActivityNoChoice(context, intent, failedResId, mBrowserPackages);
    }

    public static boolean startActivityNoChoice(Context context, Intent intent) {
        return startActivityNoChoice(context, intent, 0);
    }

    public static boolean startActivityNoChoice(Context context, Intent intent, int failedResId) {
        return startActivityNoChoice(context, intent, failedResId, null);
    }

    private static boolean startActivityNoChoice(Context context, Intent intent, int failedResId, List<String> recommendPackages) {
        boolean success = false;

        try {
            PackageManager pm = context.getPackageManager();
            ResolveInfo resolve = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            List<ResolveInfo> appList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            ResolveInfo startResolve = null;
            if (!hasDefaultActivity(resolve, appList)) {
                startResolve = getResolveInfo(pm, appList, recommendPackages);
            }
            if (startResolve != null) {
                intent.setPackage(startResolve.activityInfo.packageName);
            }
            try {
                context.startActivity(intent);
                success = true;
            } catch (Exception ae) {}
        } catch (Exception e) {
            Logger.d(TAG, e.toString());
        }

        if (!success && failedResId > 0)
            Toast.makeText(context, failedResId, Toast.LENGTH_SHORT).show();
        return success;
    }

    public static boolean hasDefaultAppMarket(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()));
        return hasDefaultActivity(context, intent);
    }

    public static boolean hasDefaultBrowser(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ushareit.com"));
        return hasDefaultActivity(context, intent);
    }

    public static boolean hasDefaultActivity(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        List<ResolveInfo> appList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return hasDefaultActivity(resolved, appList);
    }

    // If the list contains the above resolved activity, then it can't be
    // ResolverActivity itself.
    public static boolean hasDefaultActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
        if (resolved == null || appList == null || appList.size() < 1)
            return false;
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo tmp = appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name)
                    && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private static ResolveInfo getResolveInfo(PackageManager pm, List<ResolveInfo> appList, List<String> recommendPackages) {
        if (appList != null && appList.size() > 0) {
            if (recommendPackages != null && appList.size() > 1)
                Collections.sort(appList, new ResolveComparator(recommendPackages));
            return appList.get(0);
        }

        return null;
    }

    private static boolean hasActivity(Context context, Intent intent, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> appList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo info : appList) {
            if (info.activityInfo.packageName.equals(packageName))
                return true;
        }
        return false;
    }

    private static boolean hasGPAppDetailActivity(Context context, String uri, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setClassName("com.android.vending", "com.google.android.finsky.activities.MainActivity");
            return hasActivity(context, intent, packageName);
        } catch(Throwable e) {}

        return false;
    }

    private static class ResolveComparator implements Comparator<ResolveInfo> {
        private List<String> mRecommendPackages;

        public ResolveComparator(List<String> recommendPackages) {
            mRecommendPackages = recommendPackages;
        }

        public int compare(ResolveInfo info1, ResolveInfo info2) {
            int priority1 = mRecommendPackages.contains(info1.activityInfo.packageName)
                    ? mRecommendPackages.indexOf(info1.activityInfo.packageName)
                    : mRecommendPackages.size();
            int priority2 = mRecommendPackages.contains(info2.activityInfo.packageName)
                    ? mRecommendPackages.indexOf(info2.activityInfo.packageName)
                    : mRecommendPackages.size();

            return priority1 - priority2;
        }
    };

    public static final String SEN_START_APP_MARKET = "StartAppMarket";

    public static void collectStartAppMarket(Context context, String url, String pkgName, boolean isBrowser) {
        if (context == null)
            return;

        HashMap<String, String> info = new LinkedHashMap<String, String>();
        info.put("url", url);
        info.put("pkg_name", pkgName);
        info.put("start_way", isBrowser ? "browser" : "market_app");

        Logger.d(TAG, "collectStartAppMarket: " + info.toString());
        Stats.onEvent(context, SEN_START_APP_MARKET, info);
    }

    public static boolean startSend(Context context, String path) {
        return startSend(context, path, null, null, null);
    }

    public static boolean startSend(Context context, String path, String mimeType, String specialPackage, Bundle extra) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);

            if (!TextUtils.isEmpty(mimeType))
                intent.setType(mimeType);
            else
                intent.setType("*/*");

            if (!TextUtils.isEmpty(specialPackage))
                intent.setPackage(specialPackage);

            if (!TextUtils.isEmpty(path)) {
                Uri uri = SFile.create(path).toUri();
                if (uri != null)
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
            }

            if (extra != null)
                intent.putExtras(extra);

            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean startSendMultiple(Context context, List<String> pathList) {
        return startSendMultiple(context, pathList, null, null, null);
    }

    public static boolean startSendMultiple(Context context, List<String> pathList, String mimeType, String specialPackage, Bundle extra) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

            if (!TextUtils.isEmpty(mimeType))
                intent.setType(mimeType);
            else
                intent.setType("*/*");

            if (!TextUtils.isEmpty(specialPackage))
                intent.setPackage(specialPackage);

            if (pathList != null && !pathList.isEmpty()) {
                ArrayList<Uri> uris = new ArrayList<Uri>();
                for (String path : pathList) {
                    Uri uri = SFile.create(path).toUri();
                    if (uri != null)
                        uris.add(uri);
                }
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }

            if (extra != null)
                intent.putExtras(extra);

            if(!(context instanceof Activity)){
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
