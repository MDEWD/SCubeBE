package com.scube.scubebackend.model.dto;

import lombok.Data;

/**
 * 检查二维码扫描状态响应DTO
 */
@Data
public class CheckTicketResponse {
    /**
     * 是否已扫描
     */
    private Boolean scanned;
    
    /**
     * 微信用户信息（扫描后才有值）
     */
    private WeChatUserInfo userInfo;
}

