package com.ushareit.core.cache;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.io.sfile.SFile.Filter;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.utils.ui.EmojiFilterUtils;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.lang.thread.TaskHelper.RunnableWithName;
import com.ushareit.core.lang.ContentType;
import com.ushareit.core.utils.MimeTypes;
import com.ushareit.core.algo.CommonExtendHashUtils;
import com.ushareit.core.utils.WWUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /* file store for remote items.
 * /* external storage: (under main sdcard root)
 * /* QieZi\ or SHAREit\
 * /* Apps\
 * /* Contacts\
 * /* ...
 * /* Files\
 * /* .tmp\ // received files that not completely downloaded yet
 * /* .thumbnails\ // all received items' thumbnails
 */
public final class DefaultRemoteFileStore implements IRemoteFileStore {
    private static final String TAG = "DefaultRemoteFileStore";

    private static final String DIR_EXTERNAL_CACHES = ".caches/";
    private static final String DIR_EXTERNAL_TEMP = DIR_EXTERNAL_CACHES + ".tmp/";
    private static final String DIR_EXTERNAL_CACHE = DIR_EXTERNAL_CACHES + ".cache/";
//    private static final String DIR_EXTERNAL_CLOUD_THUMB = DIR_EXTERNAL_CACHES + ".cloudthumbs/";
    private static final String DIR_EXTERNAL_LOG = DIR_EXTERNAL_CACHES + ".log/";
    private static final String DIR_EXTERNAL_THUMBNAIL = ".thumbnails/";

    // sub dirs for content items
    private static final String DIR_EXTERNAL_APP = "apps/";
//    private static final String DIR_EXTERNAL_CONTACT = "contacts/";
//    private static final String DIR_EXTERNAL_MUSIC = "audios/";
//    private static final String DIR_EXTERNAL_VIDEO = "videos/";
    private static final String DIR_EXTERNAL_PICTURE = "pictures/";
    private static final String DIR_EXTERNAL_FILE = "files/";
    private static final String DIR_EXTERNAL_DOWNLOAD = "download/";
//    private static final String DIR_EXTERNAL_PAYMENT = "payment/";

//    private static final String DIR_MEDIA_THUMBNAIL = ".mediathumbs/";

    private SFile mExternalAppRootDir;

    private SFile mExternalTempDir;
    private SFile mExternalCacheDir;
    private SFile mExternalThumbnailDir;
//    private SFile mExternalCloudThumbDir;
    private SFile mExternalLogDir;
    private SFile mExternalDownloadDir;
//    private SFile mExternalPaymentDir;

//    private SFile mMediaThumbDir;

    private Context mContext;

    public DefaultRemoteFileStore(Context context, SFile appRoot) {
        this(context, appRoot, true);
    }

    public DefaultRemoteFileStore(Context context, SFile appRoot, boolean deleteTemp) {
        mContext = context;
        mExternalAppRootDir = appRoot;

        Logger.d(TAG, "remote file stored in:" + appRoot.getAbsolutePath());

        initAppDirs(deleteTemp);

        if (deleteTemp) {
            TaskHelper.execZForSDK(new RunnableWithName("DefaultRemoteFileStore.removeFolder") {
                @Override
                public void execute() {
                    removeInvalidFolders();
                }
            });
        }
    }

    @Override
    public SFile getExternalRootDir() {
        Assert.notNull(mExternalAppRootDir);
        if (!mExternalAppRootDir.exists())
        	mExternalAppRootDir.mkdirs();
        return mExternalAppRootDir;
    }

    @Override
    public SFile getExternalTempDir() {
        Assert.notNull(mExternalTempDir);
        if (!mExternalTempDir.exists())
            mExternalTempDir.mkdirs();
        return mExternalTempDir;
    }

    @Override
    public File getPrimaryTempDir() {
        String rootDir = WWUtils.getAppRootDirName(mContext) + "/";
        String tempPath = rootDir + DIR_EXTERNAL_TEMP;
        File primaryTmp = new File(Environment.getExternalStorageDirectory(), tempPath);
        if (!primaryTmp.exists())
            primaryTmp.mkdirs();
        return primaryTmp;
    }

    @Override
    public SFile getPaymentDir() {
//        Assert.notNull(mExternalPaymentDir);
//        if (!mExternalPaymentDir.exists())
//            mExternalPaymentDir.mkdirs();
        return getExternalCacheDir();
    }

    @Override
    public SFile getExternalCacheDir() {
        Assert.notNull(mExternalCacheDir);
        if (!mExternalCacheDir.exists())
            mExternalCacheDir.mkdirs();
        return mExternalCacheDir;
    }

    @Override
    public SFile getExternalLogDir() {
        Assert.notNull(mExternalLogDir);
        if (!mExternalLogDir.exists())
            mExternalLogDir.mkdirs();
        return mExternalLogDir;
    }

    @Override
    public SFile createTempFileName(String suggestedFileName) {
        String tempFileName = suggestedFileName;
        if (TextUtils.isEmpty(tempFileName))
            tempFileName = UUID.randomUUID().toString() + ".tmp";
        return SFile.create(getExternalTempDir(), tempFileName);
    }

    @Override
    public SFile getRemoteItemDir(ContentType type, String fileName) {
        return getRemoteItemDir(type, null, fileName);
    }

    @Override
    public SFile getRemoteItemDir(ContentType type, String parentPath, String fileName) {
        if (type == ContentType.FILE && TextUtils.isEmpty(parentPath)) {
            type = MimeTypes.getRealContentType(FileUtils.getExtension(fileName));
            type = (type == null) ? ContentType.FILE : type;
        }

        String subDir = null;
        switch (type) {
            case PHOTO:
                subDir = DIR_EXTERNAL_PICTURE;
                break;
            case GAME:
            case APP:
                subDir = DIR_EXTERNAL_APP;
                break;
            case MUSIC:
//                subDir = DIR_EXTERNAL_MUSIC;
//                break;
            case VIDEO:
//                subDir = DIR_EXTERNAL_VIDEO;
//                break;
            case CONTACT:
//                subDir = DIR_EXTERNAL_CONTACT;
//                break;
            case FILE:
                subDir = DIR_EXTERNAL_FILE;
                break;
            default:
                Assert.isTrue(false, "can not create item dir by invalid type!");
                return null;
        }

        String dirPath = subDir;
        if (!TextUtils.isEmpty(parentPath))
            dirPath += parentPath;

        SFile subDirFile = SFile.create(mExternalAppRootDir, dirPath);
        if (!subDirFile.exists())
            subDirFile.mkdirs();

        return subDirFile;
    }

    @Override
    public SFile getThumbnailDir() {
        Assert.notNull(mExternalThumbnailDir);
        if (!mExternalThumbnailDir.exists())
            mExternalThumbnailDir.mkdirs();
        return mExternalThumbnailDir;
    }

    @Override
    public SFile getCloudThumbDir() {
//        Assert.notNull(mExternalCloudThumbDir);
//        if (!mExternalCloudThumbDir.exists())
//            mExternalCloudThumbDir.mkdirs();
        return getThumbnailDir();
    }

    @Override
    public SFile getRemoteItemThumbnail() {
        return SFile.create(getThumbnailDir(), "" + System.nanoTime());
    }

    @Override
    public SFile getCacheFile(String deviceId, String collectionId, String itemId, ContentType type, boolean isThumbnail, String ext) {
        String prefix = CommonExtendHashUtils.hash(deviceId + "_" + (TextUtils.isEmpty(collectionId) ? itemId : collectionId));
        String suffix = CommonExtendHashUtils.hash(itemId + type + (isThumbnail ? "thumbnail" : ""));
        String fileName = prefix + "_" + suffix + (TextUtils.isEmpty(ext) ? "" : ext);
        Logger.d(TAG, "get cache filename:" + fileName + ", length:" + fileName.length());
        return SFile.create(getExternalCacheDir(), fileName);
    }

    @Override
    public SFile getCacheFile(SFile parent, String deviceId, String collectionId, String itemId, ContentType type, boolean isThumbnail, String ext) {
        String prefix = CommonExtendHashUtils.hash(deviceId + "_" + (TextUtils.isEmpty(collectionId) ? itemId : collectionId));
        String suffix = CommonExtendHashUtils.hash(itemId + type + (isThumbnail ? "thumbnail" : ""));
        String fileName = ".cache_" + prefix + "_" + suffix + (TextUtils.isEmpty(ext) ? "" : ext);
        Logger.d(TAG, "get cache filename:" + fileName + ", length:" + fileName.length());
        return SFile.create(parent, fileName);
    }

    @Override
    public SFile getDownloadRootDir() {
        Assert.notNull(mExternalDownloadDir);
        if (!mExternalDownloadDir.exists())
            mExternalDownloadDir.mkdirs();
        return mExternalDownloadDir;
    }

    @Override
    public SFile getDownloadFile(ContentType type, String name, String url, String tag, boolean isDSV , boolean isFolder) {
        String ext = "";
        if(!isFolder){
            switch (type) {
                case MUSIC:
                    ext = url.endsWith(".esa") ? ".esa" : ".sa";
                    break;
                case VIDEO:
                    ext = url.endsWith(".esv") ? ".esv" : isDSV ? ".dsv" : (!url.endsWith(".dsv") || !url.endsWith(".tsv") ? ("." + FileUtils.getExtension(url)) : ".sv");
                    break;
                case APP:
                    ext = ".apk";
                    break;
                case PHOTO:
                    ext = ".jpeg";
                    break;
                default:
                    ext = url.substring(url.lastIndexOf("."));
                    break;
            }
        }

        if (!TextUtils.isEmpty(tag)) {
            name = "%%" + tag + "%%" + name;
        } else {
            name = name != null ? name.replaceFirst("%", "_") : name;
        }
        if(!TextUtils.isEmpty(name))
            name = FileUtils.escapeFileName(name);
        if(StringUtils.isBlank(name))
            name = "unknown";
        if (name.length() > 80)
            name = name.substring(0, 80);
        String fileName = name + ext;
        SFile dir = getDownloadItemDir(type, null, fileName);
        fileName = patchForReplaceFileNameEmoji(dir, fileName, false);
        return SFile.createUnique(dir, fileName);
    }

    @Override
    public SFile getDownloadTempFile(ContentType type, String name, String url) {
        String ext = ".tmp";

        if(!TextUtils.isEmpty(name))
            name = FileUtils.escapeFileName(name);
        if(StringUtils.isBlank(name))
            name = "unknown";
        if (name.length() > 80)
            name = name.substring(0, 80);
        String fileName = name + url.hashCode() + ext;
        SFile dir = getDownloadItemDir(type, null, fileName);
        fileName = patchForReplaceFileNameEmoji(dir, fileName, true);
        return SFile.create(dir, fileName);
    }

    @Override
    public SFile getDownloadCacheFileDir(ContentType type) {
        return getDownloadItemDir(type, DIR_EXTERNAL_CACHES, null);
    }

    private String patchForReplaceFileNameEmoji(SFile dir, String fileName, boolean isTemp) {
        if (isTemp) {
            SFile temp = SFile.create(dir, fileName);
            if (temp != null && temp.exists())
                return fileName;
        }
        return EmojiFilterUtils.filterEmoji(fileName);
    }

    @Override
    public SFile getDownloadFileDir(ContentType type) {
        return getDownloadItemDir(type, null, null);
    }

    @Override
    public SFile getCacheFile(String id, String url, ContentType type, boolean isTemp, boolean isThumbnail) {
        String prefix = CommonExtendHashUtils.hash(id + "_" + url);
        String suffix = CommonExtendHashUtils.hash(type + (isTemp ? "tmp" : "") + (isThumbnail ? "thumbnail" : ""));
        String fileName = prefix + "_" + suffix;
        Logger.d(TAG, "get cloud cache filename:" + fileName + ", length:" + fileName.length());
        return SFile.create(getDownloadCacheFileDir(type), fileName);
    }

    private SFile getDownloadItemDir(ContentType type, String parentPath, String fileName) {
        if (type == ContentType.FILE && TextUtils.isEmpty(parentPath)) {
            type = MimeTypes.getRealContentType(FileUtils.getExtension(fileName));
            type = (type == null) ? ContentType.FILE : type;
        }

        String subDir = DIR_EXTERNAL_DOWNLOAD;
        switch (type) {
            case PHOTO:
                subDir += DIR_EXTERNAL_PICTURE;
                break;
            case GAME:
            case APP:
                subDir += DIR_EXTERNAL_APP;
                break;
            case MUSIC:
//                subDir += DIR_EXTERNAL_MUSIC;
//                break;
            case VIDEO:
//                subDir += DIR_EXTERNAL_VIDEO;
//                break;
            case CONTACT:
//                subDir += DIR_EXTERNAL_CONTACT;
//                break;
            case FILE:
                subDir += DIR_EXTERNAL_FILE;
                break;
            default:
                Assert.isTrue(false, "can not create item dir by invalid type!");
                return null;
        }

        String dirPath = subDir;
        if (!TextUtils.isEmpty(parentPath))
            dirPath += parentPath;

        SFile subDirFile = SFile.create(mExternalAppRootDir, dirPath);
        if (!subDirFile.exists())
            subDirFile.mkdirs();

        return subDirFile;
    }

    @Override
    public long calculateCachesSize() {
        long size = 0;
        SFile files[] = listCaches();
        for (SFile file : files)
            size += file.length();
        return size;
    }

    @Override
    public void removeCaches() {
        SFile files[] = listCaches();
        for (SFile file : files)
            file.delete();
    }

    @Override
    public SFile getMediaThumbnailDir() {
        Assert.notNull(mExternalThumbnailDir);
        if (!mExternalThumbnailDir.exists())
            mExternalThumbnailDir.mkdirs();
        return mExternalThumbnailDir;
    }

    private final void initAppDirs(boolean deleteTemp) {
        if (!mExternalAppRootDir.exists())
            mExternalAppRootDir.mkdirs();

        FileUtils.removeNoMediaFile(mExternalAppRootDir);

        mExternalThumbnailDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_THUMBNAIL);
        if (!mExternalThumbnailDir.exists())
            mExternalThumbnailDir.mkdirs();
        FileUtils.createNoMediaFile(mExternalThumbnailDir);

        mExternalTempDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_TEMP);
        if (!mExternalTempDir.exists())
            mExternalTempDir.mkdirs();
        FileUtils.createNoMediaFile(mExternalTempDir);

        mExternalCacheDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_CACHE);
        if (!mExternalCacheDir.exists())
            mExternalCacheDir.mkdirs();
        FileUtils.createNoMediaFile(mExternalCacheDir);

//        mExternalCloudThumbDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_CLOUD_THUMB);
//        if (!mExternalCloudThumbDir.exists())
//            mExternalCloudThumbDir.mkdirs();
//        FileUtils.createNoMediaFile(mExternalCloudThumbDir);

        mExternalLogDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_LOG);
        if (!mExternalLogDir.exists())
            mExternalLogDir.mkdirs();
        FileUtils.createNoMediaFile(mExternalLogDir);

        mExternalDownloadDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_DOWNLOAD);
        if (!mExternalDownloadDir.exists())
            mExternalDownloadDir.mkdirs();

//        mExternalPaymentDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_PAYMENT);
//        if (!mExternalPaymentDir.exists())
//            mExternalPaymentDir.mkdirs();

//        SFile appSubDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_APP);
//        if (!appSubDir.exists())
//            appSubDir.mkdirs();

//        SFile picSubDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_PICTURE);
//        if (!picSubDir.exists())
//            picSubDir.mkdirs();

//        SFile musicSubDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_MUSIC);
//        if (!musicSubDir.exists())
//            musicSubDir.mkdirs();

//        SFile videoSubDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_VIDEO);
//        if (!videoSubDir.exists())
//            videoSubDir.mkdirs();

//        SFile fileSubDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_FILE);
//        if (!fileSubDir.exists())
//            fileSubDir.mkdirs();

//        SFile contactSubDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_CONTACT);
//        if (!contactSubDir.exists())
//            contactSubDir.mkdirs();

//        mMediaThumbDir = SFile.create(mExternalAppRootDir, DIR_MEDIA_THUMBNAIL);
//        if (!mMediaThumbDir.exists())
//            mMediaThumbDir.mkdirs();
//        FileUtils.createNoMediaFile(mMediaThumbDir);

        if (deleteTemp)
            gc();
    }

    private void gc() {
        FileUtils.removeFolderDescents(getExternalTempDir());
    }

    private void removeInvalidFolders() {
        SFile dir = SFile.create(mExternalAppRootDir, ".tmp");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }

        dir = SFile.create(mExternalAppRootDir, ".cache");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }

        dir = SFile.create(mExternalAppRootDir, ".cloudthumbs");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }

        dir = SFile.create(mExternalAppRootDir, ".data");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }

        dir = SFile.create(mExternalAppRootDir, ".log");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }

        dir = SFile.create(mExternalAppRootDir, ".packaged");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }

        dir = SFile.create(mExternalAppRootDir, ".packageData");
        if (dir.exists()) {
            FileUtils.removeFolderDescents(dir);
            dir.delete();
        }
    }

    private SFile[] listCaches() {
        List<SFile> caches = new ArrayList<>();
        // check cache dir
        if (mExternalCacheDir.exists()) {
            SFile files[] = mExternalCacheDir.listFiles();
            if (files != null)
                caches.addAll(Arrays.asList(files));
        }

        // check content item dir for document file
        Filter filter = new Filter() {
            @Override
            public boolean accept(SFile fs) {
                return fs.getName().startsWith(".cache");
            }
        };
        SFile subDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_APP);
        if (subDir.exists()) {
            SFile[] files = subDir.listFiles(filter);
            if (files != null)
                caches.addAll(Arrays.asList(files));
        }

        subDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_PICTURE);
        if (subDir.exists()) {
            SFile[] files = subDir.listFiles(filter);
            if (files != null)
                caches.addAll(Arrays.asList(files));
        }

//        subDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_MUSIC);
//        if (subDir.exists()) {
//            SFile[] files = subDir.listFiles(filter);
//            if (files != null)
//                caches.addAll(Arrays.asList(files));
//        }

//        subDir = SFile.create(mExternalAppRootDir, DIR_EXTERNAL_VIDEO);
//        if (subDir.exists()) {
//            SFile[] files = subDir.listFiles(filter);
//            if (files != null)
//                caches.addAll(Arrays.asList(files));
//        }

        return caches.toArray(new SFile[caches.size()]);
    }
}
