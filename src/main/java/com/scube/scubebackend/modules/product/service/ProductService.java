package com.scube.scubebackend.modules.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductPublishRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.admin.model.dto.AuditDecision;
import com.scube.scubebackend.modules.admin.model.dto.AuditRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductVO publishProduct(ProductPublishRequest request, LoginUser loginUser);
    PageResult<ProductVO> getProductList(String type, String gpuType, String region, 
                                        BigDecimal minPrice, BigDecimal maxPrice, 
                                        Integer page, Integer size);
    ProductVO getProductById(Long id);
    ProductVO updateProduct(Long id, ProductPublishRequest request, LoginUser loginUser);
    void deleteProduct(Long id, LoginUser loginUser);
    PageResult<ProductVO> getMyProducts(LoginUser loginUser, Integer page, Integer size);

    List<AuditRequest> getPendingAudits();
    void auditProduct(String auditId, AuditDecision decision);
}
