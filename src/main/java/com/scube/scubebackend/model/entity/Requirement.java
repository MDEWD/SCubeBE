package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("requirement")
public class Requirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String description;
    
    private String gpuType;
    
    private Integer gpuCount;
    
    private BigDecimal budget;
    
    private LocalDate deadline;
    
    private String status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

