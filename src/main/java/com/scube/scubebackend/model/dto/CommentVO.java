package com.scube.scubebackend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private String userName;
    private String avatar;
    private String content;
    private Integer rating;
    private Integer likes;
    private Integer dislikes;
    private LocalDateTime createTime;
}

