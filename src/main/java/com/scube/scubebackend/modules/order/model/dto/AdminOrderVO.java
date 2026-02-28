package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;

@Data
public class AdminOrderVO {
    private String id;
    private String customerName;
    private String customerProduct;
    private String customerStartDate;
    private String customerEndDate;
    private String customerPaymentMethod;
    private String customerContact;
    private String supplierName;
    private String supplierStartDate;
    private String supplierEndDate;
    private String supplierPaymentMethod;
    private String supplierContact;
}
