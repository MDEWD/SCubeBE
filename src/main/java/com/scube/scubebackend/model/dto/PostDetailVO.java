package com.scube.scubebackend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDetailVO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private List<String> tags;
    private Integer voteCount;
    private Integer answerCount;
    private Integer viewCount;
    private List<AnswerVO> answers;
    private LocalDateTime createTime;
}

