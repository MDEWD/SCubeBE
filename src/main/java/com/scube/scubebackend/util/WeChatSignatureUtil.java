package com.scube.scubebackend.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 微信签名验证工具类
 * 用于验证微信服务器回调的签名
 */
public class WeChatSignatureUtil {
    
    /**
     * 验证微信签名
     * 
     * @param signature 微信传来的签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param token 配置的Token
     * @return 验证结果
     */
    public static boolean checkSignature(String signature, String timestamp, String nonce, String token) {
        if (signature == null || timestamp == null || nonce == null || token == null) {
            return false;
        }
        
        // 1. 将token、timestamp、nonce三个参数进行字典序排序
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);
        
        // 2. 将三个参数字符串拼接成一个字符串
        StringBuilder content = new StringBuilder();
        for (String s : arr) {
            content.append(s);
        }
        
        // 3. 对拼接后的字符串进行sha1加密
        String temp = sha1(content.toString());
        
        // 4. 将加密后的字符串与signature对比
        return temp != null && temp.equals(signature);
    }
    
    /**
     * SHA1加密
     */
    private static String sha1(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(str.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

