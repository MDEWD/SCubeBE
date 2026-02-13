package com.scube.scubebackend.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.modules.product.model.entity.ProductTag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductTagMapper extends BaseMapper<ProductTag> {
}

