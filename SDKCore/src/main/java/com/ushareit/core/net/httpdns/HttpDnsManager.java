package com.ushareit.core.net.httpdns;

import android.net.Uri;
import android.text.TextUtils;

import com.ushareit.core.Logger;
import com.ushareit.core.lang.thread.TaskHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpDnsManager {
    private static final String TAG = "DNS_HttpDnsManager";
    private static Set<String> sDNSHost = new HashSet<>();
    private static Map<String, DNSEntity> mDNSEntities = new HashMap<>();
    private static Map<String, FailedEntry> mFailedHost = new HashMap<>();

    private static HttpDnsManager sHttpDnsManager = new HttpDnsManager();
    private HttpDnsCacheLoader mHttpDnsCacheLoader;
    private HttpDnsRequest mDNSConfigRequestWorker;
    private HttpDnsSchedulerWorker mDnsSchedulerworker;

    private AtomicBoolean mInited = new AtomicBoolean(false);
    private AtomicBoolean mRunning = new AtomicBoolean(false);

    private long mLastTime = 0;

    static {
        sDNSHost.add("api2.wshareit.com");
        sDNSHost.add("cf.hermes.wshareit.com");
    }

    private HttpDnsManager() {
        mHttpDnsCacheLoader = new HttpDnsCacheLoader();
        mDNSConfigRequestWorker = new HttpDnsRequest();
    }

    public static HttpDnsManager getInstance() {
        return sHttpDnsManager;
    }

    public void registerDnsRequestApi(IDNSRequestApi api) {
        if (api == null)
            return;
        mDNSConfigRequestWorker.registerDnsRequestApi(api);
    }

    private void initEntrties() {
        if (!mInited.compareAndSet(false, true))
            return;

        synchronized (mDNSEntities) {
            if (mDNSEntities.isEmpty()) {
                mDNSEntities.putAll(mHttpDnsCacheLoader.listDnsEntries());
            }

            for (String host : sDNSHost) {
                if (mDNSEntities.containsKey(host))
                    continue;
                mDNSEntities.put(host, new DNSEntity(host));
            }
        }
    }

    public boolean inIpDirectWhiteList(String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        try {
            Uri url = Uri.parse(host);
            return sDNSHost.contains(url.getHost());
        } catch (Exception e) {
        }

        return false;
    }

    public boolean ipReady(String host) {
        if (TextUtils.isEmpty(host))
            return false;
        try {
            Uri url = Uri.parse(host);
            DNSEntity cachedEntity = null;
            synchronized (mDNSEntities) {
                cachedEntity = mDNSEntities.get(url.getHost());
            }
            return (cachedEntity != null && !cachedEntity.ips.isEmpty());
        } catch (Exception e) {
        }
        return false;
    }


    public void getDnsEntries() {
        if (!DnsConfig.sEnableDnsList)
            return;

        if (!mRunning.compareAndSet(false, true))
            return;

        try {
            long current = System.currentTimeMillis();
            if (Math.abs(current - mLastTime) < DnsConfig.sMinGetDnsInterval * 1000)
                return ;

            Map<String, DNSEntity> cachedEntities = new HashMap<>();
            synchronized (mDNSEntities) {
                cachedEntities.putAll(mDNSEntities);
            }
            boolean result = mDNSConfigRequestWorker.loadDnsEntries(cachedEntities);
            mLastTime = current;

            if (result) {
                synchronized (mFailedHost) {
                    mFailedHost.clear();
                }
            }
            synchronized (mDNSEntities) {
                mDNSEntities.clear();
                mDNSEntities.putAll(cachedEntities);
            }
            mHttpDnsCacheLoader.saveDnsEntries(cachedEntities);
        } catch (Exception e) {}
        finally {
            mRunning.set(false);
        }
    }

    public String getAddress(final String host) {
        synchronized (mFailedHost) {
            FailedEntry failedEntry = mFailedHost.get(host);
            if (failedEntry != null && failedEntry.count >= DnsConfig.sMaxFailedCount) {
                Logger.v(TAG, "connect failed count had over the max, host " + host);
                return null;
            }
        }

        DNSEntity cachedEntity;
        synchronized (mDNSEntities) {
            cachedEntity = mDNSEntities.get(host);
        }
        if (cachedEntity == null) {
            Logger.w(TAG, "Can not find dns entity, host:" + host);
            return null;
        }

        if (cachedEntity.isTimeout()) {
            TaskHelper.execZForSDK(new TaskHelper.RunnableWithName("get_single_host_dns") {
                @Override
                public void execute() {
                    initEntrties();
                    getDnsEntries();
                }
            });
        }

        return cachedEntity.getRandomIp();
    }

    public void notifyConnectResult(String host, boolean connectSuccess) {
        if (connectSuccess) {
            synchronized (mFailedHost) {
                FailedEntry failedEntry = mFailedHost.get(host);
                if (failedEntry != null) {
                    failedEntry.count = 0;
                    Logger.v(TAG, "notify connect host " + host + " succeed, failed entry:" + mFailedHost);
                }
            }
            return;
        }

        synchronized (mFailedHost) {
            FailedEntry failedEntry = mFailedHost.get(host);
            if (failedEntry == null) {
                failedEntry = new FailedEntry();
                mFailedHost.put(host, failedEntry);
            }
            failedEntry.count ++;
            Logger.v(TAG, "notify connect host " + host + " failed, failed entry:" + mFailedHost);
        }
    }

    public void tryStartDnsService() {
        if (!DnsConfig.sEnableScheduleDns || dnsServiceAlive()) {
            Logger.d(TAG, "can not start dns service or service has launched!, enabled:" + DnsConfig.sEnableScheduleDns);
            return;
        }
        initEntrties();

        Logger.d(TAG, "schedule worker start");
        mDnsSchedulerworker = new HttpDnsSchedulerWorker();
        mDnsSchedulerworker.start();
    }

    private boolean dnsServiceAlive() {
        if (mDnsSchedulerworker == null)
            return false;
        return mDnsSchedulerworker.isAlive();
    }

    class HttpDnsSchedulerWorker extends Thread {
        private static final String TAG = "DNS_HttpDnsSchedulerWorker";

        @Override
        public void run() {
            while (true) {
                if (!DnsConfig.sEnableScheduleDns)
                    return;
                getDnsEntries();
                try {
                    Thread.sleep(DnsConfig.sScheduleDnsTimerSeconds * 1000);
                } catch (Exception e) {
                    Logger.d(TAG, "shareit exception , " + e.getMessage());
                }
            }
        }
    }

    private static class FailedEntry {
        int count;

        @Override
        public String toString() {
            return "FailedEntry{" +
                    "count=" + count +
                    '}';
        }
    }
}
