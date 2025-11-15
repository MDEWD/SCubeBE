package com.scube.scubebackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "操作不能为空")
    private String action; // like or dislike
}

