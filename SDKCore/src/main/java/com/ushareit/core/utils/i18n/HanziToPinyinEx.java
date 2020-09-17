package com.ushareit.core.utils.i18n;

import android.content.Context;
import android.text.TextUtils;

import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.utils.i18n.HanziToPinyin.Token;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class HanziToPinyinEx {
    private final static String TAG = "HanziToPinyinEx";

    private static HanziToPinyinEx mInstance;

    public static HanziToPinyinEx getInstance() {
        if (mInstance == null) {
            synchronized (HanziToPinyinEx.class) {
                if (mInstance == null) {
                    mInstance = new HanziToPinyinEx(ObjectStore.getContext());
                    mInstance.init();
                }
            }
        }
        return mInstance;
    }

    private Properties mDB;

    protected HanziToPinyinEx(Context context) {
        mDB = new Properties();
    }

    public ArrayList<Token> get(final String input) {
        ArrayList<Token> tokens = new ArrayList<Token>();
        if (TextUtils.isEmpty(input)) {
            // return empty tokens.
            return tokens;
        }
        final int inputLength = input.length();
        final StringBuilder sb = new StringBuilder();
        int tokenType = Token.LATIN;
        // Go through the input, create a new token when
        // a. Token type changed
        // b. Get the Pinyin of current charater.
        // c. current character is space.
        for (int i = 0; i < inputLength; i++) {
            final char character = input.charAt(i);
            if (character == ' ') {
                if (sb.length() > 0) {
                    addToken(sb, tokens, tokenType);
                }
            } else if (character < 256) {
                if (tokenType != Token.LATIN && sb.length() > 0) {
                    addToken(sb, tokens, tokenType);
                }
                tokenType = Token.LATIN;
                sb.append(character);
            } else {
                Token t = getToken(character);
                if (t.type == Token.PINYIN) {
                    if (sb.length() > 0) {
                        addToken(sb, tokens, tokenType);
                    }
                    tokens.add(t);
                    tokenType = Token.PINYIN;
                } else {
                    if (tokenType != t.type && sb.length() > 0) {
                        addToken(sb, tokens, tokenType);
                    }
                    tokenType = t.type;
                    sb.append(character);
                }
            }
        }
        if (sb.length() > 0) {
            addToken(sb, tokens, tokenType);
        }
        return tokens;
    }

    private void init() {
        Logger.i(TAG, "init pinyin memory!");
        InputStream in = null;
        try {
            in = ObjectStore.getContext().getAssets().open("hanzi_to_pinyin.txt");
            mDB.load(in);
        } catch (Exception e) {
            Logger.w(TAG, "not find pinyin resource!");
        } finally {
            Utils.close(in);
        }
    }

    private void free() {
        mDB.clear();
    }
    
    private Token getToken(char character) {
        Token token = new Token();
        final String letter = Character.toString(character);
        token.source = letter;
        if (character < 256) {
            token.type = Token.LATIN;
            token.target = letter;
            return token;
        } else {
            int codePointOfChar = character;
            String codepointHexStr = LocaleUtils.toUpperCaseIgnoreLocale(Integer.toHexString(codePointOfChar));
            // fetch from hashtable
            String value = mDB.getProperty(codepointHexStr);
            String foundRecord = isValidRecord(value) ? value : null;
            String pinyin = null;
            if (null != foundRecord) {
                int left = foundRecord.indexOf(Field.LEFT_BRACKET);
                int right = foundRecord.lastIndexOf(Field.RIGHT_BRACKET);
                String striped = foundRecord.substring(left + Field.LEFT_BRACKET.length(), right);
                String[] pinyins = striped.split(Field.COMMA);
                if (pinyins.length > 0 && pinyins[0].length() > 0)
                    pinyin = pinyins[0].substring(0, pinyins[0].length() - 1);
            }
            token.type = (pinyin == null) ? Token.UNKNOWN : Token.PINYIN;
            token.target = (pinyin == null) ? letter : pinyin;
            return token;
        }
    }

    private void addToken(final StringBuilder sb, final ArrayList<Token> tokens, final int tokenType) {
        String str = sb.toString();
        tokens.add(new Token(tokenType, str, str));
        sb.setLength(0);
    }

    private boolean isValidRecord(String record) {
        final String noneStr = "(none0)";
        return ((null != record) && !record.equals(noneStr) && record.startsWith(Field.LEFT_BRACKET) && record.endsWith(Field.RIGHT_BRACKET));
    }

    class Field {
        static final String LEFT_BRACKET = "(";
        static final String RIGHT_BRACKET = ")";
        static final String COMMA = ",";
    }
}
