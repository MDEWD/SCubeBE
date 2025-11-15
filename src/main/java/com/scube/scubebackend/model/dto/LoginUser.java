package com.scube.scubebackend.model.dto;

import lombok.Data;

@Data
public class LoginUser {
    private Long id;
    private String openId;
    private String userRole;
}

