package com.ushareit.core.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public final class FileScanner {
    private static final String TAG = "FileScanner";

    private FileScanner() {}

    public static List<String> scanFiles(Context context, String extName) {
        List<String> results = new ArrayList<String>();
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Logger.w(TAG, "scanFiles(): SD card isn't mounted.");
            return results;
        }

        if (scanFilesFromDB(results, context, extName)) {
            Logger.d(TAG, "scanFiles(): Get files from DB success and count = " + results.size());
            return results;
        }

        results.clear();
        return results;
    }

    // SW use selection and selectionArgs to improve performance, don't filter results
    public static boolean scanFilesFromDB(List<String> results, Context context, String extName) {
        Cursor cursor = null;
        try {
            // Search from External Database
            cursor = context.getContentResolver().query(Uri.parse("content://media/external/file"), new String[] { "_id", "_data", "_size" },
                    "_data LIKE '%" + extName + "' AND _size>0", null, null);
            if (cursor == null)
                return false;

            while (cursor.moveToNext()) {
                String url = cursor.getString(1);
                results.add(url);
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return false;
        } finally {
            Utils.close(cursor);
        }

        return true;
    }

    // use selection and selectionArgs to improve performance, don't filter results
    // like: SELECT path FROM table WHERE path LIKE xxx OR path LIKE yyy OR path LIKE zzz ...
    // notice if there is a limitation for number of WHERE conditions or SQL length
    public static boolean scanFilesFromDB(List<String> results, Context context, String[] keys) {
        Cursor cursor = null;
        try {
            String selection = "";
            for (String key : keys) {
                if (!TextUtils.isEmpty(selection))
                    selection = selection.concat(" or ");
                selection = selection.concat("_data LIKE '%" + key + "%'");
            }
            // Search from External Database
            cursor = context.getContentResolver().query(Uri.parse("content://media/external/file"), new String[] { "_id", "_data" }, selection, null, null);
            if (cursor == null)
                return false;

            while (cursor.moveToNext()) {
                String url = cursor.getString(1);
                results.add(url);
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return false;
        } finally {
            Utils.close(cursor);
        }

        return true;
    }

    public static boolean scanFilesFromDB(List<String> results, Context context, String[] keys, String[] includeExtNames, String[] exclusiveExtNames) {
        try {
            String[] projection = new String[]{"_id", "_data"};

            String keySelection = "";
            for (String key : keys) {
                if (!TextUtils.isEmpty(keySelection))
                    keySelection = keySelection.concat(" or ");
                keySelection = keySelection.concat("_data LIKE '%" + key + "%'");
            }

            if (includeExtNames == null && exclusiveExtNames == null)
                return scanFilesFromDB(results, context, projection, keySelection);

            if (includeExtNames != null) {
                String includeSelection = "";
                for (String extName : includeExtNames) {
                    if (!TextUtils.isEmpty(includeSelection))
                        includeSelection = includeSelection.concat(" or ");
                    includeSelection = includeSelection.concat("_data LIKE '%" + extName + "'");
                }

                if (!TextUtils.isEmpty(keySelection) && !TextUtils.isEmpty(includeSelection))
                    includeSelection = keySelection + " AND " + includeSelection;
                else if (!TextUtils.isEmpty(keySelection) && TextUtils.isEmpty(includeSelection))
                    includeSelection = keySelection;
                scanFilesFromDB(results, context, projection, includeSelection);
            }

            if (exclusiveExtNames != null) {
                String exclusiveSelection = "";
                for (String extName : exclusiveExtNames) {
                    if (!TextUtils.isEmpty(exclusiveSelection))
                        exclusiveSelection = exclusiveSelection.concat(" and ");
                    exclusiveSelection = exclusiveSelection.concat("_data NOT LIKE '%" + extName + "'");
                }

                if (!TextUtils.isEmpty(keySelection) && !TextUtils.isEmpty(exclusiveSelection))
                    exclusiveSelection = keySelection + " AND " + exclusiveSelection;
                else if (!TextUtils.isEmpty(keySelection) && TextUtils.isEmpty(exclusiveSelection))
                    exclusiveSelection = keySelection;
                scanFilesFromDB(results, context, projection, exclusiveSelection);
            }
            return true;
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return false;
        }
    }

    private static boolean scanFilesFromDB(List<String> results, Context context, String[] projection, String selection) {
        Cursor cursor = null;
        try {
            // Search from External Database
            cursor = context.getContentResolver().query(Uri.parse("content://media/external/file"), projection, selection, null, null);
            if (cursor == null)
                return false;

            while (cursor.moveToNext()) {
                String url = cursor.getString(1);
                results.add(url);
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return false;
        } finally {
            Utils.close(cursor);
        }

        return true;
    }

}
