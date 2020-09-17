package com.ushareit.core.utils.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.text.TextUtils;

import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.utils.PackageUtils;
import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * contains all bitmap related utility functions.
 */
public final class BitmapUtils {
    private static final String tag = "BitmapUtils";

    public static final int THUMBNAIL_WIDTH = 800;
    public static final int THUMBNAIL_HEIGHT = 480;

    private BitmapUtils() {}

    // Only from File, if it has no exist file, return null, and then please call getPhotoThumbnailStreamByKind
    public static String getPhotoThumbnailPathByKind(ContentResolver cr, int itemId, int kind, int width, int height) {
        if (kind == Thumbnails.FULL_SCREEN_KIND) {
            String imagePath = getImagePathById(cr, itemId);
            if (StringUtils.isBlank(imagePath))
                return "";
            int rate = getPhotoSampleSize(imagePath, width, height, 0);
            if (rate <= 1)
                return imagePath;
            else
                return "";
        } else if (kind == Thumbnails.MICRO_KIND)
            return getThumbnailPathByKind(cr, itemId, Thumbnails.MICRO_KIND);
        else
            return getThumbnailPathByKind(cr, itemId, Thumbnails.MINI_KIND);
    }

    public static ByteArrayOutputStream getPhotoThumbnailStreamByKind(ContentResolver cr, int itemId, int kind, int width, int height) {
        if (kind != Thumbnails.FULL_SCREEN_KIND)
            return getPhotoMicroThumbnailStream(cr, itemId);

        // now try to make the thumbnail on the fly from original raw image file

        // find image path
        String imagePath = getImagePathById(cr, itemId);
        if (TextUtils.isEmpty(imagePath))
            return null;

        // decode it to Bitmap in memory, with specified (decreased) sample size
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = getPhotoSampleSize(imagePath, width, height, 0);
            bitmap = BitmapFactory.decodeFile(imagePath, opts);
        } catch (OutOfMemoryError e) {
            System.gc();
        }
        if (bitmap == null)
            return null;

        // compress it again to PNG or JPG, return them as a byte array stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String ext = FileUtils.getExtension(imagePath);
        boolean isPNG = "png".equalsIgnoreCase(ext);
        boolean succ = bitmap.compress(isPNG ? CompressFormat.PNG : CompressFormat.JPEG, 80, out);
        return succ ? out : null;
    }

    public static Bitmap getPhotoThumbnail(ContentResolver cr, int mediaId) {
        // get photo orientation
        int orientation = 0;
        Cursor cursor = null;
        try {
            String whereClause = Media._ID + "=" + mediaId;
            String[] originprj = { Media.ORIENTATION };
            cursor = cr.query(Media.EXTERNAL_CONTENT_URI, originprj, whereClause, null, Media.DEFAULT_SORT_ORDER);
            if (cursor != null && cursor.moveToFirst()) {
                orientation = cursor.getInt(0);
            }
        } finally {
            Utils.close(cursor);
        }

        return getPhotoThumbnail(cr, mediaId, orientation, null);
    }

    public static Bitmap getPhotoThumbnail(ContentResolver cr, int mediaId, int orientation, BitmapFactory.Options opts) {
        // note: this will also create the thumbnail internally if not yet generated, so may be slow in such cases
        Bitmap bitmap = Thumbnails.getThumbnail(cr, mediaId, Thumbnails.MICRO_KIND, opts);
        if (bitmap == null)
            return null;

        Matrix matrix = new Matrix();
        matrix.setRotate(orientation); // TODO can we bypass re-create bitmap if rotate is actually not needed
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // TODO why "true" parameter here?
    }

    // TODO can we find some way to just get the path or stream of thumbnail (in compressed format) to bypass "decode to bitmap and compress again" overhead.
    public static ByteArrayOutputStream getPhotoMicroThumbnailStream(ContentResolver cr, int mediaId) {
        Bitmap bitmap = getPhotoThumbnail(cr, mediaId);
        if (bitmap == null)
            return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, out);
        return out;
    }

    public static Bitmap getVideoThumbnail(ContentResolver cr, int mediaId) {
        return MediaStore.Video.Thumbnails.getThumbnail(cr, mediaId, Thumbnails.MICRO_KIND, null);
    }

    public static ByteArrayOutputStream getVideoMicroThumbnailStream(ContentResolver cr, int mediaId) {
        Bitmap bitmap = getVideoThumbnail(cr, mediaId);
        if (bitmap == null)
            return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, out);
        return out;
    }

    public static ByteArrayOutputStream getFileMicroThumbnailStream(Context context, String filePath) {
        String mimeType = FileUtils.getMimeType(filePath);
        if (mimeType == null)
            return null;

        Bitmap bitmap = null;
        if (mimeType.startsWith(FileUtils.PREFIX_PHOTO))
            bitmap = extractThumbnailFromPhoto(filePath);
        else if (mimeType.startsWith(FileUtils.PREFIX_VIDEO))
            bitmap = extractThumbnailFromVideo(filePath);
        else if (mimeType.startsWith(FileUtils.PREFIX_APK)) {
            Drawable drawable = PackageUtils.Extractor.getPackageIconByPath(context, filePath);
            bitmap = (drawable != null) ? ((BitmapDrawable)drawable).getBitmap() : null;
        }

        if (bitmap == null)
            return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, out);
        return out;
    }

    public static String getMiniThumbnailPath(ContentResolver cr, int imageid) {
        String path = null;

        String[] orientationProjection = { Media.ORIENTATION };
        String selection = Media._ID + "=?";
        String[] selectionArgs = { String.valueOf(imageid) };

        int orientation = 0;
        Cursor origin = cr.query(Media.EXTERNAL_CONTENT_URI, orientationProjection, selection, selectionArgs, Media.DEFAULT_SORT_ORDER);
        try {
            if (origin != null && origin.moveToFirst()) {
                orientation = origin.getInt(0);
            }
        } finally {
            Utils.close(origin);
        }

        if (orientation != 0) // if orientation != 0, we should use another method to get thumbnail
            return path;

        String[] projection = { Thumbnails.DATA };
        Cursor cursor = Thumbnails.queryMiniThumbnail(cr, imageid, Thumbnails.MINI_KIND, projection);
        try {
            if (cursor != null && cursor.moveToFirst())
                path = cursor.getString(0);
        } finally {
            Utils.close(cursor);
        }

        return path;
    }

    public static String getThumbnailPathByKind(ContentResolver cr, int imageid) {
        String[] projection = { Thumbnails.DATA };
        String selection = Thumbnails.IMAGE_ID + "=?";
        String[] selectionArgs = { String.valueOf(imageid) };

        String path = null;
        Cursor cursor = cr.query(Thumbnails.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);

        try {
            if (cursor != null && cursor.moveToFirst())
                path = cursor.getString(0);
        } finally {
            Utils.close(cursor);
        }

        return path;
    }

    public static String getThumbnailPathByKind(ContentResolver cr, int imageid, int kind) {
        String[] projection = { Thumbnails.DATA };

        String path = null;
        Cursor cursor = Thumbnails.queryMiniThumbnail(cr, imageid, kind, projection);
        try {
            if (cursor != null && cursor.moveToFirst())
                path = cursor.getString(0);
        } finally {
            Utils.close(cursor);
        }

        return path;
    }

    public static String getImagePathById(ContentResolver cr, int imageid) {
        String[] projection = { Media.DATA };
        String selection = BaseColumns._ID + "=?";
        String[] selectionArgs = { String.valueOf(imageid) };

        String imagePath = null;
        Cursor cursor = cr.query(Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        try {
            if (cursor != null && cursor.moveToFirst())
                imagePath = cursor.getString(0);
        } finally {
            Utils.close(cursor);
        }
        return imagePath;
    }

    public static void recycle(Bitmap bitmap) {
        try {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                Logger.v(tag, "recycle a bitmap.");
            }
        } catch (Throwable e) {}
    }

    public static boolean saveBitmapToPngFile(Bitmap bitmap, File file) {
        return saveBitmapToFile(bitmap, file, CompressFormat.PNG, 100);
    }

    public static boolean saveBitmapToFile(Bitmap bitmap, File file, CompressFormat format, int quality) {
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(file));
            return bitmap.compress(format, quality, stream);
        } catch (FileNotFoundException e) {
            Logger.e(tag, "FileNotFoundException", e);
            return false;
        } finally {
            Utils.close(stream);
        }
    }

    public static Bitmap extractThumbnailFromPhoto(String file) {
        return extractThumbnailFromPhoto(file, 256, 256);
    }

    public static Bitmap extractThumbnailFromPhoto(String file, int width, int height) {
        Bitmap source = getThumbnailFromFile(file, width, height);
        // if ((source != null) && ((source.getWidth() > width || source.getHeight() > height)))
        // return ThumbnailUtils.extractThumbnail(source, width, width, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return source;
    }

    public static Bitmap extractThumbnailFromVideo(String file) {
        return ThumbnailUtils.createVideoThumbnail(file, Thumbnails.MINI_KIND);
    }

    public static Bitmap getThumbnailFromFile(String file, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, opts);

        opts.inSampleSize = computeSampleSize(opts, Math.min(width, height), width * height);
        opts.inJustDecodeBounds = false;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file, opts);
    }

    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        double sample = (1.0 * options.outWidth * options.outHeight) / (maxNumOfPixels * roundedSize * roundedSize);
        if (sample <= 0.5)
            return roundedSize / 2;
        else
            return roundedSize;
    }

    public static Bitmap getBitmapFromStream(InputStream stream, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, opts);

        opts.inSampleSize = computeFullSampleSize(opts, Math.min(width, height), width * height);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(stream, null, opts);
    }

    public static Bitmap getBitmapFromFile(String file, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, opts);

        opts.inSampleSize = computeFullSampleSize(opts, Math.min(width, height), width * height);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, opts);
    }

    public static int computeFullSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int)Math.ceil(Math
                .sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int)Math.min(Math
                .floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static int getPhotoSampleSize(String path, int width, int height, int orientation) {
        int sampleSize = 1;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        if (opts.outWidth != 0 && opts.outHeight != 0) {
            Rect screenRect = new Rect(0, 0, width, height);
            Rect imageRect = new Rect(0, 0, opts.outWidth, opts.outHeight);
            Rect displayRect = getImageRectOnScreen(screenRect, imageRect, orientation);
            if (orientation == 90 || orientation == 270)
                sampleSize = opts.outWidth / displayRect.height();
            else
                sampleSize = opts.outWidth / displayRect.width();
        }
        if (sampleSize <= 0)
            sampleSize = 1;
        return sampleSize;
    }

    public static Rect getImageRectOnScreen(Rect screen, Rect image, int orientation) {
        Rect realImage = image;
        if (orientation == 90 || orientation == 270)
            realImage = new Rect(realImage.top, realImage.left, realImage.bottom, realImage.right);
        int deltaX = screen.width() - realImage.width();
        int deltaY = screen.height() - realImage.height();

        if (deltaX >= 0 && deltaY >= 0) {
            return new Rect(realImage);
        } else {
            float rateX = (float)realImage.width() / screen.width();
            float rateY = (float)realImage.height() / screen.height();
            if (Float.floatToIntBits(rateX) == Float.floatToIntBits(rateY)) {
                return new Rect(screen);
            } else if (rateX > rateY) {
                Rect realRect = new Rect(screen);
                int realHeight = realImage.height() * screen.width() / realImage.width();
                realRect.bottom = realRect.top + realHeight;
                return realRect;
            } else {
                Rect realRect = new Rect(screen);
                int realWidth = realImage.width() * screen.height() / realImage.height();
                realRect.right = realRect.left + realWidth;
                return realRect;
            }
        }
    }

    public static Bitmap createNinePatchBitmap(Bitmap bitmap, int width, int height, int padding) {
        NinePatch np = new NinePatch(bitmap, bitmap.getNinePatchChunk(), null);
        Bitmap output_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output_bitmap.setDensity(bitmap.getDensity());
        Canvas canvas = new Canvas(output_bitmap);
        np.draw(canvas, new Rect(padding, padding, width - padding, height - padding));
        return output_bitmap;
    }

    public static Bitmap scaleBitmap(Bitmap src, int compareW, int compareH, int expW, int expH) {
        Assert.notNull(src);

        try {
            int width = src.getWidth();
            int height = src.getHeight();
            if (width <= compareW && height <= compareH)
                return src;

            float scale = Math.min(expW * 1.0f / width, expH * 1.0f / height);
            if (scale > 1.0f)
                return src;

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            Bitmap dst = Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
            recycle(src);
            return dst;
        } catch (Throwable e) {
            Logger.w(tag, "scaleBitmap failed!", e);
        }
        return src;
    }
}
