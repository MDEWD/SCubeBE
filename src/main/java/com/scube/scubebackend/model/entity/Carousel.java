package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("carousel")
public class Carousel {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String summary;
    
    private String image;
    
    private String link;
    
    private Integer order;
}

