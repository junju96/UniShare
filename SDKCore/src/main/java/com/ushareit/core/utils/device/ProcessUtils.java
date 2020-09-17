package com.ushareit.core.utils.device;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProcessUtils {
    public static boolean isAppMainProcess(Context context) {
        try {
            int pid = android.os.Process.myPid();
            String pkgName = context.getPackageName();
            String processName = getProcessName(pid);

            return (processName == null || processName.equals(pkgName));
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isAppMainProcess(Context context, String processName) {
        try {
            String pkgName = context.getPackageName();
            return (processName == null || processName.equals(pkgName));
        } catch (Exception e) {
            return true;
        }
    }

    public static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }
}
