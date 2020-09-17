package com.ushareit.core.utils.device;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IMSUtils {

    public enum SimType {
        UNKNOW, NO_SIM /* 不支持SIM */, SINGLE_SIM /* 支持单卡 */ , DUAL_SIM /* 支持双卡双待 */
    }

    public enum ActiveState {
        UNKNOW, NO_ACTIVE, SINGLE_ACTIVE, DOUBLE_ACTIVE
    }

    public static class IMSInfo {
        public SimType mSimType = SimType.UNKNOW;
        public ActiveState mActiveState = ActiveState.UNKNOW;
        public String mIMEI1;
        public String mIMEI2;
        public String mIMSI1;
        public String mIMSI2;

        public boolean isAvailable() {
            return mSimType != SimType.UNKNOW && mActiveState != ActiveState.UNKNOW
                    && ((mIMEI1 != null && mIMEI1.length() >= 10) || (mIMEI2 != null && mIMEI2.length() >= 10));
        }

        private boolean checkValueAvaliable(String value) {
            return !TextUtils.isEmpty(value) && value.length() >= 10;
        }

        public String getBetterIMSI() {
            return checkValueAvaliable(mIMSI1) ? mIMSI1 : mIMSI2;
        }

        public String getBetterIMEI() {
            return checkValueAvaliable(mIMEI1) ? mIMEI1 : mIMEI2;
        }

        public List<String> getIMSIList() {
            Set<String> imsiSet = new HashSet<String>();
            if (checkValueAvaliable(mIMSI1)) {
                imsiSet.add(mIMSI1);
            }
            if (checkValueAvaliable(mIMSI2)) {
                imsiSet.add(mIMSI2);
            }
            return new ArrayList<String>(imsiSet);
        }

        public List<String> getIMEIList() {
            List<String> imeiSet = new ArrayList<String>();
            if (checkValueAvaliable(mIMEI1)) {
                imeiSet.add(mIMEI1);
            }
            if (checkValueAvaliable(mIMEI2)) {
                imeiSet.add(mIMEI2);
            }
            return new ArrayList<String>(imeiSet);
        }

        // 当没有接口判断时，根据IMSI为空的情况判断激活状态
        public void updateStateManual() {
            List<String> imsiSet = getIMSIList();
            if (imsiSet.isEmpty()) {
                mActiveState = ActiveState.NO_ACTIVE;
            } else if (imsiSet.size() < 2) {
                mActiveState = ActiveState.SINGLE_ACTIVE;
            } else {
                mActiveState = ActiveState.DOUBLE_ACTIVE;
            }
        }

        // 当没有接口判断时，根据IMEI是否为空的情况判断
        public void upadteTypeManual() {
            List<String> imeiSet = getIMEIList();
            if (imeiSet.isEmpty()) {
                mSimType = SimType.NO_SIM;
            } else if (imeiSet.size() < 2) {
                mSimType = SimType.SINGLE_SIM;
            } else {
                mSimType = SimType.DUAL_SIM;
            }
            // fix 异常情况，如果获取到了两个不同的imsi，一定是支持双卡的
            if (mActiveState == ActiveState.DOUBLE_ACTIVE) {
                mSimType = SimType.DUAL_SIM;
            }
        }

        public String toString() {
            String ret = "";
            if (isAvailable()) {
                ret += "SIM Type: " + mSimType + "\n";
                ret += "Active state: " + mActiveState + "\n";
                ret += "IMEI1: " + mIMEI1 + "\n";
                ret += "IMEI2: " + mIMEI2 + "\n";
                ret += "IMSI1: " + mIMSI1 + "\n";
                ret += "IMSI2: " + mIMSI2 + "\n";
            }
            return ret;
        }
    }

    /**
     * 获取手机SIM信息，先通过反射尝试各芯片的接口，如果失败了使用系统默认接口
     */
    public static IMSInfo getIMSInfo(Context context) {
        if (context == null) {
            return null;
        }
        IMSInfo imsInfo = getMtkDoubleSim(context);
        if (imsInfo != null && imsInfo.isAvailable()) {
            return imsInfo;
        }
        imsInfo = getQualcommDoubleSim(context);
        if (imsInfo != null && imsInfo.isAvailable()) {
            return imsInfo;
        }
        imsInfo = getSpreadDoubleSim(context);
        return (imsInfo != null && imsInfo.isAvailable()) ? imsInfo : getDefaultSim(context);
    }

    /**
     * MTK的芯片
     */
    public static IMSInfo getMtkDoubleSim(Context context) {
        // Default Sim Id, card 1: 0, card 2: 1
        Integer simId1 = 0, simId2 = 1;
        try {
            Class<?> c = Class.forName("com.android.internal.telephony.Phone");
            try {
                Field f = c.getField("GEMINI_SIM_1");
                f.setAccessible(true);
                simId1 = (Integer)f.get(null);
                f.setAccessible(false);
            } catch (Throwable e) {}
            try {
                Field f = c.getField("GEMINI_SIM_2");
                f.setAccessible(true);
                simId2 = (Integer)f.get(null);
                f.setAccessible(false);
            } catch (Throwable e) {}
        } catch (Throwable e) {}

        // Method 1
        IMSInfo imsInfo = new IMSInfo();
        try {
            Class<?> c = Class.forName("android.provider.MultiSIMUtils"); // never null
            Method m = c.getMethod("getDefault", Context.class); // never null
            Object multiSimUtils = m.invoke(c, context);
            if (multiSimUtils != null) {
                Method md = c.getMethod("getDeviceId", int.class); // never null
                Method ms = c.getMethod("getSubscriberId", int.class); // never null
                imsInfo.mSimType = SimType.DUAL_SIM;
                imsInfo.mIMEI1 = (String)md.invoke(multiSimUtils, simId1);
                imsInfo.mIMSI1 = (String) ms.invoke(multiSimUtils, simId1);
                imsInfo.mIMEI2 = (String)md.invoke(multiSimUtils, simId2);
                imsInfo.mIMSI2 = (String) ms.invoke(multiSimUtils, simId2);
                imsInfo.updateStateManual();
                imsInfo.upadteTypeManual();
            }
        } catch (Throwable e) {}

        if (imsInfo.isAvailable()) {
            return imsInfo;
        }

        TelephonyManager tm = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null)
            return null;

        // Method 2
        try {
            Method md = TelephonyManager.class.getDeclaredMethod("getDeviceIdGemini", int.class);
            Method ms = TelephonyManager.class.getDeclaredMethod("getSubscriberIdGemini", int.class);
            imsInfo.mSimType = SimType.DUAL_SIM;
            imsInfo.mIMEI1 = (String)md.invoke(tm, simId1);
            imsInfo.mIMSI1 = (String) ms.invoke(tm, simId1);
            imsInfo.mIMEI2 = (String)md.invoke(tm, simId2);
            imsInfo.mIMSI2 = (String) ms.invoke(tm, simId2);
            imsInfo.updateStateManual();
            imsInfo.upadteTypeManual();
        } catch (Throwable e) {}
        if (imsInfo.isAvailable())
            return imsInfo;

        // Method 3
        try {
            Class<?> telephonyClass = Class.forName(tm.getClass().getName());
            Method m = telephonyClass.getMethod("getDefault", int.class); // never null
            TelephonyManager tm1 = (TelephonyManager)m.invoke(tm, simId1);
            TelephonyManager tm2 = (TelephonyManager)m.invoke(tm, simId2);
            if (tm1 != null && tm2 != null) {
                imsInfo.mSimType = SimType.DUAL_SIM;
                imsInfo.mIMEI1 = tm1.getDeviceId();
                imsInfo.mIMSI1 = tm1.getSubscriberId();
                imsInfo.mIMEI2 = tm2.getDeviceId();
                imsInfo.mIMSI2 = tm2.getSubscriberId();
                imsInfo.updateStateManual();
                imsInfo.upadteTypeManual();
            }
        } catch (Throwable e) {}
        return imsInfo;
    }

    /**
     * 高通芯片
     */
    public static IMSInfo getQualcommDoubleSim(Context context) {
        IMSInfo imsInfo = new IMSInfo();
        try {
            Class<?> c = Class.forName("android.telephony.MSimTelephonyManager"); // never null
            Object obj = context.getApplicationContext().getSystemService("phone_msim");
            if (obj == null)
                return null;
            Method md = c.getMethod("getDeviceId", int.class); // never null
            Method ms = c.getMethod("getSubscriberId", int.class); // never null
//            Method mx = c.getMethod("getPreferredDataSubscription", int.class);
//            Method is = c.getMethod("isMultiSimEnabled", int.class);
            Integer simId1 = 0, simId2 = 1;
            imsInfo.mSimType = SimType.DUAL_SIM;
            imsInfo.mIMEI1 = (String)md.invoke(obj, simId1);
            imsInfo.mIMSI1 = (String) ms.invoke(obj, simId1);
            imsInfo.mIMEI2 = (String)md.invoke(obj, simId2);
            imsInfo.mIMSI2 = (String) ms.invoke(obj, simId2);
            imsInfo.updateStateManual();
            imsInfo.upadteTypeManual();
        } catch (Throwable e) {}
        return imsInfo;
    }

    public static String getQualcommCardName() {
        try {
            Class<?> c = Class.forName("android.telephony.MSimTelephonyManager"); // never null
            Method m = c.getDeclaredMethod("getTelephonyProperty", String.class, int.class, String.class);
            return (String)m.invoke(c, "gsm.operator.alpha", 0, null);
        } catch (Throwable e) {}
        return null;
    }

    /**
     * 展讯芯片
     */
    public static IMSInfo getSpreadDoubleSim(Context context) {
        IMSInfo imsInfo = new IMSInfo();
        try {
            Class<?> c = Class.forName("com.android.internal.telephony.PhoneFactory"); // never null
            Method m = c.getMethod("getServiceName", String.class, int.class); // never null
            String spreadTMService = (String)m.invoke(c, Context.TELEPHONY_SERVICE, 1);
            if (spreadTMService == null || spreadTMService.length() == 0) {
                return imsInfo;
            }
            TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                imsInfo.mSimType = SimType.SINGLE_SIM;
                imsInfo.mIMEI1 = tm.getDeviceId();
                imsInfo.mIMSI1 = tm.getSubscriberId();
            }
            TelephonyManager spreadTM = (TelephonyManager) context.getApplicationContext().getSystemService(spreadTMService);
            if (spreadTM != null) {
                imsInfo.mSimType = SimType.DUAL_SIM;
                imsInfo.mIMEI2 = spreadTM.getDeviceId();
                imsInfo.mIMSI2 = spreadTM.getSubscriberId();
            }
            imsInfo.updateStateManual();
            imsInfo.upadteTypeManual();
        } catch (Throwable a) {}
        return imsInfo;
    }


    /**
     * 通过系统API获取
     */
    public static IMSInfo getDefaultSim(Context context) {
        TelephonyManager tm = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return null;
        }
        IMSInfo imsInfo = new IMSInfo();
        try {
            imsInfo.mIMEI1 = getDefaultImei(tm, 0);
            imsInfo.mIMEI2 = getDefaultImei(tm, 1);
            imsInfo.mIMSI1 = getDefaultImsi(context, tm, 0);
            imsInfo.mIMSI2 = getDefaultImsi(context, tm, 1);
            imsInfo.mSimType = getSimType(tm);
            imsInfo.mActiveState = getSimState(context, tm);
            if (imsInfo.mActiveState == ActiveState.UNKNOW) {
                imsInfo.updateStateManual();
            }
            if (imsInfo.mSimType == SimType.UNKNOW) {
                imsInfo.upadteTypeManual();
            }
        } catch (Exception e) {}
        return imsInfo;
    }

    private static SimType getSimType(TelephonyManager tm) {
        SimType simType = SimType.UNKNOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int phoneCount = tm.getPhoneCount();
            switch (phoneCount) {
                case 0:
                    simType = SimType.NO_SIM;
                    break;
                case 1:
                    simType = SimType.SINGLE_SIM;
                    break;
                case 2:
                    simType = SimType.DUAL_SIM;
                    break;
                default:
                    simType = SimType.NO_SIM;
            }
        }
        return simType;
    }

    private static ActiveState getSimState(Context context, TelephonyManager tm) {
        ActiveState state = ActiveState.UNKNOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int sim1State = tm.getSimState(0);
            int sim2State = tm.getSimState(1);
            if (sim1State == TelephonyManager.SIM_STATE_READY
                    && sim2State == TelephonyManager.SIM_STATE_READY) {
                state = ActiveState.DOUBLE_ACTIVE;
            } else if (sim1State == TelephonyManager.SIM_STATE_READY ||
                    sim2State == TelephonyManager.SIM_STATE_READY) {
                state = ActiveState.SINGLE_ACTIVE;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = (SubscriptionManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            int activeCount = sm.getActiveSubscriptionInfoCount();
            switch (activeCount) {
                case 0:
                    state = ActiveState.NO_ACTIVE;
                    break;
                case 1:
                    state = ActiveState.SINGLE_ACTIVE;
                    break;
                case 2:
                    state = ActiveState.DOUBLE_ACTIVE;
                    break;
                default:
                    state = ActiveState.NO_ACTIVE;
            }
        }
        return state;
    }

    private static String getDefaultImei(TelephonyManager tm, int slotIndex) {
        String imei = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            imei = tm.getImei(slotIndex);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            imei = tm.getDeviceId(slotIndex);
        } else {
            // getDeviceId只能获取第一个卡槽的
            imei = slotIndex == 0 ? tm.getDeviceId() : null;
        }
        return imei;
    }

    private static String getDefaultImsi(Context context, TelephonyManager tm, int slotIndex) {
        String imsi = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(slotIndex);
                if (subscriptionInfo != null) {
                    int subId = subscriptionInfo.getSubscriptionId();
                    imsi = tm.createForSubscriptionId(subId).getSubscriberId();
                }
            } else {
                Method ms = TelephonyManager.class.getMethod("getSubscriberId", int.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SubscriptionManager sm = (SubscriptionManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    SubscriptionInfo subscriptionInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(slotIndex);
                    if (subscriptionInfo != null) {
                        imsi = (String) ms.invoke(tm, subscriptionInfo.getSubscriptionId()); // Sim slot 1 IMSI
                    }
                } else {
                    imsi = (String) ms.invoke(tm, slotIndex);
                }
                return imsi;
            }
        } catch(Exception e){
            imsi = (slotIndex == 0) ? tm.getSubscriberId() : null;
        }
        return imsi;
    }

}