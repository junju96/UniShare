package com.ushareit.core.net.httpdns;

import android.text.TextUtils;

import com.ushareit.core.CloudConfig;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;

import org.json.JSONObject;

public class DnsConfig {
    private static final String TAG = "DNS_DnsConfig";
    private static final String CONFIG_DNS = "dns_config";
    private static final String KEY_ENABLE_GET_DNS_LIST = "enable_dns_list";
    private static final String KEY_SCHEDULE_GET_DNS = "schedule_get_dns";
    private static final String KEY_SCHEDULE_GET_DNS_TIMER = "get_dns_timer";
    private static final String KEY_API_USE_IP_DIRECT = "use_ip_direct";
    private static final String KEY_API_FIRST_USE_IP_DIRECT = "first_use_ip_direct";
    private static final String KEY_HOST_TTL = "host_ttl";
    private static final String KEY_MAX_FAILED_COUNT = "max_failed_cnt";
    private static final String KEY_GET_INTERVAL = "get_interval";
    public static boolean sEnableDnsList;
    public static boolean sEnableScheduleDns;
    public static int sScheduleDnsTimerSeconds;
    public static boolean sFirstUseIpDirect;
    public static boolean sUseIpDirect;
    public static int sDefaultTTL;
    public static int sMaxFailedCount;
    public static long sMinGetDnsInterval;
    static {
        String config = CloudConfig.getStringConfig(ObjectStore.getContext(), CONFIG_DNS, "");
        if (!TextUtils.isEmpty(config)) {
            try {
                JSONObject json = new JSONObject(config);
                sEnableDnsList = json.optBoolean(KEY_ENABLE_GET_DNS_LIST, true);
                sEnableScheduleDns = json.optBoolean(KEY_SCHEDULE_GET_DNS, true);
                sScheduleDnsTimerSeconds = json.optInt(KEY_SCHEDULE_GET_DNS_TIMER, 60);
                sFirstUseIpDirect = json.optBoolean(KEY_API_FIRST_USE_IP_DIRECT, true);
                sUseIpDirect = json.optBoolean(KEY_API_USE_IP_DIRECT, true);
                sDefaultTTL = json.optInt(KEY_HOST_TTL, 10 * 60 * 1000);
                sMaxFailedCount = json.optInt(KEY_MAX_FAILED_COUNT, 3);
                sMinGetDnsInterval = json.optInt(KEY_GET_INTERVAL, 60);
            } catch (Exception e) {
                Logger.d(TAG, "dns config error, " + e.getMessage());
            }
        }
    }
}
