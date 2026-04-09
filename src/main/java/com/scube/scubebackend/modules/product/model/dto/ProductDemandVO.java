package com.scube.scubebackend.modules.product.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDemandVO {
    private Long id;
    private Long userId;
    private String userDisplayId;
    private String DemandId;
    private String model;
    private Integer quantity;
    private Integer rentalPeriodValue;
    private String rentalPeriod;
    private BigDecimal expectedPrice;
    private String regionRequirement;
    private String contact;
    private String remark;
    private LocalDateTime createTime;
}
