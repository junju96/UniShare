package com.ushareit.core.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;

import com.ushareit.core.CloudConfig;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.os.AndroidHelper;
import com.ushareit.core.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class NetUtils {
    private static final String TAG = "NetUtils";

    public static String getUrlNoQuery(String url) {
        if (StringUtils.isBlank(url))
            return null;
        int index = url.indexOf("?");
        if (index < 0)
            return url;
        return url.substring(0, index);
    }

    public interface IntenetCallback {
        void onConnected(boolean connected);
    }

    // Check current network whether is connected
    // return <MobileConnected, WirelessConnected>, return null if error
    public static Pair<Boolean, Boolean> checkConnected(Context context) {
        boolean isMobileConnected = false;
        boolean isWifiConnected = false;

        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        int netType = networkInfo.getType();
                        if (netType == ConnectivityManager.TYPE_MOBILE) {
                            isMobileConnected = true;
                        } else if (netType == ConnectivityManager.TYPE_WIFI) {
                            isWifiConnected = true;
                        } else {
                            isMobileConnected = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            isMobileConnected = false;
            isWifiConnected = false;
        }

        return new Pair<Boolean, Boolean>(isMobileConnected, isWifiConnected);
    }

    // InetAddress#isReachable dont worked mostly; ping dont worked for some server or under some network
    public static boolean isReachable() {
        HttpURLConnection urlConnect = null;
        try {
            URL url = new URL("http://www.bing.com");
            urlConnect = (HttpURLConnection) url.openConnection();
            urlConnect.setConnectTimeout(3000);
            urlConnect.getContent();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (urlConnect != null)
                urlConnect.disconnect();
        }
    }

    public static synchronized LinkedList<String> getIps() {
        LinkedList<String> ipList = new LinkedList<String>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            if (en == null)
                return null;

            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet6Address)
                        continue;

                    if (!inetAddress.isLoopbackAddress() &&
                            !inetAddress.isMCGlobal() &&
                            !inetAddress.isAnyLocalAddress()) {
                        String addr = inetAddress.getHostAddress();
                        ipList.add(addr);
                    }
                }
            }

            if (ipList.size() == 0)
                return null;
            return ipList;
        } catch (SocketException ex) {
            return null;
        }
    }

    public static String getNetworkTypeName(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null) ? info.getTypeName() : null; // cmwap/cmnet/wifi/uniwap/uninet
        } catch (Exception e) {
            return null;
        }
    }

    public static int getNetworkType(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null) ? info.getType() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getWifiInfoMacAddress(Context ctx) {
        try {
            WifiManager wifi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            return info.getMacAddress();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getNetworkTypeDetailName(Context context) {
        int type = getNetworkType(context);

        switch (type) {
            case ConnectivityManager.TYPE_WIFI:
                return "WIFI";
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                int telephonyType = -1;
                try {
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    telephonyType = tm.getNetworkType();
                } catch (Exception e) {
                }
                switch (telephonyType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        return "GPRS";
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        return "EDGE";
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        return "UMTS";
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                        return "HSDPA";
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                        return "HSUPA";
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                        return "HSPA";
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        return "CDMA";
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        return "CDMA - EvDo rev. 0";
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        return "CDMA - EvDo rev. A";
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        return "CDMA - EvDo rev. B";
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                        return "CDMA - 1xRTT";
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        return "LTE";
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                        return "CDMA - eHRPD";
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        return "iDEN";
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        return "HSPA+";
                    case TelephonyManager.NETWORK_TYPE_GSM:
                        return "GSM";
                    case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                        return "TD_SCDMA";
                    case TelephonyManager.NETWORK_TYPE_IWLAN:
                        return "IWLAN";
                    case 19://Current network is LTE_CA
                        return "LTE_CA";
                    default:
                        return "UNKNOWN";
                }
            default:
                return "UNKNOWN";
        }
    }

    public static boolean pingRemoteIp(String host, int port) {
        final int CONNECT_TIMEOUT = 5 * 1000;

        SocketAddress socketAddress = new InetSocketAddress(host, port);
        Socket socket = new Socket();
        try {
            socket.connect(socketAddress, CONNECT_TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public static boolean pingRemoteUrl(String urlString, int timeout) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
            urlConnect.setConnectTimeout(timeout);
            urlConnect.getResponseCode();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void removeNetwork(Context context, String ssid) {
        if (TextUtils.isEmpty(ssid))
            return;

        try {
            WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<WifiConfiguration> configs = wifiMgr.getConfiguredNetworks();
            if (configs == null)
                return;

            boolean removed = false;
            for (WifiConfiguration config : configs) {
                String configuredSsid = normalizeSsid(config.SSID);
                if (!ssid.equals(configuredSsid))
                    continue;

                if (Build.VERSION.SDK_INT == AndroidHelper.ANDROID_VERSION_CODE.LOLLIPOP && config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
                    Logger.v(TAG, "Remove specified ssid with forget method, id:" + config.networkId + ", ssid:" + config.SSID);
                    forgetNetwork(wifiMgr, config.networkId);
                } else {
                    Logger.v(TAG, "Remove specified ssid with remove method, id:" + config.networkId + ", ssid:" + config.SSID);
                    wifiMgr.removeNetwork(config.networkId);
                }
                removed = true;
            }

            if (removed)
                wifiMgr.saveConfiguration();
        } catch (Exception e) {
        }
    }

    public static void forgetNetwork(WifiManager manager, int netId) {
        try {
            Class<?> clazz = Class.forName("android.net.wifi.WifiManager$ActionListener");
            Method method = WifiManager.class.getMethod("forget", int.class, clazz);
            method.invoke(manager, netId, null);
            Logger.v(TAG, "invoked hide method: " + method);
        } catch (Exception e) {
            Logger.v(TAG, "" + e);
        }
    }

    public static boolean checkNetworkConnectedOrConnecting(Context context, int type) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(type);
            return networkInfo == null ? false : networkInfo.isConnectedOrConnecting();
        } catch (Throwable e) {
            Logger.w(TAG, "checkNetworkConnectedOrConnecting failed:" + e);
        }
        return false;
    }

    private static String normalizeSsid(String SSID) {
        if (SSID == null)
            return null;
        if (TextUtils.equals("\"", SSID))
            return SSID;

        int index = SSID.indexOf('\"');
        int lastIndex = SSID.lastIndexOf('\"');

        return index == 0 && lastIndex == SSID.length() - 1 ? SSID.substring(1, SSID.length() - 1) : SSID;
    }

    public static String getNetwork(Pair<Boolean, Boolean> network) {
        if (network == null)
            return null;
        else if (network.second)
            return "Wifi";
        else if (network.first)
            return "Data";
        else
            return "No network";
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            Pair<Boolean, Boolean> pair = checkConnected(context);
            return pair.first || pair.second;
        }
        return false;
    }

    public enum NetworkTong {
        TONG("TONG"), BLOCK("BLOCK"), UNKNOWN("UNKNOWN");
        private String mValue;

        NetworkTong(String value) {
            mValue = value;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    /**
     * 0: disable network tong
     * 1: echo success, echo failed, ping good, ping bad, known
     * 2: echo success, ping good,
     * 3: ping good, echo success, echo failed, ping bad
     */
    private static int checkSeq = -1;

    public static NetworkTong checkNetworkTong() {
        if (checkSeq == -1) {
            checkSeq = CloudConfig.getIntConfig(ObjectStore.getContext(), "net_tong_seq", 0);
        }
        if (checkSeq == 0)
            return NetworkTong.UNKNOWN;

        EchoServerHelper.Result echoResult = EchoServerHelper.getLastResult();
        Ping.EvaluateDetail pingResult = Ping.getLastEvaluateDetail();
        if (checkSeq == 1) {
            if (echoResult != null)
                return echoResult.result ? NetworkTong.TONG : NetworkTong.BLOCK;
            if (pingResult.result == Ping.EvaluateResult.Perfect || pingResult.result == Ping.EvaluateResult.Passable)
                return NetworkTong.TONG;
            else if (pingResult.result == Ping.EvaluateResult.Bad)
                return NetworkTong.BLOCK;
            return NetworkTong.UNKNOWN;
        }
        if (checkSeq == 2) {
            if (echoResult != null && echoResult.result)
                return NetworkTong.TONG;
            else if (pingResult.result == Ping.EvaluateResult.Perfect || pingResult.result == Ping.EvaluateResult.Passable)
                return NetworkTong.TONG;
            else if (echoResult != null || pingResult.result == Ping.EvaluateResult.Bad)
                return NetworkTong.BLOCK;
            return NetworkTong.UNKNOWN;
        }
        if (checkSeq == 3) {
            if (pingResult.result == Ping.EvaluateResult.Perfect || pingResult.result == Ping.EvaluateResult.Passable)
                return NetworkTong.TONG;
            if (echoResult != null && echoResult.result)
                return NetworkTong.TONG;
            if (echoResult != null || pingResult.result == Ping.EvaluateResult.Bad)
                return NetworkTong.BLOCK;
            return NetworkTong.UNKNOWN;
        }
        if (checkSeq == 4) {
            if (echoResult != null)
                return echoResult.result ? NetworkTong.TONG : NetworkTong.BLOCK;

            if (pingResult.pinNetResult == Ping.PingNetResult.Available)
                return NetworkTong.TONG;
            if (pingResult.pinNetResult == Ping.PingNetResult.Unavailable)
                return NetworkTong.BLOCK;
            return NetworkTong.UNKNOWN;
        }
        if (checkSeq == 5) {
            if (echoResult != null && echoResult.result)
                return NetworkTong.TONG;
            if (pingResult.pinNetResult == Ping.PingNetResult.Available)
                return NetworkTong.TONG;
            else if (echoResult != null || pingResult.pinNetResult == Ping.PingNetResult.Unavailable)
                return NetworkTong.BLOCK;
            return NetworkTong.UNKNOWN;
        }

        return NetworkTong.UNKNOWN;
    }
}
