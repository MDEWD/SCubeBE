package com.scube.scubebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}

