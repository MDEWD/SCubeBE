package com.scube.scubebackend.modules.user.model.dto;

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

    /**
     * 是否需要再走一次网页授权（snsapi_userinfo）才能拿到昵称/头像
     */
    private Boolean needAuthorize;

    /**
     * 当 needAuthorize=true 时，后端返回给前端的授权链接
     */
    private String authorizationUrl;
}
