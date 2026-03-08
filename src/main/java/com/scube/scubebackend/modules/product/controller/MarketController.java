package com.scube.scubebackend.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.mapper.ProductMapper;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.product.model.entity.Product;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 市场商品查询（对外列表）
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    @Autowired
    private ProductMapper productMapper;

    /**
     * 获取所有商品（支持筛选+分页）
     *
     * 请求参数：gpuBrand, region, gpuCount, gpuType, page, pageSize
     */
    @GetMapping("/listProducts")
    public BaseResponse<PageResult<ProductVO>> listProducts(
            @RequestParam(required = false) String gpuBrand,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer gpuCount,
            @RequestParam(required = false) String gpuType,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;

        Page<Product> pageParam = new Page<>(safePage, safePageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getIsDelete, 0)
                // 市场列表只展示已上架
                .eq(Product::getStatus, "ACTIVE");

        if (region != null && !region.isBlank()) {
            wrapper.eq(Product::getRegion, region);
        }
        if (gpuType != null && !gpuType.isBlank()) {
            wrapper.eq(Product::getGpuType, gpuType);
        }
        if (gpuCount != null) {
            wrapper.eq(Product::getGpuCount, gpuCount);
        }
        // gpuBrand：当前表结构里没有独立字段，这里用 gpuType 前缀/包含来做兼容过滤
        // 例如: NVIDIA / AMD 等。
        if (gpuBrand != null && !gpuBrand.isBlank()) {
            wrapper.like(Product::getGpuBrand, gpuBrand);
        }

        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(pageParam, wrapper);

        List<ProductVO> items = productPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return BaseResponse.success(new PageResult<>(items, productPage.getTotal(), (long) safePage, (long) safePageSize));
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}

