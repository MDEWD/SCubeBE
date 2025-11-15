package com.scube.scubebackend.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 500, message = "评论内容长度必须在1-500之间")
    private String content;
    
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分至少为1")
    @Max(value = 5, message = "评分最多为5")
    private Integer rating;
}

