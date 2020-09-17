package com.ushareit.core.algo;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.utils.Utils;

import android.util.SparseArray;

/*
/* 数据报文的编码格式
/* 为了使采集系统在传输过程中更加安全，现做如下改进：
/* 1.package格式：
/*      Type(1字节) 必选
/*      Pwd length (4字节) 可选
/*      Pwd (pwd length) 可选
/*      Encoded contents
/* 2.详细：
/*      2.1. type == 1
/*           01 + zipped contents
/*
/*      2.2. type == 2
/*           02 + aesed pwd length (xx xx xx xx) + aesed pwd (xx xx xx …) + AESED contents;
/*           1.) 对数据zip压缩;
/*           2.) 生成动态16字节密码，并对压缩过的数据进行AES加密
/*
/*      2.3. type == 3
/*           03 + rsaed pwd length (xx xx xx xx) + rsaed pwd (xx xx xx …) + AESED contents;
/*           1.) 对数据zip压缩;
/*           2.) 生成动态16字节密码，并对压缩过的数据进行AES加密
/*           3.) 使用公钥对动态密码进行AES加密
 */

public class DecorativePacket {
    private static final String TAG = "Beyla.DecorP";

    private static final byte[] AESDATA_C = {54,-22,-74,-7,-54,122,-21,-91,-48,-85,93,-67,51,22,-87,33};
    private static boolean supportAES = false;

    static {
        checkEncryptMethod();
    }

    enum EncodeType{
        ZIP(1), ENCRYPT_CONTENTS(2), ENCRYPT_KEY_CONTENTS(3);

        private int mValue;
        EncodeType(int value) {
            mValue = value;
        }
        private static SparseArray<EncodeType> mValues = new SparseArray<EncodeType>();
        static {
            for (EncodeType item : EncodeType.values())
                mValues.put(item.mValue, item);
        }
        public int toInt() {
            return mValue;
        }
    }
    private final static int AESED_DYNAMIC_PWD_LEN = 16;

    public static byte[] encodePacket(String str) throws Exception {
        // packet format: type + key.length + key + contents
        EncodeType type = EncodeType.ZIP;
        byte[] key = null;
        byte[] contents = null;

        //1. ZIP compress the plain packet.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = new GZIPOutputStream(stream);
        gzipStream.write(str.getBytes("UTF-8"));
        gzipStream.close();
        byte[] zipedContents = stream.toByteArray();
        contents = zipedContents;

        //2. generate 16Byte password and AES encrypt ziped contents with passward
        if (supportAES) {
            String aesedPwd = StringUtils.randomString(AESED_DYNAMIC_PWD_LEN);
            Assert.isTrue(aesedPwd.length() == AESED_DYNAMIC_PWD_LEN);
            byte[] bAesedKey = aesedPwd.getBytes("UTF-8");
            byte[] encryptedContents = AES.encrypt(zipedContents, bAesedKey);
            // encrypted data length must 16 multipul.
            if (encryptedContents != null && (encryptedContents.length % 16 == 0)) {
                type = EncodeType.ENCRYPT_CONTENTS;
                key = bAesedKey;
                contents = encryptedContents;

                //3. As AES succeed, encrypt the AESed password by RSA
                final String PUB_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDBnfRKIUm5FCy+vMxaGPwIpK0y573bFJIzebpcg1mXA5QOEg/xg0wtjZ+Sc+WI2LflEm7H3sf6G9vh30j7Ua94LQr/e8Th44o57dmq38JY8ZYU6Tyxd2zUCS3nqB6XQF9wfqFdST3BK2uMXE7SUcxSJHXbizt1cnt6xXtFGgaJ0wIDAQAB";
                byte[] bRsaedKey = RSA.encrypt(bAesedKey, PUB_KEY);
                if (bRsaedKey != null) {
                    type = EncodeType.ENCRYPT_KEY_CONTENTS;
                    key = bRsaedKey;
                }
            }
        }

        Logger.v(TAG, "encrpyt type:" + type);
        //4. output the encoded packet
        ByteArrayOutputStream encodedPacket = new ByteArrayOutputStream();
        encodedPacket.write(type.toInt());
        if (key != null) {
            encodedPacket.write(Utils.toBytes(key.length));
            encodedPacket.write(key);
        }
        encodedPacket.write(contents);
        return encodedPacket.toByteArray();
    }

    public static String encodePacketBase64(String str) throws Exception {
        // packet format: type + key.length + key + contents
        int type = 1;
        byte[] key = null;
        byte[] contents = null;

        byte[] zipedContents = str.getBytes("UTF-8");
        contents = zipedContents;

        //2. generate 16Byte password and AES encrypt ziped contents with passward
        if (supportAES) {
            String aesedPwd = StringUtils.randomString(AESED_DYNAMIC_PWD_LEN);
            Assert.isTrue(aesedPwd.length() == AESED_DYNAMIC_PWD_LEN);
            byte[] bAesedKey = aesedPwd.getBytes("UTF-8");
            byte[] encryptedContents = AES.encrypt(zipedContents, bAesedKey);
            // encrypted data length must 16 multipul.
            if (encryptedContents != null && (encryptedContents.length % 16 == 0)) {
                type = 2;
                key = bAesedKey;
                contents = encryptedContents;

                //3. As AES succeed, encrypt the AESed password by RSA
                final String PUB_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDBnfRKIUm5FCy+vMxaGPwIpK0y573bFJIzebpcg1mXA5QOEg/xg0wtjZ+Sc+WI2LflEm7H3sf6G9vh30j7Ua94LQr/e8Th44o57dmq38JY8ZYU6Tyxd2zUCS3nqB6XQF9wfqFdST3BK2uMXE7SUcxSJHXbizt1cnt6xXtFGgaJ0wIDAQAB";
                byte[] bRsaedKey = RSA.encrypt(bAesedKey, PUB_KEY);
                if (bRsaedKey != null) {
                    type = 3;
                    key = bRsaedKey;
                }
            }
        }

        Logger.v(TAG, "encrpyt type:" + type);
        //4. output the encoded packet
        ByteArrayOutputStream encodedPacket = new ByteArrayOutputStream();
        encodedPacket.write(type);
        if (key != null) {
            encodedPacket.write(Utils.toBytes(key.length));
            encodedPacket.write(key);
        }
        encodedPacket.write(contents);
        return Base64.encode(encodedPacket.toByteArray());
    }

    public static byte[] encodePacketBase64(byte[] zipedContents) throws Exception {
        // packet format: type + key.length + key + contents
        int type = 1;
        byte[] key = null;
        byte[] contents = zipedContents;

        //2. generate 16Byte password and AES encrypt ziped contents with passward
        if (supportAES) {
            String aesedPwd = StringUtils.randomString(AESED_DYNAMIC_PWD_LEN);
            Assert.isTrue(aesedPwd.length() == AESED_DYNAMIC_PWD_LEN);
            byte[] bAesedKey = aesedPwd.getBytes("UTF-8");
            byte[] encryptedContents = AES.encrypt(zipedContents, bAesedKey);
            // encrypted data length must 16 multipul.
            if (encryptedContents != null && (encryptedContents.length % 16 == 0)) {
                type = 2;
                key = bAesedKey;
                contents = encryptedContents;

                //3. As AES succeed, encrypt the AESed password by RSA
                final String PUB_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDBnfRKIUm5FCy+vMxaGPwIpK0y573bFJIzebpcg1mXA5QOEg/xg0wtjZ+Sc+WI2LflEm7H3sf6G9vh30j7Ua94LQr/e8Th44o57dmq38JY8ZYU6Tyxd2zUCS3nqB6XQF9wfqFdST3BK2uMXE7SUcxSJHXbizt1cnt6xXtFGgaJ0wIDAQAB";
                byte[] bRsaedKey = RSA.encrypt(bAesedKey, PUB_KEY);
                if (bRsaedKey != null) {
                    type = 3;
                    key = bRsaedKey;
                }
            }
        }

        Logger.v(TAG, "encrpyt type:" + type);
        //4. output the encoded packet
        ByteArrayOutputStream encodedPacket = new ByteArrayOutputStream();
        encodedPacket.write(type);
        if (key != null) {
            encodedPacket.write(Utils.toBytes(key.length));
            encodedPacket.write(key);
        }
        encodedPacket.write(contents);
        return encodedPacket.toByteArray();
    }

    private static void checkEncryptMethod() {
        Settings settings = new Settings(ObjectStore.getContext(),"beyla_settings");
        if (settings.contains("support_aes")) {
            supportAES = settings.getBoolean("support_aes");
            Logger.v(TAG, "support aes:" + supportAES);
            return;
        }

        try {
            byte[] aesData = AES.encrypt("best shareit!".getBytes("UTF-8"), getTeskKey().getBytes("UTF-8"));
            supportAES = (aesData == null ? false : Arrays.equals(aesData, AESDATA_C));
        } catch (Throwable e) {}
        settings.setBoolean("support_aes", supportAES);
    }

    private static String getTeskKey() {
        String left = "1234567890";
        String right = "abcdef";

        return left.concat(right);
    }
}
