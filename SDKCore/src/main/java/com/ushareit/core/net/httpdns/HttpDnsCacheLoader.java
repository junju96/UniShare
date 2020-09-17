package com.ushareit.core.net.httpdns;

import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.ObjectStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpDnsCacheLoader {
    private static final String TAG = "DNS_HttpDnsCacheLoader";
    private static final String KEY_SETTINGS_DNS_CACHE = "dns_cache_list";
    private static Settings sDnsLocalSettings = new Settings(ObjectStore.getContext(), KEY_SETTINGS_DNS_CACHE);

    HttpDnsCacheLoader(){}

    synchronized Map<String, DNSEntity> listDnsEntries() {
        Map<String, DNSEntity> dnsEntries = new HashMap<>();

        Map<String, String> dnsSettings = (Map<String, String>) sDnsLocalSettings.getAll();
        if (dnsSettings == null || dnsSettings.size() <= 0) {
            Logger.d(TAG, "no local cache, request dns from server, getAllConfigAddress");
            return dnsEntries;
        }

        Set<Map.Entry<String, String>> entrySet = dnsSettings.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            String host = entry.getKey();
            String value = entry.getValue();
            DNSEntity dnsEntity = new DNSEntity(host);
            try {
                dnsEntity.updateByJSON(new JSONObject(value));
                dnsEntries.put(host, dnsEntity);
            } catch (JSONException e) {
                Logger.e(TAG, "loadDnsCache error, " + e.getMessage());
            }
        }
        Logger.v(TAG, "cached DNS: " + dnsEntries);
        return dnsEntries;
    }

    synchronized void saveDnsEntries(Map<String, DNSEntity> entityMap) {
        Map<String, DNSEntity> dnsEntries = new HashMap<>(entityMap);
        if (dnsEntries == null || dnsEntries.size() <= 0)
            return;

        Set<Map.Entry<String, DNSEntity>> entrySet = dnsEntries.entrySet();
        for (Map.Entry<String, DNSEntity> entry : entrySet) {
            String host = entry.getKey();
            DNSEntity dnsEntity = entry.getValue();
            String jsonDnsEntity = dnsEntity.toJson().toString();
            sDnsLocalSettings.set(host, jsonDnsEntity);
            Logger.v(TAG, "save dns entry:" + jsonDnsEntity);
        }
    }
}
