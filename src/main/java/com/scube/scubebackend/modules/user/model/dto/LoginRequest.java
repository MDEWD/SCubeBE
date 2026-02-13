package com.scube.scubebackend.modules.user.model.dto;

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

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}

