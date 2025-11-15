package com.scube.scubebackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "验证码不能为空")
    private String code;
    
    @NotBlank(message = "openId不能为空")
    private String openId;
}

