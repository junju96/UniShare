package com.ushareit.core.utils;

import android.content.Context;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.StreamUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class AssetsUtils {
    private static final String TAG = "AssetsUtils";

    public static boolean extractAssetsFile(Context context, String srcFileName, String dstFileName) {
        Logger.d(TAG, "Start extractAssetsFile() : " + srcFileName);

        InputStream is = null;
        FileOutputStream os = null;
        try {
            byte[] buff = new byte[4096];
            int count = 0;

            is = context.getAssets().open(srcFileName);
            os = new FileOutputStream(dstFileName);
            while ((count = is.read(buff)) > 0)
                os.write(buff, 0, count);
        } catch (IOException e) {
            Logger.d(TAG, "IOException in extractAssetsFile(): " + srcFileName);
            return false;
        } finally {
            Utils.close(os);
            Utils.close(is);
        }

        File dstFile = new File(dstFileName);
        boolean exists = dstFile.exists();
        Logger.d(TAG, "Finish extractAssetsFile() : " + srcFileName + " and exists: " + exists);
        return exists;
    }
    
    public static int[] readIntArrayFromAsset(Context context, String name) throws IOException {
        Assert.notNull(context);
        Assert.notNE(name);

        InputStream is = null;
        try {
            is = context.getAssets().open(name);

            return StreamUtils.readIntArrayFromInputStream(is);
        } catch (IOException e) {
            Logger.e(TAG, e);
            throw e;
        } finally {
            Utils.close(is);
        }
    }

    public static String readFromAsset(Context context, String name) throws IOException {
        Assert.notNull(context);
        Assert.notNE(name);

        BufferedReader bufReader = null;
        try {
            bufReader = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
            String line = "";
            String result = "";

            while ((line = bufReader.readLine()) != null)
                result += line;
            return result;
        } catch (IOException e) {
            Logger.e(TAG, e);
            throw e;
        } finally {
            Utils.close(bufReader);
        }
    }
}