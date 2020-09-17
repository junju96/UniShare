package com.ushareit.core.net;

import android.text.TextUtils;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.algo.br.BrotliInputStream;
import com.ushareit.core.lang.ObjectStore;


import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;
import okio.Source;

public class OkHttpClientFactory {

    private static final String CONFIG_CONN_POOL_SIZE = "conn_pool_size";
    private static final int DEFAULT_CONN_TIMEOUT = 15;
    private static final int DEFAULT_WRITE_TIMEOUT = 15;
    private static final int DEFAULT_READ_TIMEOUT = 15;

    private static OkHttpClient sApiClient;
    private static OkHttpClient sExoClient;
    private static OkHttpClient sCDNClient;
    private static OkHttpClient sDownloadClient;
    private static OkHttpClient sDownloadCookieClient;
    private static OkHttpClient sVideoBrowserClient;
    private static int sConnPoolSize = 5;
    static {
        sConnPoolSize = CloudConfig.getIntConfig(ObjectStore.getContext(), CONFIG_CONN_POOL_SIZE, -1);
    }

    public static OkHttpClient obtainGlideClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .eventListener(new OkEventListenerStats())
                .build();
    }

    public static OkHttpClient obtainApiClient() {
        if (sApiClient != null)
            return sApiClient;
        synchronized (OkHttpClientFactory.class) {
            if (sApiClient == null) {
                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(false)
                        .eventListener(new OkEventListenerStats());

                if (CloudConfig.getBooleanConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_API_SUPPORT_BR, true))
                    builder.addInterceptor(new BrotliResponseInterceptor());

                if (sConnPoolSize > 0)
                    builder.connectionPool(new ConnectionPool(sConnPoolSize, 5, TimeUnit.MINUTES));

                sApiClient = builder.build();

            }
        }
        return sApiClient;
    }

    public static OkHttpClient createClient(OkHttpClient.Builder builder) {
        if (builder == null)
            builder = new OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS);

        return builder.eventListener(new OkEventListenerStats()).build();
    }

    /**
     * direct parser client for direct url head request
     */
    public static OkHttpClient obtainCDNClient() {
        if (sCDNClient != null) {
            return sCDNClient;
        }
        sCDNClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
        return sCDNClient;
    }

    public static OkHttpClient obtainExoClient() {
        if (sExoClient != null)
            return sExoClient;

        synchronized (OkHttpClientFactory.class) {
            if (sExoClient == null) {
                sExoClient = new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .eventListener(new OkDownloadStatsEventListener())
                        .cookieJar(new CookieJar() {
                            private final HashMap<String, HashMap<String, Cookie>> cookieStore = new HashMap<String, HashMap<String, Cookie>>();

                            @Override
                            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                                HashMap<String, Cookie> map = cookieStore.get(url.host());
                                if (map == null)
                                    map = new HashMap<String, Cookie>();
                                for (Cookie cookie : cookies) {
                                    map.put(cookie.name(), cookie);
                                }
                                cookieStore.put(url.host(), map);
                            }

                            @Override
                            public List<Cookie> loadForRequest(HttpUrl url) {
                                HashMap<String, Cookie> cookies = cookieStore.get(url.host());
                                return cookies != null ? new ArrayList<Cookie>(cookies.values()) : new ArrayList<Cookie>();
                            }
                        })
                        .build();
            }
        }
        return sExoClient;
    }

    public static OkHttpClient obtainDownloadClient() {
        if (sDownloadClient != null)
            return sDownloadClient;

        synchronized (OkHttpClientFactory.class) {
            if (sDownloadClient == null) {
                sDownloadClient = new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(false)
                        .protocols(Arrays.asList(new Protocol[]{Protocol.HTTP_1_1}))
                        .eventListener(new OkDownloadStatsEventListener())
                        .build();
            }
        }
        return sDownloadClient;
    }

    public static OkHttpClient obtainDownloadClientWithCookie() {
        if (sDownloadCookieClient != null) {
            return sDownloadCookieClient;
        }

        synchronized (OkHttpClientFactory.class) {
            if (sDownloadCookieClient == null) {
                CookieManager cookieManager = new CookieManager();
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
                sDownloadCookieClient = new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .cookieJar(new JavaNetCookieJar(cookieManager))
                        .retryOnConnectionFailure(false)
                        .protocols(Arrays.asList(new Protocol[]{Protocol.HTTP_1_1}))
                        .eventListener(new OkDownloadStatsEventListener())
                        .build();
            }
        }
        return sDownloadCookieClient;
    }

    public static OkHttpClient obtainVideoBrowserClient() {
        if (sVideoBrowserClient != null)
            return sVideoBrowserClient;

        synchronized (OkHttpClientFactory.class) {
            if (sVideoBrowserClient == null) {
                sVideoBrowserClient = new OkHttpClient.Builder()
                        .sslSocketFactory(createSSLSocketFactory(), new TrustAllCerts())
                        .hostnameVerifier(new TrustAllHostnameVerifier())
                        .connectTimeout(DEFAULT_CONN_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(false)
                        .build();
            }
        }
        return sVideoBrowserClient;
    }

    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
        }
        return ssfFactory;
    }

    private static class TrustAllCerts implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static class BrotliResponseInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request userRequest = chain.request();
            Request.Builder requestBuilder = userRequest.newBuilder();

            // If we add an "Accept-Encoding: br" header field we're responsible for also decompressing the transfer stream.
            if (userRequest.header("Accept-Encoding") == null)
                requestBuilder.addHeader("Accept-Encoding", "gzip,br");

            Response networkResponse = chain.proceed(requestBuilder.build());
            String encoding = networkResponse.header("Content-Encoding");
            if (TextUtils.isEmpty(encoding) || !HttpHeaders.hasBody(networkResponse))
                return networkResponse;
            Response.Builder responseBuilder = networkResponse.newBuilder()
                    .request(userRequest);
            Headers strippedHeaders = networkResponse.headers().newBuilder()
                    .removeAll("Content-Encoding")
                    .removeAll("Content-Length")
                    .add("SI-X-Content-Encoding", encoding)
                    .build();
            if ("br".equalsIgnoreCase(encoding)) {
                BrotliInputStream brotliInputStream = new BrotliInputStream(networkResponse.body().source().inputStream());
                responseBuilder.headers(strippedHeaders);
                responseBuilder.body(new RealResponseBody(networkResponse.header("Content-Type"), -1L, Okio.buffer(Okio.source(brotliInputStream))));
                return responseBuilder.build();
            } else if ("gzip".equalsIgnoreCase(encoding)) {
                Source source = new GzipSource(networkResponse.body().source());
                responseBuilder.headers(strippedHeaders);
                responseBuilder.body(new RealResponseBody(networkResponse.header("Content-Type"), -1L, Okio.buffer(source)));
                return responseBuilder.build();
            }
            return networkResponse;
        }
    }

}