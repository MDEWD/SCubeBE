package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class CustomerOrdersResponse {
    private List<CustomerOrderVO> list;

    @Data
    public static class CustomerOrderVO {
        private String id;
        private String customerName;
        private String customerProduct;
        private String productId;
        private Integer customerProductQuantity;
        private String startDate;
        private String endDate;
        private String paymentMethod;
        private String contact;
        private Double customerAmount;
        private Double customerTotalAmount;
        private String remarks;
    }
}
