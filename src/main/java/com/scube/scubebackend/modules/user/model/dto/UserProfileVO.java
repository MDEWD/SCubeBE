package com.scube.scubebackend.modules.user.model.dto;

import lombok.Data;

@Data
public class UserProfileVO {
    private String id;
    private String name;
    private String email;
    private String role;
    private String avatar;
    private String joinDate;
}
