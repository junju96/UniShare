package com.ushareit.core.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static NotificationChannel genNotificationChannel(String id, String name) {
        return getNotificationChannel(id, name, false, NotificationManager.IMPORTANCE_DEFAULT, false);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static NotificationChannel genNotificationChannelSilent(String id, String name) {
        return getNotificationChannel(id, name, false, NotificationManager.IMPORTANCE_DEFAULT, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static NotificationChannel genNotificationChannelLow(String id, String name) {
        return getNotificationChannel(id, name, false, NotificationManager.IMPORTANCE_LOW, false);
    }

    public static Notification getEmptyNotification(Context context, String channelId, String channelName){
        NotificationCompat.Builder notificationBuilder = getNotificationCompatBuilder(context, channelId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null){
                NotificationChannel channel = genNotificationChannel(channelId, channelName);
                nm.createNotificationChannel(channel);
            }
        }
        return notificationBuilder.build();
    }

    private static NotificationChannel getNotificationChannel(String id, String name, boolean isEnableLight, int importance,boolean isSilent) {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(id, name, importance);
            channel.enableLights(isEnableLight);
            if(isSilent)
                channel.setSound(null, null);
        }
        return channel;
    }

    public static NotificationCompat.Builder getNotificationCompatBuilder(Context context, String channelId) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new NotificationCompat.Builder(context, channelId)
                : new NotificationCompat.Builder(context);
    }

    public static void cancelNotification(Context context, int id) {
        NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifyManager.cancel(id);
    }
}