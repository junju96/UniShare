package com.ushareit.core.algo;

import com.ushareit.core.lang.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ShaUtil {
    //TODO: 私钥之后需要放到组件公共数据层
    public static final String SHA_KEY = "bc99961bfd2e1a0887c591487";

    /**
     * SHA加密数据
     * @param key 私钥
     * @param data 加密字符串
     * @return 返回加密结果
     */
    public static String HMACSHA256(String key, String data){
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            byte[] PRIVATE_KEY = StringUtils.stringToByte(key);
            SecretKeySpec secretKey = new SecretKeySpec(PRIVATE_KEY, "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] array = sha256HMAC.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
            }
            String aesKey = sb.toString().toLowerCase();
            return aesKey.substring(25, 41);
        }catch (Exception exception){

        }
        return null;
    }
}
