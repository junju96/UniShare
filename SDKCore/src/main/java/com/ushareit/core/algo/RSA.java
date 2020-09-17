package com.ushareit.core.algo;

import android.annotation.SuppressLint;

import com.ushareit.core.Logger;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class RSA {

    private static final String TAG = "RSA";

    private RSA() {};
    //非对称加密算法
    public static final String KEY_ALGORITHM = "RSA";
    //数字签名 签名/验证算法
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";
    /**
     * NOTE: src.length must less than public key length - 11;
     * @param src
     * @param key
     * @return
     * @throws Exception
     */
    @SuppressLint("TrulyRandom")
    public static byte[] encrypt(byte[] src, String key) {
        if (src == null || key == null)
            return null;

        try {
            RSAPublicKey pubKey = getPublicKey(key);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            return cipher.doFinal(src);
        } catch (Exception e) {
            Logger.w(TAG, "can not support RSAEncrypt!", e);
            return null;
        }
    }

    public static byte[] decrypt(byte[] src, String key) {
        try {
            RSAPrivateKey privateKey = getPrivateKey(key);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(src);
        } catch (Exception e) {
            Logger.w(TAG, "can not support RSADecrypt!", e);
            return null;
        }
    }

    /**
     * 验签
     *
     * @param data 原始字符串
     * @param publicKey 公钥
     * @param sign 签名
     * @return 是否验签通过
     */
    public static boolean verify(byte[] data, byte[] publicKey, byte[] sign) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(data);
        return signature.verify(sign);
    }

    private static RSAPublicKey getPublicKey(String publicKey) throws Exception {
        byte[] keyBytes = Base64.decode(publicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey)keyFactory.generatePublic(spec);
    }

    private static RSAPrivateKey getPrivateKey(String privateKey) throws Exception {
        byte[] keyBytes = Base64.decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey)keyFactory.generatePrivate(keySpec);
    }
}
