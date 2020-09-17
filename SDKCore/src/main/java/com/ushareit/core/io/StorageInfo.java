package com.ushareit.core.io;

public class StorageInfo {
    public int mStorageCount;
    public long mCurrentFreeSpace;
    public long mCurrentUsedSpace;
    public long mCurrentTotalSpace;
    public long mAllFreeSpace;
    public long mAllUsedSpace;
    public long mAllTotalSpace;

    public StorageInfo(int count, long currentFree, long currentTotal, long allFree, long allTotal) {
        mStorageCount = count;
        mCurrentFreeSpace = currentFree;
        mCurrentTotalSpace = currentTotal;
        mCurrentUsedSpace = currentTotal - currentFree;
        mAllFreeSpace = allFree;
        mAllTotalSpace = allTotal;
        mAllUsedSpace = allTotal - allFree;
    }
}
