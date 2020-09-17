package com.ushareit.core.lang;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import java.util.Locale;
import java.util.Map;

/**
 * content type enumeration
 */
public enum ContentType {
    MUSIC("music"), VIDEO("video"), PHOTO("photo"), APP("app"), GAME("game"), 
    FILE("file"), DOCUMENT("doc"), ZIP("zip"), EBOOK("ebook"), CONTACT("contact"),
    TOPFREE("topfree");

    private String mValue;

    ContentType(String value) {
        mValue = value;
    }

    /**
     * convert to content type by its string format.
     * @param value
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static ContentType fromString(String value) {
        if (!TextUtils.isEmpty(value)) {
            for (ContentType type : ContentType.values()) {
                if (type.mValue.equals(value.toLowerCase()))
                    return type;
            }
        }
        return null;
    }

    /**
     * convert content type to its string format.
     */
    @Override
    public String toString() {
        return mValue;
    }

    public boolean isApp() {
        return APP.toString().equals(mValue) || GAME.toString().equals(mValue);
    }

    public static ContentType getRealContentType(String extension, Map<String, String> mMimeTypes) {
        if (StringUtils.isBlank(extension))
            return ContentType.FILE;

        String mimeType = mMimeTypes.get("." + extension.toLowerCase(Locale.US));
        if (StringUtils.isBlank(mimeType))
            return ContentType.FILE;

        if (mimeType.startsWith("image/"))
            return ContentType.PHOTO;
        else if (mimeType.startsWith("audio/"))
            return ContentType.MUSIC;
        else if (mimeType.startsWith("video/"))
            return ContentType.VIDEO;
        else if (mimeType.equalsIgnoreCase("application/vnd.android.package-archive"))
            return ContentType.APP;
        else if (mimeType.equalsIgnoreCase("text/x-vcard"))
            return ContentType.CONTACT;

        return ContentType.FILE;
    }

    // for analytics, just focus the following 6 types.
    static public int getMask(ContentType type) {
        int mask = 0;
        switch (type) {
            case CONTACT:
                mask = 32;  // 100000
                break;
            case APP:
            case GAME:
                mask = 16;  // 010000
                break;
            case PHOTO:
                mask = 8;   // 001000
                break;
            case MUSIC:
                mask = 4;   // 000100
                break;
            case VIDEO:
                mask = 2;   // 000010
                break;
            case FILE:
                mask = 1;   // 000001
                break;
            default:
                break;
        }
        return mask;
    }
}
