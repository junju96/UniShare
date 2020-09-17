package com.ushareit.core.net.httpdns;

import android.text.TextUtils;
import com.ushareit.core.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

public class DNSEntity {
    private static final String TAG = "DNSEntity";
    String host;
    long ttl;
    Vector<String> ips = new Vector<>();
    private String dataSign;
    private long initTime = 0;

    DNSEntity(String host) {
        this.host = host;
    }

    synchronized void reset() {
        ttl = 0;
        ips.clear();
        initTime = 0;
        dataSign = "";
    }

    String getRandomIp() {
        List<String> copies = new ArrayList<>(ips);
        int size = copies.size();
        return size == 0 ? null : copies.get(new Random().nextInt(size));
    }

    boolean isTimeout() {
        return System.currentTimeMillis() - initTime > ttl;
    }

    JSONObject toPartJson() throws JSONException {
        JSONObject jDNS = new JSONObject();
        jDNS.put("dn", host);
        return jDNS;
    }

    JSONObject toJson() {
        JSONObject jDNS = new JSONObject();
        try {
            jDNS.put("dn", host);
            if (!TextUtils.isEmpty(dataSign))
                jDNS.put("data_sign", dataSign);
            jDNS.put("ttl", ttl);
            jDNS.put("init_time", initTime);
            JSONArray ipAry = new JSONArray();
            for (int i = 0; i < ips.size(); i++) {
                ipAry.put(ips.get(i));
            }
            jDNS.put("ips", ipAry);
        } catch (Exception e) {
            Logger.e(TAG, "toJson error, " + e.getMessage());
        }
        return jDNS;
    }

    void updateByJSON(JSONObject json) {
        try {
            reset();

            host = json.getString("dn");
            dataSign = json.getString("data_sign");
            ttl = json.has("ttl") ? json.getInt("ttl") : DnsConfig.sDefaultTTL;
            JSONArray jips = json.getJSONArray("ips");
            for (int i = 0; i < jips.length(); i++)
                ips.add(jips.getString(i));
            initTime = json.has("init_time") ? json.optLong("init_time") : System.currentTimeMillis();
        } catch (Exception e ) {
            Logger.e(TAG, "updateByJSON error, " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "DNSEntity{" +
                "host='" + host + '\'' +
                ", ttl=" + ttl +
                ", ips=" + ips +
                ", dataSign='" + dataSign + '\'' +
                ", initTime=" + initTime +
                '}';
    }
}
