package com.scube.scubebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.ProductPublishRequest;
import com.scube.scubebackend.model.dto.ProductVO;

import java.math.BigDecimal;

public interface ProductService {
    ProductVO publishProduct(ProductPublishRequest request, LoginUser loginUser);
    PageResult<ProductVO> getProductList(String type, String gpuType, String region, 
                                        BigDecimal minPrice, BigDecimal maxPrice, 
                                        Integer page, Integer size);
    ProductVO getProductById(Long id);
    ProductVO updateProduct(Long id, ProductPublishRequest request, LoginUser loginUser);
    void deleteProduct(Long id, LoginUser loginUser);
    PageResult<ProductVO> getMyProducts(LoginUser loginUser, Integer page, Integer size);
}

