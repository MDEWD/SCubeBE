package com.scube.scubebackend.modules.admin.model.dto;

import lombok.Data;

@Data
public class AdminUserVO {
    private String id;
    private String displayId;
    private String nickname;
    private String email;
    private String userRole;
    private String joinDate;
}
