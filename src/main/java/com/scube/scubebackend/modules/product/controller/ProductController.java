package com.scube.scubebackend.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.product.mapper.ProductMapper;
import com.scube.scubebackend.modules.product.model.entity.Product;
import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductPublishRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.product.service.ProductService;
import com.scube.scubebackend.modules.product.model.dto.MyProductQueryRequest;
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController extends BaseController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public BaseResponse<ProductVO> updateProduct(@PathVariable Long id,
                                                 @RequestBody @Valid ProductPublishRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductVO product = productService.updateProduct(id, request, loginUser);
        return BaseResponse.success("更新成功", product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public BaseResponse<Void> deleteProduct(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        productService.deleteProduct(id, loginUser);
        return BaseResponse.success("删除成功", null);
    }

    @PostMapping("/my")
    public BaseResponse<PageResult<ProductVO>> getMyProducts(@RequestBody MyProductQueryRequest request) {
        LoginUser loginUser = getLoginUser();
        PageResult<ProductVO> result = productService.getMyProducts(loginUser, request);
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
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : size;

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(safePage, safeSize);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getIsDelete, 0)
                .eq(Product::getStatus, "PENDING")
                .orderByAsc(Product::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> productPage =
                productMapper.selectPage(pageParam, queryWrapper);

        List<ProductVO> items = productPage.getRecords().stream()
                .map(product -> {
                    ProductVO vo = convertToVO(product);
                    // 补充用户 display_id 给前端（ProductVO 若存在 userDisplayId 字段会被序列化）
                    User user = userMapper.selectById(product.getUserId());
                    if (user != null) {
                        setUserDisplayIdIfPresent(vo, user.getDisplayId());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return BaseResponse.success(new PageResult<>(items, productPage.getTotal(), (long) safePage, (long) safeSize));
    }

    /**
     * 管理员：审核商品（通过=ACTIVE，驳回=REJECTED）
     */
    @PostMapping("/admin/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<ProductVO> auditProduct(
            @PathVariable Long id,
            @RequestParam String action) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        if (action == null || action.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "action不能为空");
        }

        String normalized = action.trim().toLowerCase();
        String nextStatus;
        if ("approve".equals(normalized) || "approved".equals(normalized) || "active".equals(normalized)) {
            nextStatus = "ACTIVE";
        } else if ("reject".equals(normalized) || "rejected".equals(normalized)) {
            nextStatus = "REJECTED";
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "action仅支持approve或reject");
        }

        Product product = productMapper.selectById(id);
        if (product == null || product.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        if (!"PENDING".equalsIgnoreCase(product.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可审核待审核(PENDING)商品");
        }

        product.setStatus(nextStatus);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        return BaseResponse.success("操作成功", convertToVO(product));
    }

    private void setUserDisplayIdIfPresent(ProductVO vo, String displayId) {
        if (vo == null) {
            return;
        }
        try {
            // 约定字段名：userDisplayId
            java.lang.reflect.Field field = vo.getClass().getDeclaredField("userDisplayId");
            field.setAccessible(true);
            field.set(vo, displayId);
        } catch (NoSuchFieldException ignored) {
            // DTO 没有该字段时保持兼容，不影响现有接口
        } catch (IllegalAccessException ignored) {
        }
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        // ProductPublishRequest 的 tag 在 service 层会做 join/split，这里保持最小变更：只回填单值tag
        // 如果需要展示数组tag，请以后统一走 service 的 convertToVO。
        return vo;
    }
}
