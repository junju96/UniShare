package com.ushareit.core.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;


public class ServicesHelper {

    public static void startService(@NonNull Context context, @NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.startService(intent);
            } catch (Exception e) {
            }
        } else {
            context.startService(intent);
        }
    }
}
