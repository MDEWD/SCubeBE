package com.scube.scubebackend.controller;

import com.scube.scubebackend.model.dto.BaseResponse;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.ProductPublishRequest;
import com.scube.scubebackend.model.dto.ProductVO;
import com.scube.scubebackend.service.ProductService;
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
    
    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public BaseResponse<ProductVO> publishProduct(@RequestBody @Valid ProductPublishRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductVO product = productService.publishProduct(request, loginUser);
        return BaseResponse.success("发布成功", product);
    }
    
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
    
    @GetMapping("/{id}")
    public BaseResponse<ProductVO> getProductById(@PathVariable Long id) {
        ProductVO product = productService.getProductById(id);
        return BaseResponse.success(product);
    }
    
    @PutMapping("/{id}")
    public BaseResponse<ProductVO> updateProduct(@PathVariable Long id, 
                                                 @RequestBody @Valid ProductPublishRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductVO product = productService.updateProduct(id, request, loginUser);
        return BaseResponse.success("更新成功", product);
    }
    
    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteProduct(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        productService.deleteProduct(id, loginUser);
        return BaseResponse.success("删除成功", null);
    }
    
    @GetMapping("/my")
    public BaseResponse<PageResult<ProductVO>> getMyProducts(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        LoginUser loginUser = getLoginUser();
        PageResult<ProductVO> result = productService.getMyProducts(loginUser, page, size);
        return BaseResponse.success(result);
    }
}

