package com.scube.scubebackend.modules.product.service;

import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductPublishRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.admin.model.dto.AuditDecision;
import com.scube.scubebackend.modules.admin.model.dto.AuditRequest;
import com.scube.scubebackend.modules.product.model.dto.MyProductQueryRequest;

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
    PageResult<ProductVO> getMyProducts(LoginUser loginUser, MyProductQueryRequest request);

    /**
     * 管理员：查询除自己发布的商品之外的所有商品（支持分页/筛选，复用 MyProductQueryRequest）
     */
    PageResult<ProductVO> getOtherProductsForAdmin(LoginUser loginUser, MyProductQueryRequest request);

    List<AuditRequest> getPendingAudits();
    void auditProduct(String auditId, AuditDecision decision);

    /**
     * 根据展示用 productId 列表批量获取产品信息（包含详情）
     */
    List<ProductVO> getProductsByProductIds(List<String> productIds);

    /**
     * 管理员：分页查询待审核商品（status = PENDING），并补充发布者的 displayId（若 ProductVO 支持该字段）。
     */
    PageResult<ProductVO> getPendingProductsForAdmin(Integer page, Integer size);

    /**
     * 管理员：审核商品（approve -> ACTIVE，reject -> REJECTED）
     */
    ProductVO auditProductForAdmin(Long id, String action);
}
