package com.ushareit.core.utils.time;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class DateUtils {

    private static String CALENDAR_URL = "content://com.android.calendar/calendars";
    private static String CALENDAR_EVENT_URL = "content://com.android.calendar/events";
    private static String CALENDAR_REMINDER_URL = "content://com.android.calendar/reminders";

    public static boolean isSameDay(long timeMillis, long otherTimeMillis) {
        if (timeMillis == otherTimeMillis) {
            return true;
        }
        Calendar first = Calendar.getInstance();
        first.setTimeInMillis(timeMillis);

        Calendar second = Calendar.getInstance();
        second.setTimeInMillis(otherTimeMillis);

        if (first.get(Calendar.YEAR) == second.get(Calendar.YEAR)) {
            return first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
        }
        return false;
    }

    public static long getRestMillsOfDay(long target, long currentServerTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(target);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() - currentServerTime;
    }

    public static boolean insertCalendarEvent(Context context, String title, String description,
                                              long beginTimeMillis, long endTimeMillis, int remindersMethod, int remindersMinutes,
                                              String rrule) {

        if (context == null || TextUtils.isEmpty(title) || TextUtils.isEmpty(description)) {
            return false;
        }

        int calId = getCalendarAccount(context);
        if (calId == Integer.MIN_VALUE || calId < 0) {
            return false;
        }

        beginTimeMillis = beginTimeMillis <= 0 ? Calendar.getInstance().getTimeInMillis() : beginTimeMillis;
        endTimeMillis = endTimeMillis < beginTimeMillis ? beginTimeMillis + 30 * 60 * 1000 : endTimeMillis;
        remindersMethod = remindersMethod != CalendarContract.Reminders.METHOD_ALERT &&
                remindersMethod !=  CalendarContract.Reminders.METHOD_EMAIL ?
                CalendarContract.Reminders.METHOD_ALERT : remindersMethod;
        remindersMinutes = remindersMinutes < 0 ? 10 : remindersMinutes;

        if (beginTimeMillis == 0) {
            Calendar beginCalendar = Calendar.getInstance();
            beginTimeMillis = beginCalendar.getTimeInMillis();
        }

        if (endTimeMillis == 0) {
            endTimeMillis = beginTimeMillis + 30 * 60 * 1000;
        }

        try {
            ContentValues eventValues = new ContentValues();
            eventValues.put(CalendarContract.Events.DTSTART, beginTimeMillis);
            eventValues.put(CalendarContract.Events.DTEND, endTimeMillis);
            eventValues.put(CalendarContract.Events.TITLE, title);
            eventValues.put(CalendarContract.Events.DESCRIPTION, description);
            eventValues.put(CalendarContract.Events.CALENDAR_ID, calId);
            if(!TextUtils.isEmpty(rrule)) {
                eventValues.put(CalendarContract.Events.RRULE, rrule);
            }

            TimeZone tz = TimeZone.getDefault();
            eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, tz.getID());

            Uri eUri = context.getContentResolver().insert(Uri.parse(CALENDAR_EVENT_URL), eventValues);
            long eventId = ContentUris.parseId(eUri);
            if (eventId != 0) {
                ContentValues reminderValues = new ContentValues();
                reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
                reminderValues.put(CalendarContract.Reminders.MINUTES, remindersMinutes);
                reminderValues.put(CalendarContract.Reminders.METHOD, remindersMethod);
                Uri rUri = context.getContentResolver().insert(Uri.parse(CALENDAR_REMINDER_URL),
                        reminderValues);
                if (rUri == null || ContentUris.parseId(rUri) == 0) {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static int getCalendarAccount(Context context) {
        Cursor userCursor = context.getContentResolver().query(Uri.parse(CALENDAR_URL),
                null, null, null, null);
        try {
            if (userCursor == null) {
                return Integer.MIN_VALUE;
            }
            int count = userCursor.getCount();
            if (count > 0) {
                userCursor.moveToFirst();
                return userCursor.getInt(userCursor.getColumnIndex(CalendarContract.Calendars._ID));
            } else {
                return Integer.MIN_VALUE;
            }
        } finally {
            if (userCursor != null) {
                userCursor.close();
            }
        }
    }
}
