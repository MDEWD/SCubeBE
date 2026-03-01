package com.scube.scubebackend.modules.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String name;
    
    private String model;
    
    private String gpuType;

    private Integer stock;
    
    private Integer gpuCount;
    
    private String cpu;
    
    private String memory;
    
    private String systemDisk;
    
    private String storage;
    
    private String bandwidth;
    
    private String maxCudaVersion;
    
    private String driverVersion;
    
    private BigDecimal monthlyPrice;

    private String tag;
    
    private String payMode;
    
    private String region;
    
    private String position;
    
    private String type;

    private Integer highSpeedNetCard;
    
    private String status;
    
    private BigDecimal rating;
    
    private Integer viewCount;
    
    private Integer isHot;
    
    private Integer isNew;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer isDelete;
}

