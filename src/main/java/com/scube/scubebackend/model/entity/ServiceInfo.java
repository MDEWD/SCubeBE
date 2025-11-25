package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("service")
public class ServiceInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String icon;
    
    private String title;
    
    private String description;
}

