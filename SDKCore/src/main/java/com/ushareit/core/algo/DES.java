package com.ushareit.core.algo;

import java.security.Key;

import javax.crypto.Cipher;

public class DES {
    private Cipher encryptCipher = null;
    private Cipher decryptCipher = null;

    private static DES mInstance = null;

    public static DES getInstance() {
        if (mInstance == null) {
            try {
                mInstance = new DES("anyshare2013");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return mInstance;
    }

    public DES(String dictionaryKey) {
        // Security.addProvider(new com.sun.crypto.provider.SunJCE());
        try {
            Key key = getKey(dictionaryKey.getBytes());

            encryptCipher = Cipher.getInstance("DES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);

            decryptCipher = Cipher.getInstance("DES");
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] encrypt(byte[] arrB) throws Exception {
        return encryptCipher.doFinal(arrB);
    }

    public String encrypt(String strIn) throws Exception {
        return byteArr2HexStr(encrypt(strIn.getBytes()));
    }

    public byte[] decrypt(byte[] arrB) throws Exception {
        return decryptCipher.doFinal(arrB);
    }

    public String decrypt(String strIn) throws Exception {
        return new String(decrypt(hexStr2ByteArr(strIn)));
    }

    private Key getKey(byte[] arrBTmp) throws Exception {
        byte[] arrB = new byte[8];
        System.arraycopy(arrBTmp, 0, arrB, 0, Math.min(arrBTmp.length, arrB.length));
        return new javax.crypto.spec.SecretKeySpec(arrB, "DES");
    }

    private static String byteArr2HexStr(byte[] arrB) throws Exception {
        int iLen = arrB.length;
        StringBuilder builder = new StringBuilder(iLen * 2);
        for (int i = 0; i < iLen; i++) {
            int intTmp = arrB[i];
            while (intTmp < 0) {
                intTmp = intTmp + 256;
            }

            if (intTmp < 16) {
                builder.append("0");
            }
            builder.append(Integer.toString(intTmp, 16));
        }
        return builder.toString();
    }

    private static byte[] hexStr2ByteArr(String strIn) throws Exception {
        byte[] arrB = strIn.getBytes();
        int iLen = arrB.length;

        byte[] arrOut = new byte[iLen / 2];
        for (int i = 0; i < iLen; i = i + 2) {
            String strTmp = new String(arrB, i, 2);
            arrOut[i / 2] = (byte)Integer.parseInt(strTmp, 16);
        }
        return arrOut;
    }

}
