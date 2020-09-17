package com.ushareit.core.utils.i18n;

import android.content.Context;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

//TODO: [Owner: LiuFengSong] SW consider use android res-xxx mechnism to build a flexible base for i18n of builtin web pages.
public class XmlResourceUtils {
    private static final String TAG = "XmlResourceUtils";
    public static final String HOME_HTML_RESOURCE_FILE = "home_resource.xml";
    public static final String WEBSHARE_HTML_RESOURCE_FILE = "webshare_resource.xml";
    public static final String TYPE_RESOURCE_FILE = "type_resource.xml";

    private static void putKeyToHash(HashMap<String, String> hashMap, String key, Element element) {
        hashMap.put(key, element.hasAttribute(key) ? element.getAttribute(key) : "");
    }

    public static HashMap<String, String> getHtmlResource(Context context, String fileName, String language) {
        DocumentBuilder builder = null;
        Document document = null;
        InputStream inputStream = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String type = getLanguageType(language);
        HashMap<String, String> htmlResource = null;

        try {
            builder = factory.newDocumentBuilder();
            inputStream = context.getResources().getAssets().open(fileName);
            document = builder.parse(inputStream);

            Element root = document.getDocumentElement();
            NodeList nodes = root.getElementsByTagName("language");

            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element)(nodes.item(i));
                String name = element.getAttribute("name");
                if (!type.equals(name))
                    continue;

                htmlResource = new LinkedHashMap<String, String>();
                if (HOME_HTML_RESOURCE_FILE.equals(fileName)) {
                    putKeyToHash(htmlResource, "app_name", element);
                    putKeyToHash(htmlResource, "intro1", element);
                    putKeyToHash(htmlResource, "intro2", element);
                    putKeyToHash(htmlResource, "download_text", element);
                } else if (WEBSHARE_HTML_RESOURCE_FILE.equals(fileName)) {
                    putKeyToHash(htmlResource, "html_title", element);
                    putKeyToHash(htmlResource, "html_title_jio", element);
                    putKeyToHash(htmlResource, "app_name", element);
                    putKeyToHash(htmlResource, "description1", element);
                    putKeyToHash(htmlResource, "description2", element);
                    putKeyToHash(htmlResource, "description3", element);
                    putKeyToHash(htmlResource, "description4", element);
                    putKeyToHash(htmlResource, "description5", element);
                    putKeyToHash(htmlResource, "description6", element);
                    putKeyToHash(htmlResource, "no_item", element);
                } else if (TYPE_RESOURCE_FILE.equals(fileName)) {
                    putKeyToHash(htmlResource, "app", element);
                    putKeyToHash(htmlResource, "music", element);
                    putKeyToHash(htmlResource, "video", element);
                    putKeyToHash(htmlResource, "photo", element);
                    putKeyToHash(htmlResource, "contact", element);
                    putKeyToHash(htmlResource, "file", element);
                }
                break;
            }
        } catch (IOException e) {
            Logger.d(TAG, e.toString());
        } catch (SAXException e) {
            Logger.d(TAG, e.toString());
        } catch (ParserConfigurationException e) {
            Logger.d(TAG, e.toString());
        } finally {
            Utils.close(inputStream);
        }

        return htmlResource;
    }

    public static String getContentTypeString(Context context, String type, String language) {
        HashMap<String, String> htmlResource = getHtmlResource(context, TYPE_RESOURCE_FILE, language);
        if (htmlResource == null)
            return "";
        return htmlResource.get(type);
    }

    // default use en-us language
    private static String getLanguageType(String language) {
        language = LocaleUtils.toLowerCaseIgnoreLocale(language);

        if (language.startsWith("zh-cn"))
            return "zh-cn";
        else if (language.startsWith("zh-tw"))
            return "zh-tw";
        else if (language.startsWith("zh-hk"))
            return "zh-hk";
        else if (language.startsWith("en-us"))
            return "en-us";
        else if (language.startsWith("ar"))
            return "ar";
        else if (language.startsWith("bg"))
            return "bg";
        else if (language.startsWith("cs"))
            return "cs";
        else if (language.startsWith("de"))
            return "de";
        else if (language.startsWith("el"))
            return "el";
        else if (language.startsWith("es"))
            return "es";
        else if (language.startsWith("et"))
            return "et";
        else if (language.startsWith("fa"))
            return "fa";
        else if (language.startsWith("fi"))
            return "fi";
        else if (language.startsWith("fr"))
            return "fr";
        else if (language.startsWith("hi"))
            return "hi";
        else if (language.startsWith("hr"))
            return "hr";
        else if (language.startsWith("hu"))
            return "hu";
        else if (language.startsWith("in"))
            return "in";
        else if (language.startsWith("it"))
            return "it";
        else if (language.startsWith("iw"))
            return "iw";
        else if (language.startsWith("ja"))
            return "ja";
        else if (language.startsWith("ko"))
            return "ko";
        else if (language.startsWith("lt"))
            return "lt";
        else if (language.startsWith("lv"))
            return "lv";
        else if (language.startsWith("ms"))
            return "ms";
        else if (language.startsWith("pl"))
            return "pl";
        else if (language.startsWith("pt-rbr"))
            return "pt-rbr";
        else if (language.startsWith("pt-rpt"))
            return "pt-rpt";
        else if (language.startsWith("ro"))
            return "ro";
        else if (language.startsWith("ru"))
            return "ru";
        else if (language.startsWith("sk"))
            return "sk";
        else if (language.startsWith("sl"))
            return "sl";
        else if (language.startsWith("sr"))
            return "sr";
        else if (language.startsWith("th"))
            return "th";
        else if (language.startsWith("tr"))
            return "tr";
        else if (language.startsWith("uk"))
            return "uk";
        else if (language.startsWith("vi"))
            return "vi";

        return "en-us";
    }

}
