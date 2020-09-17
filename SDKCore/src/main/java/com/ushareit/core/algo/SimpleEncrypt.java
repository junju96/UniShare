package com.ushareit.core.algo;

import android.text.TextUtils;

public class SimpleEncrypt {
    private static final String TAG = "AssistHelper";
    private static String COMMON_ENCODE_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static String COMMON_DECODE_STRING = "LMOYZabSTstJKfghiuvw6BCr34DEFxyNPQRz012UVpq5GHIjklmn78WXcdeAo9";
    private static String SYMBOL_ENCODE_STRING = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    private static String SYMBOL_DECODE_STRING = "#)$*&'(=>?@[\\]^_`,<{!\"|-+%./:;}~";

    private SimpleEncrypt() {
    }

    // 编码(encode)
    public static String encode(String content) throws RuntimeException {
        if (TextUtils.isEmpty(content))
            return "";

        String encodeString = "";
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            int originIndex = COMMON_ENCODE_STRING.indexOf(c);
            if (originIndex != -1) {
                int newIndex = (originIndex + i * 3) % COMMON_DECODE_STRING.length();
                encodeString += COMMON_DECODE_STRING.charAt(newIndex);
            } else {
                originIndex = SYMBOL_ENCODE_STRING.indexOf(c);
                if (originIndex == -1)
                    throw new RuntimeException("unsupport encode content : " + content);
                int newIndex = (originIndex + i * 3) % SYMBOL_ENCODE_STRING.length();
                encodeString += SYMBOL_DECODE_STRING.charAt(newIndex);
            }
        }

        return encodeString;
    }

    // 解码(decode)
    public static String decode(String content) throws RuntimeException {
        if (content == null)
            throw new RuntimeException("decode content is null!");

        String decodeString = "";
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            int originIndex = COMMON_DECODE_STRING.indexOf(c);
            if (originIndex != -1) {
                int newIndex = (originIndex - i * 3 + COMMON_DECODE_STRING.length()) % COMMON_DECODE_STRING.length();
                decodeString += COMMON_ENCODE_STRING.charAt(newIndex);
            } else {
                originIndex = SYMBOL_DECODE_STRING.indexOf(c);
                if (originIndex == -1)
                    throw new RuntimeException("unsupport decode content : " + content);
                int newIndex = (originIndex - i * 3 + SYMBOL_DECODE_STRING.length()) % SYMBOL_DECODE_STRING.length();
                decodeString += SYMBOL_ENCODE_STRING.charAt(newIndex);
            }
        }

        return decodeString;
    }
}
