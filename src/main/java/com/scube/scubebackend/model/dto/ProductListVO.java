package com.scube.scubebackend.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductListVO {
    private Long id;
    private String name;
    private String model;
    private BigDecimal price;
    private BigDecimal rating;
    private Integer gpuAvailable;
    private Integer gpuTotal;
    private String region;
    private List<String> tags;
    private Boolean isHot;
    private Boolean isNew;
    private String type;
}

