package com.ushareit.core.utils;

import android.media.ExifInterface;

import com.ushareit.core.io.FileUtils;
import com.ushareit.core.utils.i18n.NumberUtils;

import java.io.File;
import java.io.IOException;

public final class ExifUtils {

    public static class ExifInfo {
        private String mFileName;
        private long mFileSize;

        private long mDateTime;

        private int mImageWidth;
        private int mImageHeight;

        private int mOrientation;
        private boolean mFlip;

        private String mMaker;
        private String mModel;

        private double mLongitude;
        private double mLatitude;

        public String getFileName() {
            return mFileName;
        }

        public long getFileSize() {
            return mFileSize;
        }

        public long getDataTime() {
            return mDateTime;
        }

        public int getImageWidth() {
            return mImageWidth;
        }

        public int getImageHeight() {
            return mImageHeight;
        }

        public int getOrientation() {
            return mOrientation;
        }

        public boolean getFlip() {
            return mFlip;
        }

        public String getMaker() {
            return mMaker;
        }

        public String getModel() {
            return mModel;
        }

        public double getLongitude() {
            return mLongitude;
        }

        public double getLatitude() {
            return mLatitude;
        }

        protected ExifInfo(String fileName, long fileSize, long dateTime, int width, int height,
                           int orientation, boolean flip, String maker, String model, double lng, double lat) {
            mFileName = fileName;
            mFileSize = fileSize;
            mDateTime = dateTime;
            mImageWidth = width;
            mImageHeight = height;
            mOrientation = orientation;
            mFlip = flip;
            mMaker = maker;
            mModel = model;
            mLongitude = lng;
            mLatitude = lat;
        }
    }

    public static int getImageOrientation(String filepath) {
        int digree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            exif = null;
        } catch (Throwable e){
            exif = null;
        }
        if (exif != null) {
            int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (ori) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    digree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    digree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    digree = 270;
                    break;
                default:
                    digree = 0;
                    break;
            }
        }

        return digree;
    }

    public static ExifInfo getExifInfo(String filepath) {
        String mime = FileUtils.getMimeType(filepath);
        if (mime == null || !mime.contains("jpeg"))
            return null;

        ExifInfo ret = null;
        try {
            File f = new File(filepath);
            ExifInterface exif = new ExifInterface(filepath);

            String dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME);
            long date = NumberUtils.parseDateTimeFromString(dateStr);

            int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            boolean flip = false;
            switch (ori) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    flip = true;
                    ori = 0;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                    ori = 0;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    flip = true;
                    ori = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    ori = 90;
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    flip = true;
                    ori = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    ori = 180;
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    flip = true;
                    ori = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    ori = 270;
                    break;
                default:
                    flip = false;
                    ori = 0;
            }

            ret = new ExifInfo(f.getName(), f.length(), date,
                    exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
                    exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0),
                    ori, flip,
                    exif.getAttribute(ExifInterface.TAG_MAKE),
                    exif.getAttribute(ExifInterface.TAG_MODEL),
                    exif.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, 0.0d),
                    exif.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, 0.0d));
        } catch (Exception e) {
        }
        return ret;
    }
}
