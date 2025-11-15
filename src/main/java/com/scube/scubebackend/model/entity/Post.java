package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String content;
    
    private Integer voteCount;
    
    private Integer answerCount;
    
    private Integer viewCount;
    
    private Long acceptedAnswerId;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer isDelete;
}

