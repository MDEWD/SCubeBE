package com.scube.scubebackend.modules.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderCreateRequest {

    @NotBlank(message = "userDisplayId不能为空")
    private String userDisplayId;

    private String customerName;

    private String customerProduct;

    @NotBlank(message = "productId不能为空")
    private String productId;

    private Integer customerProductQuantity;

    private BigDecimal customerAmount;

    private BigDecimal customerTotalAmount;

    @NotBlank(message = "startDate不能为空")
    private String startDate; // yyyy-MM-dd

    @NotBlank(message = "endDate不能为空")
    private String endDate; // yyyy-MM-dd

    private String paymentMethod;

    private String contact;

    private String supplierName;

    @NotBlank(message = "supplierDisplayId不能为空")
    private String supplierDisplayId;

    private String remark;
}
