package com.ushareit.core.net.ssl;

import android.text.TextUtils;

import com.ushareit.core.Logger;

import java.util.Hashtable;

public class SslCredentialStore {

    public static final String TAG = "secure.ssl.store";

    private static volatile SslCredentialStore sInstance;

    private Hashtable<String, byte[]> credentialsTable;

    public static SslCredentialStore getInstance() {
        if (sInstance == null) {
            synchronized (SslCredentialStore.class) {
                if (sInstance == null)
                    sInstance = new SslCredentialStore();
            }
        }
        return sInstance;
    }

    private SslCredentialStore() {
        credentialsTable = new Hashtable<String, byte[]>();
    }

    public synchronized boolean hasCredential(String path) {
        if (TextUtils.isEmpty(path))
            return false;
        try {
            Logger.d(TAG, "hasCredential: " + path);
            return credentialsTable.containsKey(path);
        } catch (Exception e) {
            Logger.d(TAG, "hasCredential", e);
        }
        return false;
    }

    public synchronized byte[] getCredential(String path) {
        if (TextUtils.isEmpty(path))
            return null;
        try {
            Logger.d(TAG, "getCredential: " + path);
            return credentialsTable.get(path);
        } catch (Exception e) {
            Logger.d(TAG, "getCredential", e);
        }
        return null;
    }

    public synchronized void storeCredential(String path, byte[] credential) {
        if (TextUtils.isEmpty(path) || credential == null)
            return;
        if (hasCredential(path))
            return;
        try {
            Logger.d(TAG, "storeCredential: " + path + ", " + credential.length);
            credentialsTable.put(path, credential);
        } catch (Exception e) {
            Logger.d(TAG, "storeCredential", e);
        }
    }
}
