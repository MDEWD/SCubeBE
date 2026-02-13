package com.scube.scubebackend.modules.user.model.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserVO user;
}

