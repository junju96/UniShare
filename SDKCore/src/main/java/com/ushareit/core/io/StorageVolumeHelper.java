package com.ushareit.core.io;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.thread.ThreadPollFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * contains storage volume related utility functions.
 */
public final class StorageVolumeHelper {
    private static final String TAG = "StorageVolumeHelper";

    public static final String KEY_SETTING_STORAGE = "SETTING_STORAGE";

    public static boolean isStorageMounted(Context context) {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static class Volume {
        public boolean mIsPrimary;
        public String mUuid;
        public String mDescription;
        public String mPath;
        public String mState;
        public boolean mWritable = true;
        public boolean mPrivateDirWritable = true;
        public boolean mIsMainVolume = true;
        public boolean mSupportAuth = false;

        public Volume(boolean isPrimary, String uuid, String description, String path, String state) {
            mIsPrimary = isPrimary;
            mUuid = uuid;
            mDescription = description;
            mPath = path;
            mState = state;
        }

        public Volume(String description, String path, String state) {
            this(false, null, description, path, state);
        }

        public boolean isAuth() {
            return !mIsMainVolume && !mWritable && mSupportAuth;
        }
    }

    private static String mDescription0 = "";
    private static String mDescription1 = "";

    private static Method mGetVolumeList = null;
    private static Method mGetVolumeState = null;
    private static Method mGetDescription = null;
    private static Method mGetDescriptionC = null;
    private static Method mGetPath = null;
    private static Method mGetUuid = null;
    private static Method mIsPrimary = null;

    private static Method mGetRealExternalStorageDirectory = null;
    private static Method mGetRealExternalStorageState = null;

    static {
        try {
            Class<?> StorageManagerClass = Class.forName("android.os.storage.StorageManager");
            Class<?> StorageVolumeClass = Class.forName("android.os.storage.StorageVolume");
            
            mGetVolumeList = StorageManagerClass.getDeclaredMethod("getVolumeList");
            mGetVolumeState = StorageManagerClass.getDeclaredMethod("getVolumeState", String.class);
            
            try {
                mGetDescription = StorageVolumeClass.getDeclaredMethod("getDescription");
            } catch (Exception e) {}
            
            try {
                mGetDescriptionC = StorageVolumeClass.getDeclaredMethod("getDescription", Context.class);
            } catch (Exception e) {}
            
            mGetPath = StorageVolumeClass.getDeclaredMethod("getPath");
            
            try {
                mGetUuid = StorageVolumeClass.getDeclaredMethod("getUuid");
                mIsPrimary = StorageVolumeClass.getDeclaredMethod("isPrimary");
            } catch (Exception e) {}

            Class<?> environmentClass = Class.forName("android.os.Environment");
            
            mGetRealExternalStorageDirectory = environmentClass.getDeclaredMethod("getRealExternalStorageDirectory");
            mGetRealExternalStorageState = environmentClass.getDeclaredMethod("getRealExternalStorageState");
        } catch (Exception e1) {}
    }

    // To be compatible with Android 4.2 or earlier
    private static Object getStorageManagerInstance(Context context, Class<?> StorageManager)
            throws ClassNotFoundException,
            SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Object instance;
        if (android.os.Build.VERSION.SDK_INT < 18) { // before android 4.3
            Constructor<?> StorageManagerConstructor = StorageManager.getConstructor(Looper.class);
            instance = StorageManagerConstructor.newInstance(ThreadPollFactory.ThreadLooperProvider.ThreadLooper);
        } else if (android.os.Build.VERSION.SDK_INT < 23) { // 4.3 to before 6.0
            Constructor<?> StorageManagerConstructor = StorageManager.getConstructor(ContentResolver.class, Looper.class);
            instance = StorageManagerConstructor.newInstance(context.getContentResolver(), ThreadPollFactory.ThreadLooperProvider.ThreadLooper);
        } else { // 6.0 or later
            Constructor<?> StorageManagerConstructor = StorageManager.getConstructor(Context.class, Looper.class);
            instance = StorageManagerConstructor.newInstance(context, ThreadPollFactory.ThreadLooperProvider.ThreadLooper);
        }

        return instance;
    }

    // Get all volumes by StorageManager class and Environment class
    public static List<Volume> getAllVolumeList(Context context) {
        List<Volume> list = new ArrayList<Volume>();
        try {
            Class<?> StorageManagerClass = Class.forName("android.os.storage.StorageManager");
            Object objStorageManager = getStorageManagerInstance(context, StorageManagerClass);

            Object storageVolumes = mGetVolumeList.invoke(objStorageManager);
            int size = Array.getLength(storageVolumes);
            for (int i = 0; i < size; i++) {
                Object volume = Array.get(storageVolumes, i);

                String description = "";
                if (mGetDescription != null)
                    description = (String)mGetDescription.invoke(volume);
                else if (mGetDescriptionC != null)
                    description = (String)mGetDescriptionC.invoke(volume, context);

                boolean isPrimary = (mIsPrimary == null) ? false : (Boolean)mIsPrimary.invoke(volume);
                
                String uuid = (mGetUuid == null) ? null : (String)mGetUuid.invoke(volume);
                String path = (String)mGetPath.invoke(volume);
                String state = (String)mGetVolumeState.invoke(objStorageManager, path);
                
                if (Logger.isDebugVersion && !("removed".equals(state)))
                    Logger.v(TAG, "Description: " + description + ", Path: " + path + ", State: " + state);

                Volume vol = new Volume(isPrimary, uuid, description, path, state);
                vol.mWritable = isWritable(context, path);
                vol.mPrivateDirWritable = isPrivateDirWritable(context, vol.mPath);
                vol.mIsMainVolume = isMainVolume(context, path);
                vol.mSupportAuth = isSupportAuth(context, vol.mPath);
                list.add(vol);
            }

            if (!list.isEmpty())
                return list;
        } catch (Exception e) {
            Logger.w(TAG, e.toString());
        }

        Volume vol = new Volume(mDescription0, Environment.getExternalStorageDirectory().getAbsolutePath(), Environment.getExternalStorageState());
        vol.mWritable = isWritable(context, Environment.getExternalStorageDirectory().getAbsolutePath());
        vol.mPrivateDirWritable = isPrivateDirWritable(context, vol.mPath);
        vol.mIsMainVolume = isMainVolume(context, Environment.getExternalStorageDirectory().getAbsolutePath());
        vol.mIsPrimary = vol.mIsMainVolume;
        vol.mSupportAuth = isSupportAuth(context, vol.mPath);
        list.add(vol);

        try {
            Class<?> EnvironmentClass = Class.forName("android.os.Environment");
            Constructor<?> EnvironmentConstructor = EnvironmentClass.getConstructor();
            Object objEnvironmentManager = EnvironmentConstructor.newInstance();
            File sdcardDirectory = (File)mGetRealExternalStorageDirectory.invoke(objEnvironmentManager);
            String state = (String)mGetRealExternalStorageState.invoke(objEnvironmentManager);
            vol = new Volume(mDescription1, sdcardDirectory.getAbsolutePath(), state);
            vol.mWritable = isWritable(context, sdcardDirectory.getAbsolutePath());
            vol.mPrivateDirWritable = isPrivateDirWritable(context, vol.mPath);
            vol.mIsMainVolume = isMainVolume(context, sdcardDirectory.getAbsolutePath());
            vol.mIsPrimary = vol.mIsMainVolume;
            vol.mSupportAuth = isSupportAuth(context, vol.mPath);
            list.add(vol);
        } catch (Exception e1) {
            Logger.w(TAG, e1.toString());
        }

        return list;
    }

    // Get the mounted volumes list.
    public static List<Volume> getVolumeList(Context context) {
        List<Volume> mountedVolumes = new ArrayList<Volume>();

        List<Volume> allVolumes = getAllVolumeList(context);
        for (Volume volume : allVolumes) {
            if (Environment.MEDIA_MOUNTED.equals(volume.mState))
                mountedVolumes.add(volume);
        }

        return mountedVolumes;
    }

    // cannot set a null volume
    public static void setVolume(Context context, Volume volume) {
        Assert.notNull(volume);
        new Settings(context).set(KEY_SETTING_STORAGE, volume.mPath);
    }

    public static Volume getVolume(Context context) {
        List<Volume> volumes = getAllVolumeList(context);

        String path = new Settings(context).get(KEY_SETTING_STORAGE);
        if (TextUtils.isEmpty(path))
            path = Environment.getExternalStorageDirectory().getAbsolutePath();

        for (Volume volume : volumes) {
            if (path.equals(volume.mPath))
                return volume;
        }

        return volumes.get(0);
    }

    public final static StorageInfo getStorageInfo(Context context) {
        long allFree = 0;
        long allTotal = 0;
        long currentFree = 0;
        long currentTotal = 0;
        int count = 0;

        List<Volume> allVolumes = getAllVolumeList(context);
        String path = new Settings(context).get(KEY_SETTING_STORAGE);
        if (TextUtils.isEmpty(path))
            path = Environment.getExternalStorageDirectory().getAbsolutePath();

        for (Volume volume : allVolumes) {
            if (Environment.MEDIA_MOUNTED.equals(volume.mState)) {
                if (path.equals(volume.mPath)) {
                    currentFree = FileUtils.getStorageAvailableSize(volume.mPath);
                    currentTotal = FileUtils.getStorageTotalSize(volume.mPath);
                }
                allFree += FileUtils.getStorageAvailableSize(volume.mPath);
                allTotal += FileUtils.getStorageTotalSize(volume.mPath);
                count++;
            }
        }
        return new StorageInfo(count, currentFree, currentTotal, allFree, allTotal);
    }

    public static boolean isVolumeMounted(Context context) {
        List<Volume> volumes = getVolumeList(context);
        if (!volumes.isEmpty())
            return true;
        else
            return false;
    }

    public static void setDescriptions(String desc0, String desc1) {
        mDescription0 = desc0;
        mDescription1 = desc1;
    }

    private static boolean isWritable(Context context, String path) {
        File file = new File(path + "/" + TAG + ".tmp");
        //Logger.v(TAG, file.getAbsolutePath());
        
        try {
            if (!file.exists())
                file.createNewFile();
            file.delete();
            return true;
        } catch (IOException e) {
            // Logger.d(TAG, "cannot write file: " + path + ", " + e.toString());
            return false;
        }
    }

    private static boolean isPrivateDirWritable(Context context, String path) {
        File file = FileUtils.getPrivateExtAppDir(context, path);
        return (file != null && file.exists()) ? isWritable(context, file.getAbsolutePath()) : false;
    }

    private static boolean isMainVolume(Context context, String path) {
        if (android.os.Build.VERSION.SDK_INT < 19)
            return true;

        String mainPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (path.equalsIgnoreCase(mainPath))
            return true;
        else
            return false;
    }

    private static boolean isSupportAuth(Context context, String path) {
        return (android.os.Build.VERSION.SDK_INT >= 21);
    }
}
