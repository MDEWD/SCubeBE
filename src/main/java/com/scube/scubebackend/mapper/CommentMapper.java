package com.scube.scubebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    /**
     * 计算商品平均评分
     * @param productId 商品ID
     * @return 平均评分
     */
    BigDecimal calculateAverageRating(@Param("productId") Long productId);
}

