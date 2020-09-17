package com.ushareit.core.utils;

import android.content.Context;
import com.ushareit.core.Assert;

public class WWUtils {
	private static Boolean sIsWWVersion = null;
	private static String sRootDirName = null;

	public static void setIsWWVersion(boolean isWWVersion) {
		sIsWWVersion = isWWVersion;
	}

	public static void setAppRootDirName(String rootName) {
	    sRootDirName = rootName;
    }

    // Judge world wide version
    // return true when package name is "com.lenovo.anyshare.gps"
    public static boolean isWWVersion(Context ctx) {
        if (sIsWWVersion != null)
            return sIsWWVersion;

        String packageName = ctx.getPackageName();
        return packageName == null || (packageName.equalsIgnoreCase("com.lenovo.anyshare.gps"));
    }

    // the root directory is different from internal version to world wide version
    public static String getAppRootDirName(Context context) {
        Assert.notNull(sRootDirName);
        return sRootDirName;
    }
}
