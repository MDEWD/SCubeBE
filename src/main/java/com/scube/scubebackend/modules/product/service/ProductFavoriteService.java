package com.scube.scubebackend.modules.product.service;

import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;

import java.util.List;

public interface ProductFavoriteService {
    boolean toggleFavorite(String userDisplayId, String productId);
    List<String> listFavorites(String userDisplayId);
    boolean deleteFavorite(String userDisplayId, String productId);
    boolean existsFavorite(String userDisplayId, String productId);

    PageResult<ProductVO> listFavoritesPaged(String userDisplayId, int page, int pageSize);
}
