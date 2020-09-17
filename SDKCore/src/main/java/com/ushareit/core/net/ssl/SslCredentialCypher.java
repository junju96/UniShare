package com.ushareit.core.net.ssl;

import android.text.TextUtils;

import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.algo.SimpleEncrypt;

import java.io.InputStream;

public class SslCredentialCypher {
    public static final String TAG = "secure.ssl.cypher";

    public static byte[] getCredential(String assetsPath) {
        InputStream credentialInputStream = null;
        try {
            credentialInputStream = ObjectStore.getContext().getAssets().open(assetsPath);
            byte[] credential = new byte[credentialInputStream.available()];
            if (credentialInputStream.read(credential) > 0) {
                for (int i = 0; i < credential.length; i++) {
                    credential[i] = (byte) (credential[i] ^ i);
                }
                return credential;
            }
        } catch (Exception e) {
            Logger.d(TAG, "getCredential: " + assetsPath, e);
        } finally {
            Utils.close(credentialInputStream);
        }

        return null;
    }

    public static String parsePwd(String encryptPwd) {
        if (TextUtils.isEmpty(encryptPwd))
            return "";

        return SimpleEncrypt.decode(encryptPwd);
    }
}
