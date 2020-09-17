package com.ushareit.core.net.httpdns;

import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.algo.DecorativePacket;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.AppDist;
import com.ushareit.core.utils.device.DeviceHelper;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.stats.Stats;
import com.ushareit.core.net.NetUtils;
import com.ushareit.core.net.HttpUtils;
import com.ushareit.core.net.UrlResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpDnsRequest {
    private static final String TAG = "DNS_HttpDnsRequest";
    private static final String DEFAULT_IP = Logger.isDebugVersion ? "http://13.248.139.116/dns/info" : "http://13.248.136.70/dns/info";
    private static final int CONNECT_RW_TIMEOUT = 5000;

    private IDNSRequestApi mDnsRequestApi;

    void registerDnsRequestApi(IDNSRequestApi api) {
        this.mDnsRequestApi = api;
    }


    public boolean loadDnsEntries(Map<String, DNSEntity> cachedEntries) {
        if (!DnsConfig.sEnableDnsList || cachedEntries.isEmpty())
            return false;

        try {
            if (mDnsRequestApi != null)
                return fetchDNSByInjectApi(cachedEntries);
            else
                return defaultFetchDns(cachedEntries);
        } catch (Exception e) {}
        return false;
    }

    private boolean fetchDNSByInjectApi(Map<String, DNSEntity> cachedEntries) {
        return mDnsRequestApi.fetchAllDns(cachedEntries);
    }

    private boolean defaultFetchDns(Map<String, DNSEntity> cachedEntries) {
        String dnsIP = CloudConfig.getStringConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_DNS_IP, DEFAULT_IP);
        int connectRWTimeout = CloudConfig.getIntConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_DNS_REQ_TIMEOUT, CONNECT_RW_TIMEOUT);
        String errMsg = null;
        try {
            JSONObject jParams = generateParams(cachedEntries.values());
            UrlResponse response = HttpUtils.postJSON(dnsIP, jParams.toString().getBytes("utf-8"), connectRWTimeout, connectRWTimeout);
            JSONObject resultJsonObj = new JSONObject(response.getContent());
            Logger.v(TAG, "POST response dns result:" + resultJsonObj.toString());
            if (!resultJsonObj.has("result_code")) {
                Logger.d(TAG, "request dns info without code!");
                errMsg = "request without code";
                return false;
            }
            int code = resultJsonObj.getInt("result_code");
            if (code != 200) {
                Logger.d(TAG, "request dns info code = " + code);
                errMsg = ("result code = " + code);
                return false;
            }
            JSONObject jdata = resultJsonObj.getJSONObject("data");
            JSONArray jdns = jdata.getJSONArray("dns");
            for (int i = 0; i < jdns.length(); i++) {
                JSONObject item = jdns.getJSONObject(i);
                try {
                    String host = item.getString("dn");
                    if (TextUtils.isEmpty(host)) {
                        Logger.d(TAG, "can not host field:" + item.toString());
                        continue;
                    }
                    DNSEntity entity = cachedEntries.get(host);
                    if (entity == null) {
                        Logger.d(TAG, "can not find host in request:" + host);
                        continue;
                    }
                    entity.updateByJSON(item);
                } catch (Exception e) {
                    Logger.d(TAG, "deserialize the dns entity failed!", e);
                    errMsg = "item error:" + e.getMessage();
                }
            }
            return true;
        } catch (Exception e) {
            Logger.w(TAG, "request DNS entity failed!", e);
            errMsg = "request error:" + e.getMessage();
        } finally {
            collectDNSResult(dnsIP, errMsg, connectRWTimeout);
        }
        return false;
    }

    private JSONObject generateParams(Collection<DNSEntity> entities) throws JSONException {
        String appId = AppDist.getAppId();
        int appVer = Utils.getVersionCode(ObjectStore.getContext());
        int osVer = Build.VERSION.SDK_INT;
        String imsi = DeviceHelper.getIMSI(ObjectStore.getContext());
        JSONArray jDNSArray = new JSONArray();
        for (DNSEntity entity : entities) {
            jDNSArray.put(entity.toPartJson());
        }

        JSONObject jParams = new JSONObject();
        JSONObject jsParams = new JSONObject();
        jParams.put("app_id", appId);
        jParams.put("app_version", appVer);
        jParams.put("os_version", osVer);
        if (TextUtils.isEmpty(imsi))
            jsParams.put("imsi", imsi);
        jParams.put("dns", jDNSArray);
        if (jsParams.length() > 0) {
            try {
                jParams.put("s", DecorativePacket.encodePacketBase64(jsParams.toString()));
            } catch (Exception e) {
            }
        }
        return jParams;
    }

    private void collectDNSResult(String ip, String errorMsg, int timeout) {
        try {
            Pair<Boolean, Boolean> connected = NetUtils.checkConnected(ObjectStore.getContext());
            HashMap<String, String> info = new LinkedHashMap<>();
            info.put("result", errorMsg == null ? "success" : "failed");
            info.put("ip", ip);
            info.put("cur_net", NetUtils.getNetwork(connected));
            info.put("msg", (errorMsg == null) ? null : errorMsg);
            info.put("timeout", String.valueOf(timeout));

            Stats.onEvent(ObjectStore.getContext(), "dns_req_result", info);
            Logger.v(TAG, "collectUploadResult:" + info);
        } catch (Exception ex) {
        }
    }

}
