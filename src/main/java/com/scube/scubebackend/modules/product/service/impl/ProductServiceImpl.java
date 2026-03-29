package com.scube.scubebackend.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.product.model.dto.MyProductQueryRequest;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductPublishRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.product.service.ProductService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.scube.scubebackend.modules.product.mapper.ProductApplicationSceneMapper;
import com.scube.scubebackend.modules.product.mapper.ProductImageMapper;
import com.scube.scubebackend.modules.product.mapper.ProductMapper;
import com.scube.scubebackend.modules.product.mapper.ProductTagMapper;
import com.scube.scubebackend.modules.product.model.entity.Product;
import com.scube.scubebackend.modules.product.model.entity.ProductApplicationScene;
import com.scube.scubebackend.modules.product.model.entity.ProductImage;
import com.scube.scubebackend.modules.product.model.entity.ProductTag;
import com.scube.scubebackend.modules.admin.model.dto.AuditDecision;
import com.scube.scubebackend.modules.admin.model.dto.AuditRequest;
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.entity.User;
import com.scube.scubebackend.util.UserContext;
import com.scube.scubebackend.util.DisplayIDGenerator;

@Log4j2
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

    @Autowired
    private UserMapper userMapper; // Inject UserMapper

    @Autowired
    private DisplayIDGenerator displayIDGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO publishProduct(ProductPublishRequest request, LoginUser loginUser) {
        // 创建商品主表
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setTag(joinTags(request.getTag()));
        product.setUserId(loginUser.getId());
        product.setProductId("P" + generateUniqueProductId());
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
            queryWrapper.ge(Product::getMonthlyPrice, minPrice);
        }
        if (maxPrice != null) {
            queryWrapper.le(Product::getMonthlyPrice, maxPrice);
        }
        
        queryWrapper.eq(Product::getStatus, "ACTIVE");
        queryWrapper.orderByDesc(Product::getCreateTime);
        
        Page<Product> productPage = productMapper.selectPage(pageParam, queryWrapper);
        
        List<ProductVO> voList = productPage.getRecords().stream()
                .map(product -> {
                    ProductVO vo = convertToVO(product);
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
        
        String role = loginUser.getUserRole();
        if (!"ADMIN".equals(role) && !"PARTNER".equals(role)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改此商品");
        }
        if ("PARTNER".equals(role) && !product.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改此商品");
        }
        
        // 更新商品主表
        BeanUtils.copyProperties(request, product);
        product.setTag(joinTags(request.getTag()));
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        
        // 删除旧的关联数据
//        LambdaQueryWrapper<ProductTag> tagWrapper = new LambdaQueryWrapper<>();
//        tagWrapper.eq(ProductTag::getProductId, id);
//        productTagMapper.delete(tagWrapper);
//
//        LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
//        imageWrapper.eq(ProductImage::getProductId, id);
//        productImageMapper.delete(imageWrapper);
//
//        LambdaQueryWrapper<ProductApplicationScene> sceneWrapper = new LambdaQueryWrapper<>();
//        sceneWrapper.eq(ProductApplicationScene::getProductId, id);
//        productApplicationSceneMapper.delete(sceneWrapper);
        
        // 插入新的关联数据
//        if (request.getTag() != null && !request.getTag().isEmpty()) {
//            for (String tag : request.getTag()) {
//                ProductTag productTag = new ProductTag();
//                productTag.setProductId(id);
//                productTag.setTagName(tag);
//                productTag.setCreateTime(LocalDateTime.now());
//                productTagMapper.insert(productTag);
//            }
//        }

//        if (request.getImages() != null && !request.getImages().isEmpty()) {
//            for (int i = 0; i < request.getImages().size(); i++) {
//                ProductImage productImage = new ProductImage();
//                productImage.setProductId(id);
//                productImage.setImageUrl(request.getImages().get(i));
//                productImage.setSortOrder(i);
//                productImage.setCreateTime(LocalDateTime.now());
//                productImageMapper.insert(productImage);
//            }
//        }
        
//        if (request.getApplicationScenes() != null && !request.getApplicationScenes().isEmpty()) {
//            for (String scene : request.getApplicationScenes()) {
//                ProductApplicationScene sceneEntity = new ProductApplicationScene();
//                sceneEntity.setProductId(id);
//                sceneEntity.setSceneName(scene);
//                sceneEntity.setCreateTime(LocalDateTime.now());
//                productApplicationSceneMapper.insert(sceneEntity);
//            }
//        }
        
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

        String role = loginUser.getUserRole();
        if (!"ADMIN".equals(role) && !"PARTNER".equals(role)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除此商品");
        }
        if ("PARTNER".equals(role) && !product.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除此商品");
        }

        log.info("Deleting product: id={}, role={}, userId={}, ownerId={}, currentIsDelete={}",
                id, role, loginUser.getId(), product.getUserId(), product.getIsDelete());

        int rows = productMapper.deleteById(id);
        log.info("Delete product update result: id={}, rows={}", id, rows);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败，更新未生效");
        }

        clearProductCache();
    }
    
    @Override
    public PageResult<ProductVO> getMyProducts(LoginUser loginUser, MyProductQueryRequest request) {
        Integer page = request != null ? request.getPage() : null;
        Integer size = request != null ? request.getPageSize() : null;
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }

        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getUserId, loginUser.getId())
                .eq(Product::getIsDelete, 0);

        if (request != null && request.getGpuTypes() != null && !request.getGpuTypes().isEmpty()) {
            queryWrapper.in(Product::getGpuType, request.getGpuTypes());
        }

        LocalDateTime startTime = parseStartTime(request != null ? request.getPublishTimeStart() : null);
        LocalDateTime endTime = parseEndTime(request != null ? request.getPublishTimeEnd() : null);
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "publishTimeEnd不能早于publishTimeStart");
        }
        if (startTime != null) {
            queryWrapper.ge(Product::getCreateTime, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(Product::getCreateTime, endTime);
        }

        queryWrapper.orderByDesc(Product::getCreateTime);

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

    private LocalDateTime parseStartTime(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateText).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "publishTimeStart格式错误，期望yyyy-MM-dd");
        }
    }

    private LocalDateTime parseEndTime(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateText).atTime(LocalTime.MAX);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "publishTimeEnd格式错误，期望yyyy-MM-dd");
        }
    }

    @Override
    public List<AuditRequest> getPendingAudits() {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null || !"ADMIN".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, "PENDING")
                   .eq(Product::getIsDelete, 0)
                   .orderByAsc(Product::getCreateTime);

        List<Product> pendingProducts = productMapper.selectList(queryWrapper);

        return pendingProducts.stream().map(product -> {
            AuditRequest request = new AuditRequest();
            request.setId(String.valueOf(product.getId()));

            User user = userMapper.selectById(product.getUserId());
            request.setUserName(user != null ? user.getNickname() : "Unknown");

            request.setApplyTime(product.getCreateTime() != null ? product.getCreateTime().toString() : "");
            request.setStatus("pending");
            request.setReason("New Product Application: " + product.getName());
            return request;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(String auditId, AuditDecision decision) {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null || !"ADMIN".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Long productId = Long.valueOf(auditId);
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Product not found");
        }

        if ("approved".equalsIgnoreCase(decision.getStatus())) {
            product.setStatus("ACTIVE");
        } else if ("rejected".equalsIgnoreCase(decision.getStatus())) {
            product.setStatus("REJECTED");
            // potentially store reject reason somewhere, maybe in a new field or audit log
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid decision status");
        }

        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        clearProductCache();
    }

    @Override
    public List<ProductVO> getProductsByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getProductId, productIds)
               .eq(Product::getIsDelete, 0)
               .eq(Product::getStatus, "ACTIVE");
        List<Product> products = productMapper.selectList(wrapper);
        List<ProductVO> vos = products.stream().map(this::convertToVO).toList();

        // preserve input order by productIds
        return productIds.stream()
                .map(pid -> vos.stream().filter(v -> pid.equals(v.getProductId())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setTag(splitTags(product.getTag()));
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
            vo.setTag(tags.stream().map(ProductTag::getTagName).collect(Collectors.toList()));
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

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .collect(Collectors.joining(","));
    }

    private List<String> splitTags(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(tagsText.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toList());
    }

    private String generateUniqueProductId() {
        String productId;
        boolean isUnique;
        do {
            productId = displayIDGenerator.generateDisplayID();
            isUnique = !productMapper.existsByProductId(productId);
        } while (!isUnique);
        return productId;
    }
}
