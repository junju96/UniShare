package com.ushareit.core.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.SparseArray;

import com.ushareit.core.change.ChangeListenerManager;
import com.ushareit.core.change.ChangedKeys;
import com.ushareit.core.change.ChangedListener;
import com.ushareit.core.lang.DynamicValue;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.device.IMSUtils;

public class NetworkStatus {
    private static final String TAG = "NetworkStatus";
    public static enum NetType {
        UNKNOWN(0), OFFLINE(1), WIFI(2), MOBILE(3);

        private int mValue;

        NetType(int value) {
            mValue = value;
        }

        private final static SparseArray<NetType> VALUES = new SparseArray<NetType>();
        static {
            for (NetType item : NetType.values())
                VALUES.put(item.mValue, item);
        }

        public static NetType fromInt(int value) {
            return VALUES.get(value);
        }

        public int getValue() {
            return mValue;
        }
    }

    private static ShareitHotInterface mShareitHotInterface;

    public interface ShareitHotInterface{
        boolean isShareitHot(String ipAddress, String ssid);
    }

    public static void setShareitHotInterface (ShareitHotInterface shareitHotInterface){
        mShareitHotInterface = shareitHotInterface;
    }

    public static enum MobileDataType {
        UNKNOWN(0), MOBILE_2G(1), MOBILE_3G(2), MOBILE_4G(3);

        private int mValue;

        MobileDataType(int value) {
            mValue = value;
        }

        private final static SparseArray<MobileDataType> VALUES = new SparseArray<MobileDataType>();
        static {
            for (MobileDataType item : MobileDataType.values())
                VALUES.put(item.mValue, item);
        }

        public static MobileDataType fromInt(int value) {
            return VALUES.get(value);
        }

        public int getValue() {
            return mValue;
        }
    }

    private NetType mNetType;
    private String mNetTypeDetail;
    private MobileDataType mMobileDataType;
    private String mCarrier;
    private String mName;
    private String mNumeric;
    private Boolean mIsWifiHot = false;
    private boolean mIsConnected = true;

    protected NetworkStatus(NetType netType, MobileDataType mobileDataType, String carrier, String name, String numeric) {
        mNetType = netType;
        mMobileDataType = mobileDataType;
        mCarrier = carrier;
        mName = name;
        mNumeric = numeric;
    }

    public NetType getNetType() {
        return mNetType;
    }

    /**
     * @return 手机网络状态
     */
    public String getNetTypeDetail(){
        return mNetTypeDetail;
    }

    /**
     * 判断应用在后台被省电断网, 手机无网也是true，只有手机有网且应用被断网时是false
     */
    public boolean isIsConnected(){
        return mIsConnected;
    }

    /**
     * 仅用作事件采集network参数
     * _TONG 和 _BLOCK 后缀有延迟，不可作为有无网判断
     * 详细解释见：https://doc.ushareit.me/web/#/28?page_id=1504
     */
    public String getNetTypeDetailForStats(){
        if(NetType.OFFLINE.equals(mNetType))
            return mNetTypeDetail;
        String isConnected;
        if(mIsConnected)
            isConnected = "_CONNECT";
        else
            isConnected = "_OFFLINE";
        String isTong;
        NetUtils.NetworkTong tong = NetUtils.checkNetworkTong();
        if(tong == NetUtils.NetworkTong.UNKNOWN){
            isTong  = "";
        } else {
            isTong = "_" + tong.toString();
        }
        return mNetTypeDetail + isConnected + isTong;
    }

    public MobileDataType getMobileDataType() {
        return mMobileDataType;
    }

    public String getCarrier() {
        return mCarrier;
    }
    public String getNumeric() {
        return mNumeric;
    }

    public String getNetworkName() {
        return mName;
    }

    private static DynamicValue sNetworkStatus;

    private static ChangedListener mOnNetworkChangedListener = new ChangedListener() {
        @Override
        public void onListenerChange(String key, Object value) {
            if (sNetworkStatus != null)
                sNetworkStatus.updateValue(NetworkStatus.getNetworkStatus(ObjectStore.getContext()));
        }
    };

    public static NetworkStatus getNetworkStatusEx(Context context) {
        if (sNetworkStatus == null) {
            ChangeListenerManager.getInstance().unregisterChangedListener(ChangedKeys.KEY_CONNECTIVITY_CHANGE, mOnNetworkChangedListener);
            sNetworkStatus = new DynamicValue(NetworkStatus.getNetworkStatus(context), true, 1000);
            ChangeListenerManager.getInstance().registerChangedListener(ChangedKeys.KEY_CONNECTIVITY_CHANGE, mOnNetworkChangedListener);
        }
        else if (sNetworkStatus.isNeedUpdate())
            sNetworkStatus.updateValue(NetworkStatus.getNetworkStatus(context));

        return (NetworkStatus)sNetworkStatus.getObjectValue();
    }

    public static NetworkStatus getNetworkStatus(Context context) {
        TelephonyManager telManager = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        ConnectivityManager connManager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkStatus status = new NetworkStatus(NetType.OFFLINE, MobileDataType.UNKNOWN, null, null, null);
        if (telManager == null || connManager == null) {
            status.mNetTypeDetail = getNetTypeDetail(status);
            return status;
        }

        // get mobile network operator
        status.mCarrier = telManager.getSimOperatorName();
        status.mNumeric = telManager.getSimOperator();
        if (status.mCarrier == null || status.mCarrier.length() <= 0 || status.mCarrier.equals("null"))
            status.mCarrier = IMSUtils.getQualcommCardName();

        NetworkInfo networkInfo = null;
        try {
            networkInfo = connManager.getActiveNetworkInfo();
        }catch (Exception e){

        }
        if (networkInfo == null || !networkInfo.isAvailable()) {
            status.mNetTypeDetail = getNetTypeDetail(status);
            return status;
        }

        // network is available
        int netType = networkInfo.getType();
        status.mIsConnected = networkInfo.isConnected();
        // check mobile 3G/4G
        if (netType == ConnectivityManager.TYPE_MOBILE) {
            status.mNetType = NetType.MOBILE;
            int netSubtype = telManager.getNetworkType();
            status.mMobileDataType = getNetworkClass(netSubtype);
        }
        // check wifi
        else if (netType == ConnectivityManager.TYPE_WIFI) {
            WifiManager wm = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null) {
                    String ssid = wi.getSSID();
                    status.mName = (ssid != null && ssid.length() > 0) ? ssid : null;
                    String ipAddress = intIP2StringIP(wi.getIpAddress());
                    if(mShareitHotInterface != null && ssid != null)
                        status.mIsWifiHot = mShareitHotInterface.isShareitHot(ipAddress, ssid.replace("\"", ""));
                }
            }
            status.mNetType = NetType.WIFI;
        } else
            // Other network (not mobile or wifi)
            status.mNetType = NetType.UNKNOWN;
        status.mNetTypeDetail = getNetTypeDetail(status);

        return status;
    }

    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    public static NetType getNetworkType(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager == null)
            return NetType.OFFLINE;

        NetworkInfo networkInfo = null;
        try {
            // get mobile network operator
            networkInfo = connManager.getActiveNetworkInfo();
        }catch (Exception e){

        }
        if (networkInfo == null || !networkInfo.isAvailable())
            return NetType.OFFLINE;

        // network is available
        int netType = networkInfo.getType();
        return (netType == ConnectivityManager.TYPE_MOBILE) ? NetType.MOBILE : (netType == ConnectivityManager.TYPE_WIFI ? NetType.WIFI : NetType.UNKNOWN);
    }

    public static String getNetWorkName(Context context) {
        try {
            NetworkStatus networkStatus = getNetworkStatus(context);
            if (networkStatus == null)
                return "UnKnown";
            if (networkStatus.getNetType() == NetType.MOBILE)
                return networkStatus.getMobileDataType() == MobileDataType.UNKNOWN ? "MOBILE_UnKnown" : networkStatus.getMobileDataType().name();
            return networkStatus.getNetType().name();
        } catch (Exception e) {}
        return "UnKnown";
    }

    private static String getNetTypeDetail(NetworkStatus status) {
        switch (status.getNetType()) {
            case OFFLINE:
                return "OFFLINE";
            case WIFI:
                return status.mIsWifiHot ? "WIFI_HOT" : "WIFI";
            case MOBILE:
                switch (status.mMobileDataType) {
                    case MOBILE_2G:
                        return "MOBILE_2G";
                    case MOBILE_3G:
                        return "MOBILE_3G";
                    case MOBILE_4G:
                        return "MOBILE_4G";
                    default:
                        return "MOBILE_UNKNOWN";
                }
            default:
                return "UNKNOWN";
        }
    }

    @SuppressLint("MissingPermission")
    public static boolean isWifiOr3GNetwork(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager == null)
            return false;
        NetworkInfo wifiNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetInfo != null && wifiNetInfo.isConnected())
            return true;

        NetworkInfo mobNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobNetInfo != null && mobNetInfo.isConnected()) {
            TelephonyManager telManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (telManager == null)
                return false;

            MobileDataType mobileDataType = getNetworkClass(telManager.getNetworkType());
            switch (mobileDataType) {
                case MOBILE_2G:
                    return false;
                default:
                    return true;
            }
        }
        return false;
    }

    /** Network type is unknown */
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    /** Current network is GPRS */
    public static final int NETWORK_TYPE_GPRS = 1;
    /** Current network is EDGE */
    public static final int NETWORK_TYPE_EDGE = 2;
    /** Current network is UMTS */
    public static final int NETWORK_TYPE_UMTS = 3;
    /** Current network is CDMA: Either IS95A or IS95B*/
    public static final int NETWORK_TYPE_CDMA = 4;
    /** Current network is EVDO revision 0*/
    public static final int NETWORK_TYPE_EVDO_0 = 5;
    /** Current network is EVDO revision A*/
    public static final int NETWORK_TYPE_EVDO_A = 6;
    /** Current network is 1xRTT*/
    public static final int NETWORK_TYPE_1xRTT = 7;
    /** Current network is HSDPA */
    public static final int NETWORK_TYPE_HSDPA = 8;
    /** Current network is HSUPA */
    public static final int NETWORK_TYPE_HSUPA = 9;
    /** Current network is HSPA */
    public static final int NETWORK_TYPE_HSPA = 10;
    /** Current network is iDen */
    public static final int NETWORK_TYPE_IDEN = 11;
    /** Current network is EVDO revision B*/
    public static final int NETWORK_TYPE_EVDO_B = 12;
    /** Current network is LTE */
    public static final int NETWORK_TYPE_LTE = 13;
    /** Current network is eHRPD */
    public static final int NETWORK_TYPE_EHRPD = 14;
    /** Current network is HSPA+ */
    public static final int NETWORK_TYPE_HSPAP = 15;
    /** Current network is GSM */
    public static final int NETWORK_TYPE_GSM = 16;
    /** Current network is TD_SCDMA */
    public static final int NETWORK_TYPE_TD_SCDMA = 17;
    /** Current network is IWLAN */
    public static final int NETWORK_TYPE_IWLAN = 18;
    /** Current network is LTE_CA  */
    public static final int NETWORK_TYPE_LTE_CA = 19;
    public static NetworkStatus.MobileDataType getNetworkClass(int networkType) {
        switch (networkType) {
            case NETWORK_TYPE_GPRS:
            case NETWORK_TYPE_GSM:
            case NETWORK_TYPE_EDGE:
            case NETWORK_TYPE_CDMA:
            case NETWORK_TYPE_1xRTT:
            case NETWORK_TYPE_IDEN:
                return NetworkStatus.MobileDataType.MOBILE_2G;
            case NETWORK_TYPE_UMTS:
            case NETWORK_TYPE_EVDO_0:
            case NETWORK_TYPE_EVDO_A:
            case NETWORK_TYPE_HSDPA:
            case NETWORK_TYPE_HSUPA:
            case NETWORK_TYPE_HSPA:
            case NETWORK_TYPE_EVDO_B:
            case NETWORK_TYPE_EHRPD:
            case NETWORK_TYPE_HSPAP:
            case NETWORK_TYPE_TD_SCDMA:
                return NetworkStatus.MobileDataType.MOBILE_3G;
            case NETWORK_TYPE_LTE:
            case NETWORK_TYPE_IWLAN:
            case NETWORK_TYPE_LTE_CA:
                return NetworkStatus.MobileDataType.MOBILE_4G;
            default:
                return NetworkStatus.MobileDataType.UNKNOWN;
        }
    }

    public static NetworkStatus.MobileDataType getMobileDataType(Context context) {
        TelephonyManager telManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager == null)
            return NetworkStatus.MobileDataType.UNKNOWN;

        return getNetworkClass(telManager.getNetworkType());
    }


}
