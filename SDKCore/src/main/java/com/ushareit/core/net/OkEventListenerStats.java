package com.ushareit.core.net;

import android.text.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

class OkEventListenerStats extends EventListener {
    private static final String TAG = "OkEventListenerStats";
    private static final int ERROR_CODE_IOE = -100;

    @Override
    public void callStart(Call call) {
        super.callStart(call);
        HttpAnalyzerHelper.obtain(call, true).traceStart();
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        super.dnsStart(call, domainName);
        HttpAnalyzerHelper.obtain(call).traceDnsStart(domainName);
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        HttpAnalyzerHelper.obtain(call).traceDnsStop();
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        super.connectStart(call, inetSocketAddress, proxy);
        HttpAnalyzerHelper.obtain(call).traceConnectStart(inetSocketAddress.getAddress().getHostAddress());
    }

    @Override
    public void secureConnectStart(Call call) {
        super.secureConnectStart(call);
        HttpAnalyzerHelper.obtain(call).traceConnectSStart();
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        super.secureConnectEnd(call, handshake);
        HttpAnalyzerHelper.obtain(call).traceConnectSEnd();
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol);
        HttpAnalyzerHelper.obtain(call).traceConnectEnd();
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
        HttpAnalyzerHelper.obtain(call).traceConnectFailed();
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        super.connectionAcquired(call, connection);
        HttpAnalyzerHelper.obtain(call).traceConnectAcquired();
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        super.connectionReleased(call, connection);
    }

    @Override
    public void requestHeadersStart(Call call) {
        super.requestHeadersStart(call);
        HttpAnalyzerHelper.obtain(call).traceSendHeaderStart();
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        super.requestHeadersEnd(call, request);
        HttpAnalyzerHelper.obtain(call).traceSendHeaderEnd();
    }

    @Override
    public void requestBodyStart(Call call) {
        super.requestBodyStart(call);
        HttpAnalyzerHelper.obtain(call).traceSendBodyStart();
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        super.requestBodyEnd(call, byteCount);
        HttpAnalyzerHelper.obtain(call).traceSendBodyEnd(byteCount);
    }

    @Override
    public void responseHeadersStart(Call call) {
        super.responseHeadersStart(call);
        HttpAnalyzerHelper.obtain(call).traceRecvHeaderStart();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
        long size = 0;
        String zipEncoding = "unknown";
        StringBuilder cacheHit = null;
        try {
            size = response.header("Content-Length") == null ? 0 : Long.valueOf(response.header("Content-Length"));
            zipEncoding = response.header("Content-Encoding") == null ? "unknown" : response.header("Content-Encoding");
        } catch (Exception e) {
        }

        // cache hit info
        // x-cache
        try {
            String cacheInfo = response.header("X-Cache") != null ? response.header("X-Cache").toLowerCase() : null;
            if (!TextUtils.isEmpty(cacheInfo)) {
                cacheHit = new StringBuilder();
                if (cacheInfo.contains("hit")) cacheHit.append("X-Cache:hit");
                if (cacheInfo.contains("miss")) cacheHit.append("X-Cache:miss");
            }
        } catch (Exception e) {
        }
        // x-cache-remote
        try {
            String cacheInfo = response.header("X-Cache-Remote") != null ? response.header("X-Cache-Remote").toLowerCase() : null;
            if (!TextUtils.isEmpty(cacheInfo)) {
                if (cacheHit == null)
                    cacheHit = new StringBuilder();
                else
                    cacheHit.append(" ");
                if (cacheInfo.contains("hit")) cacheHit.append("X-Cache-Remote:hit");
                if (cacheInfo.contains("miss")) cacheHit.append("X-Cache-Remote:miss");
            }
        } catch (Exception e) {
        }
        if (response.code() == 301 || response.code() == 302) {
            HttpAnalyzerHelper.obtain(call).traceRevRedirect(response.code(),
                    response.header("Location"));
            return;
        }
        HttpAnalyzerHelper.obtain(call).traceRecvHeaderEnd(response.code(), size, cacheHit == null ? null : cacheHit.toString(), zipEncoding);

        if (response.code() < 200 || response.code() >= 300)
            HttpAnalyzerHelper.traceEnd(call, null);
    }

    @Override
    public void responseBodyStart(Call call) {
        super.responseBodyStart(call);
        HttpAnalyzerHelper.obtain(call).traceRecvBodyStart();
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        HttpAnalyzerHelper.obtain(call).traceRecvBodyEnd(byteCount);
    }

    @Override
    public void callEnd(Call call) {
        super.callEnd(call);
        HttpAnalyzerHelper.traceEnd(call, null);
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        super.callFailed(call, ioe);
        HttpAnalyzerHelper.traceEnd(call, ioe);
    }

    private static class HttpAnalyzerHelper {

        private static Map<String, HttpAnalyzer> sAnalyzerPool = new ConcurrentHashMap<String, HttpAnalyzer>();
        private static HttpAnalyzer sEmptyAnalyzer = new HttpAnalyzer("null", "null", "null", "null");

        static HttpAnalyzer obtain(Call call) {
            return obtain(call, false);
        }

        static HttpAnalyzer obtain(Call call, boolean needCreate) {
            try {
                String traceId = call.request().header("trace_id");
                if (sAnalyzerPool.containsKey(traceId))
                    return sAnalyzerPool.get(traceId);
                if (!needCreate)
                    return sEmptyAnalyzer;

                HttpAnalyzer analyzer = new HttpAnalyzer(traceId, call.request().url().toString(), call.request().header("portal"), call.request().method());
                sAnalyzerPool.put(traceId, analyzer);
                return analyzer;
            } catch (Exception e) {
                return sEmptyAnalyzer;
            }
        }

        static void traceEnd(Call call, Exception e) {
            HttpAnalyzer analyzer = obtain(call);
            if (analyzer == sEmptyAnalyzer)
                return;

            analyzer.traceEnd(e);
            releaseAnalyzer(analyzer);
        }

        private static void releaseAnalyzer(HttpAnalyzer analyzer) {
            try {
                if (!sAnalyzerPool.containsValue(analyzer))
                    return;
                sAnalyzerPool.remove(analyzer.getTraceId());
            } catch (Exception e) {
            }
        }

    }
}
