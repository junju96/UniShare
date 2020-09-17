package com.ushareit.core.algo;

import com.ushareit.core.Logger;
import com.ushareit.core.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @deprecated replace with HashUtils
 */
public class CommonExtendHashUtils {
    private static final String TAG = "CommonExtendHashUtils";
    private static final String DIGEST_NAME = "MD5";

    private CommonExtendHashUtils() {}

    private static MessageDigest messageDigest;

    public synchronized static MessageDigest getMessageDigest() {
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance(DIGEST_NAME);
            } catch (NoSuchAlgorithmException e) {
                Logger.e(TAG, e.getMessage(), e);
            }
        }
        return messageDigest;
    }

    public static String hash(String string) {
        if (string != null) {
            try {
                return StringUtils.toHex(hash(string.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                Logger.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    public static byte[] hash(byte[] bytes) {
        if (bytes == null)
            return null;

        MessageDigest md = getMessageDigestCopy();
        if (md == null)
            return null;

        md.update(bytes);
        return md.digest();
    }

    public static MessageDigest getMessageDigestCopy() {
        MessageDigest md = getMessageDigest();
        if (md != null) {
            try {
                md = (MessageDigest) md.clone();
            } catch (Exception e) {
                // Some device can not support clone(). example: HUAWEI C8550
                Logger.d(TAG, e.toString());
            }
        } // Maybe cannot get instance of message digest anyway!
        return md;
    }
}
