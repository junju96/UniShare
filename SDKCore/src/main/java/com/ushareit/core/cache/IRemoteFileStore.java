package com.ushareit.core.cache;

import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.lang.ContentType;

/**
 * interface of remote file store.
 * manage where to store received items (thumbnails / raw files / temp files).
 */
public interface IRemoteFileStore extends IFileStore {
    String TRANSFER_CACHE_EXTENSION = ".rfbp";

    /**
     * get directory for store downloaded remote item.
     * this is usually called once just before download remote item's raw file.
     * @param type ContentType of this remote item
     * @param fileName the target file name (actually only extensions meaningful)
     * @return the directory in which downloaded remote item will be created.
     */
    SFile getRemoteItemDir(ContentType type, String fileName);

    /**
     * get directory for store downloaded remote item.
     * this is usually called once just before download remote item's raw file.
     * @param type ContentType of this remote item
     * @param parentPath create relative parent path in root
     * @param fileName the target file name (actually only extensions meaningful)
     * @return the directory in which downloaded remote item will be created.
     */
    SFile getRemoteItemDir(ContentType type, String parentPath, String fileName);

    /**
     * get thumbnail file of specified remote item.
     * @return the file object of thumbnail file.
     */
    SFile getRemoteItemThumbnail();

    /**
     * get cache file of specified item
     * @param deviceId the original device id this remote item comes from
     * @param collectionId the collection identifier, null means that the cache file is useless for collection
     * @param itemId item id.
     * @param type content type, if cache file is useful for collection, this is collection type.
     * @param isThumbnail thumbnail file flag
     * @param ext cache ext for delete when distinct
     * @return cache file
     */
    SFile getCacheFile(String deviceId, String collectionId, String itemId, ContentType type, boolean isThumbnail, String ext);

    /**
     * get cache file of specified item in specified folder
     * @param deviceId the original device id this remote item comes from
     * @param collectionId the collection identifier, null means that the cache file is useless for collection
     * @param itemId item id.
     * @param type content type, if cache file is useful for collection, this is collection type.
     * @param isThumbnail thumbnail file flag
     * @param ext cache ext for delete when distinct
     * @return cache file
     */
    SFile getCacheFile(SFile parent, String deviceId, String collectionId, String itemId, ContentType type, boolean isThumbnail, String ext);

    /**
     * get origin file of download item
     * @param type content type
     * @return download origin file
     */
    SFile getDownloadFile(ContentType type, String name, String url, String tag, boolean isDSV , boolean isFolder);

    /**
     * get cache file of download item
     * @param type content type
     * @return download temp file
     */
    SFile getDownloadTempFile(ContentType type, String name, String url);

    /**
     * get download file dir
     * @param type content type
     * @return download file dir
     */
    SFile getDownloadFileDir(ContentType type);

    /**
     * get download offline video file dir
     * @param type content type
     * @return download file dir
     */
    SFile getDownloadCacheFileDir(ContentType type);

    /**
     * get cache file of download from cloud
     */
    SFile getCacheFile(String id, String url, ContentType type, boolean isTemp, boolean isThumbnail);
}
