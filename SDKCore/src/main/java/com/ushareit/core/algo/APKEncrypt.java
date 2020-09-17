package com.ushareit.core.algo;

import com.ushareit.core.utils.Utils;
import com.ushareit.core.Logger;
import com.ushareit.core.io.sfile.SFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class APKEncrypt {
    private static final String TAG = "UpgradeHelper";

    private static final String CIPHER_TRIPLE_AES = "AES/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 128;
    private static final int CACHE_SIZE = 64 * 1024;
    private static final byte[] salt = {(byte) 0xA4, (byte) 0x0B, (byte) 0xC8, (byte) 0x34, (byte) 0xD6, (byte) 0x95, (byte) 0xF3, (byte) 0x13};


    private static OutputStream createDecryptOutputStream(long appVersion, OutputStream outputStream) {
        try {
            SecretKey secretKeySpec = generalKey(String.valueOf(appVersion), KEY_SIZE);
            Cipher cipher = Cipher.getInstance(CIPHER_TRIPLE_AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return new CipherOutputStream(outputStream, cipher);
        } catch (Exception e) {
        }
        return null;
    }

    public static InputStream createDecryptInputStream(long appVersion, InputStream inputStream) {
        try {
            SecretKey secretKeySpec = generalKey(String.valueOf(appVersion), KEY_SIZE);
            Cipher cipher = Cipher.getInstance(CIPHER_TRIPLE_AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return new CipherInputStream(inputStream, cipher);
        } catch (Exception e) {
        }
        return null;
    }

    private static SecretKey generalKey(String password, int length) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 5, length);
        SecretKey tmp = factory.generateSecret(spec);

        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static File decryptApk(long appVersion, String sourceName, SFile tmpDir) {
        SFile target = SFile.create(tmpDir, System.currentTimeMillis() + ".apk");
        OutputStream os = null;
        InputStream is = null;

        try {
            os = createDecryptOutputStream(appVersion, target.getOutputStream());
            is = SFile.create(sourceName).getInputStream();
            byte[] cache = new byte[CACHE_SIZE];
            int nRead;
            while ((nRead = is.read(cache)) != -1)
                os.write(cache, 0, nRead);

            return target.toFile();
        } catch (Exception e) {
            Logger.d(TAG, "decrypt apk failed!", e);
        } finally {
            Utils.close(os);
            Utils.close(is);
        }
        return null;
    }

    public static File decryptFile(long key, String sourceName, String targetName, SFile tmpDir) {
        SFile target = SFile.create(tmpDir, targetName);
        OutputStream os = null;
        InputStream is = null;

        try {
            os = createDecryptOutputStream(key, target.getOutputStream());
            is = SFile.create(sourceName).getInputStream();
            byte[] cache = new byte[CACHE_SIZE];
            int nRead;
            while ((nRead = is.read(cache)) != -1)
                os.write(cache, 0, nRead);

            return target.toFile();
        } catch (Exception e) {
            Logger.d(TAG, "decrypt file failed!", e);
        } finally {
            Utils.close(os);
            Utils.close(is);
        }
        return null;
    }
}
