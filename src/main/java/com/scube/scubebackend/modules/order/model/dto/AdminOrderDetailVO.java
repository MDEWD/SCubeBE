package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderDetailVO {
    private Long id;

    private String orderNo;

    private Long userId;

    private String userDisplayId;

    private String customerName;

    private String customerProduct;

    private String productId;

    private Integer customerProductQuantity;

    private BigDecimal customerAmount;

    private BigDecimal customerTotalAmount;

    private String startDate;

    private String endDate;

    private String paymentMethod;

    private String contact;

    private String supplierName;

    private Long supplierId;

    private String supplierDisplayId;

    private String remark;

    private String status;

    private String createTime;

    private String updateTime;
}
