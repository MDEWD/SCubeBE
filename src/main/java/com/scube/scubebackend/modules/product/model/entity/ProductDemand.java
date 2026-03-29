package com.scube.scubebackend.modules.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_demand")
public class ProductDemand {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("user_display_id")
    private String userDisplayId;

    // 型号
    private String model;

    // 数量
    private Integer quantity;

    // 租期：时长数字，例如 3（配合租期单位 MONTHS 表示 3 个月）
    @TableField("rental_period_value")
    private Integer rentalPeriodValue;

    // 租期单位：DAYS 或 MONTHS
    @TableField("rental_period")
    private String rentalPeriod;

    // 预期价格（可为每月或总价，业务上可解释）
    private BigDecimal expectedPrice;

    // 地区有无要求
    private String regionRequirement;

    // 联系方式
    private String contact;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}
