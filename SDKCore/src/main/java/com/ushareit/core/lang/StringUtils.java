package com.ushareit.core.lang;

import android.content.Context;

import com.ushareit.core.Assert;
import com.ushareit.sdk_core.R;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Random;

public final class StringUtils {

    public static String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < length; i++) {
            int num = random.nextInt(62);
            buf.append(str.charAt(num));
        }

        return buf.toString();
    }

    public static int quietParseToInt(String pValue, int defaultValue) {
        if (pValue == null)
            return defaultValue;
        try {
            return Integer.parseInt(pValue);
        } catch (Exception e) {} ;
        return defaultValue;
    }

    public static long quietParseToLong(String pValue, long defaultValue) {
        if (pValue == null)
            return defaultValue;
        try {
            return Long.parseLong(pValue);
        } catch (Exception e) {} ;
        return defaultValue;
    }

    /**
     * 将String转为byte[]
     * @param text 目标String
     * @return 转换后的byte[]
     */
    public static byte[] stringToByte(String text){
        if(isEmpty(text)){
            return new byte[0];
        }
        return text.getBytes(Charset.defaultCharset());
    }

    private static DecimalFormat sFormat = new DecimalFormat("0.#");

    public static String getViewCount(Context context, int views) {
        String viewCount;
        if (views >= 10000000) {
            viewCount = views / 1000000 + context.getString(R.string.sz_media_view_times_unit_m);
        } else if (views >= 1000000) {
            viewCount = sFormat.format(views / 1000000f).replace(".0", "") + context.getString(R.string.sz_media_view_times_unit_m);
        } else if (views >= 10000) {
            viewCount = views / 1000 + context.getString(R.string.sz_media_view_times_unit_k);
        } else if (views >= 1000) {
            viewCount = sFormat.format(views / 1000f).replace(".0", "") + context.getString(R.string.sz_media_view_times_unit_k);
        } else {
            viewCount = String.valueOf(views);
        }
        return viewCount;
    }

    public static String getSubString(String s, int length) {
        byte[] bytes = {};
        try {
            bytes = s.getBytes("Unicode");
        } catch (UnsupportedEncodingException e) {}

        int n = 0; // 表示当前的字节数
        int i = 2; // 前两个字节是标志位，bytes[0] = -2，bytes[1] = -1。所以从第3位开始截取。
        for (; i < bytes.length && n < length; i++) {
            // 奇数位置，如3、5、7等，为UCS2编码中两个字节的第二个字节
            if (i % 2 != 0) {
                n++; // 在UCS2第二个字节时n加1
            } else {
                // 当UCS2编码的第一个字节不等于0时，该UCS2字符为汉字，一个汉字算两个字节
                if (bytes[i] != 0) {
                    n++;
                }
            }
        }

        // 如果i为奇数时，处理成偶数
        if (i % 2 != 0) { // 该UCS2字符是汉字时，去掉这个截一半的汉字
            if (bytes[i - 1] != 0)
                i = i - 1; // 该UCS2字符是字母或数字，则保留该字符
            else
                i = i + 1;
        }

        String newStr = "";
        try {
            newStr = new String(bytes, 0, i, "Unicode");
        } catch (UnsupportedEncodingException e) {}
        return newStr;
    }

    public static String leftPad(String str, int len, char ch) {
        StringBuilder builder = new StringBuilder();
        int start = str == null ? 0 : str.length();
        for (int i = start; i < len; i++)
            builder.append(ch);
        if (str != null)
            builder.append(str);
        return builder.toString();
    }

    public static String rightPad(String str, int len, char ch) {
        StringBuilder builder = new StringBuilder();
        if (str != null)
            builder.append(str);
        int start = str == null ? 0 : str.length();
        for (int i = start; i < len; i++)
            builder.append(ch);
        return builder.toString();
    }

    public static String toHex(byte value) {
        int unsignedInt = value < 0 ? 256 + value : value;
        return leftPad(Integer.toHexString(unsignedInt), Byte.SIZE / 4, '0');
    }

    public static String toHex(int value) {
        return leftPad(Integer.toHexString(value), Integer.SIZE / 4, '0');
    }

    public static String toHex(long value) {
        return leftPad(Long.toHexString(value), Long.SIZE / 4, '0');
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null)
            return null;

        StringBuilder builder = new StringBuilder();
        for (byte b : bytes)
            builder.append(toHex(b));
        return builder.toString();
    }

    public static String toHex(byte[] bytes, char seperator) {
        Assert.notNull(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes)
            builder.append(toHex(b)).append(seperator);
        return builder.toString();
    }

    public static boolean isBlank(String string) {
        return string == null || "".equals(string) || "".equals(string.trim());
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    public static boolean isEmpty(String string) {
        return string == null || "".equals(string);
    }

    public static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        String str = "0123456789abcdef";
        char[] hexs = s.toCharArray();
        byte[] bytes = new byte[s.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) << 4;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return  bytes;
    }
}
