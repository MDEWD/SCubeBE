package com.scube.scubebackend.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private String model;
    private String gpuType;
    private Integer gpuCount;
    private String cpu;
    private String memory;
    private String systemDisk;
    private String dataDisk;
    private String bandwidth;
    private String maxCudaVersion;
    private String driverVersion;
    private BigDecimal price;
    private BigDecimal rating;
    private Integer gpuAvailable;
    private Integer gpuTotal;
    private String region;
    private String location;
    private String type;
    private String status;
    private List<String> tags;
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
}

