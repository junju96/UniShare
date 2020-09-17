package com.ushareit.core.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.ushareit.core.Assert;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.utils.device.DeviceHelper;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * miscellaneous utilities methods that not belong to all specific utilities classes such as FileUtils/NumberUtils, etc.
 */
//TODO liufs: need refine
public final class Utils {
    private static final int BUFFER_SIZE_16K = 1024 * 16;

    private static String[] CHARS = new String[]{"a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z"};

    private Utils() {
    }

    public static boolean isActivityDestroy(Activity activity) {
        if (activity == null)
            return true;

        if (Build.VERSION.SDK_INT >= 17) {
            return activity.isDestroyed();
        }
        return activity.isFinishing();
    }

    // this will force import the java package which contains some class
    // eg: forceImportPackage(R.id.class) will force import package com.lenovo.anyshare.R
    public static void forceImportPackage(Class<?> clazz) {
    }

    public static void inputStreamToOutputStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE_16K];
        int r;
        while ((r = input.read(buffer)) != -1) {
            output.write(buffer, 0, r);
        }
    }

    // read everything in an input stream and return as string (trim-ed, and may apply optional utf8 conversion)
    public static String inputStreamToString(final InputStream is, final boolean sourceIsUTF8) throws IOException {
        InputStreamReader isr = sourceIsUTF8 ? new InputStreamReader(is, Charset.forName("UTF-8")) : new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line);
        br.close();
        return sb.toString().trim();
    }

    // url encode a string with UTF-8 encoding
    public static String urlEncode(String src) {
        try {
            return URLEncoder.encode(src, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    public static int readBuffer(InputStream input, byte[] buffer) throws IOException {
        return readBuffer(input, buffer, 0, buffer.length);
    }

    /**
     * inputstream 读取byte[] buffer不能保证�?��完整读取，使用本方法可以保证填满buffer
     *
     * @param input
     * @param buffer
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static int readBuffer(InputStream input, byte[] buffer, int offset, int length) throws IOException {
        int sum = 0;
        int r;
        while (length > 0 && (r = input.read(buffer, offset, length)) != -1) {
            sum += r;
            offset += r;
            length -= r;
        }
        return sum;
    }

    public static long max(long value1, long value2) {
        return (value1 > value2 ? value1 : value2);
    }

    public static String getSimCountryIso(Context ctx) {
        try {
            TelephonyManager telManager = (TelephonyManager) ctx.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            return telManager.getSimCountryIso() + "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getAppVersion(Context ctx) {
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            return info.versionName + "-" + info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int toInt(byte b) {
        return b & 0xFF;
    }

    public static byte[] toBytes(long value) {
        byte[] result = new byte[Long.SIZE / Byte.SIZE];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) value;
            value >>= Byte.SIZE;
        }
        return result;
    }

    public static int toInt(String sInt) {
        int result = -1;
        try {
            result = Integer.valueOf(sInt);
        } catch (Exception e) {
        }
        return result;
    }

    public static int toInt(String sInt, int radix) {
        int result = -1;
        try {
            result = Integer.valueOf(sInt, radix);
        } catch (Exception e) {
        }
        return result;
    }

    public static long toLong(String sLong) {
        long result = -1;
        try {
            result = Long.valueOf(sLong);
        } catch (Exception e) {
        }
        return result;
    }

    public static byte[] toBytes(int value) {
        byte[] result = new byte[Integer.SIZE / Byte.SIZE];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) value;
            value >>= Byte.SIZE;
        }
        return result;
    }

    public static int toInt(byte[] buffer, int start) {
        int result = 0;
        int end = Math.min(buffer.length, start + (Integer.SIZE / Byte.SIZE));
        int moved = 0;
        for (int i = start; i < end; i++) {
            result |= (buffer[i] & 0xFF) << moved;
            moved += Byte.SIZE;
        }
        return result;
    }

    public static int[] toIntArray(String[] args) {
        if (args == null)
            return null;
        int[] result = new int[args.length];
        try {
            for (int i = 0; i < args.length; i++) {
                result[i] = Integer.valueOf(args[i]);
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public static boolean isInt(String value) {
        try {
            Integer.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEquals(Object obj1, Object obj2) {
        boolean isNull01 = obj1 == null;
        boolean isNull02 = obj2 == null;

        if (isNull01 ^ isNull02)
            return false;
        if (isNull01 && isNull02)
            return true;
        return obj1.equals(obj2);
    }

    /**
     * close cursor, catch and ignore all exceptions.
     *
     * @param cursor the cursor object, may be null
     */
    public static void close(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * close object quietly, catch and ignore all exceptions.
     *
     * @param object the closeable object like inputstream, outputstream, reader, writer, randomaccessfile.
     */
    public static void close(Closeable object) {
        if (object != null) {
            try {
                object.close();
            } catch (Throwable e) {
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public static void close(MediaMetadataRetriever retriever) {
        if (retriever != null) {
            try {
                retriever.release();
            } catch (Throwable e) {
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }

    public static int getNavBarColor(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return activity.getWindow().getNavigationBarColor();
        }
        return -1;
    }

    public enum DEVICETYPE {
        DEVICE_PHONE("phone"), DEVICE_PAD("pad");

        private String mValue;

        DEVICETYPE(String value) {
            mValue = value;
        }

        private final static Map<String, DEVICETYPE> VALUES = new HashMap<String, DEVICETYPE>();

        static {
            for (DEVICETYPE item : DEVICETYPE.values())
                VALUES.put(item.mValue, item);
        }

        @SuppressLint("DefaultLocale")
        public static DEVICETYPE fromString(String value) {
            return VALUES.get(value.toLowerCase());
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    public static DEVICETYPE detectDeviceType(Context ctx) {
        double screenSize = 0D;
        try {
            DisplayMetrics displaymetrics = ctx.getApplicationContext().getResources().getDisplayMetrics();
            float width = displaymetrics.widthPixels;
            float height = displaymetrics.heightPixels;
            float xdpi = displaymetrics.densityDpi > displaymetrics.xdpi ? displaymetrics.densityDpi : displaymetrics.xdpi;
            float ydpi = displaymetrics.densityDpi > displaymetrics.ydpi ? displaymetrics.densityDpi : displaymetrics.ydpi;
            float inchW = width / xdpi;
            float inchH = height / ydpi;
            screenSize = Math.sqrt(Math.pow(inchW, 2D) + Math.pow(inchH, 2D));
        } catch (Exception exception) {
            return DEVICETYPE.DEVICE_PHONE;
        }
        if (screenSize >= 6.5D) // some device has 6" screen, such as K7 mini
            return DEVICETYPE.DEVICE_PAD;

        return DEVICETYPE.DEVICE_PHONE;
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * fix land screen width < height
     *
     * @param context
     * @return
     */
    public static int getScreenWidthSafe(Context context) {
        DisplayMetrics displaymetrics = context.getApplicationContext().getResources().getDisplayMetrics();
        int screenWidth = displaymetrics.widthPixels;
        int screenHeight = displaymetrics.heightPixels;
        if (isLandscape(context)) {
            return Math.max(screenWidth, screenHeight);
        } else {
            return Math.min(screenWidth, screenHeight);
        }
    }

    public static int getScreenHeightSafe(Context context) {
        DisplayMetrics displaymetrics = context.getApplicationContext().getResources().getDisplayMetrics();
        int screenWidth = displaymetrics.widthPixels;
        int screenHeight = displaymetrics.heightPixels;
        if (isLandscape(context)) {
            return Math.min(screenWidth, screenHeight);
        } else {
            return Math.max(screenWidth, screenHeight);
        }
    }

    public static int getScreenWidth(Context ctx) {
        DisplayMetrics displaymetrics = ctx.getApplicationContext().getResources().getDisplayMetrics();
        return displaymetrics.widthPixels;
    }

    public static int getScreenHeight(Context ctx) {
        DisplayMetrics displaymetrics = ctx.getApplicationContext().getResources().getDisplayMetrics();
        return displaymetrics.heightPixels;
    }

    public static int getRootViewHeight(Context ctx) {
        return getScreenHeight(ctx) - getStatusBarHeihgt(ctx) - getNavigationBarHeight();
    }

    public static int getStatusBarHeihgt(Context ctx) {
        int result = 0;
        if (ctx instanceof Activity) {
            Activity activity = (Activity) ctx;
            Rect rect = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            result = rect.top;
            if (result != 0) {
                return result;
            }
        }
        int resourceId = ObjectStore.getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = ObjectStore.getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavigationBarHeight() {
        int result = 0;
        if (hasNavigationBar(ObjectStore.getContext())) {
            Resources res = ObjectStore.getContext().getResources();
            int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = res.getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    public static boolean hasNavigationBar(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag
            String sNavBarOverride = getNavBarOverride();
            if ("1".equals(sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else { // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    private static String getNavBarOverride() {
        String sNavBarOverride = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Class c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable e) {
            }
        }
        return sNavBarOverride;
    }

    public static String getStringFromBundle(Bundle bundle, String key) {
        String strValue = bundle.getString(key);
        if (strValue != null)
            return strValue;
        else {
            int intValue = bundle.getInt(key);
            if (intValue != 0)
                return String.valueOf(intValue);
            else
                return null;
        }
    }

    public static int getVersionCode(Context context) {
        String pn = context.getPackageName();
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(pn, 0).versionCode;
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    public static String getVersionName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(packageName, 0).versionName;
        } catch (NameNotFoundException e) {
            return "unknown";
        }
    }

    public static boolean isDevVersion(Context context) {
        boolean ret = true;

        try {
            String pn = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pn, 0);
            ret = (pi.versionCode == 1);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static boolean isSupportPhone(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    public static String createUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String createShortUUID() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = createUniqueId();
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(CHARS[x % CHARS.length]);
        }
        return shortBuffer.toString();
    }

    /**
     * Returns {@code true} if called on the main thread, {@code false} otherwise.
     */
    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Returns {@code true} if called on the main thread, {@code false} otherwise.
     */
    public static boolean isOnBackgroundThread() {
        return !isOnMainThread();
    }

    public static void disableAccessibility(Context context) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
                if (!am.isEnabled()) {
                    //Not need to disable accessibility
                    return;
                }
                Method setState = am.getClass().getDeclaredMethod("setState", int.class);
                setState.setAccessible(true);
                setState.invoke(am, 0);/**{@link AccessibilityManager#STATE_FLAG_ACCESSIBILITY_ENABLED}*/
            } catch (Throwable t) {
            }
        }
    }

    // must call on main ui.
    public static String getWebViewUA() {
        //todo: Adapt andrid p webview bug
//        if(CompatibilityP.isIncompatibleWebViewDevice()){
//            return "";
//        }
        String mUserAgent = (String) ObjectStore.get("ua");
        if (!TextUtils.isEmpty(mUserAgent))
            return mUserAgent;
        try {
            Constructor constructor = WebSettings.class.getDeclaredConstructor(new Class[]{Context.class, WebView.class});
            constructor.setAccessible(true);
            mUserAgent = ((WebSettings) constructor.newInstance(new Object[]{ObjectStore.getContext(), null})).getUserAgentString();
            constructor.setAccessible(false);
        } catch (Throwable ex1) {
            try {
                mUserAgent = new WebView(ObjectStore.getContext()).getSettings().getUserAgentString();
            } catch (Throwable ex2) {
            }
        } finally {
            disableAccessibility(ObjectStore.getContext());
        }

        if (!TextUtils.isEmpty(mUserAgent))
            ObjectStore.add("ua", mUserAgent);
        return mUserAgent;
    }


    public static void reportTrackUrls(final List<String> urls) {
        if (urls == null || urls.isEmpty())
            return;

        TaskHelper.exec(new TaskHelper.UITask() {
            @Override
            public void callback(Exception e) {
                final String ua = getWebViewUA();
                TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("Utils.ReportTracker") {
                    @Override
                    public void execute() {
                        for (String url : urls) {
                            if (StringUtils.isEmpty(url))
                                continue;

                            String trackUrl = url;
                            if (url.contains("{TIMESTAMP}") || url.contains("{timestamp}")) {
                                String timestamp = String.valueOf(System.currentTimeMillis());
                                trackUrl = trackUrl.replace("{TIMESTAMP}", timestamp).replace("{timestamp}", timestamp);
                            }
                            if (url.contains("{GAID}") || url.contains("{gaid}")) {
                                String gaid = DeviceHelper.getGAID(ObjectStore.getContext());
                                trackUrl = trackUrl.replace("{GAID}", gaid).replace("{gaid}", gaid);
                            }
                            if (url.contains("{ANDROIDID}") || url.contains("{androidid}")) {
                                String androidid = DeviceHelper.getAndroidID(ObjectStore.getContext());
                                trackUrl = trackUrl.replace("{ANDROIDID}", androidid).replace("{androidid}", androidid);
                            }
                            reportTrackUrlWithUA(trackUrl, ua);
                        }
                    }
                });
            }
        });
    }

    public static boolean reportTrackUrlWithUA(String strUrl, String userAgent) {
        HttpURLConnection urlConnect = null;
        try {
            URL url = new URL(strUrl);
            urlConnect = (HttpURLConnection) url.openConnection();
            urlConnect.setConnectTimeout(15 * 1000);
            urlConnect.setReadTimeout(15 * 1000);
            urlConnect.setInstanceFollowRedirects(false);
            urlConnect.setRequestProperty("User-Agent", userAgent);
            urlConnect.getContent();
            if (urlConnect.getResponseCode() == 302) {
                String location = urlConnect.getHeaderField("Location");
                return reportTrackUrlWithUA(location, userAgent);
            } else if (urlConnect.getResponseCode() == 200)
                return true;
        } catch (Exception e) {
        } finally {
            if (urlConnect != null)
                urlConnect.disconnect();
        }

        return false;
    }

    //获取适配android 8.x RequestedOrientation
    public static void setAdaptationRequestedOrientation(Activity activity, int orientation) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && Utils.isTranslucentOrFloating(activity)) {
            try {
                //通过反射修改screenOrientation
                Class<Activity> activityClass = Activity.class;
                Field mActivityInfoField = activityClass.getDeclaredField("mActivityInfo");
                mActivityInfoField.setAccessible(true);
                ActivityInfo activityInfo = (ActivityInfo) mActivityInfoField.get(activity);
                //设置屏幕不固定
                activityInfo.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            activity.setRequestedOrientation(orientation);
        }
    }

    public static boolean isTranslucentOrFloating(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        boolean isTranslucentOrFloating = false;
        try {
            Class<?> styleableClass = Class.forName("com.android.internal.R$styleable");
            Field WindowField = styleableClass.getDeclaredField("Window");
            WindowField.setAccessible(true);
            int[] styleableRes = (int[]) WindowField.get(null);
            //先获取到TypedArray
            final TypedArray typedArray = activity.obtainStyledAttributes(styleableRes);
            Class<?> ActivityInfoClass = ActivityInfo.class;
            //调用检查是否屏幕旋转
            Method isTranslucentOrFloatingMethod = ActivityInfoClass.getDeclaredMethod("isTranslucentOrFloating", TypedArray.class);
            isTranslucentOrFloatingMethod.setAccessible(true);
            isTranslucentOrFloating = (boolean) isTranslucentOrFloatingMethod.invoke(null, typedArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isTranslucentOrFloating;
    }


    public static Activity findActivityRecursively(Context context) {
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            return (Activity) context;
        }

        if (context instanceof ContextWrapper) {
            return findActivityRecursively(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }


    /**
     * Set the navigation bar's color.
     *
     * @param window The window.
     * @param color  The navigation bar's color.
     */
    public static void setNavBarColor(@NonNull final Window window, @ColorInt final int color) {
        if (isSupportNavBar() && Build.VERSION.SDK_INT >= 26) {
            if (window.getNavigationBarColor() != color) {
                window.setNavigationBarColor(color);
            }
        }
    }

    public static int getNavBarColor(final Window window) {
        if (isSupportNavBar() && Build.VERSION.SDK_INT >= 26) {
            return window.getNavigationBarColor();
        }
        return -1;
    }

    public static void setLightNavigationBar(Activity activity, boolean light) {
        if (!isSupportNavBar() || Build.VERSION.SDK_INT < 26) {
            return;
        }
        int vis = activity.getWindow().getDecorView().getSystemUiVisibility();
        if (light) {
            vis |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; // 黑色
        } else {
            //白色
            vis &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(vis);
    }

    /**
     * Return whether the navigation bar visible.
     *
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean isSupportNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (ObjectStore.getContext() == null) {
                return false;
            }
            WindowManager wm = (WindowManager) ObjectStore.getContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return false;
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);
            return realSize.y != size.y || realSize.x != size.x;
        }
        boolean menu = ViewConfiguration.get(ObjectStore.getContext()).hasPermanentMenuKey();
        boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        return !menu && !back;
    }


}
