package com.ushareit.core.algo;

import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.device.DeviceHelper;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ABTestUtils {

    private static final long DIVISOR = 4294967296L;
    private static Map<String, Integer> sTestIdCache = new HashMap<>();
    private static Map<String, String> sTestCaseCache = new HashMap<>();

    public synchronized static <T> T getABTestCase(String cloudConfig, AbCaseGenerator<T> generator) {
        T value = generator.generateAbCaseViaCloudValue();
        if (value == null) {
            value = generator.generateAbCaseViaTestId(getABTestId(cloudConfig));
        }

        if (generator.recordToAbStats()) {
            String abCase = null;
            if (value != null)
                abCase = value.toString();

            sTestCaseCache.put(cloudConfig, abCase);
            sAbCaseListChanged = true;
        }
        return value;
    }

    private static String sAbTestCaseString = null;
    private static boolean sAbCaseListChanged = false;

    public synchronized static String getAbCaseList() {
        if (sAbTestCaseString == null || sAbCaseListChanged) {
            if (sTestCaseCache.isEmpty()) {
                sAbTestCaseString = "";
            } else {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                int size = sTestCaseCache.size();
                for (Map.Entry<String, String> entry : sTestCaseCache.entrySet()) {
                    builder.append(entry.getKey() + ":" + entry.getValue());
                    if (i < size - 1)
                        builder.append(",");
                    i++;
                }
                return sAbTestCaseString = builder.toString();
            }
        }
        return sAbTestCaseString;
    }

    private static int getABTestId(String cloudConfig) {
        if (cloudConfig == null)
            cloudConfig = "";

        if (sTestIdCache.containsKey(cloudConfig))
            return sTestIdCache.get(cloudConfig);
        String deviceId = DeviceHelper.getOrCreateDeviceId(ObjectStore.getContext());
        String baseStr = deviceId + cloudConfig;
        String md5 = MD5(baseStr);
        long hashCode = calABTestHashCode(md5);
        int testId;
        if (hashCode >= 0 && hashCode < 100)
            testId = (int) hashCode;
        else if (hashCode > -100 && hashCode < 0)
            testId = Math.abs((int) hashCode);
        else {
            String str = String.valueOf(hashCode);
            int len = str.length();
            testId = Integer.parseInt(str.substring(len - 2, len));
        }
        testId += 1;
        sTestIdCache.put(cloudConfig, testId);
        return testId;
    }

    private static long calABTestHashCode(String value) {
        long result = 0;
        int len = value.length();
        if (len == 0)
            return result;

        int prime = 31;
        for (int i = 0; i < len; i++) {
            int item = value.charAt(i);
            result = result * prime + item;
            result = result % DIVISOR;
        }

        return result;
    }

    private static String MD5(String sourceStr) {
        try {
            // 获得MD5摘要算法的 MessageDigest对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(sourceStr.getBytes());
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < md.length; i++) {
                int tmp = md[i];
                if (tmp < 0)
                    tmp += 256;
                if (tmp < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(tmp));
            }
            return buf.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Integer sABRatio;

    public static int getRatioValueViaDeviceId() {
        if (sABRatio != null)
            return sABRatio;

        long hashCode = calABTestHashCode(DeviceHelper.getOrCreateDeviceId(ObjectStore.getContext()));
        if (hashCode >= 0 && hashCode < 100)
            sABRatio = (int) hashCode;
        else if (hashCode > -100 && hashCode < 0)
            sABRatio = Math.abs((int) hashCode);
        else {
            String str = String.valueOf(hashCode);
            int len = str.length();
            sABRatio = Integer.parseInt(str.substring(len - 2, len));
        }
        return sABRatio;
    }

    public interface AbCaseGenerator<T> {
        T generateAbCaseViaCloudValue();

        T generateAbCaseViaTestId(int testId);

        boolean recordToAbStats();
    }
}
