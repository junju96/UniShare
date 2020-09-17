package com.ushareit.core.algo;

import android.os.Build;

import com.ushareit.core.Logger;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.io.sfile.SFile.OpenMode;
import com.ushareit.core.lang.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class HashUtils {
    private static final String TAG = "HashUtils";

    private static String COMMON_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DIGEST_NAME = "MD5";

    private static final int BUFFER_SIZE = 1024 * 8;
    private static final int BLOCK_COUNT = 8;
    private static final long MAX_TOTAL_LENGTH = 1024L * 1024 * 8;

    private HashUtils() {}

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        long highBites = uuid.getMostSignificantBits();
        long lowBites = uuid.getLeastSignificantBits();
        return StringUtils.toHex(highBites) + StringUtils.toHex(lowBites);
    }

    private static MessageDigest messageDigest;

    private synchronized static MessageDigest getMessageDigest() {
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance(DIGEST_NAME);
            } catch (NoSuchAlgorithmException e) {
                Logger.e(TAG, e.getMessage(), e);
            }
        }
        return messageDigest;
    }

    private static MessageDigest getMessageDigestCopy() {
        MessageDigest md = getMessageDigest();
        if (md != null) {
            try {
                md = (MessageDigest)md.clone();
            } catch (Exception e) {
                // Some device can not support clone(). example: HUAWEI C8550
                Logger.d(TAG, e.toString());
            }
        } // Maybe cannot get instance of message digest anyway!
        return md;
    }

    public static String hashToString(SFile file) {
        return (file != null) ? StringUtils.toHex(hash(file)) : null;
    }

    public static String hashToStringEx(SFile file) {
        if (file == null)
            return null;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            MessageDigest md = getMessageDigest();
            if (md != null)
                synchronized (HashUtils.class) {
                    return StringUtils.toHex(hash(md, file));
                }
        }

        MessageDigest md = getMessageDigestCopy();
        return (file != null) ? StringUtils.toHex(hash(md, file)) : null;
    }

    public static byte[] hash(SFile file) {
        if (file == null)
            return null;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            MessageDigest md = getMessageDigest();
            if (md != null)
                synchronized (HashUtils.class) {
                    return newHash(md, file);
                }
        }

        MessageDigest md = getMessageDigestCopy();
        return (md != null) ? newHash(md, file) : null;
    }

    public static byte[] hashEx(SFile file) {
        if (file == null)
            return null;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            MessageDigest md = getMessageDigest();
            if (md != null)
                synchronized (HashUtils.class) {
                    return hash(md, file);
                }
        }

        MessageDigest md = getMessageDigestCopy();
        return (md != null) ? hash(md, file) : null;
    }

    private static byte[] newHash(MessageDigest digest, SFile file) {
        byte[] ret = null;
        try {
            long startTime = System.currentTimeMillis(), fileLength = file.length();

            // Extract file-length as a independent file-attribute,
            // And do not digest file-length anymore.
            // digest.update(Utils.toBytes(fileLength));

            file.open(OpenMode.Read);

            int count = 1;
            long blockSize = fileLength, step = 0;
            if (fileLength > MAX_TOTAL_LENGTH) {
                count = BLOCK_COUNT;
                blockSize = MAX_TOTAL_LENGTH / count;
                step = (fileLength - MAX_TOTAL_LENGTH) / (count - 1);
            }

            long sum = 0, startIndex = 0;
            for (int i = 0; i < count; i++) {
                sum += updateHash(digest, file, startIndex, blockSize);
                startIndex += blockSize + step;
            }

            ret = digest.digest();
            Logger.v(TAG, sum + "/" + file.length() + " bytes file newHash, cost-time: "
                    + (System.currentTimeMillis() - startTime) / 1000.0 + " s.");
        } catch (FileNotFoundException e) {
            Logger.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage(), e);
        } finally {
            file.close();
        }
        return ret;
    }

    private static long updateHash(MessageDigest digest, SFile file, long start, long length) throws IOException {
        long sum = 0;
        file.seek(OpenMode.Read, start);
        byte[] buffer = new byte[BUFFER_SIZE];
        int r, len = (int)Math.min(BUFFER_SIZE, length - sum);
        while (len > 0 && (r = file.read(buffer, 0, len)) != -1) {
            digest.update(buffer, 0, r);

            sum += r;
            start += r;
            len = (int)Math.min(BUFFER_SIZE, length - sum);
        }
        return sum;
    }

    public static byte[] hash(final MessageDigest digest, final SFile file) {
        long sum = 0;
        long start = System.currentTimeMillis();
        try {
            file.open(OpenMode.Read);
            byte[] buffer = new byte[BUFFER_SIZE];
            int r;
            while ((r = file.read(buffer)) != -1) {
                digest.update(buffer, 0, r);
                sum += r;
            }

            return digest.digest();
        } catch (FileNotFoundException e) {
            Logger.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage(), e);
        } finally {
            file.close();
            Logger.v(TAG, sum + " bytes file hash -> " + (System.currentTimeMillis() - start) / 1000.0 + " s.");
        }

        return null;
    }

    public static String hashToString(byte[] bytes) {
        return (bytes != null) ? StringUtils.toHex(hash(bytes)) : null;
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

    public static String generateSimpleHashString(String content, int length) {
        byte result[] = new byte[length];
        for (int i = 0; i < content.length(); i++) {
            result[i % length] ^= content.charAt(i);
        }

        String randomString = "";
        for (int i = 0; i < length; i++) {
            randomString += COMMON_STRING.charAt(result[i] % COMMON_STRING.length());
        }

        Logger.d(TAG, "generateSimpleHashString content = " + content + " randomString = " + randomString);
        return randomString;
    }
}
