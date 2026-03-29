package com.scube.scubebackend.modules.product.service.impl;

import com.scube.scubebackend.modules.product.mapper.ProductFavoriteMapper;
import com.scube.scubebackend.modules.product.model.entity.ProductFavorite;
import com.scube.scubebackend.modules.product.service.ProductFavoriteService;
import com.scube.scubebackend.modules.product.service.ProductService;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.common.model.dto.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    @Autowired
    private ProductFavoriteMapper favoriteMapper;

    @Autowired
    private ProductService productService;

    @Override
    public boolean toggleFavorite(String userDisplayId, String productId) {
        ProductFavorite existing = favoriteMapper.findByUserAndProduct(userDisplayId, productId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            // exists -> un-favorite (logical delete)
            int updated = favoriteMapper.markDeleted(userDisplayId, productId, now);
            return updated > 0; // if already deleted, updated may be 0
        } else {
            // not exists -> insert
            int inserted = favoriteMapper.insertFavorite(userDisplayId, productId, now, now);
            return inserted > 0;
        }
    }

    @Override
    public List<String> listFavorites(String userDisplayId) {
        return favoriteMapper.selectProductIdsByUser(userDisplayId);
    }

    @Override
    public boolean deleteFavorite(String userDisplayId, String productId) {
        int updated = favoriteMapper.markDeleted(userDisplayId, productId, LocalDateTime.now());
        return updated > 0;
    }

    @Override
    public boolean existsFavorite(String userDisplayId, String productId) {
        ProductFavorite existing = favoriteMapper.findByUserAndProduct(userDisplayId, productId);
        return existing != null;
    }

    @Override
    public PageResult<ProductVO> listFavoritesPaged(String userDisplayId, int page, int pageSize) {
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 20;
        int total = favoriteMapper.countByUser(userDisplayId);
        int offset = (page - 1) * pageSize;
        List<String> productIds = favoriteMapper.selectProductIdsByUserPaged(userDisplayId, offset, pageSize);
        List<ProductVO> products = productIds == null || productIds.isEmpty() ? java.util.Collections.emptyList() : productService.getProductsByProductIds(productIds);
        return new PageResult<>(products, (long) total, (long) page, (long) pageSize);
    }
}
