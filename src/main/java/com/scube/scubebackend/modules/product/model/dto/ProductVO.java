package com.scube.scubebackend.modules.product.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private String model;
    private String productId;
    private String gpuBrand;
    private String gpuType;
    private Integer gpuCount;
    private String cpu;
    private String cpuKernel;
    private String memory;
    private String systemDisk;
    private String storage;
    private String bandwidth;
    private String maxCudaVersion;
    private String driverVersion;
    private String payMode;
    private BigDecimal price;
    private BigDecimal monthlyPrice;
    private Integer stock;
    private BigDecimal rating;
    private Integer gpuAvailable;
    private Integer gpuTotal;
    private String region;
    private String position;
    private String type;
    private String status;
    /**
     * 发布者的展示ID（user.display_id），用于后台审核列表展示
     */
    private String userDisplayId;
    private List<String> tag;
    private List<String> images;
    private List<String> applicationScenes;
    private String dataCenterLocation;
    private List<String> dataCenterImages;
    private Boolean isNewDataCenter;
    private String dataCenterDescription;
    private Integer viewCount;
    private Boolean isHot;
    private Boolean isNew;
    private LocalDateTime createTime;
    private String comment;
}
