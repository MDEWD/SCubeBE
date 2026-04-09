package com.scube.scubebackend.modules.user.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String displayId;
    private String openId;
    private String nickname;
    private String avatar;
    private String userRole;
    private LocalDateTime createTime;
}

