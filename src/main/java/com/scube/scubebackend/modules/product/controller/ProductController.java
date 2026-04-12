package com.scube.scubebackend.modules.product.controller;

import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductPublishRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.product.service.ProductService;
import com.scube.scubebackend.modules.product.model.dto.MyProductQueryRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/product")
public class ProductController extends BaseController {

    @Autowired
    private ProductService productService;

    /**
     * 发布商品
     */
    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public BaseResponse<ProductVO> publishProduct(@RequestBody @Valid ProductPublishRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductVO product = productService.publishProduct(request, loginUser);
        return BaseResponse.success("发布成功", product);
    }

    /**
     * 查询商品列表
     */
    @GetMapping("/list")
    public BaseResponse<PageResult<ProductVO>> getProductList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String gpuType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        PageResult<ProductVO> result = productService.getProductList(type, gpuType, region, minPrice, maxPrice, page, size);
        return BaseResponse.success(result);
    }

    /**
     * 根据ID查询商品
     */
    @GetMapping("/{id}")
    public BaseResponse<ProductVO> getProductById(@PathVariable Long id) {
        ProductVO product = productService.getProductById(id);
        return BaseResponse.success(product);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public BaseResponse<ProductVO> updateProduct(@PathVariable Long id,
                                                 @RequestBody @Valid ProductPublishRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductVO product = productService.updateProduct(id, request, loginUser);
        return BaseResponse.success("更新成功", product);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public BaseResponse<Void> deleteProduct(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        productService.deleteProduct(id, loginUser);
        return BaseResponse.success("删除成功", null);
    }

    /**
     * 查询我的商品
     */
    @PostMapping("/my")
    public BaseResponse<PageResult<ProductVO>> getMyProducts(@RequestBody MyProductQueryRequest request) {
        LoginUser loginUser = getLoginUser();
        PageResult<ProductVO> result = productService.getMyProducts(loginUser, request);
        return BaseResponse.success(result);
    }

    /**
     * 管理员：查询除我发布之外的所有商品
     */
    @PostMapping("/admin/others")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<PageResult<ProductVO>> getOtherProductsForAdmin(@RequestBody MyProductQueryRequest request) {
        LoginUser loginUser = getLoginUser();
        PageResult<ProductVO> result = productService.getOtherProductsForAdmin(loginUser, request);
        return BaseResponse.success(result);
    }

    /**
     * 管理员：查询待审核商品（status = PENDING）
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<PageResult<ProductVO>> getPendingProducts(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        PageResult<ProductVO> result = productService.getPendingProductsForAdmin(page, size);
        return BaseResponse.success(result);
    }

    /**
     * 管理员：审核商品（通过=ACTIVE，驳回=REJECTED）
     */
    @PostMapping("/admin/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<ProductVO> auditProduct(
            @PathVariable Long id,
            @RequestParam String action) {
        ProductVO updated = productService.auditProductForAdmin(id, action);
        return BaseResponse.success("操作成功", updated);
    }
}
