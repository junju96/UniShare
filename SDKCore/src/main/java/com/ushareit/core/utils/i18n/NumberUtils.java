package com.ushareit.core.utils.i18n;

import android.annotation.SuppressLint;
import android.util.Pair;

import com.ushareit.core.Assert;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.sdk_core.R;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * contains Number (conversion) related utility functions.
 */
public final class NumberUtils {
    private static final long MS_OF_MIN = 60 * 1000;
    private static final long MS_OF_SEC = 1000;

    private NumberUtils() {}

    // convert size (in bytes) to readable string
    public static String sizeToString(final long length) {
        Pair<String, String> result = sizeToStringPair(length);
        return result.first + result.second;
    }

    // convert size (in bytes) to readable pair string, first is num, second is unit
    public static Pair<String, String> sizeToStringPair(final long length) {
        String resultNumber;
        String resultUnit;

        int count = 0;
        double size = length;
        while (size >= 1024) {
            count++;
            size = size / 1024;
        }

        switch (count) {
            case 0:
                resultNumber = length + "";
                resultUnit = "B";
                break;
            case 1:
                resultNumber = LocaleUtils.formatStringIgnoreLocale("%.0f", size);
                resultUnit = "KB";
                break;
            case 2:
                resultNumber = LocaleUtils.formatStringIgnoreLocale("%.1f", size);
                resultUnit = "MB";
                break;
            case 3:
                resultNumber = LocaleUtils.formatStringIgnoreLocale("%.2f", size);
                resultUnit = "GB";
                break;
            default:
                resultNumber = length + "";
                resultUnit = "B";
                break;
        }

        return Pair.create(resultNumber, resultUnit);
    }

    // convert duration (in seconds) to human readable string
    public static String durationToString(long duration) {
        if (duration >= MS_OF_MIN) {
            return duration / MS_OF_MIN + "\'" + duration % MS_OF_MIN / 1000 + "\"";
        } else {
            return duration / MS_OF_SEC + "\"";
        }
    }

    public static String durationToString2(long duration) {
        duration = duration / 1000;
        int hour = (int)(duration / 3600);
        int minute = (int)((duration - hour * 3600) / 60);
        int second = (int)((duration - hour * 3600 - minute * 60));
        return LocaleUtils.formatStringIgnoreLocale("%02d:%02d:%02d", hour, minute, second);
    }

    public static String durationToAdapterString(long duration) {
        if (duration <= 0) {
            return "00:00";
        }
        duration = duration / 1000;
        int hour = (int)(duration / 3600);
        int minute = (int)((duration - hour * 3600) / 60);
        int second = (int)((duration - hour * 3600 - minute * 60));
        if (hour > 0) {
            return LocaleUtils.formatStringIgnoreLocale("%02d:%02d:%02d", hour, minute, second);
        } else {
            return LocaleUtils.formatStringIgnoreLocale("%02d:%02d",  minute, second);
        }
    }

    public static String durationToNumString(long duration) {
        if (duration >= MS_OF_MIN) {
            float min = duration * 1.0F / MS_OF_MIN;
            if (min == 0)
                return "1.0";
            return LocaleUtils.formatStringIgnoreLocale("%.1f", min);
        } else {
            long sec = duration / MS_OF_SEC;
            if (sec == 0)
                return "1";
            return String.valueOf(sec);
        }
    }

    public static String durationToNumString(long duration, String defaultValue) {
        if (duration >= MS_OF_MIN) {
            float min = duration * 1.0F / MS_OF_MIN;
            if (min == 0)
                return defaultValue;
            return LocaleUtils.formatStringIgnoreLocale("%.1f", min);
        } else {
            long sec = duration / MS_OF_SEC;
            if (sec == 0)
                return defaultValue;
            return String.valueOf(sec);
        }
    }

    public static String durationToUnitString(long duration) {
        if (duration >= MS_OF_MIN) {
            return "Min";
        } else {
            return "Sec";
        }
    }

    // convert application version name to string only with number and ".",
    // Because some application version name include some description.
    public static String getNumberVersionName(String version) {
        Assert.notNEWS(version);

        StringBuilder builder = new StringBuilder();
        for (char value : version.toCharArray()) {
            if ((value >= '0' && value <= '9') || (value == '.'))
                builder.append(value);
            else
                break;
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '.')
            builder.deleteCharAt(builder.length() - 1);

        return builder.toString();
    }

    @SuppressLint("SimpleDateFormat")
    public static long parseDateTimeFromString(String dateStr) {
        if (dateStr == null || dateStr.length() == 0)
            return -1;

        ParsePosition pos = new ParsePosition(0);
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date dateObj = format.parse(dateStr, pos);
            return (dateObj != null) ? dateObj.getTime() : -1;
        } catch (Exception e) {}
        return -1;
    }

    public static String timeToString(long time) {
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
        return format.format(new Date(time));
    }

    public static String toPercentString(long numerator, long denominator) {
        return (toPercent(numerator, denominator) + "%");
    }

    public static long toPercent(long numerator, long denominator) {
        return (denominator == 0) ? 0 : (numerator * 100 / denominator);
    }

    private static final long MINUTE = 60 * 1000;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 12 * MONTH;

    public static String timeAgoString(long time) {
        String timeStr = "";
        long now = System.currentTimeMillis();
        long delTime = now - time;
        if (delTime > YEAR) {
            int year = (int) (delTime / YEAR);
            if (year == 1)
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_one_year);
            else
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_years, String.valueOf(year));
        } else if (delTime > MONTH) {
            int month = (int) (delTime / MONTH);
            if (month == 1)
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_one_month);
            else
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_months, String.valueOf(month));
        } else if (delTime > DAY) {
            int day = (int) (delTime / DAY);
            if (day == 1)
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_one_day);
            else
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_days, String.valueOf(day));
        } else if (delTime > HOUR) {
            int hour = (int) (delTime / HOUR);
            if (hour == 1)
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_one_hour);
            else
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_hours, String.valueOf(hour));
        } else if (delTime > MINUTE) {
            int minute = (int) (delTime / MINUTE);
            if (minute == 1)
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_one_minute);
            else
                timeStr = ObjectStore.getContext().getString(R.string.timer_ago_minutes, String.valueOf(minute));
        } else {
            timeStr = ObjectStore.getContext().getString(R.string.timer_ago_one_minute);
        }
        return timeStr;
    }
}
