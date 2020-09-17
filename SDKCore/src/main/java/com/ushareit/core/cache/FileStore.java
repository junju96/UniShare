package com.ushareit.core.cache;

import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.lang.ObjectStore;

import java.io.File;

public class FileStore {
    private static final String TAG = "FileStore";

    private static IFileStoreFactory sFactory;

    public static void init(IFileStoreFactory factory) {
        sFactory = factory;
    }

    private static IFileStore sInstance = null;

    protected static IFileStore getInstance() {
        // double checked singleton design pattern
        if (sInstance == null) {
            synchronized(FileStore.class) {
                if (sInstance == null) {
                    sInstance = sFactory.create(ObjectStore.getContext());
                    Logger.v(TAG, "FileStore inited");
                }
            }
        }
        return sInstance;
    }

    /**
     * refresh will recreate the underlying instance, because some settings changed
     */
    protected static void refresh() {
        sInstance = null;
    }

    protected FileStore() {}

    public static synchronized SFile getExternalRootDir() {
        return getInstance().getExternalRootDir();
    }

    public static synchronized SFile getExternalTempDir() {
        return getInstance().getExternalTempDir();
    }

    public static synchronized SFile getExternalCacheDir() {
        return getInstance().getExternalCacheDir();
    }

    public static synchronized SFile getExternalLogDir() {
        return getInstance().getExternalLogDir();
    }

    public static SFile getThumbnailDir() {
        return getInstance().getThumbnailDir();
    }

    public static SFile getCloudThumbDir() {
        return getInstance().getCloudThumbDir();
    }

    public static SFile getDownloadRootDir() {
        return getInstance().getDownloadRootDir();
    }

    public static SFile createTempFileName(String suggestedFileName) {
        return getInstance().createTempFileName(suggestedFileName);
    }

    public static long calculateCachesSize() {
        return getInstance().calculateCachesSize();
    }

    public static void removeCaches() {
        getInstance().removeCaches();
    }

    public static boolean isEnoughSpace(long needSize) {
        String externalPath = FileUtils.getExternalStorage(ObjectStore.getContext());
        if (externalPath == null)
            return false;

        long availableSpace = FileUtils.getStorageAvailableSize(externalPath);
        return availableSpace > needSize;
    }

    public static File getPrimaryTempDir() {
        return getInstance().getPrimaryTempDir();
    }

    /**
     * special method for testing the rename method is useful on device
     * @param remoteFileStore remote file store
     */
    public static void checkDocumentFileAPI(SFile root) {
        SFile src = getInstance().getExternalCacheDir();
        SFile dst = getInstance().getExternalTempDir();
        checkDocumentFileAPI(root, src, dst);
    }

    public static SFile getMediaThumbnailDir() {
        return getInstance().getMediaThumbnailDir();
    }

    /**
     * special method for testing the rename method is useful on device
     * @param remoteFileStore remote file store
     */
    private static void checkDocumentFileAPI(SFile root, SFile dirSrc, SFile dirDst) {
        SFile src = SFile.create(dirSrc, "tt");
        boolean support = false;
        if (src.exists() || src.createFile()) {
            SFile dst = SFile.create(dirDst, "tt");
            dst.delete();
            if (src.checkRenameTo(dst) && dst.exists())
                support = true;
        }
        SFile.setSupportRenameTo(root, support);
        src.delete();
    }
}
