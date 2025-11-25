package com.scube.scubebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    /**
     * 增加浏览量
     * @param id 商品ID
     */
    void incrementViewCount(@Param("id") Long id);
    
    /**
     * 根据用户ID查询商品列表
     * @param userId 用户ID
     * @return 商品列表
     */
    List<Product> selectByUserId(@Param("userId") Long userId);
}

