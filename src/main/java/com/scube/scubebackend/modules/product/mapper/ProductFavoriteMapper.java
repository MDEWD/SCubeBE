package com.scube.scubebackend.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.modules.product.model.entity.ProductFavorite;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductFavoriteMapper extends BaseMapper<ProductFavorite> {
    @Select("SELECT * FROM product_favorite WHERE user_display_id = #{userDisplayId} AND product_id = #{productId} AND is_delete = 0 LIMIT 1")
    ProductFavorite findByUserAndProduct(@Param("userDisplayId") String userDisplayId, @Param("productId") String productId);

    @Select("SELECT product_id FROM product_favorite WHERE user_display_id = #{userDisplayId} AND is_delete = 0 ORDER BY create_time DESC")
    List<String> selectProductIdsByUser(@Param("userDisplayId") String userDisplayId);

    @Select("SELECT COUNT(1) FROM product_favorite WHERE user_display_id = #{userDisplayId} AND is_delete = 0")
    int countByUser(@Param("userDisplayId") String userDisplayId);

    @Select("SELECT product_id FROM product_favorite WHERE user_display_id = #{userDisplayId} AND is_delete = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<String> selectProductIdsByUserPaged(@Param("userDisplayId") String userDisplayId, @Param("offset") int offset, @Param("limit") int limit);

    @Update("UPDATE product_favorite SET is_delete = 1, update_time = #{updateTime} WHERE user_display_id = #{userDisplayId} AND product_id = #{productId} AND is_delete = 0")
    int markDeleted(@Param("userDisplayId") String userDisplayId, @Param("productId") String productId, @Param("updateTime") LocalDateTime updateTime);

    @Insert("INSERT INTO product_favorite (user_display_id, product_id, create_time, update_time, is_delete) VALUES (#{userDisplayId}, #{productId}, #{createTime}, #{updateTime}, 0) "
            + "ON DUPLICATE KEY UPDATE is_delete = 0, update_time = #{updateTime}")
    int insertFavorite(@Param("userDisplayId") String userDisplayId, @Param("productId") String productId, @Param("createTime") LocalDateTime createTime, @Param("updateTime") LocalDateTime updateTime);
}
