package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderUpdateRequest {

    private String userDisplayId;

    private String customerName;

    private String customerProduct;

    private String productId;

    private Integer customerProductQuantity;

    private BigDecimal customerAmount;

    private BigDecimal customerTotalAmount;

    private String startDate; // yyyy-MM-dd

    private String endDate; // yyyy-MM-dd

    private String paymentMethod;

    private String contact;

    private String supplierName;

    private String supplierDisplayId;

    private String remark;

    private String status;
}
