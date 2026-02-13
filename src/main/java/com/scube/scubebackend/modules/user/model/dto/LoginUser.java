package com.scube.scubebackend.modules.user.model.dto;

import lombok.Data;

@Data
public class LoginUser {
    private Long id;
    private String displayId;
    private String openId;
    private String userRole;
}

