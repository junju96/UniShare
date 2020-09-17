package com.ushareit.core.utils;

public class HtmlUtils {

    public static String getColorString(String color, String str) {
        return "<font color=" + color + ">" + str + "</font>";
    }

    public static String getSizeString(boolean isBigSize, String str) {
        return isBigSize ? "<big>" + str + "</big>" : "<small>" + str + "</small>";
    }

    public static String getBoldString(String str) {
        return "<B>" + str + "</B>";
    }

    public static String replaceLineBreak(String str) {
        return str.replaceAll("\n", "<br>");
    }

    public static String getImageString(int resId) {
        return "<img src=\"" + resId + "\" />";
    }

    public static String getLineString(String str) {
        return "<u>" + str + "</u>";
    }
}