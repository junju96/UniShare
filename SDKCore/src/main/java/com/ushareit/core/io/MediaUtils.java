package com.ushareit.core.io;

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Pair;

import com.ushareit.core.os.AndroidHelper;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.Reflector;
import com.ushareit.core.utils.i18n.LocaleUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaUtils {

    private static final String TAG = "MediaUtils";

    private static final Uri IMAGE_STORAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final Uri VIDEO_STORAGE_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private static final Uri AUDIO_STORAGE_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private static List<String> sNoMediaPaths = new ArrayList<String>();
    private static Set<String> sMediaPaths = new HashSet<String>();

    /**
     * Del record from library datebase and notify scan
     * @param context
     * @param file
     */
    public static void scanFileForDel(final Context context, File file) {
        if (file == null)
            return;

        doDeleteFromDB(context, file, null);

        try {
            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, null);
        } catch (Exception e) { // maybe throw SecurityException, about files read and write permissions
            Logger.d(TAG, e.toString());
        }
    }

    public static void scanFile(final Context context, File file, boolean forceInsert) {
        if (file == null || !file.exists())
            return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0)
                return;
            for (File f : files)
                scanFile(context, f, forceInsert);

            return;
        }

        // photo, music and video in private folder must maunel insert. scan file is not valid.
        if (forceInsert && hasNoMediaFile(file))
            insertFileToDB(context, file);

        try {
            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, null);
        } catch (Exception e) { // maybe throw SecurityException, about files read and write permissions
            Logger.d(TAG, e.toString());
        }
    }

    public static void insertFileToDB(Context context, File file) {
        if (file == null || !file.exists())
            return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0)
                return;
            for (File f : files)
                insertFileToDB(context, f);

            return;
        }
        doInsertToDB(context, file);
    }

    public static void deleteFileFromDB(Context context, File file) {
        deleteFileFromDB(context, file, null);
    }

    public static void deleteFileFromDB(Context context, File file, String pMimeType) {
        if (file == null || !file.exists())
            return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0)
                return;
            for (File f : files)
                deleteFileFromDB(context, f, pMimeType);

            return;
        }
        doDeleteFromDB(context, file, pMimeType);
    }

    public static void clearNoMediasCache() {
        sNoMediaPaths.clear();
        sMediaPaths.clear();
    }

    public static boolean hasNoMediaFile(File file) {
        if (file == null || !file.exists())
            return false;

        for (String path : sNoMediaPaths)
            if (file.getAbsolutePath().contains(path))
                return true;
        while ((file = file.getParentFile()) != null) {
            String pPath = file.getAbsolutePath();
            if (sMediaPaths.contains(pPath))
                return false;
            if (new File(file.getAbsolutePath(), ".nomedia").exists()) {
                sNoMediaPaths.add(file.getAbsolutePath());
                return true;
            }
            sMediaPaths.add(file.getAbsolutePath());
        }
        return false;
    }

    public static void restoreSysMediaLib(Context context, File file) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            return;
        Uri fileUri = MediaStore.Files.getContentUri("external");
        String[] projects = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA
        };

        Cursor cursor = null;
        ContentResolver resolver = context.getContentResolver();
        List<Pair<String, Integer>> items = new ArrayList<Pair<String, Integer>>();
        try {
            cursor = resolver.query(fileUri, projects, LocaleUtils.formatStringIgnoreLocale("%s LIKE ?", MediaStore.Files.FileColumns.DATA), new String[] {file.getAbsolutePath() + "%"}, null);
            if (cursor == null)
                return;

            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                if (hasNoMediaFile(new File(path)))
                    continue;

                int fileType = MediaFile.getFileTypeForMimeType(MediaFile.getMimeTypeForFile(path));
                int mediaType;
                if (MediaFile.isAudioFileType(fileType))
                    mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
                else if (MediaFile.isVideoFileType(fileType))
                    mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                else if (MediaFile.isImageFileType(fileType))
                    mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
                else
                    continue;

                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                items.add(new Pair<String, Integer>(id, mediaType));
            }
        } catch (Exception e) {
            Logger.w(TAG, "query media file items failed: " + file.getAbsolutePath(), e);
        } finally {
            Utils.close(cursor);
        }

        try {
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            for (Pair<String, Integer> item : items) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Files.FileColumns.MEDIA_TYPE, item.second);
                ContentProviderOperation operation = ContentProviderOperation.newUpdate(fileUri).withValues(values).withSelection(LocaleUtils.formatStringIgnoreLocale("%s = %s", MediaStore.Files.FileColumns._ID, item.first), null).build();
                operations.add(operation);
            }
            resolver.applyBatch(MediaStore.AUTHORITY, operations);
        } catch (Exception e) {
            Logger.w(TAG, "batch update media type failed: " + file.getAbsolutePath(), e);
        }
    }

    private static void doInsertToDB(Context context, File file) {
        if (file == null || !file.exists())
            return;

        String mimeType = FileUtils.getMimeType(file);
        try {
            if (mimeType.startsWith("image") && !fileExistInDB(context, IMAGE_STORAGE_URI, file.getAbsolutePath()))
                insertImage(context, file, mimeType);
            else if (mimeType.startsWith("video") && !fileExistInDB(context, VIDEO_STORAGE_URI, file.getAbsolutePath()))
                insertVideo(context, file, mimeType);
            else if (mimeType.startsWith("audio") && !fileExistInDB(context, AUDIO_STORAGE_URI, file.getAbsolutePath()))
                insertAudio(context, file, mimeType);
        } catch (Exception e) {
            Logger.w(TAG, "insertMediaDB error " + e.getMessage());
        }
    }

    private static void doDeleteFromDB(Context context, File file, String pMimeType) {
        if (file == null)
            return;

        try {
            String mimeType = TextUtils.isEmpty(pMimeType) ? FileUtils.getMimeType(file) : pMimeType;
            if (!file.exists()) {

                if (mimeType.startsWith("image"))
                    context.getContentResolver().delete(IMAGE_STORAGE_URI, MediaStore.Images.Media.DATA + "='" + file.getAbsolutePath() + "'", null);
                else if (mimeType.startsWith("video"))
                    context.getContentResolver().delete(VIDEO_STORAGE_URI, MediaStore.Video.Media.DATA + "='" + file.getAbsolutePath() + "'", null);
                else if (mimeType.startsWith("audio"))
                    context.getContentResolver().delete(AUDIO_STORAGE_URI, MediaStore.Audio.Media.DATA + "='" + file.getAbsolutePath() + "'", null);
            } else {
                Logger.w(TAG, "deleteMediaDB : file is exits");
            }
        } catch (Exception e) {
            Logger.w(TAG, "deleteMediaDB : file error " + e.getMessage());
        }
    }

    private static Uri insertImage(Context context, File file, String mimeType) {
        Logger.d("liufs", "insertImage");
        ContentValues values = new ContentValues(6);
        values.put(MediaStore.Images.Media.TITLE, file.getName());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.ORIENTATION, 0);
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        return context.getContentResolver().insert(IMAGE_STORAGE_URI, values);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    private static Uri insertVideo(Context context, File file, String mimeType) {
        if (Build.VERSION.SDK_INT < AndroidHelper.ANDROID_VERSION_CODE.GINGERBREAD_MR1) {
            Assert.fail("Can not support insert video to system media library under GINGERBREAD_MR1!");
            return null;
        }

        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            ContentValues values = new ContentValues();
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            values.put(MediaStore.Video.Media.TITLE, TextUtils.isEmpty(title) ? file.getName() : title);
            values.put(MediaStore.Video.Media.DISPLAY_NAME, file.getName());
            String date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            values.put(MediaStore.Video.Media.DATE_MODIFIED, !TextUtils.isEmpty(date) && Utils.isInt(date) ? Utils.toInt(date) : System.currentTimeMillis());
            values.put(MediaStore.Video.Media.MIME_TYPE, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            values.put(MediaStore.Video.Media.DURATION, !TextUtils.isEmpty(duration) && Utils.isInt(duration) ? Utils.toInt(duration) : 0);
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            values.put(MediaStore.Video.Media.WIDTH, !TextUtils.isEmpty(width) && Utils.isInt(width) ? Utils.toInt(width) : 0);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            values.put(MediaStore.Video.Media.HEIGHT, !TextUtils.isEmpty(height) && Utils.isInt(height) ? Utils.toInt(height) : 0);
            values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
            return context.getContentResolver().insert(VIDEO_STORAGE_URI, values);
        } catch (Throwable e) {
            Logger.w(TAG, "Can not insert video file to media library:" + file.getAbsolutePath(), e);
        } finally {
            Utils.close(retriever);
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    private static Uri insertAudio(Context context, File file, String mimeType) {
        if (Build.VERSION.SDK_INT < AndroidHelper.ANDROID_VERSION_CODE.GINGERBREAD_MR1) {
            Assert.fail("Can not support insert music to system media library under GINGERBREAD_MR1!");
            return null;
        }

        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            ContentValues values = new ContentValues();
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            values.put(MediaStore.Audio.Media.TITLE, TextUtils.isEmpty(title) ? file.getName() : title);
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, file.getName());
            String date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            values.put(MediaStore.Audio.Media.DATE_MODIFIED, !TextUtils.isEmpty(date) && Utils.isInt(date) ? Utils.toInt(date) : System.currentTimeMillis());
            values.put(MediaStore.Audio.Media.MIME_TYPE, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            values.put(MediaStore.Audio.Media.DURATION, !TextUtils.isEmpty(duration) && Utils.isInt(duration) ? Utils.toInt(duration) : 0);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (!TextUtils.isEmpty(album))
                values.put(MediaStore.Audio.Media.ALBUM, album);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (!TextUtils.isEmpty(artist))
                values.put(MediaStore.Audio.Media.ARTIST, artist);
            values.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
            return context.getContentResolver().insert(AUDIO_STORAGE_URI, values);
        } catch (Throwable e) {
            Logger.w(TAG, "Can not insert audio file to media library:" + file.getAbsolutePath(), e);
        } finally {
            Utils.close(retriever);
        }
        return null;
    }

    private static boolean fileExistInDB(Context context, Uri uri, String absolutePath) {
        String seclection = LocaleUtils.formatStringIgnoreLocale("%s = ?", MediaStore.MediaColumns.DATA);
        String[] selectionArgs = { absolutePath };
        Cursor cursor = context.getContentResolver().query(uri, null, seclection, selectionArgs, null);
        boolean exist = (cursor == null) ? false : cursor.getCount() > 0;
        Logger.d("liufs", absolutePath + ":" + exist);
        Utils.close(cursor);
        return exist;
    }

    public static boolean isRecordingAlbum(String path) {
        return path.contains("/Audio/SoundRecorder") || path.contains("/Audio");
    }

    public static boolean isRecordingArtist(String name) {
        return name.contains("RecordArtist") || name.contains("Your recordings");
    }

    /*
     this file for invoke framework MediaFile via Reflect
     */
    static class MediaFile {
        private final static String TAG = "MediaFile";

        private static Class mMediaFileClazz = null;
        static {
            try {
                mMediaFileClazz = Class.forName("android.media.MediaFile");
            } catch (Exception e) {
                Logger.w(TAG, "android.mediaMediaFile class not found!", e);
            }
        }
        public static String getMimeTypeForFile(String path) {
            if (mMediaFileClazz == null)
                return null;

            try {
                return (String) Reflector.invokeStaticMethod(mMediaFileClazz, "getMimeTypeForFile", new Class[]{String.class}, new Object[]{path});
            } catch (Exception e) {
                Logger.d(TAG, "getMimeTypeForFile failed", e);
            }
            return null;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public static int getFileTypeForMimeType(String mimeType) {
            if (mMediaFileClazz == null || mimeType == null)
                return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? MediaStore.Files.FileColumns.MEDIA_TYPE_NONE : 0;

            try {
                return (Integer) Reflector.invokeStaticMethod(mMediaFileClazz, "getFileTypeForMimeType", new Class[]{String.class}, new Object[]{mimeType});
            } catch (Exception e) {
                Logger.d(TAG, "getFileTypeForMimeType failed", e);
            }

            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? MediaStore.Files.FileColumns.MEDIA_TYPE_NONE : 0;

        }

        public static boolean isAudioFileType(int fileType) {
            if (mMediaFileClazz == null)
                return false;

            try {
                return((Boolean) Reflector.invokeStaticMethod(mMediaFileClazz, "isAudioFileType", new Class[]{int.class}, new Object[]{fileType}));
            } catch (Exception e) {
                Logger.d(TAG, "isAudioFileType failed", e);
            }
            return false;
        }

        public static boolean isVideoFileType(int fileType) {
            if (mMediaFileClazz == null)
                return false;

            try {
                return((Boolean) Reflector.invokeStaticMethod(mMediaFileClazz, "isVideoFileType", new Class[]{int.class}, new Object[]{fileType}));
            } catch (Exception e) {
                Logger.d(TAG, "isVideoFileType failed", e);
            }
            return false;
        }

        public static boolean isImageFileType(int fileType) {
            if (mMediaFileClazz == null)
                return false;

            try {
                return((Boolean) Reflector.invokeStaticMethod(mMediaFileClazz, "isImageFileType", new Class[]{int.class}, new Object[]{fileType}));
            } catch (Exception e) {
                Logger.d(TAG, "isImageFileType failed", e);
            }
            return false;
        }

        public static boolean isPlayListFileType(int fileType) {
            if (mMediaFileClazz == null)
                return false;

            try {
                return((Boolean) Reflector.invokeStaticMethod(mMediaFileClazz, "isPlayListFileType", new Class[]{int.class}, new Object[]{fileType}));
            } catch (Exception e) {
                Logger.d(TAG, "isPlayListFileType failed", e);
            }
            return false;
        }
    }
}
