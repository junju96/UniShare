package com.ushareit.core.net;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.StreamUtils;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.stats.Stats;
import com.ushareit.core.net.ssl.KeyManagerCreator;
import com.ushareit.core.net.ssl.SSLSocketFactoryCompat;
import com.ushareit.core.net.ssl.SslHostnameVerifier;
import com.ushareit.core.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class HttpUtils {
    public static final String SEN_CLOUD_REQUEST_RESULT = "CLOUD_RequestResult";
    private static final String TAG = "HttpUtils";

    public static class UrlBuilder {
        private StringBuilder builder = new StringBuilder();
        private char link = '?';

        public UrlBuilder(String url, String cmd) {
            builder.append(url);
            builder.append(cmd);
        }

        public UrlBuilder append(String key, Object value) {
            if (key == null || value == null) {
                return this;
            }

            String str;
            if (value instanceof String) {
                str = HttpUtils.urlEncode((String)value);
            } else {
                str = HttpUtils.urlEncode(value.toString());
            }

            builder.append(link).append(key).append("=").append(str);
            if (link == '?')
                link = '&';
            return this;
        }

        public String toString() {
            return builder.toString();
        }
    }

    private HttpUtils() {}

    // url encode a string with UTF-8 encoding
    public static String urlEncode(String src) {
        try {
            return URLEncoder.encode(src, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    public static UrlResponse okHead(String portal, String urlStr, Map<String, String> headers, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        if (params==null)
            params = new LinkedHashMap<>();

        StringBuilder builder = new StringBuilder(urlStr);
        if (!urlStr.contains("?"))
            builder.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (builder.toString().contains("="))
                builder.append("&");
            builder.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
        }

        URL url = new URL(builder.toString());
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.head().url(url);
        if (headers!=null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet())
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
        }
        requestBuilder.addHeader("trace_id", traceId);
        requestBuilder.addHeader("portal", portal);

        OkHttpClient client = getApiOkHttpClient(connectTimeout, readTimeout, null, null, null);
        Response response = client.newCall(requestBuilder.build()).execute();
        return new UrlResponse(response);
    }

    public static UrlResponse okGet(String portal, String urlStr, Map<String, String> headers, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        return okGet(portal, urlStr, headers, params, connectTimeout, readTimeout, null, null, null);
    }

    public static UrlResponse okGet(String portal, String urlStr, Map<String, String> headers, Map<String, String> params, int connectTimeout, int readTimeout, X509TrustManager trustManager, KeyManagerCreator keyManagerCreator, HostnameVerifier hostnameVerifier) throws IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        if (params==null)
            params = new LinkedHashMap<String, String>();
        params.put("trace_id", traceId);

        StringBuilder builder = new StringBuilder(urlStr);
        if (!urlStr.contains("?"))
            builder.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (builder.toString().contains("="))
                builder.append("&");
            builder.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
        }

        URL url = new URL(builder.toString());
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        if (headers!=null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet())
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
        }
        requestBuilder.addHeader("trace_id", traceId);
        requestBuilder.addHeader("portal", portal);

        OkHttpClient client = getApiOkHttpClient(connectTimeout, readTimeout, trustManager, keyManagerCreator, hostnameVerifier);
        Response response = client.newCall(requestBuilder.build()).execute();
        return new UrlResponse(response);
    }

    public static UrlResponse okGet(String portal, String urlStr, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        return okGet(portal, urlStr, null, params, connectTimeout, readTimeout);
    }

    public static UrlResponse get(String urlStr, Map<String, String> headers, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        UrlResponse response = null;

        StringBuilder builder = new StringBuilder(urlStr);
        if (params != null && params.size() > 0) {
            if (!urlStr.contains("?")) {
                builder.append("?");
            }
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (builder.toString().contains("=")) {
                    builder.append("&");
                }
                builder.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
            }
        }

        Logger.d(TAG, "get url -> " + builder.toString());
        URL url = new URL(builder.toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            // conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setInstanceFollowRedirects(true);
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            response = new UrlResponse(conn);
        } finally {
            conn.disconnect();
        }

        Logger.d(TAG, "response" + response.getContent());
        return response;
    }

    public static UrlResponse get(String urlStr, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        return get(urlStr, null, params, connectTimeout, readTimeout);
    }

    public static UrlResponse post(String urlStr, Map<String, String> headers, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        Writer writer = null;
        UrlResponse response = null;

        Logger.d(TAG, "post url -> " + urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setInstanceFollowRedirects(true);
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (params != null && params.size() > 0) {
                StringBuilder builder = new StringBuilder();
                writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                boolean isfirst = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (isfirst) {
                        isfirst = false;
                    } else {
                        writer.write("&");
                        builder.append("&");
                    }

                    writer.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
                    builder.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
                }
                writer.flush();
                Logger.v(TAG, "post params: " + builder);
            }

            response = new UrlResponse(conn);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {}
            }
            conn.disconnect();
        }

        Logger.d(TAG, "response" + response.getContent());
        return response;
    }

    public static UrlResponse postPublicData(String urlStr, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder builder = new StringBuilder(urlStr);
        if (!urlStr.contains("?"))
            builder.append("?");
        if (builder.toString().contains("="))
            builder.append("&");
        builder.append("trace_id").append("=").append(urlEncode(traceId));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("trace_id", traceId);

        return post(builder.toString(), headers, params, connectTimeout, readTimeout);
    }

    public static UrlResponse post(String urlStr, Map<String, String> params, int connectTimeout, int readTimeout) throws IOException {
        return post(urlStr, null, params, connectTimeout, readTimeout);
    }

    public static UrlResponse post(String urlStr, Map<String, String> params, byte[] buffer, int connectTimeout, int readTimeout) throws IOException {
        UrlResponse response = null;

        StringBuilder builder = new StringBuilder(urlStr);
        if (params != null && params.size() > 0) {
            if (!urlStr.contains("?")) {
                builder.append("?");
            }
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (builder.toString().contains("=")) {
                    builder.append("&");
                }
                builder.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
            }
        }

        OutputStream out = null;
        Logger.d(TAG, "post buffer url -> " + builder.toString());
        URL url = new URL(builder.toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            if (buffer != null) {
                out = new BufferedOutputStream(conn.getOutputStream());
                out.write(buffer);
                out.flush();
            }

            response = new UrlResponse(conn);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
            conn.disconnect();
        }

        return response;
    }

    public static UrlResponse post(String urlStr, byte[] buffer, int connectTimeout, int readTimeout) throws IOException {
        UrlResponse response = null;
        String boundary = "ZnGpCtePMx0KrHw_G0Xl9Yefer8JZlRJSXe";
        final String RN = "\r\n";
        final String name = "pic";
        final String fileName = "icon.jpg";
        final String suffix = "jpg";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        BufferedOutputStream out = null;
        try {
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            boundary = "--" + boundary;
            Writer writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.append(boundary).append(RN);
            writer.append("Content-Disposition: form-data; name=\"")
                    .append(name).append("\"; filename=\"").append(fileName).append("\"").append(RN);
            writer.append("Content-Type: image/").append(suffix).append(RN).append(RN);
            writer.flush();

            if (buffer != null) {
                out = new BufferedOutputStream(conn.getOutputStream());
                out.write(buffer);
                out.flush();
            }

            writer.append(RN);
            writer.append(boundary).append("--").append(RN);
            writer.flush();

            response = new UrlResponse(conn);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
            conn.disconnect();
        }

        return response;
    }

    public static UrlResponse okPostData(String portal, String urlStr, Map<String, String> headers, byte[] buffer, int connectTimeout, int readTimeout) throws IOException {
        return okPostData(portal, urlStr, headers, buffer, connectTimeout, readTimeout, null, null, null);
    }

    public static UrlResponse okPostData(String portal, String urlStr, Map<String, String> headers, byte[] buffer, int connectTimeout, int readTimeout, X509TrustManager trustManager, KeyManagerCreator keyManagerCreator, HostnameVerifier hostnameVerifier) throws IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        StringBuilder builder = new StringBuilder(urlStr);
        if (!urlStr.contains("?"))
            builder.append("?");
        if (builder.toString().contains("="))
            builder.append("&");
        builder.append("trace_id").append("=").append(urlEncode(traceId));

        if (headers == null)
            headers = new LinkedHashMap<String, String>();
        headers.put("trace_id", traceId);
        headers.put("portal", portal);

        URL url = new URL(builder.toString());
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        for (Map.Entry<String, String> header:headers.entrySet())
            requestBuilder.addHeader(header.getKey(), header.getValue());

        String contentType = headers.containsKey("Content-Type") ? headers.get("Content-Type") : "application/octet-stream";
        RequestBody requestBody = RequestBody.create(MediaType.parse(contentType), buffer);
        requestBuilder.post(requestBody);

        OkHttpClient client = getApiOkHttpClient(connectTimeout, readTimeout, trustManager, keyManagerCreator, hostnameVerifier);
        try {
            Response response = client.newCall(requestBuilder.build()).execute();
            return new UrlResponse(response);
        } catch (Error error) {
            throw new IOException("client execute throw error! " + error.getClass() + error.getMessage());
        }
    }

    public static UrlResponse okPostMultiPartyData(String portal, String urlStr, Map<String, String> headers, List<Pair<String, Object>> params, int connectTimeout, int readTimeout) throws IOException {
        return okPostMultiPartyData(portal, urlStr, headers, params, connectTimeout, readTimeout, null, null, null);
    }

    public static UrlResponse okPostMultiPartyData(String portal, String urlStr, Map<String, String> headers, List<Pair<String, Object>> params, int connectTimeout, int readTimeout, X509TrustManager trustManager, KeyManagerCreator keyManagerCreator, HostnameVerifier hostnameVerifier) throws IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        StringBuilder builder = new StringBuilder(urlStr);
        if (!urlStr.contains("?"))
            builder.append("?");
        if (builder.toString().contains("="))
            builder.append("&");
        builder.append("trace_id").append("=").append(urlEncode(traceId));

        if (headers == null)
            headers = new LinkedHashMap<String, String>();
        headers.put("trace_id", traceId);
        headers.put("portal", portal);

        URL url = new URL(builder.toString());
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        for (Map.Entry<String, String> header:headers.entrySet())
            requestBuilder.addHeader(header.getKey(), header.getValue());
        MultipartBody.Builder multiPartyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        for (Pair<String, Object> param : params) {
            String key = param.first;
            Object value = param.second;
            if (value instanceof File)
                multiPartyBuilder.addFormDataPart(key, ((File)value).getName(), RequestBody.create(MediaType.parse("application/octet-stream"), (File)value));
            else if (value instanceof byte[])
                multiPartyBuilder.addFormDataPart(key, String.valueOf(System.currentTimeMillis()), RequestBody.create(MediaType.parse("application/octet-stream"), (byte[]) value));
            else
                multiPartyBuilder.addFormDataPart(key, String.valueOf(value));
        }
        requestBuilder.post(multiPartyBuilder.build());

        OkHttpClient client = getApiOkHttpClient(connectTimeout, readTimeout, trustManager, keyManagerCreator, hostnameVerifier);
        Response response = client.newCall(requestBuilder.build()).execute();
        return new UrlResponse(response);
    }

    private static OkHttpClient getApiOkHttpClient(int connectTimeout, int readTimeout, X509TrustManager trustManager, KeyManagerCreator keyManagerCreator, HostnameVerifier hostnameVerifier) {
        OkHttpClient.Builder clientBuilder = OkHttpClientFactory.obtainApiClient().newBuilder();
        clientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).writeTimeout(readTimeout, TimeUnit.MILLISECONDS).readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        if (trustManager != null)
            clientBuilder.sslSocketFactory(new SSLSocketFactoryCompat(trustManager, keyManagerCreator), trustManager)
                    .hostnameVerifier(SslHostnameVerifier.INSTANCE);
        else if (hostnameVerifier != null) {
            clientBuilder.hostnameVerifier(hostnameVerifier);
        }
        return clientBuilder.build();
    }

    public static UrlResponse postData(String urlStr, Map<String, String> headers, byte[] buffer, int connectTimeout, int readTimeout) throws IOException {
        UrlResponse response = null;

        Logger.d(TAG, "post url -> " + urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        OutputStream out = null;
        try {
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setChunkedStreamingMode(0);
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet())
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (buffer != null) {
                out = new BufferedOutputStream(conn.getOutputStream());
                out.write(buffer);
                out.flush();
            }
            response = new UrlResponse(conn);
        } finally {
            Utils.close(out);
            conn.disconnect();
        }

        Logger.d(TAG, "response" + response.getContent());
        return response;
    }

    public static Map<String, String> parseUrl(String url) {
        Assert.notNEWS(url);
        int index = url.indexOf('?');
        String paramsString = null;
        if (index >= 0) {
            paramsString = url.substring(index + 1);
        } else {
            return null;
        }
        if (TextUtils.isEmpty(paramsString))
            return null;
        String[] params = paramsString.split("&");
        if (params.length == 0)
            return null;
        Map<String, String> paramsMap = new HashMap<String, String>();
        String preKey = null;
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            String[] values = param.split("=");
            if (values.length != 2) {
                if (preKey != null) {
                    String preValue = paramsMap.get(preKey);
                    paramsMap.put(preKey, preValue + "&" + param);
                }
                continue;
            }
            try {
                values[1] = URLDecoder.decode(values[1], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Logger.w(TAG, "param decode failed, " + e.getMessage());
            } catch (Exception e){
                Logger.w(TAG, "param decode failed, " + e.getMessage());
            }
            paramsMap.put(values[0], values[1]);
            preKey = values[0];
        }
        return paramsMap;
    }

    public static UrlResponse postJSON(String urlStr, byte[] buffer, int connectTimeout, int readTimeout) throws IOException {
        UrlResponse response = null;

        StringBuilder builder = new StringBuilder(urlStr);

        OutputStream out = null;
        Logger.d(TAG, "post buffer url -> " + builder.toString());
        URL url = new URL(builder.toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept-Charset", "UTF-8");

            if (buffer != null) {
                out = new BufferedOutputStream(conn.getOutputStream());
                out.write(buffer);
                out.flush();
            }

            response = new UrlResponse(conn);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
            conn.disconnect();
        }

        return response;
    }

    public static UrlResponse postMultipart(String urlStr, Map<String, Object> params, int connectTimeout, int readTimeout) throws IOException {
        final String RN = "\r\n";

        OutputStream out = null;
        Writer writer = null;
        UrlResponse response = null;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            String boundary = "----Java" + "ZnGpCtePMx0KrHw_G0Xl9Yefer8JZlRJSXe";

            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setInstanceFollowRedirects(true);

            if (params != null && params.size() > 0) {
                StringBuilder builder = new StringBuilder();
                out = conn.getOutputStream();
                writer = new OutputStreamWriter(out, "UTF-8");
                boundary = "--" + boundary;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    writer.append(boundary).append(RN);
                    if (value instanceof SFile) {
                        SFile file = (SFile)value;
                        writer.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"; filename=\"")
                                .append(file.getName()).append("\"").append(RN);
                        writer.append("Content-Type: application/octet-stream").append(RN).append(RN);

                        builder.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"; filename=\"")
                                .append(file.getName()).append("\"").append(RN);
                        builder.append("Content-Type: image/").append(FileUtils.getExtension(file.getName()))
                                .append(RN).append(RN);
                        builder.append("[FILE]");

                        writer.flush();
                        StreamUtils.writeFileToStream(file, out);
                        out.flush();
                    } else {
                        // String string = urlEncode(value.toString());
                        String string = value.toString();
                        writer.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"").append(RN).append(RN);
                        writer.append(string);

                        builder.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"").append(RN).append(RN);
                        builder.append(string);
                    }

                    writer.write(RN);
                    builder.append(RN);
                }
                writer.append(boundary).append("--").append(RN);
                writer.flush();

                builder.append(boundary).append("--").append(RN);
                Logger.d(TAG, builder.toString());
            }

            response = new UrlResponse(conn);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Logger.e(TAG, "", e);
                }
            }
            conn.disconnect();
        }

        return response;
    }

    public static UrlResponse postMultipartWithImage(String urlStr, Map<String, Object> params, int connectTimeout, int readTimeout) throws IOException {
        final String RN = "\r\n";

        OutputStream out = null;
        Writer writer = null;
        UrlResponse response = null;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            String boundary = "----Java" + "ZnGpCtePMx0KrHw_G0Xl9Yefer8JZlRJSXe";

            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setInstanceFollowRedirects(true);

            if (params != null && params.size() > 0) {
                StringBuilder builder = new StringBuilder();
                out = conn.getOutputStream();
                writer = new OutputStreamWriter(out, "UTF-8");
                boundary = "--" + boundary;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    writer.append(boundary).append(RN);
                    if (value instanceof SFile) {
                        SFile file = (SFile)value;
                        writer.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"; filename=\"")
                                .append(file.getName()).append("\"").append(RN);

                        String fileExtension = FileUtils.getExtension(file.getName());
                        if(fileExtension.equals("jpeg") || fileExtension.equals("png") || fileExtension.equals("jpg") || fileExtension.equals("bmp")) {
                            writer.append("Content-Type: image/").append(FileUtils.getExtension(file.getName())).append(RN).append(RN);
                        } else {
                            writer.append("Content-Type: application/octet-stream").append(RN).append(RN);
                        }

                        builder.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"; filename=\"")
                                .append(file.getName()).append("\"").append(RN);
                        if(fileExtension.equals("jpeg") || fileExtension.equals("png") || fileExtension.equals("jpg") || fileExtension.equals("bmp")) {
                            builder.append("Content-Type: image/").append(FileUtils.getExtension(file.getName())).append(RN).append(RN);
                        } else {
                            builder.append("Content-Type: application/octet-stream").append(RN).append(RN);
                        }
                        builder.append("[FILE]");

                        writer.flush();
                        StreamUtils.writeFileToStream(file, out);
                        out.flush();
                    } else {
                        // String string = urlEncode(value.toString());
                        String string = value.toString();
                        writer.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"").append(RN).append(RN);
                        writer.append(string);

                        builder.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"").append(RN).append(RN);
                        builder.append(string);
                    }

                    writer.write(RN);
                    builder.append(RN);
                }
                writer.append(boundary).append("--").append(RN);
                writer.flush();

                builder.append(boundary).append("--").append(RN);
                Logger.d(TAG, builder.toString());
            }

            response = new UrlResponse(conn);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Logger.e(TAG, "", e);
                }
            }
            conn.disconnect();
        }

        return response;
    }

    public static UrlResponse postMultipartWithTrunk(String urlStr, Map<String, Object> params, int connectTimeout, int readTimeout) throws IOException {
        final String RN = "\r\n";

        OutputStream out = null;
        Writer writer = null;
        UrlResponse response = null;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            String boundary = "----Java" + "ZnGpCtePMx0KrHw_G0Xl9Yefer8JZlRJSXe";

            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setChunkedStreamingMode(64 * 1024);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setInstanceFollowRedirects(true);

            if (params != null && params.size() > 0) {
                StringBuilder builder = new StringBuilder();
                out = conn.getOutputStream();
                writer = new OutputStreamWriter(out, "UTF-8");
                boundary = "--" + boundary;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    writer.append(boundary).append(RN);
                    if (value instanceof SFile) {
                        SFile file = (SFile)value;
                        writer.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"; filename=\"")
                                .append(file.getName()).append("\"").append(RN);
                        writer.append("Content-Type: application/octet-stream").append(RN).append(RN);

                        builder.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"; filename=\"")
                                .append(file.getName()).append("\"").append(RN);
                        builder.append("Content-Type: image/").append(FileUtils.getExtension(file.getName()))
                                .append(RN).append(RN);
                        builder.append("[FILE]");

                        writer.flush();
                        StreamUtils.writeFileToStream(file, out);
                        out.flush();
                    } else {
                        // String string = urlEncode(value.toString());
                        String string = value.toString();
                        writer.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"").append(RN).append(RN);
                        writer.append(string);

                        builder.append("Content-Disposition: form-data; name=\"")
                                .append(name).append("\"").append(RN).append(RN);
                        builder.append(string);
                    }

                    writer.write(RN);
                    builder.append(RN);
                }
                writer.append(boundary).append("--").append(RN);
                writer.flush();

                builder.append(boundary).append("--").append(RN);
                Logger.d(TAG, builder.toString());
            }

            response = new UrlResponse(conn);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Logger.e(TAG, "", e);
                }
            }
            conn.disconnect();
        }

        return response;
    }

    public static final int CONNECT_TIMEOUT = 10 * 1000;
    public static final int SO_TIMEOUT = 10 * 1000;

    public static UrlResponse doRetryPostJSON(String portal, String urlStr, byte[] buffer, int maxRetryCount) throws IOException {
        return doRetryPostJSON(portal, urlStr, buffer, maxRetryCount, CONNECT_TIMEOUT, SO_TIMEOUT);
    }

    public static UrlResponse doRetryPostJSON(String portal, String urlStr, byte[] buffer, int maxRetryCount, int connectTimeOut, int soTimeout) throws IOException {
        return doRetryPostJSON(portal, urlStr, buffer, maxRetryCount, connectTimeOut, soTimeout, connectTimeOut, soTimeout);
    }

    public static UrlResponse doRetryPostJSON(String portal, String urlStr, byte[] buffer, int maxRetryCount, int connectTimeOut, int soTimeout, int reConnectTimeOut, int reSoTimeout) throws IOException {
        int retryCount = 0;
        IOException ioe = new IOException();
        while (retryCount < maxRetryCount) {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Charset", "UTF-8");
                UrlResponse response;
                if (retryCount == 0)
                    response = HttpUtils.okPostData(portal, urlStr, headers, buffer, connectTimeOut, soTimeout);
                else
                    response = HttpUtils.okPostData(portal, urlStr, headers, buffer, reConnectTimeOut, reSoTimeout);
                collectRequestResult(ObjectStore.getContext(), "success", urlStr, System.currentTimeMillis() - startTime, null);
                return response;
            } catch (IOException e) {
                retryCount++;
                ioe = e;
                Logger.e(TAG, "doRetryPostJSON(): URL: " + urlStr + ", Retry count:" + retryCount + " and exception:" + e.toString());
                collectRequestResult(ObjectStore.getContext(), "failed", urlStr, System.currentTimeMillis() - startTime, e.toString());
            }
        }
        throw ioe;
    }

    public static UrlResponse doRetryPost(String urlStr, Map<String, String> params, int maxRetryCount) throws IOException {
        int retryCount = 0;
        IOException ioe = new IOException();
        while (retryCount < maxRetryCount) {
            long startTime = System.currentTimeMillis();
            try {
                UrlResponse response = HttpUtils.post(urlStr, params, CONNECT_TIMEOUT, SO_TIMEOUT);
                collectRequestResult(ObjectStore.getContext(), "success", urlStr, System.currentTimeMillis() - startTime, null);
                return response;
            } catch (IOException e) {
                retryCount++;
                ioe = e;
                Logger.e(TAG, "doRetryPost(): URL: " + urlStr + ", Retry count:" + retryCount + " and exception:" + e.toString());
                collectRequestResult(ObjectStore.getContext(), "failed", urlStr, System.currentTimeMillis() - startTime, e.toString());
            }
        }
        throw ioe;
    }

    public static UrlResponse doRetryPostMultipart(String urlStr, Map<String, Object> params, int maxRetryCount) throws IOException {
        int retryCount = 0;
        IOException ioe = new IOException();
        while (retryCount < maxRetryCount) {
            long startTime = System.currentTimeMillis();
            try {
                UrlResponse response = HttpUtils.postMultipart(urlStr, params, CONNECT_TIMEOUT, SO_TIMEOUT);
                collectRequestResult(ObjectStore.getContext(), "success", urlStr, System.currentTimeMillis() - startTime, null);
                return response;
            } catch (IOException e) {
                retryCount++;
                ioe = e;
                Logger.e(TAG, "doRetryPostMultipart(): URL: " + urlStr + ", Retry count:" + retryCount + " and exception:" + e.toString());
                collectRequestResult(ObjectStore.getContext(), "failed", urlStr, System.currentTimeMillis() - startTime, e.toString());
            }
        }
        throw ioe;
    }

    //TODO liufs: need collect?
    public static void collectRequestResult(Context context, String result, String url, long duration, String error) {
        if (context == null)
            return;

        try {
            if (!Stats.isRandomCollect(1000))
                return;

            HashMap<String, String> map = new LinkedHashMap<String, String>();
            map.put("result", result);
            map.put("url", NetUtils.getUrlNoQuery(url));
            map.put("duration", duration + "");
            map.put("error", error);

            Stats.onEvent(context, SEN_CLOUD_REQUEST_RESULT, map);
        } catch (Exception e) {}
    }
}
