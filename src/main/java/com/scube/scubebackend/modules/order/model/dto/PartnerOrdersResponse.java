package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PartnerOrdersResponse {
    private List<PartnerOrderVO> list;

    @Data
    public static class PartnerOrderVO {
        private String id;
        private String productId;
        private String supplierName;
        private String startDate;
        private String endDate;
        private Double customerAmount;
        private Double customerTotalAmount;
        private String paymentMethod;
        private String contact;
    }
}
