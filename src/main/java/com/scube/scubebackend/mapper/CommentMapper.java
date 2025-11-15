package com.scube.scubebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}

