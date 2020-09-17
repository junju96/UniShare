package com.ushareit.core.algo;

import com.ushareit.core.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {
    public static final String TAG = "AES";

    private AES() {}

    /*
     * 将字节数组AES加密
     * key.length == 16 || 32
     */
    public static byte[] encrypt(byte[] src, byte[] key) {
        if (src == null || key == null)
            return null;

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(key));
            return cipher.doFinal(src);
        } catch (Exception e) {
            Logger.e(TAG, "encrypt error: " + e.toString());
        }
        return null;
    }

    /*
     * 将字节数组AES加密
     * key.length == 16 || 32
     * 加密顺序如下�? * 先AES加密
     * 然后在base64编码
     * 然后去掉base64编码中的空格�?
     */
    public static String encrypt(String src, byte[] key) {
        if (src == null || key == null)
            return null;

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(key));
            byte[] encrypted = cipher.doFinal(src.getBytes("UTF-8"));
            String srcEncode = Base64.encode(encrypted);
            srcEncode = srcEncode.replaceAll("\\s", ""); // \s A whitespace character: [ \t\n\x0B\f\r]
            // srcEncode = URLEncoder.encode(srcEncode,"UTF-8");
            return srcEncode;
        } catch (Exception e) {
            Logger.e(TAG, "encrypt error: " + e.toString());
        }
        return null;
    }

    /*
     * 将字符串AES加密
     * 加密顺序如下�? * 先AES加密
     * 然后在base64编码
     * 然后去掉base64编码中的空格�?
     */
    public static String encrypt(String src, final String seed) {
        if (src == null || seed == null)
            return null;

        try {
            byte[] key = seed.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(key));
            byte[] encrypted = cipher.doFinal(src.getBytes("UTF-8"));
            String srcEncode = Base64.encode(encrypted);
            srcEncode = srcEncode.replaceAll("\\s", ""); // \s A whitespace character: [ \t\n\x0B\f\r]
            // srcEncode = URLEncoder.encode(srcEncode,"UTF-8");
            return srcEncode;
        } catch (Exception e) {
            Logger.e(TAG, "encrypt error: " + e.toString());
        }
        return null;
    }

    /*
     * 将字节数组AES解密
     * key.length == 16 || 32
     */
    public static byte[] decrypt(byte[] src, byte[] key) {
        if (src == null || key == null)
            return null;

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(key));
            return cipher.doFinal(src);
        } catch (Exception e) {
            Logger.e(TAG, "decrypt error: " + e.toString());
        }
        return null;
    }

    /*
     * 将字节数组AES解密
     * key.length == 16 || 32
     * 解密顺序如下�? * 先Base64解码
     * 然后AES解密
     * 然后转换成String 返回
     */
    public static String decrypt(String src, byte[] key) {
        if (src == null || key == null)
            return null;

        try {
            byte[] srcDecode = Base64.decode(src);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(key));
            byte[] dencrypted = cipher.doFinal(srcDecode);
            return new String(dencrypted, "UTF-8");
        } catch (Exception e) {
            Logger.e(TAG, "decrypt error: " + e.toString());
        }
        return null;
    }

    /*
     * 将字符串AES解密
     * 解密顺序如下�? * 先Base64解码
     * 然后AES解密
     * 然后转换成String 返回
     */
    public static String decrypt(String src, final String seed) {
        if (src == null || seed == null)
            return null;

        try {
            byte[] srcDecode = Base64.decode(src);
            byte[] key = seed.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(key));
            byte[] dencrypted = cipher.doFinal(srcDecode);
            return new String(dencrypted, "UTF-8");
        } catch (Exception e) {
            Logger.e(TAG, "decrypt error: " + e.toString());
        }
        return null;
    }
}
