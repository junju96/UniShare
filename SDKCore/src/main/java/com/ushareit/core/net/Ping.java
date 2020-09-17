package com.ushareit.core.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import com.ushareit.app.CommonActivityLifecycle;
import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.cmd.CmdUtils;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.stats.Stats;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Ping {
    private static final String TAG = "Ping";

    public static class PingConfig {
        public static boolean pingAllTime = false;
        private static boolean permit = false;
        private static boolean loopPermit = false;
        private static int pingCount = 5;
        private static int pingTimer = 5;// ping per 5 min;
        private static int appForegroundPingTimer = 1 * 60 * 1000;
        private static int appbackgroundPingTimer = 5 * 60 * 1000;
        private static int syncMaxTime = 2 * 1000;
        private static int asyncMaxTime = 12 * 1000;
        private static int recvPPPerfect = 75;
        private static int recvPPPassable = 50;
        private static int avgTimePerfect = 100;
        private static int avgTimePassable = 300;
        private static String[] configAddress = null;
        private static boolean ping3G = false;
        private static boolean ping2G = false;
        static {
            String config = CloudConfig.getStringConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_PING_ADDRESSES, "");
            if (!TextUtils.isEmpty(config)) {
                try {
                    JSONObject json = new JSONObject(config);
                    permit = json.has("permit") ? json.getBoolean("permit") : false;
                    loopPermit = json.has("loop_permit") ? json.getBoolean("loop_permit") : false;
                    pingCount = json.has("ping_count") ? json.getInt("ping_count") : 5;
                    pingTimer = json.has("ping_timer") ? json.getInt("ping_timer") : 5;
                    recvPPPerfect = json.has("recv_pp_perfect") ? json.optInt("recv_pp_perfect") : 75;
                    recvPPPassable = json.has("recv_pp_passable") ? json.optInt("recv_pp_passable") : 50;
                    avgTimePerfect = json.has("avg_time_perfect") ? json.getInt("avg_time_perfect") : 100;
                    avgTimePassable = json.has("avg_time_passable") ? json.getInt("avg_time_passable") : 300;
                    ping3G = json.has("ping_3g") ? json.getBoolean("ping_3g") : false;
                    ping2G = json.optBoolean("ping_2g", false);
                    if (json.has("app_fg_timer"))//app foreground ping timer
                        appForegroundPingTimer = json.optInt("app_fg_timer"); //FORBID LESS THAN 20s
                    if (json.has("app_bg_timer"))//app background ping timer
                        appbackgroundPingTimer = json.optInt("app_bg_timer"); //FORBID LESS THAN 20s
                    if (json.has("ping_all_time"))
                        pingAllTime = json.optBoolean("ping_all_time");
                    if (json.has("sync_max_time"))
                        syncMaxTime = json.optInt("sync_max_time");
                    if (json.has("async_max_time"))
                        asyncMaxTime = json.optInt("async_max_time");
                    if (permit && json.has("address")) {
                        JSONArray jarray = json.getJSONArray("address");
                        configAddress = new String[jarray.length()];
                        for (int i = 0; i < jarray.length(); i++)
                            configAddress[i] = jarray.getString(i);
                    }
                } catch (Exception e) {}
            }
        }

        final static String[] DEFAULT_ADDRESS = {
                "api2.wshareit.com"
        };

        static String[] getAddress() {
            if (configAddress != null && configAddress.length > 0)
                return configAddress;

            return DEFAULT_ADDRESS;
        }

        static void collectPingResult(Context context, EvaluateResult result, CmdUtils.PingResult pingResult, String errMessage) {
            try {
                HashMap<String, String> params = new LinkedHashMap<String, String>();
                params.put("result", result != null ? result.name() : "Null");
                params.put("err_msg", errMessage);
                if (pingResult != null) {
                    params.put("recv_pac", String.valueOf(pingResult.recvPackagePercent));
                    params.put("average_time", String.valueOf(pingResult.avgTime));
                }
                Stats.onRandomEvent(context, "PingResult", params);
            } catch (Exception e) {}
        }

        static void collectPingInfo(Context context, PingTask.TaskStatus status, EvaluateResult result, CmdUtils.PingResult pingResult, PingNetResult pingNetResult, String pingMsg) {
            try {
                if (!PingConfig.pingAllTime) {
                    return;
                }
                HashMap<String, String> params = new LinkedHashMap<String, String>();
                params.put("result", result != null ? result.name() : "Null");
                params.put("ping_msg", pingMsg);
                params.put("ping_status", status.name());
                params.put("permit", String.valueOf(PingConfig.permit));
                params.put("loop_permit", String.valueOf(PingConfig.loopPermit));
                params.put("recv_pac_percent", pingResult != null ? String.valueOf(pingResult.recvPackagePercent) : "-1");
                params.put("average_time", pingResult != null ? String.valueOf(pingResult.avgTime) : "-1");
                params.put("app_status", CommonActivityLifecycle.isAppInBackground() ? "background" : "foreground");
                params.put("ping_net_result", pingNetResult != null ? pingNetResult.name() : "Null");
                if (pingResult != null && pingNetResult != null && pingNetResult != PingNetResult.Available)
                    params.put("cmd_out_msg", pingResult.cmdOut);
                Stats.onEvent(context, "PingInfo", params);
            } catch (Exception e) {}
        }
    }

    public enum PingNetResult {
        Available, Unavailable, Unknown, Unexpected;

        static PingNetResult evaluate(CmdUtils.PingResult ping) {
            String netType = NetworkStatus.getNetworkStatusEx(ObjectStore.getContext()).getNetTypeDetailForStats();
            if (netType != null && !netType.contains("OFFLINE") && !TextUtils.isEmpty(ping.errMsg) && ping.errMsg.contains("Operation not permitted"))
                return PingNetResult.Unknown;
            if (ping.avgTime > 0 && ping.recvPackagePercent > 0)
                return PingNetResult.Available;
            if (!ping.succeed && !TextUtils.isEmpty(ping.errMsg) && !ping.errMsg.contains("exception:"))
                return PingNetResult.Unavailable;
            if (TextUtils.isEmpty(ping.cmdOut) || ping.cmdOut.trim().equals("[]"))
                return PingNetResult.Unexpected;
            return PingNetResult.Unknown;
        }
    }

    public enum EvaluateResult {
        Perfect, Passable, Bad, Unknown;

        static EvaluateResult evaluate(CmdUtils.PingResult ping) {
            if (!ping.succeed || ping.recvPackagePercent <= 0)
                return EvaluateResult.Unknown;

            if (ping.recvPackagePercent >= PingConfig.recvPPPerfect) {
                if (ping.avgTime < PingConfig.avgTimePerfect)
                    return EvaluateResult.Perfect;
                else if (ping.avgTime < PingConfig.avgTimePassable)
                    return EvaluateResult.Passable;
                return EvaluateResult.Bad;
            } else if (ping.recvPackagePercent >= PingConfig.recvPPPassable) {
                if (ping.avgTime < PingConfig.avgTimePassable)
                    return EvaluateResult.Passable;
                else
                    return EvaluateResult.Bad;
            }
            return EvaluateResult.Bad;
        }
    }

    public static class EvaluateDetail {
        EvaluateDetail(EvaluateResult result, CmdUtils.PingResult pingResult, PingNetResult pingNetResult, boolean isOffline, String pingMsg) {
            this.result = result;
            this.pinNetResult = pingNetResult;
            revcPercent = pingResult != null ? pingResult.recvPackagePercent : -1;
            roundTrip = pingResult != null ? pingResult.avgTime : -1;
            this.pingResultDesc = pingMsg;
            this.isOffline = isOffline;
        }

        public EvaluateResult result;
        public PingNetResult pinNetResult;
        public int revcPercent;
        public int roundTrip;
        public String pingResultDesc;
        public boolean isOffline;

        @Override
        public String toString() {
            return "EvaluateDetail{" +
                    "result=" + result +
                    ", revcPercent=" + revcPercent +
                    ", roundTrip=" + roundTrip +
                    '}';
        }
    }

    private static EvaluateDetail evaluateCurrentNetwork(Context context, int pingTimeout) {
        Logger.d(TAG, "begin check ping!");
        CmdUtils.PingResult pingResult = null;
        EvaluateResult finalResult = EvaluateResult.Unknown;
        PingNetResult pingNetResult = PingNetResult.Unknown;
        boolean isOffline = false;
        String message = null;
        try {
            Pair<Boolean, Boolean> connected = NetUtils.checkConnected(context);
            NetworkStatus.NetType netType = NetworkStatus.getNetworkStatusEx(context).getNetType();
            //1. check no network
            if (!connected.first && !connected.second && netType == NetworkStatus.NetType.OFFLINE) {
                finalResult = EvaluateResult.Bad;
                pingNetResult = PingNetResult.Unavailable;
                isOffline = true;
                message = "no network";
            }
            //2. check 2G, 3G
            else if (connected.first && !connected.second && !isNeedCheckPingForMobileData(context)) {
                finalResult = EvaluateResult.Bad;
                message = "2G3G";
            }
            //3. check permit ping in wifi or 4G
            else if (!PingConfig.permit) {
                finalResult = EvaluateResult.Unknown;
                message = "nopermit";
            }
            //4. check ping in wifi or 4G
            else {
                String[] addresses = PingConfig.getAddress();
                for (String address : addresses) {
                    CmdUtils.PingResult result = CmdUtils.execPing(PingConfig.pingCount, address, pingTimeout);
                    EvaluateResult evaluateResult = EvaluateResult.evaluate(result);
                    pingNetResult = PingNetResult.evaluate(result);
                    message = "ping done";
                    if (evaluateResult.ordinal() < finalResult.ordinal()) {
                        finalResult = evaluateResult;
                    }
                    pingResult = result;
                }
            }

            if (pingResult != null && !TextUtils.isEmpty(pingResult.errMsg))
                message = pingResult.errMsg;
        } catch (Exception e) {
            message = "exception:" + e.getMessage();
        } finally {
            Logger.d(TAG, "completed check ping, result:" + finalResult);
            PingConfig.collectPingInfo(context, PingTask.mStatus, finalResult, pingResult, pingNetResult, message);
            PingConfig.collectPingResult(context, finalResult, pingResult, message);
        }
        return new EvaluateDetail(finalResult, pingResult, pingNetResult, isOffline, message);

    }

    private static boolean isNeedCheckPingForMobileData(Context context) {
        NetworkStatus.MobileDataType dataType = NetworkStatus.getMobileDataType(context);
        if (dataType == NetworkStatus.MobileDataType.MOBILE_4G)
            return true;
        if (dataType == NetworkStatus.MobileDataType.MOBILE_3G && PingConfig.ping3G)
            return true;
        if (dataType == NetworkStatus.MobileDataType.MOBILE_2G && PingConfig.ping2G)
            return true;
        return false;
    }




    private static class PingTask {
        private enum TaskStatus {
            Running, Stop, Pause
        }
        private static TaskStatus mStatus = TaskStatus.Stop;
        private static EvaluateDetail mLastDetail = new EvaluateDetail(EvaluateResult.Unknown, null, PingNetResult.Unknown, false, "init");
        private static Object mLock = new Object();
        private static Object mScheduleLock = new Object();
        private static AtomicBoolean mEvaluated = new AtomicBoolean(false);
    }

    private static AtomicBoolean mFromApplicationStart = new AtomicBoolean(false);
    public static void startPingTask(final boolean fromApplication) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
            return;

        if (!PingConfig.loopPermit)
            return;

        mFromApplicationStart.set(fromApplication);
        synchronized (PingTask.mLock) {
            if (PingTask.mStatus == PingTask.TaskStatus.Running) {
                Logger.d(TAG, "ping task is running");
                return;
            }

            if (PingTask.mStatus == PingTask.TaskStatus.Pause) {
                PingTask.mStatus = PingTask.TaskStatus.Running;
                PingTask.mLock.notifyAll();
                Logger.d(TAG, "ping task re running");
                return;
            }

            PingTask.mStatus = PingTask.TaskStatus.Running;
            register();
        }
        try {
            Logger.d(TAG, "start ping task");

            TaskHelper.execZForSDK(new Runnable() {
                @Override
                public void run() {
                    while (PingTask.mStatus != PingTask.TaskStatus.Stop) {
                        if (PingTask.mStatus != PingTask.TaskStatus.Running) {
                            Logger.d(TAG, "loop ping, current is not running, status:" + PingTask.mStatus);
                            synchronized (PingTask.mScheduleLock) {
                                try {
                                    PingTask.mScheduleLock.wait(getScheduleTimer());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            continue;
                        }
                        boolean isFirst = !PingTask.mEvaluated.get();
                        if (isFirst && PingTask.mEvaluated.get()) {
                            Logger.d(TAG, "had evaluate by evaluate now just!");
                            return;
                        }
                        EvaluateDetail evaluateDetail =  evaluateCurrentNetwork(ObjectStore.getContext(),  PingConfig.asyncMaxTime);
                        synchronized (PingTask.mEvaluated) {
                            PingTask.mLastDetail = evaluateDetail;
                            PingTask.mEvaluated.set(true);
                        }
                        Logger.d(TAG, "loop ping:" + PingTask.mLastDetail.toString());
                        synchronized (PingTask.mScheduleLock) {
                            try {
                                PingTask.mScheduleLock.wait(getScheduleTimer());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch(Exception canceled) {}
    }

    private static int getScheduleTimer() {
        return CommonActivityLifecycle.isAppInBackground() && !mFromApplicationStart.get() ? PingConfig.appbackgroundPingTimer : PingConfig.appForegroundPingTimer;
    }

    public static void stopPingTask() {
        synchronized (PingTask.mLock) {
            if (PingConfig.pingAllTime)
                return;
            if (PingTask.mStatus != PingTask.TaskStatus.Running)
                return;
            PingTask.mStatus = PingTask.TaskStatus.Pause;
            Logger.d(TAG, "pause the ping task");
        }

        TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("Task.Ping") {
            @Override
            public void execute() {
                synchronized (PingTask.mLock) {
                    if (PingTask.mStatus == PingTask.TaskStatus.Running) {
                        Logger.d(TAG, "resume the ping task without wait");
                        return;
                    }
                    try {
                        PingTask.mLock.wait(PingConfig.pingTimer * 60 * 1000);
                    } catch (InterruptedException e) {}

                    if (PingTask.mStatus == PingTask.TaskStatus.Running) {
                        Logger.d(TAG, "resume the ping task");
                        return;
                    }

                    PingTask.mStatus = PingTask.TaskStatus.Stop;
                    Logger.d(TAG, "stop the ping task");
                    unregister();
                }
                synchronized (PingTask.mScheduleLock) {
                    PingTask.mScheduleLock.notifyAll();
                }

            }

        });

    }

    public static EvaluateDetail getLastEvaluateDetail() {
        return PingTask.mLastDetail;
    }

    public static EvaluateDetail getEvaluateDetailNow() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            Logger.w(TAG, "Can not run evaluate network in UI thread!");
            throw new RuntimeException("Can not run evaluate network in UI thread!");
        }

        if (PingConfig.loopPermit && PingTask.mEvaluated.get()) {
            Logger.d(TAG, "Ping value exist return NOW!");
            return PingTask.mLastDetail;
        }

        final AtomicBoolean pingResult = new AtomicBoolean(false);
        TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("Evaluate.Now") {
            @Override
            public void execute() {
                synchronized (PingTask.mEvaluated) {
                    if (PingConfig.loopPermit && PingTask.mEvaluated.get()) {
                        Logger.d(TAG, "Ping value exist when get now!");
                        pingResult.set(true);
                        PingTask.mEvaluated.notifyAll();
                        return;
                    }
                }
                EvaluateDetail evaluateDetail =  evaluateCurrentNetwork(ObjectStore.getContext(), PingConfig.syncMaxTime);
                synchronized (PingTask.mEvaluated) {
                    PingTask.mLastDetail = evaluateDetail;
                    PingTask.mEvaluated.set(true);
                    pingResult.set(true);
                    PingTask.mEvaluated.notifyAll();
                    Logger.d(TAG, "evaluate now completed!");
                }
            }
        });

        synchronized (PingTask.mEvaluated) {
            Logger.d(TAG, "begin wait evaluate, max 2s!");
            try {
                if (!pingResult.get())
                    PingTask.mEvaluated.wait(PingConfig.syncMaxTime);
            } catch (InterruptedException e) {}
            Logger.d(TAG, "Wait evaluate completed!");
        }
        return PingTask.mLastDetail;
    }

    private static void refreshEvaluateDetail() {
        EvaluateDetail evaluateDetail = evaluateCurrentNetwork(ObjectStore.getContext(),  PingConfig.asyncMaxTime);
        synchronized (PingTask.mEvaluated) {
            PingTask.mLastDetail = evaluateDetail;
            PingTask.mEvaluated.set(true);
            PingTask.mEvaluated.notifyAll();
            Logger.d(TAG, "refresh evaluate now completed!");
        }
    }

    private static BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("Evaluate.Now") {
                @Override
                public void execute() {
                    refreshEvaluateDetail();
                }
            });
        }
    };

    private static void register() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        ObjectStore.getContext().registerReceiver(mReceiver, filter);
    }

    private static void unregister() {
        try {
            ObjectStore.getContext().unregisterReceiver(mReceiver);
        } catch (Exception e) {}
    }

}
