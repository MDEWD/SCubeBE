package com.scube.scubebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("answer")
public class Answer {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long postId;
    
    private Long userId;
    
    private String content;
    
    private Integer voteCount;
    
    private Integer isAccepted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer isDelete;
}

