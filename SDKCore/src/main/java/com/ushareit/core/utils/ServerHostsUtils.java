package com.ushareit.core.utils;

import android.content.Context;
import android.text.TextUtils;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.Assert;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.ObjectStore;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// manage all the server hosts (root urls), and the switch between test servers and production servers
public final class ServerHostsUtils {

    // the "test/production server flag"
    // true means use test servers, otherwise production servers, default must be false
    private static boolean useTestServers = false;
    // indicate whether the value of useTestServers loaded from persisted settings or not
    private static boolean useTestServersLoaded = false;

    private static final String SETTINGS_KEY_USE_TEST_SERVERS = "USE_TEST_SERVERS";
    private static final String CHANNEL_NAME_TEST_SERVERS = "TEST_SERVERS";

    private static Map<String, String> mConfigHosts = new HashMap<>();
    private static AtomicBoolean mInited = new AtomicBoolean(false);

    private static synchronized void initHost() {
        if (!mInited.compareAndSet(false, true))
            return;

        try {
            String hostsString = CloudConfig.getStringConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_CONFIG_HOST);
            if (TextUtils.isEmpty(hostsString))
                return;
            JSONObject json = new JSONObject(hostsString);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                mConfigHosts.put(key, json.getString(key));
            }
        } catch (Throwable e) {} finally {
            mInited.set(true);
        }
    }

    private ServerHostsUtils() {}

    // check should we use test servers or production servers
    public static boolean shouldUseTestServers(Context context) {
        if (!useTestServersLoaded) {
            Assert.notNull(context);
            Settings settings = new Settings(context);
            if (settings.contains(SETTINGS_KEY_USE_TEST_SERVERS))
                useTestServers = settings.getBoolean(SETTINGS_KEY_USE_TEST_SERVERS, useTestServers);
            else if (CHANNEL_NAME_TEST_SERVERS.equalsIgnoreCase(AppDist.getChannel()))
                useTestServers = true;
            useTestServersLoaded = true;
        }

        return useTestServers;
    }

    // change the "test/production server flag" on the fly (by UI)
    public static void setUseTestServers(Context context, boolean value) {
        useTestServers = value;
        useTestServersLoaded = true;
        new Settings(context).setBoolean(SETTINGS_KEY_USE_TEST_SERVERS, useTestServers);
    }

    public static String getConfigHost(String key, String defaultHost) {
        initHost();

        String configHost = mConfigHosts.get(key);
        return TextUtils.isEmpty(configHost) ? defaultHost : configHost;
    }
}
