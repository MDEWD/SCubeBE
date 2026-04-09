package com.scube.scubebackend.modules.product.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteRequest {
    @NotBlank(message = "productId不能为空")
    private String productId;
}

