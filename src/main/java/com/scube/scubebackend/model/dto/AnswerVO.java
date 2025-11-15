package com.scube.scubebackend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerVO {
    private Long id;
    private String content;
    private String authorName;
    private Integer voteCount;
    private Boolean isAccepted;
    private LocalDateTime createTime;
}

