package com.ushareit.core.utils.i18n;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocaleUtils {
    public static String formatStringIgnoreLocale(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }

    public static String decimalFormatIgnoreLocale(String pattern, double value) {
        return new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.US)).format(value);
    }

    // 阿拉伯国家不使用阿拉伯数字显示数字信�? 采集系统通过此方法获取可识别的date
    public static String simpleDateFormatIgnoreLocale(String template, Date date) {
        SimpleDateFormat format = new SimpleDateFormat(template, Locale.US);
        return format.format(date);
    }

    public static Date simpleDateParseIgnoreLocale(String template, String str) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(template, Locale.US);
        return format.parse(str);
    }

    public static String toLowerCaseIgnoreLocale(String str) {
        return str.toLowerCase(Locale.US);
    }

    public static String toUpperCaseIgnoreLocale(String str) {
        return str.toUpperCase(Locale.US);
    }
}
