package com.ushareit.core.utils.time;

import android.content.Context;
import android.text.format.Time;

import com.ushareit.core.lang.ObjectStore;
import com.ushareit.sdk_core.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {
    private static final long MS_OF_MIN = 60 * 1000;

    public static boolean isBeforeDate(int year, int month, int day) {
        Calendar deadline = Calendar.getInstance();
        deadline.set(year, month - 1, day, 0, 0, 0);
        Calendar now = Calendar.getInstance();
        return now.before(deadline);
    }

    public static boolean isBeforeTime(int year, int month, int day, int hour, int minute, int second) {
        Calendar deadline = Calendar.getInstance();
        deadline.set(year, month - 1, day, hour, minute, second);
        Calendar now = Calendar.getInstance();
        return now.before(deadline);
    }

    public static boolean isAfterDate(int year, int month, int day) {
        Calendar deadline = Calendar.getInstance();
        deadline.set(year, month - 1, day, 0, 0, 0);
        Calendar now = Calendar.getInstance();
        return now.after(deadline);
    }

    public static boolean isAfterTime(int year, int month, int day, int hour, int minute, int second) {
        Calendar deadline = Calendar.getInstance();
        deadline.set(year, month - 1, day, hour, minute, second);
        Calendar now = Calendar.getInstance();
        return now.after(deadline);
    }

    public static boolean isUnreached(long startTime) {
        return (startTime != -1 && System.currentTimeMillis() < startTime);
    }

    public static boolean isUnreachedServerTime(long startTime, long serverTime) {
        return (startTime != -1 && serverTime < startTime);
    }

    public static boolean isExpired(long endTime) {
        return (endTime != -1 && System.currentTimeMillis() > endTime);
    }

    public static boolean isExpired(long endTime, long overTime) {
        return (endTime != -1 && System.currentTimeMillis() > (endTime + overTime));
    }

    public static boolean isExpiredServerTime(long endTime, long serverTime) {
        return (endTime != -1 && serverTime > endTime);
    }

    public static boolean isExpiredServerTime(long endTime, long serverTime, long overTime) {
        return (endTime != -1 && serverTime > (endTime + overTime));
    }

    public static boolean isSameDay(long time) {
        return isSameDay(System.currentTimeMillis(), time);
    }

    public static boolean isSameDay(long time1, long time2) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String time1Str = sf.format(time1);
        String time2Str = sf.format(time2);
        return time1Str.equals(time2Str);
    }

    public static String getFormatDate(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return format.format(new Date(time));
    }

    public static String getFormatDate(String formater, long time) {
        SimpleDateFormat format = new SimpleDateFormat(formater, Locale.US);
        return format.format(new Date(time));
    }

    public static String getFormatMMdd(long time) {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd", Locale.US);
        return format.format(new Date(time));
    }

    private static SimpleDateFormat getHourTimeFormat(Context context) {
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        }
        return new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
    }

    public static String get24HourFormatDate(long time) {
        SimpleDateFormat format = getHourTimeFormat(ObjectStore.getContext());
        return format.format(new Date(time));
    }

    public static String getWeekHourFormatDate(long time) {
        SimpleDateFormat format = new SimpleDateFormat("E dd MMM, h:mm a", Locale.US);
        return format.format(new Date(time));
    }

    public static long getToday() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long getTimestampOfTodayTime(int hourOfDay, int minute, int second, int millisecond) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, millisecond);
        return c.getTimeInMillis();
    }

    public static boolean isAfterTime(long currentTimeMillis, int hour, int min){
        Time now = new Time();
        now.set(currentTimeMillis);
        if(now.hour > hour){
            return true;
        }else if(now.hour == hour){
            return  now.minute > min;
        }
        return false;
    }

    public static boolean isCurrentInTimeScope(long currentTimeMillis, int beginHour, int beginMin, int endHour, int endMin)
    {
        boolean result = false;
        final long aDayInMillis = 1000 * 60 * 60 * 24;
        try {
            Time now = new Time();
            now.set(currentTimeMillis);
            Time startTime = new Time();
            startTime.set(currentTimeMillis);
            startTime.hour = beginHour;
            startTime.minute = beginMin;
            Time endTime = new Time();
            endTime.set(currentTimeMillis);
            endTime.hour = endHour;
            endTime.minute = endMin;
            // 跨天的特殊情况(比如22:00-8:00)
            if (!startTime.before(endTime)) {
                startTime.set(startTime.toMillis(true) - aDayInMillis);
                result = !now.before(startTime) && !now.after(endTime); // startTime <= now <= endTime
                Time startTimeInThisDay = new Time();
                startTimeInThisDay.set(startTime.toMillis(true) + aDayInMillis);
                if (!now.before(startTimeInThisDay)) {
                    result = true;
                }
                //普通情况(比如 8:00 - 14:00)
            } else {
                result = !now.before(startTime) && !now.after(endTime); // startTime <= now <= endTime
            }
        }catch (Exception e){

        }
        return result;
    }

    public static String durationToUnitString(Context context, long duration) {
        if (duration >= MS_OF_MIN) {
            return context.getString(R.string.common_unit_time_minute);
        } else {
            return context.getString(R.string.common_unit_time_second);
        }
    }
}