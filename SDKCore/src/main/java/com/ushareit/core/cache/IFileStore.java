package com.ushareit.core.cache;

import com.ushareit.core.io.sfile.SFile;

import java.io.File;


/**
 * interface of application file store.
 * manage where to store application items (thumbnails / raw files / temp files).
 */
public interface IFileStore {
    /**
     * get external root directory.
     * @return the external root directory.
     */
    SFile getExternalRootDir();

    /**
     * get external temp dir for store temp files.
     * @return the temp directory.
     */
    SFile getExternalTempDir();

    /**
     * get external cache dir for store cache files
     * @return the cache directory.
     */
    SFile getExternalCacheDir();

    /**
     * get directory for store log.
     * @return the log directory
     */
    SFile getExternalLogDir();

    /**
     * get directory for store remote item's thumbnails.
     * all remote item's thumbnails are stored in one directory.
     * @return the thumbnail directory
     */
    SFile getThumbnailDir();

    /**
     * get directory for store cloud item's thumbnails.
     * all remote item's thumbnails are stored in one directory.
     * @return the thumbnail directory
     */
    SFile getCloudThumbDir();

    /**
     * get directory for store downloaded remote item.
     * @return the directory in which downloaded remote item will be created.
     */
    SFile getDownloadRootDir();

    /**
     * get primary external temp dir for store temp files, only useful after android 5.0
     * @return the temp directory.
     */
    File getPrimaryTempDir();

    /**
     * get directory for payment feature.
     * @return the directory store files about payment feature.
     */
    SFile getPaymentDir();

    /**
     * create an temp file name in external temp directory.
     * use specified fileName if not empty.
     * @param suggestedFileName optional suggested temp file name, null/empty means generate one automatically
     * @return the temp File, note the file is not created in local file system yet
     */
    SFile createTempFileName(String suggestedFileName);

    /**
     * calculate the caches size
     * @return
     */
    long calculateCachesSize();

    /**
     * remove the cache files
     */
    void removeCaches();

    /**
     * get local media thumbnail folder
     */
    SFile getMediaThumbnailDir();
}
