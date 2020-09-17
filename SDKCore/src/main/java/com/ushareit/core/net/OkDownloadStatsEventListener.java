package com.ushareit.core.net;

import android.os.SystemClock;
import android.text.TextUtils;

import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.stats.Stats;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Response;

import static com.ushareit.core.net.HttpAnalyzer.KEY_CFG_HTTP_STATS_RATE;

public class OkDownloadStatsEventListener extends OkEventListenerStats {
    private static final String TAG = "OkHttp.BandwidthAnalyzer";

    @Override
    public void responseBodyStart(Call call) {
        super.responseBodyStart(call);
        DownloadAnalyzer.obtain(call, true).responseBodyStart();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
        DownloadAnalyzer.obtain(call, false).responseHeadersEnd(response);
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        DownloadAnalyzer.obtain(call, false).responseBodyEnd(byteCount);
    }

    @Override
    public void callEnd(Call call) {
        super.callEnd(call);
        DownloadAnalyzer.obtain(call, false).traceEnd(true);
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        super.callFailed(call, ioe);
        DownloadAnalyzer.obtain(call, false).traceEnd(false);
    }

    private static class DownloadAnalyzer {

        private static Map<String, DownloadAnalyzer> sAnalyzerPool = new ConcurrentHashMap<String, DownloadAnalyzer>();
        private static DownloadAnalyzer sEmptyAnalyzer = new DownloadAnalyzer("", "", "");

        static DownloadAnalyzer obtain(Call call, boolean needCreate) {
            try {
                String traceId = call.request().header("trace_id");
                if (sAnalyzerPool.containsKey(traceId))
                    return sAnalyzerPool.get(traceId);
                if (!needCreate)
                    return sEmptyAnalyzer;

                DownloadAnalyzer analyzer = new DownloadAnalyzer(traceId, call.request().url().toString(), call.request().header("portal"));
                sAnalyzerPool.put(traceId, analyzer);
                return analyzer;
            } catch (Exception e) {
                return sEmptyAnalyzer;
            }
        }

        private String mTraceId;
        private String mUrl;
        private String mPortal;
        private long mStartTime;
        private String mCacheHit;
        private HashMap<String, String> mParamsDetail;

        DownloadAnalyzer(String traceId, String url, String portal) {
            mTraceId = traceId;
            mUrl = url;
            mPortal = portal;
            mCacheHit = "null";
        }

        void responseBodyStart() {
            mStartTime = SystemClock.elapsedRealtime();
        }

        void responseHeadersEnd(Response response) {
            try {
                mCacheHit = response.header("X-Cache");
            } catch (Exception e) {
            }
        }

        void responseBodyEnd(long byteCount) {
            if (TextUtils.isEmpty(mTraceId))
                return;


            long timeUsed = SystemClock.elapsedRealtime() - mStartTime;
            try {
                URL url = new URL(mUrl);
                String paramNet = NetworkStatus.getNetworkStatusEx(ObjectStore.getContext()).getNetTypeDetailForStats();

                mParamsDetail = new LinkedHashMap<String, String>();
                mParamsDetail.put("trace_id", mTraceId);
                mParamsDetail.put("url", mUrl);
                mParamsDetail.put("host", url.getHost());
                mParamsDetail.put("path", url.getPath());
                mParamsDetail.put("portal", mPortal);
                mParamsDetail.put("network", paramNet);
                mParamsDetail.put("cache_hit", mCacheHit);
                mParamsDetail.put("download_duration", String.valueOf(timeUsed));
                mParamsDetail.put("download_length", String.valueOf(byteCount));
                mParamsDetail.put("download_speed", String.valueOf(byteCount * 1000 / timeUsed));
            } catch (Exception e) {
            }
        }

        void traceEnd(boolean isSucc) {
            if (TextUtils.isEmpty(mTraceId) || mParamsDetail == null)
                return;

            try {
                if (!mUrl.endsWith(".m3u8") && !mUrl.endsWith(".mpd")) {
                    int rateDenominator = CloudConfig.getIntConfig(ObjectStore.getContext(), KEY_CFG_HTTP_STATS_RATE, 10);;
                    if (!Stats.isRandomCollect(rateDenominator))
                        return;
                }

                mParamsDetail.put("result", Boolean.toString(isSucc));

                Logger.v(TAG, "Net_HttpConnectDetail1:" + mParamsDetail.toString());
                Stats.onEvent(ObjectStore.getContext(), "Net_HttpConnectDetail1", mParamsDetail);
                sAnalyzerPool.remove(mTraceId);
            } catch (Exception e) {
            }
        }

    }
}
