package com.company.wechat.util;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 企业微信签名验证工具
 * 
 * @author Company
 */
public class WechatSignUtil {

    /**
     * 验证签名
     * 
     * @param token 企业微信Token
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param encrypt 加密内容
     * @param signature 待验证的签名
     * @return 是否验证通过
     */
    public static boolean verifySignature(String token, String timestamp, String nonce, String encrypt, String signature) {
        String computedSignature = computeSignature(token, timestamp, nonce, encrypt);
        return computedSignature != null && computedSignature.equals(signature);
    }

    /**
     * 计算签名
     * 
     * @param token 企业微信Token
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param encrypt 加密内容
     * @return 签名
     */
    public static String computeSignature(String token, String timestamp, String nonce, String encrypt) {
        try {
            String[] params = {token, timestamp, nonce, encrypt};
            Arrays.sort(params);
            
            StringBuilder sb = new StringBuilder();
            for (String param : params) {
                sb.append(param);
            }
            
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(sb.toString().getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
