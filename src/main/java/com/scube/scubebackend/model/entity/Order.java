package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long productId;
    
    private String orderNo;
    
    private BigDecimal amount;
    
    private String status;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

