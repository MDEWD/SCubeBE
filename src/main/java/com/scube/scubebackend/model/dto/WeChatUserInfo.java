package com.scube.scubebackend.model.dto;

import lombok.Data;

/**
 * 微信用户信息DTO
 */
@Data
public class WeChatUserInfo {
    /**
     * 用户的openId
     */
    private String openId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatar;
}

