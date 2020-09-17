package com.ushareit.core.net;

import android.net.Uri;
import android.text.TextUtils;

import com.ushareit.app.CommonActivityLifecycle;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.stats.Stats;
import com.ushareit.core.utils.i18n.LocaleUtils;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class EchoServerHelper {
    private static final String TAG = "EchoServerHelper";
    private static final String ECHO_SERVER_CONFIG = "echo_serv_config";
    private static String hostUrl = "";
    private static boolean looper = true;
    private static int appForegroundTimer = 1 * 60 * 1000;
    private static int appbackgroundTimer = 5 * 60 * 1000;
    private static boolean supportEchoServer = true;
    private static AtomicReference<EchoStatus> mStatus = new AtomicReference<EchoStatus>(EchoStatus.Idle);
    private static Object mLock = new Object();
    private static Result mLastResult;
    private static AtomicBoolean mFromApplicationStart = new AtomicBoolean(false);

    enum EchoStatus {
        Idle, Running
    }

    static {
        String config = CloudConfig.getStringConfig(ObjectStore.getContext(), ECHO_SERVER_CONFIG, "");
        if (!TextUtils.isEmpty(config)) {
            try {
                JSONObject json = new JSONObject(config);
                appForegroundTimer = json.optInt("app_fg_timer", 1 * 60 * 1000); //FORBID LESS THAN 20s
                appbackgroundTimer = json.optInt("app_bg_timer", 5 * 60 * 1000); //FORBID LESS THAN 20s
                hostUrl = json.optString("host_url", "");
                supportEchoServer = json.optBoolean("support_echo", true);
                looper = json.optBoolean("looper", true);
            } catch (Exception e) {
            }
        }
    }

    static EchoStatus getStatus() {
        return mStatus.get();
    }

    static void tryConnectEchoServer(final IEchoServer iEchoServer) {
        if (!supportEchoServer) {
            return;
        }

        if (TextUtils.isEmpty(hostUrl))
            return;
        if (mStatus.get() == EchoStatus.Running)
            return;
        TaskHelper.execZForUI(new TaskHelper.RunnableWithName("Connect.Test") {
            @Override
            public void execute() {
                synchronized (mLock) {
                    if (mStatus.get() == EchoStatus.Running) {
                        Logger.d(TAG, "echo server is running , return ");
                        return;
                    }
                    if (!mStatus.compareAndSet(EchoStatus.Idle, EchoStatus.Running)) {
                        Logger.d(TAG, "echo server compareAndSet running , but origin status is not idle,  return ");
                        return;
                    }
                }

                while (true) {
                    long start = System.currentTimeMillis();
                    Exception exception = null;
                    boolean useIp = iEchoServer != null && !TextUtils.isEmpty(iEchoServer.getIp(hostUrl));
                    try {
                        String url = useIp ? iEchoServer.getIp(hostUrl) : hostUrl;
                        Map<String, String> params = new HashMap<>();
                        Map<String, String> headers = new HashMap<>();
                        if (useIp) {
                            Uri uri = Uri.parse(hostUrl);
                            headers.put("Host", uri.getHost());
                        }
                        UrlResponse urlResponse = HttpUtils.okGet("echo_server", url, headers, params, 15 * 1000, 15 * 1000);
                        int statusCode = urlResponse.getStatusCode();
                        if (statusCode != HttpURLConnection.HTTP_OK)
                            throw new RuntimeException(LocaleUtils.formatStringIgnoreLocale("Http status code: %d", statusCode));

                    } catch (Exception e) {
                        exception = e;
                    } finally {
                        long finalTime = System.currentTimeMillis();
                        long duration = finalTime - start;
                        mLastResult = new Result(duration, exception == null, useIp, finalTime);
                        Logger.d(TAG, " result = " + mLastResult.duration + "   " + mLastResult.result);
                        collectTestConnectResult(useIp, exception, duration);
                    }

                    if (!looper) {
                        return;
                    }
                    synchronized (mLock) {
                        try {
                            mLock.wait(getScheduleTimer());
                        } catch (InterruptedException e) {
                            Logger.e(TAG, "connect.Test is interrupted");
                        }
                    }
                }
            }
        });
    }

    public static void startEchoServer(boolean fromApplication, IEchoServer iEchoServer) {
        mFromApplicationStart.set(fromApplication);
        if (EchoServerHelper.getStatus() == EchoServerHelper.EchoStatus.Running)
            return;
        EchoServerHelper.tryConnectEchoServer(iEchoServer);
    }

    private static int getScheduleTimer() {
        return CommonActivityLifecycle.isAppInBackground() && !mFromApplicationStart.get() ? appbackgroundTimer : appForegroundTimer;
    }

    private static void collectTestConnectResult(boolean isIp, Exception e, long duration) {
        try {
            HashMap<String, String> info = new LinkedHashMap<>();
            info.put("result", e == null ? "success" : "failed");
            info.put("msg", e == null ? null : e.getMessage());
            info.put("duration", String.valueOf(duration));
            info.put("exception", e == null ? null : (e.getCause() == null ? e.getClass().getSimpleName() : e.getCause().getClass().getSimpleName()));
            info.put("address", isIp ? "ip" : "host");
            Stats.onRandomEvent(ObjectStore.getContext(), "test_connect_result", info);
            Logger.v(TAG, "collectTestConnectResult:" + info);
        } catch (Exception ex) {
            Logger.e(TAG, "collectTestConnectResult failed", ex);
        }
    }

    public static Result getLastResult() {
        return mLastResult;
    }
    public static class Result{
        public long duration;
        public boolean result;
        public long timeStamp;
        public String requestBy;

        Result(long duration, boolean result, boolean useIp, long timeStamp) {
            this.duration = duration;
            this.result = result;
            requestBy = useIp ? "ip" : "host";
            this.timeStamp = timeStamp;
        }
    }

    public interface IEchoServer {
        String getIp(String hostUrl);
    }

}
