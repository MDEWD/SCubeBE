package com.scube.scubebackend.modules.admin.model.dto;

import lombok.Data;

@Data
public class AdminUserVO {
    private String id;
    private String name;
    private String email;
    private String role;
    private String joinDate;
}
