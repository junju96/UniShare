package com.ushareit.core.net.ssl;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class KeyManagerCreator {
    private static final String TAG = "secure.ssl.ks";

    public static final String KEY_STORE_TYPE_P12 = "PKCS12";

    private String mKeyStoreType;
    private String mKeyStoreAssets;
    private String mPwd;

    /**
     * a creator of KeyManger[], using to init sslContext.
     * @param keyStoreType see {@link #KEY_STORE_TYPE_P12}
     * @param keyStoreAssets the encrypted keyStore file's path in assets folder.
     * @param pwd the encrypted password of the keystore.
     */
    public KeyManagerCreator(String keyStoreType, String keyStoreAssets, String pwd) {
        mKeyStoreType = keyStoreType == null ? "" : keyStoreType;
        mKeyStoreAssets = keyStoreAssets == null ? "" : keyStoreAssets;
        mPwd = SslCredentialCypher.parsePwd(pwd);
    }

    public KeyManager[] create() {
        Logger.d(TAG, "create: " + mKeyStoreAssets + ", type: " + mKeyStoreType);

        KeyManagerFactory keyManagerFactory;
        InputStream ksInputSteam = null;
        try {
            if (SslCredentialStore.getInstance().hasCredential(mKeyStoreAssets))
                ksInputSteam = new ByteArrayInputStream(SslCredentialStore.getInstance().getCredential(mKeyStoreAssets));
            else {
                byte[] credential = SslCredentialCypher.getCredential(mKeyStoreAssets);
                ksInputSteam = new ByteArrayInputStream(credential);
                SslCredentialStore.getInstance().storeCredential(mKeyStoreAssets, credential);
            }

            KeyStore clientKeyStore = KeyStore.getInstance(mKeyStoreType);
            clientKeyStore.load(ksInputSteam, mPwd.toCharArray());
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, mPwd.toCharArray());
        } catch (Exception e) {
            keyManagerFactory = null;
            Logger.d(TAG, "create", e);
        } finally {
            Utils.close(ksInputSteam);
        }

        return keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers();
    }
}
