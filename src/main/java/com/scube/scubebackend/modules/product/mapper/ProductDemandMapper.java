package com.scube.scubebackend.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.modules.product.model.entity.ProductDemand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductDemandMapper extends BaseMapper<ProductDemand> {
    List<ProductDemand> selectByUserId(@Param("userId") Long userId);
}

