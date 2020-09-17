package com.ushareit.core.stats;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Pair;
import android.util.SparseArray;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.i18n.LocaleUtils;

import java.util.LinkedHashMap;
import java.util.List;

public final class StatsUtils {
    private static final String TAG = "StatsUtils";

    public static LinkedHashMap<String, String> collectCallerInfo(Context context) {
        ComponentName cn = getPossibleCaller(context);
        if (cn == null)
            return null;

        String clsName = cn.getClassName();
        String pkgName = cn.getPackageName();
        LinkedHashMap<String, String> info = new LinkedHashMap<String, String>();
        info.put("package", pkgName);
        info.put("activity", clsName);

        return info;
    }

    public static String getNetwork(Pair<Boolean, Boolean> network) {
        if (network == null)
            return null;
        else if (network.second)
            return "Wifi";
        else if (network.first)
            return "Data";
        else
            return "No network";
    }

    private static ComponentName getPossibleCaller(Context context) {
        ComponentName cn = null;
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        // RECENT_WITH_EXCLUDED and RECENT_IGNORE_UNAVAILABLE
        List<ActivityManager.RecentTaskInfo> list = activityManager.getRecentTasks(10, ActivityManager.RECENT_WITH_EXCLUDED);
        if (list != null && !list.isEmpty())
            cn = list.get(0).baseIntent.getComponent();

        Logger.d(TAG, "getPosibleCaller(): caller: " + cn);
        return cn;
    }

    /*************************************************************
     * compute range for old events
     *************************************************************/
    // / Utils to compute the size range
    private static SparseArray<String> mSizeRangeMap;
    private static int[] mSizeArray;
    private static final int MAX_SIZE_IN_MB = 999999; // large enough
    private static final int MIN_SIZE_IN_MB = 0; // large enough

    private static SparseArray<String> mUsedTimeRangeMap;
    private static int[] mUsedTimeArray;
    private static SparseArray<String> mTimeRangeMap;
    private static int[] mTimeArray;
    private static final int MAX_TIME_IN_S = 60 * 60 * 24; // a day
    private static final int MIN_TIME_IN_S = 0;

    // Utils to compute the count range
    private static SparseArray<String> mCountRangeMap;
    private static int[] mCountArray;
    private static final int MAX_COUNT = 999999; // large enough
    private static final int MIN_COUNT = 0; // large enough

    static {
        mSizeRangeMap = new SparseArray<String>();
        mSizeArray = new int[] { MIN_SIZE_IN_MB, 3, 5, 10, 20, 50, 100, 200, 300, 500, 700, MAX_SIZE_IN_MB };

        mSizeRangeMap.put(MIN_SIZE_IN_MB, "0to3M");
        mSizeRangeMap.put(3, "0to3M");
        mSizeRangeMap.put(5, "3to5M");
        mSizeRangeMap.put(10, "5to10M");
        mSizeRangeMap.put(20, "10to20M");
        mSizeRangeMap.put(50, "20to50M");
        mSizeRangeMap.put(100, "50to100M");
        mSizeRangeMap.put(200, "100to200M");
        mSizeRangeMap.put(300, "200to300M");
        mSizeRangeMap.put(500, "300to500M");
        mSizeRangeMap.put(700, "500to700M");
        mSizeRangeMap.put(MAX_SIZE_IN_MB, "700Mabove");

        mTimeRangeMap = new SparseArray<String>();
        mTimeArray = new int[] { MIN_TIME_IN_S, 2, 5, 10, 20, 30, 60, 120, 180, 240, 300, MAX_TIME_IN_S };
        mTimeRangeMap.put(MIN_TIME_IN_S, "0to2s");
        mTimeRangeMap.put(2, "0to2s");
        mTimeRangeMap.put(5, "2to5s");
        mTimeRangeMap.put(10, "5to10s");
        mTimeRangeMap.put(20, "10to20s");
        mTimeRangeMap.put(30, "20to30s");
        mTimeRangeMap.put(60, "30to60s");
        mTimeRangeMap.put(120, "60to120s");
        mTimeRangeMap.put(180, "120to180s");
        mTimeRangeMap.put(240, "180to240s");
        mTimeRangeMap.put(300, "240to300s");
        mTimeRangeMap.put(MAX_TIME_IN_S, "300s+");

        mUsedTimeRangeMap = new SparseArray<String>();
        mUsedTimeArray = new int[] { MIN_TIME_IN_S, 10, 30, 60, 120, 300, 600, 1200, 1800, 3600, 7200, MAX_TIME_IN_S };
        mUsedTimeRangeMap.put(MIN_TIME_IN_S, "0~10s");
        mUsedTimeRangeMap.put(10, "0~10s");
        mUsedTimeRangeMap.put(30, "10~30s");
        mUsedTimeRangeMap.put(60, "30~60s");
        mUsedTimeRangeMap.put(120, "1~2m");
        mUsedTimeRangeMap.put(300, "2~5m");
        mUsedTimeRangeMap.put(600, "5~10m");
        mUsedTimeRangeMap.put(1200, "10~20m");
        mUsedTimeRangeMap.put(1800, "20~30m");
        mUsedTimeRangeMap.put(3600, "30~60m");
        mUsedTimeRangeMap.put(7200, "1~2h");
        mUsedTimeRangeMap.put(MAX_TIME_IN_S, "2h+");

        mCountRangeMap = new SparseArray<String>();
        mCountArray = new int[] { MIN_COUNT, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, MAX_COUNT };
        mCountRangeMap.put(MIN_COUNT, "0~5");
        mCountRangeMap.put(5, "0~5");
        mCountRangeMap.put(10, "5~10");
        mCountRangeMap.put(20, "10~20");
        mCountRangeMap.put(50, "20~50");
        mCountRangeMap.put(100, "50~100");
        mCountRangeMap.put(200, "100~200");
        mCountRangeMap.put(500, "200~500");
        mCountRangeMap.put(1000, "500~1000");
        mCountRangeMap.put(2000, "1000~2000");
        mCountRangeMap.put(5000, "2000~5000");
        mCountRangeMap.put(MAX_COUNT, "5000+");
    }

    public static String computeSizeRange(long size) {
        long sizeInMb = size / (1024 * 1024);

        return computeRange(sizeInMb, mSizeArray, mSizeRangeMap);
    }

    public static String computeTimeRange(long duration) {
        return computeRange(duration, mTimeArray, mTimeRangeMap);
    }

    public static String computeUsedTimeRange(long duration) {
        return computeRange(duration, mUsedTimeArray, mUsedTimeRangeMap);
    }

    public static String computeCountRange(long count) {
        return computeRange(count, mCountArray, mCountRangeMap);
    }

    private static String computeRange(long val, int[] rangeArray, SparseArray<String> rangeMap) {
        String range = null;
        try {
            for (int i = 1; i < rangeArray.length; i++) {
                if (inRange(val, rangeArray[i - 1], rangeArray[i])) {
                    range = rangeMap.get(rangeArray[i]);
                    break;
                }
            }
        } catch (Exception e) {
            range = null;
        }

        Logger.d(TAG, "range: " + range);
        return range;
    }

    // return ture if value in [min, max)
    private static boolean inRange(long value, long min, long max) {
        return value >= min && value < max;
    }

    /**************************************************************
     * compute range for new events
     **************************************************************/
    private static final long KBYTES = 1024;
    private static final long MBYTES = 1024 * KBYTES;
    private static final long GBYTES = 1024 * MBYTES;
    private static final long[] DEFAULT_SPEEDS = { KBYTES, 10 * KBYTES, 50 * KBYTES, 100 * KBYTES, 300 * KBYTES, 500 * KBYTES, MBYTES, (long)(1.5 * MBYTES), 2 * MBYTES, (long)(2.5 * MBYTES),
            3 * MBYTES,
            4 * MBYTES, 5 * MBYTES, 6 * MBYTES, 7 * MBYTES, 8 * MBYTES, 9 * MBYTES, 10 * MBYTES };
    private static final int[] DEFAULT_FILE_COUNTS = { 0, 1, 2, 3, 5, 10, 20, 30, 50, 100, 200, 300, 500, 1000, 2000, 3000, 5000, 10000 };
    private static final long[] DEFAULT_FILESIZES = { 10 * KBYTES, 50 * KBYTES, 100 * KBYTES, 300 * KBYTES, 500 * KBYTES, MBYTES, (long)2 * MBYTES, 3 * MBYTES, (long)5 * MBYTES, 10 * MBYTES,
            15 * MBYTES, 20 * MBYTES, 30 * MBYTES, 50 * MBYTES, 100 * MBYTES, 300 * MBYTES, 500 * MBYTES, GBYTES, 2 * GBYTES, 3 * GBYTES, 5 * GBYTES, 10 * GBYTES, 20 * GBYTES, 30 * GBYTES, 50 * GBYTES, 100 * GBYTES, 200 * GBYTES };
    private static final float[] DEFAULT_DURATIONS = { 3, 5, 10, 15, 20, 30, 60, 180, 300, 600, 1800, 3600 };
    private static final float[] DEFAULT_PERCENT_VALUES = { .01f, .03f, .05f, .07f, .1f, .12f, .15f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f };

    public static final int[] DEFAULT_COUNTS_LOW = { 0, 1, 2, 3, 4, 5, 10, 15, 20, 30, 50, 100, 200, 300, 500, 1000 };
    public static final int[] DEFAULT_COUNTS_MID = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 30, 50, 100, 200, 300, 500, 1000 };
    public static final int[] DEFAULT_COUNTS_HIGH = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 30, 50, 100, 200, 300, 500, 1000 };


    // speed = filesize / second
    public static String getSpeedScope(float speed) {
        return getSpeedScope(speed, DEFAULT_SPEEDS);
    }

    public static String getSpeedScope(float speed, long[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (speed >= sections[i])
                continue;
            if (i == 0)
                return "<" + formatNumber(sections[i]) + "B/s";
            return ">=" + formatNumber(sections[i - 1]) + "B/s, <" + formatNumber(sections[i]) + "B/s";
        }
        return ">=" + formatNumber(sections[sections.length - 1]) + "B/s";
    }

    // keep the same style with old version, insert space after "<" ;
    public static String getSpeedScopeEx(float speed) {
        long[] sections = DEFAULT_SPEEDS;

        for (int i = 0; i < sections.length; i++) {
            if (speed >= sections[i])
                continue;
            if (i == 0)
                return "<" + formatNumber(sections[i]) + "B/s";
            return ">=" + formatNumber(sections[i - 1]) + "B/s, < " + formatNumber(sections[i]) + "B/s";
        }
        return ">=" + formatNumber(sections[sections.length - 1]) + "B/s";
    }

    public static String getFileCountScope(int fileCount) {
        return getCountScope(fileCount, DEFAULT_FILE_COUNTS);
    }

    public static String getCountScope(int fileCount, int[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (fileCount == sections[i] && (i == 0 || (sections[i] - sections[i - 1] == 1)))
                return String.valueOf(sections[i]);

            if (fileCount >= sections[i])
                continue;
            if (i == 0)
                return "<" + formatNumber(sections[i]);
            return ">=" + formatNumber(sections[i - 1]) + ", <" + formatNumber(sections[i]);
        }
        return ">=" + formatNumber(sections[sections.length - 1]);
    }

    // keep the same style with old version
    public static String getFileCountScopeEx(int fileCount) {
        int[] sections = DEFAULT_FILE_COUNTS;
        for (int i = 0; i < sections.length; i++) {
            if (fileCount == sections[i] && (i == 0 || (sections[i] - sections[i - 1] == 1)))
                return String.valueOf(sections[i]);

            if (fileCount > sections[i])
                continue;
            return ">" + formatNumber(sections[i - 1]) + ", <=" + formatNumber(sections[i]);
        }
        return ">" + formatNumber(sections[sections.length - 1]);
    }

    public static String getFileSizeScope(long fileSize) {
        return getSizeScope(fileSize, DEFAULT_FILESIZES);
    }

    public static String getSizeScope(long fileSize, long[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (fileSize >= sections[i])
                continue;
            if (i == 0)
                return "<" + formatNumber(sections[i]);
            return ">=" + formatNumber(sections[i - 1]) + ", <" + formatNumber(sections[i]);
        }
        return ">=" + formatNumber(sections[sections.length - 1]);
    }

    public static String getDurationScope(float duration) {
        return getTimeScope(duration, DEFAULT_DURATIONS);
    }

    public static String getTimeScope(float duration, float[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (Float.compare(duration, sections[i]) == 0 && (i == 0 || (sections[i] - sections[i - 1] == 1)))
                return formatTime(sections[i]);

            if (duration >= sections[i])
                continue;
            if (i == 0)
                return "<" + formatTime(sections[i]);
            return ">=" + formatTime(sections[i - 1]) + ", <" + formatTime(sections[i]);
        }
        return ">=" + formatTime(sections[sections.length - 1]);
    }

    public static String getPercentScope(float value) {
        return getPercentScope(value, DEFAULT_PERCENT_VALUES);
    }

    public static String getPercentScope(float value, float[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (value >= sections[i])
                continue;
            if (i == 0)
                return "<" + formatPercent(sections[i]);
            return ">=" + formatPercent(sections[i - 1]) + ", <" + formatPercent(sections[i]);
        }
        return ">=" + formatPercent(sections[sections.length - 1]);
    }

    public static String formatNumber(float number) {
        long division = 1;
        String unit = "";
        if (number >= KBYTES) {
            division = KBYTES;
            unit = "K";
        }
        if (number >= MBYTES) {
            division = MBYTES;
            unit = "M";
        }
        if (number >= GBYTES) {
            division = GBYTES;
            unit = "G";
        }

        float result = number / division;
        return LocaleUtils.decimalFormatIgnoreLocale("#.#", result) + unit;
    }

    // time unit is second
    public static String formatTime(float time) {
        long division = 1;
        String unit = "s";
        if (time >= 60) {
            division = 60;
            unit = "m";
        }
        if (time >= 60 * 60) {
            division = 60 * 60;
            unit = "h";
        }
        if (time >= 60 * 60 * 24) {
            division = 60 * 60 * 24;
            unit = "d";
        }

        float result = time / division;
        return LocaleUtils.decimalFormatIgnoreLocale("#.#", result) + unit;
    }

    public static String formatPercent(float f) {
        int result = Math.round(f * 100);
        return result + "%";
    }
}
