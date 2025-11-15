package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_application_scene")
public class ProductApplicationScene {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private String sceneName;
    
    private LocalDateTime createTime;
}

