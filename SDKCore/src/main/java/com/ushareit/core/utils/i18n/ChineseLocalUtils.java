package com.ushareit.core.utils.i18n;

import com.ushareit.core.utils.i18n.HanziToPinyin.Token;

import java.util.ArrayList;

public final class ChineseLocalUtils {
    private ChineseLocalUtils() {}

    private static HanziToPinyin sInstance = null;
    
    public static String getSortKey(String displayName) {
        if (sInstance == null)
            sInstance = HanziToPinyin.getInstance();

        ArrayList<Token> tokens = sInstance.get(displayName);

        if (tokens != null && tokens.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Token token : tokens) {
                // Put Chinese character's pinyin, then proceed with the character itself.
                if (token.type == Token.PINYIN) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.target);
                    sb.append(' ');
                    sb.append(token.source);
                } else {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.source);
                }
            }
            return sb.toString();
        }
        return displayName;
    }
}
