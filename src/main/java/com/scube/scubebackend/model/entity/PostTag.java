package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_tag")
public class PostTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long postId;
    
    private String tagName;
    
    private LocalDateTime createTime;
}

