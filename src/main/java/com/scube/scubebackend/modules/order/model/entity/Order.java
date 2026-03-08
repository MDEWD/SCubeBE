package com.scube.scubebackend.modules.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String productId;

    private String orderNo;

    private BigDecimal amount;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // === Admin录入/台账扩展字段 ===

    @TableField("user_display_id")
    private String userDisplayId;

    @TableField("customer_name")
    private String customerName;

    @TableField("customer_product")
    private String customerProduct;

    @TableField("customer_product_quantity")
    private Integer customerProductQuantity;

    @TableField("customer_amount")
    private BigDecimal customerAmount;

    @TableField("customer_total_amount")
    private BigDecimal customerTotalAmount;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("contact")
    private String contact;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("supplier_display_id")
    private String supplierDisplayId;

    @TableField("remark")
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}
