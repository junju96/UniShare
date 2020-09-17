package com.ushareit.core.cache;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.io.MediaUtils;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.lang.ContentType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteFileStore extends FileStore {
    private static final String TAG = "RemoteFileStore";


    private static final String CFG_LOCAL_ENABLE_THIRD_MEDIA_LIB = "enable_third_media_lib";

    private static AtomicBoolean mImportRunning = new AtomicBoolean(false);

    public static void init(IFileStoreFactory factory) {
        FileStore.init(factory);
    }

    private static IRemoteFileStore sInstance = null;

    private static IRemoteFileStore getThisInstance() {
        // double checked singleton design pattern
        if (sInstance == null) {
            synchronized(RemoteFileStore.class) {
                if (sInstance == null)
                    sInstance = (IRemoteFileStore) FileStore.getInstance();
                TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("import.media") {
                    @Override
                    public void execute() {
                        enableThirdMediaLib(isEnableThirdMediaLib());
                    }
                });
            }
        }
        return sInstance;
    }

    /**
     * refresh will recreate the underlying instance, because some settings changed
     * @return
     */
    public static void refresh() {
        FileStore.refresh();
        sInstance = null;
    }

    private RemoteFileStore(IRemoteFileStore impl) {}
    
    public static SFile getRemoteItemDir(ContentType type, String fileName) {
        return getThisInstance().getRemoteItemDir(type, fileName);
    }

    public static SFile getRemoteItemThumbnail() {
        return getThisInstance().getRemoteItemThumbnail();
    }

    public static SFile getCacheFile(String deviceId, String collectionId, String itemId, ContentType type, boolean isThumbnail, String ext) {
        return getThisInstance().getCacheFile(deviceId, collectionId, itemId, type, isThumbnail, ext);
    }

    public static SFile getCacheFile(SFile parent, String deviceId, String collectionId, String itemId, ContentType type, boolean isThumbnail, String ext) {
        return getThisInstance().getCacheFile(parent, deviceId, collectionId, itemId, type, isThumbnail, ext);
    }

    public static SFile getCacheFile(String id, String url, ContentType type, boolean isTemp, boolean isThumbnail) {
        return getThisInstance().getCacheFile(id, url, type, isTemp, isThumbnail);
    }


    public static SFile getDownloadFile(ContentType type, String name, String url, String tag, boolean isDSV ,boolean isFolder) {
        return getThisInstance().getDownloadFile(type, name, url, tag, isDSV , isFolder);
    }

    public static SFile getDownloadTempFile(ContentType type, String name, String url) {
        return getThisInstance().getDownloadTempFile(type, name, url);
    }

    public static SFile getDownloadFileDir(ContentType type) {
        return getThisInstance().getDownloadFileDir(type);
    }

    // now only used for cache offline video
    public static SFile getOfflineCacheFileDir(ContentType type) {
        return getThisInstance().getDownloadCacheFileDir(type);
    }

    public static SFile getPaymentDir() {
        return getThisInstance().getPaymentDir();
    }

    public static void enableThirdMediaLib(boolean enable) {
        if (!mImportRunning.compareAndSet(false, true)) {
            Logger.d(TAG, "Importing the media file to system lib!");
            return;
        }

        try {
            SFile musicDir = getRemoteItemDir(ContentType.MUSIC, null);
            SFile videoDir = getRemoteItemDir(ContentType.VIDEO, null);
            if (!enable) {
                Logger.v(TAG, "disable third media lib, create no media file!");
                FileUtils.createNoMediaFile(musicDir);
                FileUtils.createNoMediaFile(videoDir);
            } else {
                Logger.v(TAG, "enable third media lib, should remove no media file!");
                final List<File> importDirs = new ArrayList<>();
                if (FileUtils.removeNoMediaFile(musicDir))
                    importDirs.add(musicDir.toFile());
                if (FileUtils.removeNoMediaFile(videoDir))
                    importDirs.add(videoDir.toFile());
                if (importDirs.isEmpty()) {
                    Logger.v(TAG, "there are not any nomedia files!");
                    return;
                }

                MediaUtils.clearNoMediasCache();
                for (File f : importDirs)
                    MediaUtils.restoreSysMediaLib(ObjectStore.getContext(), f);
                Logger.v(TAG, "import media file to system media lib completed!");
            }
        } finally {
            mImportRunning.set(false);
        }
    }

    public static boolean isImportingMedia() {
        return mImportRunning.get();
    }

    public static boolean isShowEnableThirdMediaMenu() {
        return new Settings(ObjectStore.getContext()).contains(CFG_LOCAL_ENABLE_THIRD_MEDIA_LIB) || CloudConfig.getBooleanConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_USE_NOMEDIA, false);
    }

    public static boolean isEnableThirdMediaLib() {
        boolean useNomedia = CloudConfig.getBooleanConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_USE_NOMEDIA, false);
        return new Settings(ObjectStore.getContext()).getBoolean(CFG_LOCAL_ENABLE_THIRD_MEDIA_LIB, !useNomedia);
    }

    public static void setEnableThirdMediaLib(boolean enable) {
        new Settings(ObjectStore.getContext()).setBoolean(CFG_LOCAL_ENABLE_THIRD_MEDIA_LIB, enable);
    }
}
