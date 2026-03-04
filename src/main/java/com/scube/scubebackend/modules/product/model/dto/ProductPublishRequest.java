package com.scube.scubebackend.modules.product.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductPublishRequest {
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 100, message = "商品名称长度必须在1-100之间")
    private String name;
    
//    @NotBlank(message = "商品型号不能为空")
    private String model;
    
    @NotBlank(message = "GPU型号不能为空")
    private String gpuType;
    
//    @NotNull(message = "GPU数量不能为空")
//    @Min(value = 1, message = "GPU数量至少为1")
    private Integer gpuCount;

    private Integer stock;
    
    private String cpu;
    private String memory;
    private String systemDisk;
    private String storage;
    private String bandwidth;
    private String maxCudaVersion;
    private String driverVersion;

    private String payMode;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0.01")
    private BigDecimal monthlyPrice;

    private String region;
    
    private String position;

    private String network;

    private int highSpeedNetCard;

    private List<String> applicationScenes;
    
    private List<String> tag;

    private List<String> images;
    
    private Boolean isNewDataCenter;
    private String dataCenterDescription;
    private List<String> dataCenterImages;
}
