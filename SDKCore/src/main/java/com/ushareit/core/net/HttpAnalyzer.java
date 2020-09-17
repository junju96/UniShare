package com.ushareit.core.net;


import android.os.SystemClock;
import android.text.TextUtils;

import com.ushareit.app.CommonActivityLifecycle;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.stats.Stats;
import com.ushareit.core.utils.device.DeviceHelper;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class HttpAnalyzer {
    private static final String TAG = "HttpAnalyzer";
    public static final String KEY_CFG_HTTP_STATS_RATE = "http_stats_rate_denom";

    enum HttpStep {
        Init("init"), DNSStart("dns_start"), DNSEnd("dns_end"), ConnectStart("connect_start"), ConnectSStart("connect_s_start"), ConnectSEnd("connect_s_end"), ConnectEnd("connect_end"), ConnectAcquire("connect_acq"),
        SendHeaderStart("send_header_start"), SendHeaderEnd("send_header_end"), SendBodyStart("send_body_start"), SendBodyEnd("send_body_end"), RecvHeaderStart("recv_header_start"), RecvHeaderEnd("recv_header_end"),
        RecvBodyStart("recv_body_start"), RecvBodyEnd("recv_body_end"), Success("success");

        private String mValue;
        HttpStep(String value) {
            mValue = value;
        }
        private final static Map<String, HttpStep> VALUES = new HashMap<String, HttpStep>();
        static {
            for (HttpStep item : HttpStep.values())
                VALUES.put(item.mValue, item);
        }

        public static HttpStep fromString(String value) {
            return VALUES.containsKey(value) ? VALUES.get(value.toLowerCase()) : Init;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    private String mTraceId;
    private String mPortal;

    private final String mUrl, mMethod;
    private HttpStep mCurrentStep;
    private String mIpAddr;
    private String mZipEncoding;
    private int mHttpCode;
    private long mContentLength, mReadBytes, mWriteBytes, mDuration, mFirstRecvDuration, mDnsDuration, mConnectDuration, mSendDuration, mRecvDuration, mRespDuration;

    private long mStartTime, mStepStartTime;
    private String mCacheHit;
    private int mRedirectCount;
    private List<String> mRedirectUrls = Collections.synchronizedList(new ArrayList<String>());
    private AtomicBoolean mCompleted = new AtomicBoolean(false);

    private static class SystemProperties {
        private static boolean isReflectInited = false;
        private static Method getPropertyMethod = null;

        public static void init() {
            if (!isReflectInited) {
                isReflectInited = true;
                try {
                    Class<?> cls = Class.forName("android.os.SystemProperties");
                    getPropertyMethod = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
                    getPropertyMethod.setAccessible(true);
                } catch (Throwable throwable) {
                }
            }
        }
        public static String get(String property,String defaultValue) {
            String propertyValue = defaultValue;
            if (getPropertyMethod != null) {
                try {
                    propertyValue = (String) getPropertyMethod.invoke(null,property,defaultValue);
                } catch (Throwable throwable) {
                }
            }
            return propertyValue;
        }

    }

    HttpAnalyzer(String traceId, String url, String portal, String method) {
        mTraceId = traceId;
        mUrl = url;
        mMethod = method;
        mPortal = portal;
        mCurrentStep = HttpStep.Init;

        Logger.v(TAG, "Http request("+ method + "):" + mUrl);
    }

    private static final String[] DNS_SERVER_PROPERTIES = new String[]{"net.dns1",
            "net.dns2","net.dns3","net.dns4"};

    public static String[] readDnsServersFromSystemProperties() {
        SystemProperties.init();
        String[] dnsServers = new String[4];
        int i = 0;
        for (String property: DNS_SERVER_PROPERTIES) {
            String server = SystemProperties.get(property,"");
            if (server != null && !server.isEmpty() && i < 4) {
                /* just need the string, no matter whether it is ip or host name.
                try {
                    InetAddress ip = InetAddress.getByName(server);
                    if (ip == null) continue;
                    server = ip.getHostAddress();
                    if (server == null || server.isEmpty()) {
                        continue;
                    }
                } catch (Throwable throwable) {
                    continue;
                }
                */
                dnsServers[i++] = server;

            }
        }
        return dnsServers;
    }

    public String getTraceId() {
        return mTraceId;
    }

    public void traceStart() {
        Logger.v(TAG, "trace Start, id:" + mTraceId);
        mStartTime = SystemClock.elapsedRealtime();
        mStepStartTime = mStartTime;
    }

    public void traceDnsStart(String domainName) {
        Logger.v(TAG, "traceDnsStart, id:" + mTraceId);
        mCurrentStep = HttpStep.DNSStart;
        mStepStartTime = SystemClock.elapsedRealtime();
    }

    public void traceDnsStop() {
        Logger.v(TAG, "traceDnsStop, id:" + mTraceId);
        long current = SystemClock.elapsedRealtime();
        mCurrentStep = HttpStep.DNSEnd;
        mDnsDuration = current - mStepStartTime;
        mStepStartTime = current;
    }

    public void traceConnectStart(String ipAddress) {
        mCurrentStep = HttpStep.ConnectStart;
        mIpAddr = ipAddress;
        Logger.v(TAG, "trace connect start, id:" + mTraceId + ", ip:" + mIpAddr);
        mStepStartTime = SystemClock.elapsedRealtime();
    }

    public void traceConnectSStart() {
        Logger.v(TAG, "traceConnectSStart, id:" + mTraceId);
        mCurrentStep = HttpStep.ConnectSStart;
    }

    public void traceConnectSEnd() {
        Logger.v(TAG, "traceConnectSEnd, id:" + mTraceId);
        mCurrentStep = HttpStep.ConnectSEnd;
    }

    public void traceConnectEnd() {
        Logger.v(TAG, "traceConnectEnd, id:" + mTraceId);
        mCurrentStep = HttpStep.ConnectEnd;
        long current = SystemClock.elapsedRealtime();
        mConnectDuration = current - mStepStartTime;
        mStepStartTime = SystemClock.elapsedRealtime();
    }

    public void traceConnectFailed() {
        Logger.v(TAG, "traceConnectFailed, id:" + mTraceId);
        long current = SystemClock.elapsedRealtime();
        mConnectDuration = current - mStepStartTime;
        mStepStartTime = current;
    }

    public void traceConnectAcquired() {
        Logger.v(TAG, "traceConnectAcquired, id:" + mTraceId);
        mCurrentStep = HttpStep.ConnectAcquire;
        mStepStartTime = SystemClock.elapsedRealtime();
    }

    public void traceSendHeaderStart() {
        Logger.v(TAG, "traceSendHeaderStart, id:" + mTraceId);
        mCurrentStep = HttpStep.SendHeaderStart;
        mStepStartTime = SystemClock.elapsedRealtime();
    }

    public void traceSendHeaderEnd() {
        Logger.v(TAG, "traceSendHeaderEnd, id:" + mTraceId);
        mCurrentStep = HttpStep.SendHeaderEnd;
        //there maybe not exist body to send
        mSendDuration = SystemClock.elapsedRealtime() - mStepStartTime;
    }

    public void traceSendBodyStart() {
        Logger.v(TAG, "traceSendBodyStart, id:" + mTraceId);
        mCurrentStep = HttpStep.SendBodyStart;
    }

    public void traceSendBodyEnd(long writeBytes) {
        Logger.v(TAG, "traceSendBodyEnd, id:" + mTraceId);
        mCurrentStep = HttpStep.SendBodyEnd;
        mWriteBytes = writeBytes;
        mSendDuration = SystemClock.elapsedRealtime() - mStepStartTime;
    }

    public void traceRecvHeaderStart() {
        Logger.v(TAG, "traceRecvHeaderStart, id:" + mTraceId);
        mCurrentStep = HttpStep.RecvHeaderStart;
        mStepStartTime = SystemClock.elapsedRealtime();
    }

    public void traceRecvHeaderEnd(int httpCode, long contentLength, String cacheHit, String zipEncoding) {
        Logger.v(TAG, "response header end, id:" + mTraceId + ", code:" + httpCode);
        mCurrentStep = HttpStep.RecvHeaderEnd;
        mHttpCode = httpCode;
        mContentLength = contentLength;
        mCacheHit = cacheHit;
        mZipEncoding = zipEncoding;
        long current = SystemClock.elapsedRealtime();
        mFirstRecvDuration = current - mStartTime;
        mRecvDuration = current - mStepStartTime;
        mRespDuration = current - mStepStartTime;

        if (mHttpCode < 200 || mHttpCode >= 300)
            traceEnd(null);
    }

    public void traceRecvBodyStart() {
        Logger.v(TAG, "traceRecvBodyStart, id:" + mTraceId);
        mCurrentStep = HttpStep.RecvBodyStart;
    }

    public void traceRecvBodyEnd(long readBytes) {
        Logger.v(TAG, "traceRecvBodyEnd, id:" + mTraceId);
        mReadBytes = readBytes;
        mCurrentStep = HttpStep.RecvBodyEnd;
        mRecvDuration = SystemClock.elapsedRealtime() - mStepStartTime;
    }

    public void traceRevRedirect(int httpCode, String location) {
        Logger.v(TAG, "traceRevRedirect, id:" + mTraceId + ", httpCode:" + httpCode + ",location:" + location);
        mRedirectCount++;
        if (mRedirectCount > 10)
            return;
        try {
            mRedirectUrls.add(location);
        } catch (Exception e) {}
    }

    public void traceEnd(Exception exception) {
        if (TextUtils.isEmpty(mTraceId) || !mCompleted.compareAndSet(false, true)) {
            Logger.d(TAG, "trace id is null or stats has completed!");
            return;
        }

        Logger.v(TAG, "trace END, id:" + mTraceId);

        mDuration = SystemClock.elapsedRealtime() - mStartTime;
        boolean success = (mHttpCode >= 200 && mHttpCode < 300) && (exception == null);
        if (success)
            mCurrentStep = HttpStep.Success;
        String errMsg = success ? null : ("http status:" + mHttpCode + ((exception != null) ? ", " + (TextUtils.isEmpty(exception.getMessage()) ? "no message" : exception.getMessage()) : ""));

        try {
            int paramPos = mUrl.indexOf("?");
            URL url = new URL(mUrl);
            String paramUrl = (mUrl.substring(0, (paramPos < 0) ? mUrl.length() : paramPos)) + "(" + mMethod + ")";
            String paramHost = url.getHost();
            String path = url.getPath(); String suffix = FileUtils.getExtension(path);
            String paramPath = TextUtils.isEmpty(suffix) ? path : "*." + suffix;

            boolean isDirectUrl = mUrl.contains("googlevideo.com");

            int rateDenominator = CloudConfig.getIntConfig(ObjectStore.getContext(), KEY_CFG_HTTP_STATS_RATE, 10);
            if (!paramPath.equals("*.m3u8") && !paramPath.equals("*.mpd") && !shouldCollect() && !Stats.isRandomCollect(rateDenominator) && !isDirectUrl)
                return;

            String paramNet = NetworkStatus.getNetworkStatusEx(ObjectStore.getContext()).getNetTypeDetailForStats();

            HashMap<String, String> paramsDetail = new LinkedHashMap<String, String>();
            paramsDetail.put("url", isDirectUrl ? mUrl : paramUrl);
            paramsDetail.put("host", paramHost);
            paramsDetail.put("path", paramPath);
            paramsDetail.put("network", paramNet);
            paramsDetail.put("result", mCurrentStep.toString());
            paramsDetail.put("total_duration", String.valueOf(mDuration));
            paramsDetail.put("first_recv_duration", String.valueOf(mFirstRecvDuration));
            paramsDetail.put("content_length", String.valueOf(mContentLength));
            paramsDetail.put("error_code", String.valueOf(mHttpCode));
            paramsDetail.put("error_msg", errMsg);

            paramsDetail.put("dns_duration", String.valueOf(mDnsDuration));
            paramsDetail.put("connect_duration", String.valueOf(mConnectDuration));
            paramsDetail.put("send_duration", String.valueOf(mSendDuration));
            paramsDetail.put("recv_duration", String.valueOf(mRecvDuration));
            paramsDetail.put("resp_duration", String.valueOf(mRespDuration));
            paramsDetail.put("read_bytes", String.valueOf(mReadBytes));
            paramsDetail.put("cdn_cache", mCacheHit);
            paramsDetail.put("redirect_count", String.valueOf(mRedirectCount));
            paramsDetail.put("write_bytes", String.valueOf(mWriteBytes));

            if( mIpAddr != null && !mIpAddr.equals("")  &&
                    ( paramPath.equals("*.mpd")||
                            paramPath.equals("*.m3u8") || isDirectUrl) && ObjectStore.get(url.toString()) == null){
                ObjectStore.add("serveraddr_"+url.toString(), mIpAddr);
            }

            float downloadSpeed = (mReadBytes == 0 || mRecvDuration == 0) ? 0 : (1.0f * mReadBytes / 1000 / (1.0f * mRecvDuration / 1000)); //KB/s
            long realSendDuration = mSendDuration + mRespDuration; // maybe some date stored in buffer, so this duration = send duration + resp header, the upload speed is slower than real speed.
            float uploadSpeed = (mWriteBytes == 0 || realSendDuration == 0) ? 0 : (1.0f * mWriteBytes / 1000 / (1.0f * realSendDuration / 1000)); //KB/s
            paramsDetail.put("download_speed", String.valueOf(downloadSpeed));
            paramsDetail.put("upload_speed", String.valueOf(uploadSpeed));
            Ping.EvaluateDetail result = Ping.getLastEvaluateDetail();
            if (result != null) {
                paramsDetail.put("ping_average_time", String.valueOf(result.roundTrip));
                paramsDetail.put("ping_rev_pac_percent", String.valueOf(result.revcPercent));
                paramsDetail.put("ping_net_result", result.pinNetResult != null ? result.pinNetResult.name() : "UNKnown");
            }
            EchoServerHelper.Result echoServerResult = EchoServerHelper.getLastResult();
            paramsDetail.put("connect_test_result", echoServerResult != null ? (echoServerResult.result ? "success" : "fail") : "None");
            JSONObject extraJson = new JSONObject();
            extraJson.put("ping_msg", result == null ? "null" : result.pingResultDesc);
            extraJson.put("app_status", CommonActivityLifecycle.isAppInBackground() ? "background" : "foreground");
            extraJson.put("connect_test_duration", echoServerResult != null ? echoServerResult.duration : -1);
            extraJson.put("connect_test_by", echoServerResult != null ? echoServerResult.requestBy : "None");
            extraJson.put("connect_timestamp", echoServerResult != null ? echoServerResult.timeStamp : -1);
            extraJson.put("si_x_content_encoding",  mZipEncoding);
            extraJson.put("trace_id", mTraceId);
            extraJson.put("portal", mPortal);
            extraJson.put("ipaddr", mIpAddr);
            extraJson.put("redirect_urls", mRedirectUrls.toString());
            extraJson.put("imsi", DeviceHelper.getIMSI(ObjectStore.getContext()));

            String dns_server = "";
            try {
                String[] dnsServers = readDnsServersFromSystemProperties();
                for(int i = 0; i < dnsServers.length && i<4 && dnsServers[i] != null && !dnsServers[i].equals(""); i++){
                    if( i != 0) dns_server += ",";
                    dns_server += dnsServers[i];
                }
            } catch (Throwable throwable) {

            }
            extraJson.put("dns_server", dns_server);
            paramsDetail.put("extra", extraJson.toString());
            Logger.v(TAG, "Net_HttpConnectDetail:" + paramsDetail.toString());
            Stats.onEvent(ObjectStore.getContext(), "Net_HttpConnectDetail", paramsDetail);
        } catch (Exception e) {}
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpAnalyzer that = (HttpAnalyzer) o;

        return mTraceId.equals(that.mTraceId);
    }

    @Override
    public int hashCode() {
        return mTraceId.hashCode();
    }

    private boolean shouldCollect() {
        return !TextUtils.isEmpty(mUrl) && mUrl.contains("/feedback/upload");
    }
}
