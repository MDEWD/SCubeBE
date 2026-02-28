package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;

@Data
public class CustomerOrderVO {
    private String id;
    private String customerName;
    private String productName;
    private Integer productQuantity;
    private String startTime;
    private String endTime;
    private Double amount;
    private Double totalAmount;
    private String paymentMethod;
    private String contact;
    private String remarks;
    private String status;
}
