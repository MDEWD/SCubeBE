package com.scube.scubebackend.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.modules.product.model.entity.ProductDemand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductDemandMapper extends BaseMapper<ProductDemand> {
    List<ProductDemand> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE product_demand SET is_delete = 1, update_time = #{updateTime} WHERE id = #{id} AND is_delete = 0")
    int markDeletedById(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
