package com.scube.scubebackend.model.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserVO user;
}

