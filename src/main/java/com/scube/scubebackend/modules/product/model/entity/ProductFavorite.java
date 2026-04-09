package com.scube.scubebackend.modules.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_favorite")
public class ProductFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_display_id")
    private String userDisplayId;

    @TableField("product_id")
    private String productId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}

