package com.scube.scubebackend.modules.product.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDemandRequest {
    @NotBlank(message = "型号不能为空")
    private String model;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotBlank(message = "租期不能为空")
    private String rentalPeriod; // 建议使用 DAYS 或 MONTHS

    @NotNull(message = "预期价格不能为空")
    private BigDecimal expectedPrice;

    private String regionRequirement;

    @NotBlank(message = "联系方式不能为空")
    private String contact;

    private String remark;
}
