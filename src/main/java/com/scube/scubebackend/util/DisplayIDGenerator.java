package com.scube.scubebackend.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class DisplayIDGenerator {
    
    // 字符集：数字 + 大写字母
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    private static final int USER_ID_LENGTH = 6;
    
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 生成6位唯一DisplayID，包含数字和大写字母
     * @return 6位唯一DisplayID
     */
    public String generateDisplayID() {
        StringBuilder sb = new StringBuilder(USER_ID_LENGTH);
        for (int i = 0; i < USER_ID_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
    
    /**
     * 检查DisplayID是否有效（6位数字+大写字母）
     * @param displayId 要检查的DisplayID
     * @return 是否有效
     */
    public boolean isValidDisplayID(String displayId) {
        if (displayId == null || displayId.length() != USER_ID_LENGTH) {
            return false;
        }
        for (char c : displayId.toCharArray()) {
            if (!CHARACTERS.contains(String.valueOf(c))) {
                return false;
            }
        }
        return true;
    }
}