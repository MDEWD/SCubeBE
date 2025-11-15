package com.scube.scubebackend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String openId;
    private String nickname;
    private String avatar;
    private String userRole;
    private LocalDateTime createTime;
}

