package com.scube.scubebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.mapper.*;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.ProductPublishRequest;
import com.scube.scubebackend.model.dto.ProductVO;
import com.scube.scubebackend.model.entity.*;
import com.scube.scubebackend.service.ProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private ProductTagMapper productTagMapper;
    
    @Autowired
    private ProductImageMapper productImageMapper;
    
    @Autowired
    private ProductApplicationSceneMapper productApplicationSceneMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO publishProduct(ProductPublishRequest request, LoginUser loginUser) {
        // 创建商品主表
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setUserId(loginUser.getId());
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        product.setIsDelete(0);
        product.setViewCount(0);
        product.setRating(BigDecimal.ZERO);
        
        // 根据角色设置状态
        if ("ADMIN".equals(loginUser.getUserRole())) {
            product.setStatus("ACTIVE");
        } else {
            product.setStatus("PENDING");
        }
        
        // 设置默认值
        if (product.getType() == null || product.getType().isEmpty()) {
            product.setType("lease");
        }
        if (product.getGpuCount() == null) {
            product.setGpuCount(1);
        }
        if (product.getIsHot() == null) {
            product.setIsHot(0);
        }
        if (product.getIsNew() == null) {
            product.setIsNew(0);
        }
        
        productMapper.insert(product);
        
        // 插入标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tag : request.getTags()) {
                ProductTag productTag = new ProductTag();
                productTag.setProductId(product.getId());
                productTag.setTagName(tag);
                productTag.setCreateTime(LocalDateTime.now());
                productTagMapper.insert(productTag);
            }
        }
        
        // 插入图片
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                ProductImage productImage = new ProductImage();
                productImage.setProductId(product.getId());
                productImage.setImageUrl(request.getImages().get(i));
                productImage.setSortOrder(i);
                productImage.setCreateTime(LocalDateTime.now());
                productImageMapper.insert(productImage);
            }
        }
        
        // 插入应用场景
        if (request.getApplicationScenes() != null && !request.getApplicationScenes().isEmpty()) {
            for (String scene : request.getApplicationScenes()) {
                ProductApplicationScene sceneEntity = new ProductApplicationScene();
                sceneEntity.setProductId(product.getId());
                sceneEntity.setSceneName(scene);
                sceneEntity.setCreateTime(LocalDateTime.now());
                productApplicationSceneMapper.insert(sceneEntity);
            }
        }
        
        // 清除缓存
        clearProductCache();
        
        return convertToVO(product);
    }
    
    @Override
    public PageResult<ProductVO> getProductList(String type, String gpuType, String region, 
                                               BigDecimal minPrice, BigDecimal maxPrice, 
                                               Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        
        // 尝试从缓存获取
        String cacheKey = String.format("product:list:%s:%s:%s:%s:%s:%d:%d", 
            type, gpuType, region, minPrice, maxPrice, page, size);
        PageResult<ProductVO> cached = (PageResult<ProductVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getIsDelete, 0);
        
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Product::getType, type);
        }
        if (gpuType != null && !gpuType.isEmpty()) {
            queryWrapper.eq(Product::getGpuType, gpuType);
        }
        if (region != null && !region.isEmpty()) {
            queryWrapper.eq(Product::getRegion, region);
        }
        if (minPrice != null) {
            queryWrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            queryWrapper.le(Product::getPrice, maxPrice);
        }
        
        queryWrapper.eq(Product::getStatus, "ACTIVE");
        queryWrapper.orderByDesc(Product::getCreateTime);
        
        Page<Product> productPage = productMapper.selectPage(pageParam, queryWrapper);
        
        List<ProductVO> voList = productPage.getRecords().stream()
                .map(product -> {
                    ProductVO vo = convertToVO(product);
                    // 加载标签（列表页只加载标签）
                    LambdaQueryWrapper<ProductTag> tagWrapper = new LambdaQueryWrapper<>();
                    tagWrapper.eq(ProductTag::getProductId, product.getId());
                    List<ProductTag> tags = productTagMapper.selectList(tagWrapper);
                    if (tags != null && !tags.isEmpty()) {
                        vo.setTags(tags.stream().map(ProductTag::getTagName).collect(Collectors.toList()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        
        PageResult<ProductVO> result = new PageResult<>(
            voList,
            productPage.getTotal(),
            (long) page,
            (long) size
        );
        
        // 缓存5分钟
        redisTemplate.opsForValue().set(cacheKey, result, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        return result;
    }
    
    @Override
    public ProductVO getProductById(Long id) {
        // 尝试从缓存获取
        String cacheKey = "product:detail:" + id;
        ProductVO cached = (ProductVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            // 异步更新浏览量
            updateViewCount(id);
            return cached;
        }
        
        Product product = productMapper.selectById(id);
        if (product == null || product.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        
        ProductVO vo = convertToVO(product);
        
        // 加载关联数据
        loadProductDetails(vo, id);
        
        // 更新浏览量
        updateViewCount(id);
        
        // 缓存5分钟（注意：需要重新加载vo，因为loadProductDetails修改了vo）
        ProductVO voForCache = convertToVO(product);
        loadProductDetails(voForCache, id);
        redisTemplate.opsForValue().set(cacheKey, voForCache, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        return vo;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO updateProduct(Long id, ProductPublishRequest request, LoginUser loginUser) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        
        // 权限检查：只有管理员或发布者可以修改
        if (!"ADMIN".equals(loginUser.getUserRole()) && !product.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改此商品");
        }
        
        // 更新商品主表
        BeanUtils.copyProperties(request, product);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        
        // 删除旧的关联数据
        LambdaQueryWrapper<ProductTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(ProductTag::getProductId, id);
        productTagMapper.delete(tagWrapper);
        
        LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.eq(ProductImage::getProductId, id);
        productImageMapper.delete(imageWrapper);
        
        LambdaQueryWrapper<ProductApplicationScene> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(ProductApplicationScene::getProductId, id);
        productApplicationSceneMapper.delete(sceneWrapper);
        
        // 插入新的关联数据
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tag : request.getTags()) {
                ProductTag productTag = new ProductTag();
                productTag.setProductId(id);
                productTag.setTagName(tag);
                productTag.setCreateTime(LocalDateTime.now());
                productTagMapper.insert(productTag);
            }
        }
        
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                ProductImage productImage = new ProductImage();
                productImage.setProductId(id);
                productImage.setImageUrl(request.getImages().get(i));
                productImage.setSortOrder(i);
                productImage.setCreateTime(LocalDateTime.now());
                productImageMapper.insert(productImage);
            }
        }
        
        if (request.getApplicationScenes() != null && !request.getApplicationScenes().isEmpty()) {
            for (String scene : request.getApplicationScenes()) {
                ProductApplicationScene sceneEntity = new ProductApplicationScene();
                sceneEntity.setProductId(id);
                sceneEntity.setSceneName(scene);
                sceneEntity.setCreateTime(LocalDateTime.now());
                productApplicationSceneMapper.insert(sceneEntity);
            }
        }
        
        clearProductCache();
        
        return convertToVO(product);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id, LoginUser loginUser) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        
        // 权限检查
        if (!"ADMIN".equals(loginUser.getUserRole()) && !product.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除此商品");
        }
        
        // 逻辑删除
        product.setIsDelete(1);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        
        clearProductCache();
    }
    
    @Override
    public PageResult<ProductVO> getMyProducts(LoginUser loginUser, Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getUserId, loginUser.getId())
                .eq(Product::getIsDelete, 0)
                .orderByDesc(Product::getCreateTime);
        
        Page<Product> productPage = productMapper.selectPage(pageParam, queryWrapper);
        
        List<ProductVO> voList = productPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(
            voList,
            productPage.getTotal(),
            (long) page,
            (long) size
        );
    }
    
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setIsHot(product.getIsHot() == 1);
        vo.setIsNew(product.getIsNew() == 1);
        return vo;
    }
    
    private void loadProductDetails(ProductVO vo, Long productId) {
        // 加载标签
        LambdaQueryWrapper<ProductTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(ProductTag::getProductId, productId);
        List<ProductTag> tags = productTagMapper.selectList(tagWrapper);
        if (tags != null && !tags.isEmpty()) {
            vo.setTags(tags.stream().map(ProductTag::getTagName).collect(Collectors.toList()));
        }
        
        // 加载图片
        LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder);
        List<ProductImage> images = productImageMapper.selectList(imageWrapper);
        if (images != null && !images.isEmpty()) {
            vo.setImages(images.stream().map(ProductImage::getImageUrl).collect(Collectors.toList()));
        }
        
        // 加载应用场景
        LambdaQueryWrapper<ProductApplicationScene> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(ProductApplicationScene::getProductId, productId);
        List<ProductApplicationScene> scenes = productApplicationSceneMapper.selectList(sceneWrapper);
        if (scenes != null && !scenes.isEmpty()) {
            vo.setApplicationScenes(scenes.stream().map(ProductApplicationScene::getSceneName).collect(Collectors.toList()));
        }
    }
    
    private void updateViewCount(Long productId) {
        // 异步更新浏览量
        new Thread(() -> {
            try {
                Product product = productMapper.selectById(productId);
                if (product != null) {
                    product.setViewCount((product.getViewCount() == null ? 0 : product.getViewCount()) + 1);
                    productMapper.updateById(product);
                }
            } catch (Exception e) {
                // 忽略更新错误
            }
        }).start();
    }
    
    private void clearProductCache() {
        // 清除相关缓存
        try {
            var keys = redisTemplate.keys("product:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 忽略缓存清除错误
        }
    }
}

