package com.scube.scubebackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "验证码不能为空")
    private String code;
    
    @NotBlank(message = "openId不能为空")
    private String openId;
    
    /**
     * 用户昵称（可选，如果提供则更新到数据库）
     */
    private String nickname;
    
    /**
     * 用户头像URL（可选，如果提供则更新到数据库）
     */
    private String avatar;
}

