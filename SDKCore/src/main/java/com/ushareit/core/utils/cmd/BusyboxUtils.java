package com.ushareit.core.utils.cmd;

import android.content.Context;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.AssetsUtils;
import com.ushareit.core.utils.cmd.RootUtils.ConsoleOutput;

import java.io.File;
import java.util.List;

public class BusyboxUtils {
    private static final String TAG = "BusyboxUtils";

    private static BusyboxUtils sInstance = null;
    private String mBusybox = "";
    private boolean mHasBusybox = false;

    public static synchronized BusyboxUtils getInstance() {
        if (sInstance == null)
            sInstance = new BusyboxUtils();
        return sInstance;
    }

    public static boolean hasSuProcess() {
        for (String path : System.getenv("PATH").split(":")) {
            File f = new File(path, "su");
            if (f.exists())
                return true;
        }
        return false;
    }

    public boolean hasBusybox() {
        return mHasBusybox;
    }

    public String getBusybox() {
        return mBusybox;
    }

    public void initBusybox(final Context context) {
        // String cmd = "mount -o remount,rw /system \n";
        // cmd += "rm -r /system/app/*AnyShare*.apk\n";
        // cmd += "rm -r /system/app/*anyshare*.apk";
        // RootUtils.executeCommand(context, cmd);
        // installSuperuser(context);
        installBusybox(context);
    }

    private void installBusybox(Context context) {
        final String busybox = "busybox";
        mBusybox = context.getFilesDir().toString() + "/" + busybox;
        if (new File(mBusybox).exists()) {
            mHasBusybox = checkBinaryPermission(mBusybox);
            return;
        }

        AssetsUtils.extractAssetsFile(context, busybox, mBusybox);
        RootUtils.executeCommand(context, "chmod 755 " + mBusybox + "\n");
        mHasBusybox = checkBinaryPermission(mBusybox);
    }

    private boolean checkBinaryPermission(String binary) {
        ConsoleOutput output = RootUtils.execConsoleCommand(binary);
        if (contains(output.error, "Permission denied") || !output.isSuccess) {
            File binaryFile = new File(binary);
            if (binaryFile.exists()) {
                binaryFile.delete();
                Logger.d(TAG, "checkBinaryPermission" + output.error);
            }
            return false;
        }
        return true;
    }

    private boolean contains(List<String> src, String compareStr) {
        if (src.size() == 0)
            return false;
        for (String str : src) {
            if (str.contains(compareStr))
                return true;
        }
        return false;
    }
}