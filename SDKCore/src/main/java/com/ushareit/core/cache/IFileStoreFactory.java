package com.ushareit.core.cache;

import android.content.Context;

public interface IFileStoreFactory {
    IFileStore create(Context context);
}