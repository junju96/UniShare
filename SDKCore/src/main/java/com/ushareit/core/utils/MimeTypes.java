package com.ushareit.core.utils;

import com.ushareit.core.lang.ContentType;
import com.ushareit.core.lang.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MimeTypes {
    private static final Map<String, String> mMimeTypes = new HashMap<String, String>();

    static {
        init();
    }

    public MimeTypes() {}

    public String getMimeType(String extension) {
        String mime = mMimeTypes.get(extension.toLowerCase(Locale.US));
        if (mime == null)
            mime = "";
        return mime;
    }

    public static ContentType getRealContentType(String extension) {
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

    private static void init() {
        mMimeTypes.put(".png", "image/png");
        mMimeTypes.put(".gif", "image/gif");
        mMimeTypes.put(".jpg", "image/jpeg");
        mMimeTypes.put(".jpeg", "image/jpeg");
        mMimeTypes.put(".bmp", "image/bmp");
        mMimeTypes.put(".wbmp", "image/wbmp");
        mMimeTypes.put(".webp", "image/webp");

        mMimeTypes.put(".mp3", "audio/mp3");
        mMimeTypes.put(".wav", "audio/wav");
        mMimeTypes.put(".mid", "audio/midi");
        mMimeTypes.put(".midi", "audio/midi");
        mMimeTypes.put(".wma", "audio/wma");
        mMimeTypes.put(".aac", "audio/aac");
        mMimeTypes.put(".ra", "audio/ra");
        mMimeTypes.put(".amr", "audio/amr");
        mMimeTypes.put(".au", "audio/au");
        mMimeTypes.put(".aiff", "audio/aiff");
        mMimeTypes.put(".ogg", "audio/ogg"); // audio/x-ogg
        mMimeTypes.put(".m4a", "audio/m4a");
        mMimeTypes.put(".f4a", "audio/f4a");
        mMimeTypes.put(".flac", "audio/flac");
        mMimeTypes.put(".ape", "audio/ape"); // audio/x-ape
        mMimeTypes.put(".imy", "audio/imy");
        mMimeTypes.put(".ac3", "audio/ac3");
        mMimeTypes.put(".mpa", "audio/mpa");
        mMimeTypes.put(".mka", "audio/mka");
        mMimeTypes.put(".mpc", "audio/mpc");
        mMimeTypes.put(".mod", "audio/mod");
        mMimeTypes.put(".dts", "audio/dts");
        mMimeTypes.put(".wv", "audio/wv");
        mMimeTypes.put(".mp2", "audio/mp2");
        mMimeTypes.put(".sa", "audio/x-si-sa");

        mMimeTypes.put(".3gp", "video/3gp");
        mMimeTypes.put(".3gpp", "video/3gpp");
        mMimeTypes.put(".divx", "video/divx");
        mMimeTypes.put(".mpeg", "video/mpeg");
        mMimeTypes.put(".rm", "video/rm");
        mMimeTypes.put(".rmvb", "video/rmvb");
        mMimeTypes.put(".avi", "video/x-msvideo");
        mMimeTypes.put(".wmv", "video/wmv");
        mMimeTypes.put(".mp4", "video/mp4");
        mMimeTypes.put(".flv", "video/flv");
        mMimeTypes.put(".fla", "video/fla");
        mMimeTypes.put(".f4v", "video/f4v");
        mMimeTypes.put(".mov", "video/mov");
        mMimeTypes.put(".mpg", "video/mpg");
        mMimeTypes.put(".asf", "video/asf");
        mMimeTypes.put(".rv", "video/rv");
        mMimeTypes.put(".mkv", "video/x-matroska");
        mMimeTypes.put(".3g2", "video/3g2");
        mMimeTypes.put(".3gp2", "video/3gp2");
        mMimeTypes.put(".m4v", "video/m4v");
        mMimeTypes.put(".mp2v", "video/mp2v");
        mMimeTypes.put(".mpeg1", "video/mpeg");
        mMimeTypes.put(".mpeg2", "video/mpeg");
        mMimeTypes.put(".mpeg4", "video/mpeg");
        mMimeTypes.put(".ts", "video/ts");
        mMimeTypes.put(".webm", "video/webm");
        mMimeTypes.put(".vob", "video/vob");
        mMimeTypes.put(".sv", "video/x-si-sv");
        mMimeTypes.put(".esv", "video/x-si-esv");
        mMimeTypes.put(".tsv", "video/x-si-tsv");
        mMimeTypes.put(".dsv", "video/x-si-dsv");

        mMimeTypes.put(".jar", "application/java-archive");
        mMimeTypes.put(".jad", "text/vnd.sun.j2me.app-descriptor");

        mMimeTypes.put(".htm", "text/html");
        mMimeTypes.put(".html", "text/html");
        mMimeTypes.put(".xhtml", "text/html");
        mMimeTypes.put(".mht", "message/rfc822");
        mMimeTypes.put(".mhtml", "message/rfc822");
        mMimeTypes.put(".php", "text/php");
        mMimeTypes.put(".txt", "text/plain");
        mMimeTypes.put(".rtf", "text/plain");
        mMimeTypes.put(".csv", "text/csv");
        mMimeTypes.put(".xml", "text/xml");
        mMimeTypes.put(".vcf", "text/x-vcard"); // contacts/vcf
        mMimeTypes.put(".vcs", "text/x-vcalendar"); // android calendar format
        mMimeTypes.put(".c", "text/plain");
        mMimeTypes.put(".h", "text/plain");
        mMimeTypes.put(".cpp", "text/plain");
        mMimeTypes.put(".cs", "text/plain");
        mMimeTypes.put(".java", "text/plain");
        mMimeTypes.put(".jsp", "text/plain");
        mMimeTypes.put(".asp", "text/plain");
        mMimeTypes.put(".aspx", "text/plain");
        mMimeTypes.put(".log", "text/plain");
        mMimeTypes.put(".ini", "text/plain");
        mMimeTypes.put(".bat", "text/bath");

        mMimeTypes.put(".apk", "application/vnd.android.package-archive");
        mMimeTypes.put(".lca", "application/vnd.android.package-archive");
        mMimeTypes.put(".doc", "application/msword");
        mMimeTypes.put(".docx", "application/msword");
        mMimeTypes.put(".dot", "application/msword");
        mMimeTypes.put(".ppt", "application/mspowerpoint");
        mMimeTypes.put(".pptx", "application/mspowerpoint");
        mMimeTypes.put(".pps", "application/mspowerpoint");
        mMimeTypes.put(".ppsx", "application/msexcel");
        mMimeTypes.put(".xls", "application/msexcel");
        mMimeTypes.put(".xlsx", "application/msexcel");
        mMimeTypes.put(".pdf", "application/pdf");
        mMimeTypes.put(".epub", "application/epub+zip");
        mMimeTypes.put(".zip", "application/zip"); // compressor/zip
        mMimeTypes.put(".gz", "application/gzip");
        mMimeTypes.put(".tar", "application/x-tar");
        mMimeTypes.put(".gtar", "application/x-gtar");

        mMimeTypes.put(".ics", "ics/calendar");

        mMimeTypes.put(".p12", "application/x-pkcs12");
        mMimeTypes.put(".cer", "application/x-x509-ca-cert");
        mMimeTypes.put(".crt", "application/x-x509-ca-cert");

        mMimeTypes.put(".dll", "application/x-msdownload");
        mMimeTypes.put(".css", "text/css");
        mMimeTypes.put(".swf", "application/x-shockwave-flash");
        mMimeTypes.put(".texi", "application/x-texinfo");
        mMimeTypes.put(".texinfo", "application/x-texinfo");
    }
}
