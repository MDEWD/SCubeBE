package com.scube.scubebackend.modules.order.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PartnerOrdersResponse {

    private PartnerStatsVO stats;
    private List<PartnerOrderVO> list;

    @Data
    public static class PartnerStatsVO {
        private String monthSettlement;
        private Integer activeContracts;
    }

    @Data
    public static class PartnerOrderVO {
        private String id;
        private String supplierName;
        private String dateStart;
        private String dateEnd;
        private String settlementAmount;
        private String paymentMethod;
        private String contact;
    }
}
