package com.scube.scubebackend.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.product.mapper.ProductDemandMapper;
import com.scube.scubebackend.modules.product.model.dto.ProductDemandRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductDemandVO;
import com.scube.scubebackend.modules.product.model.entity.ProductDemand;
import com.scube.scubebackend.modules.product.service.ProductDemandService;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductDemandServiceImpl implements ProductDemandService {

    @Autowired
    private ProductDemandMapper demandMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DataSource dataSource;

    @Override
    public ProductDemandVO createDemand(ProductDemandRequest request, LoginUser loginUser) {
        if (request.getRentalPeriod() == null || request.getRentalPeriod().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "租期单位不能为空");
        }

        ProductDemand entity = new ProductDemand();
        entity.setUserId(loginUser.getId());
        entity.setModel(request.getModel());
        entity.setQuantity(request.getQuantity());
        entity.setRentalPeriod(request.getRentalPeriod());
        entity.setExpectedPrice(request.getExpectedPrice());
        entity.setRegionRequirement(request.getRegionRequirement());
        entity.setContact(request.getContact());
        entity.setRemark(request.getRemark());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setIsDelete(0);

        // set user display id if present
        User user = userMapper.selectById(loginUser.getId());
        if (user != null) {
            entity.setUserDisplayId(user.getDisplayId());
        }

        int inserted = demandMapper.insert(entity);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发布需求失败");
        }

        ProductDemandVO vo = new ProductDemandVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public PageResult<ProductDemandVO> getMyDemands(LoginUser loginUser, int page, int pageSize) {
        int safePage = page <= 0 ? 1 : page;
        int safeSize = pageSize <= 0 ? 20 : pageSize;

        Page<ProductDemand> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<ProductDemand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductDemand::getIsDelete, 0)
                .eq(ProductDemand::getUserId, loginUser.getId())
                .orderByDesc(ProductDemand::getCreateTime);

        Page<ProductDemand> result = demandMapper.selectPage(pageParam, wrapper);
        List<ProductDemandVO> items = result.getRecords().stream().map(entity -> {
            ProductDemandVO vo = new ProductDemandVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(items, result.getTotal(), (long) safePage, (long) safeSize);
    }

    @Override
    public PageResult<ProductDemandVO> getAllDemands(int page, int pageSize) {
        int safePage = page <= 0 ? 1 : page;
        int safeSize = pageSize <= 0 ? 20 : pageSize;

        Page<ProductDemand> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<ProductDemand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductDemand::getIsDelete, 0)
                .orderByDesc(ProductDemand::getCreateTime);

        Page<ProductDemand> result = demandMapper.selectPage(pageParam, wrapper);
        List<ProductDemandVO> items = result.getRecords().stream().map(entity -> {
            ProductDemandVO vo = new ProductDemandVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(items, result.getTotal(), (long) safePage, (long) safeSize);
    }

    @Override
    public ProductDemandVO updateDemand(Long id, ProductDemandRequest request, LoginUser loginUser) {
        ProductDemand existing = demandMapper.selectById(id);
        if (existing == null || (existing.getIsDelete() != null && existing.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "需求不存在");
        }

        // Only owner or admin can update
        boolean isOwner = loginUser.getId() != null && loginUser.getId().equals(existing.getUserId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(loginUser.getUserRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改该需求");
        }

        if (request.getRentalPeriod() == null || request.getRentalPeriod().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "租期单位不能为空");
        }

        existing.setModel(request.getModel());
        existing.setQuantity(request.getQuantity());
        existing.setRentalPeriod(request.getRentalPeriod());
        existing.setExpectedPrice(request.getExpectedPrice());
        existing.setRegionRequirement(request.getRegionRequirement());
        existing.setContact(request.getContact());
        existing.setRemark(request.getRemark());
        existing.setUpdateTime(LocalDateTime.now());

        int updated = demandMapper.updateById(existing);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新需求失败");
        }

        ProductDemandVO vo = new ProductDemandVO();
        BeanUtils.copyProperties(existing, vo);
        return vo;
    }

    @Override
    public boolean deleteDemand(Long id, LoginUser loginUser) {
        // Log JDBC URL to ensure we are connected to the expected database instance
        try (Connection conn = dataSource.getConnection()) {
            String jdbcUrl = conn.getMetaData().getURL();
            log.info("DataSource JDBC URL: {}", jdbcUrl);
        } catch (SQLException e) {
            log.warn("Unable to get JDBC URL from DataSource: {}", e.getMessage());
        }
        ProductDemand existing = demandMapper.selectById(id);
        if (existing == null || (existing.getIsDelete() != null && existing.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "需求不存在");
        }

        boolean isOwner = loginUser.getId() != null && loginUser.getId().equals(existing.getUserId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(loginUser.getUserRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该需求");
        }

        log.info("Deleting product demand id={} by userId={}. current isDelete={}", id, loginUser != null ? loginUser.getId() : null, existing.getIsDelete());
        // Use a direct SQL update to avoid potential ORM caching or plugin issues
        LocalDateTime now = LocalDateTime.now();
        int deleted = demandMapper.markDeletedById(id, now);
        log.info("markDeletedById affectedRows={}", deleted);
        // re-select to verify DB state
        try {
            ProductDemand reloaded = demandMapper.selectById(id);
            Integer reloadIsDelete = reloaded != null ? reloaded.getIsDelete() : null;
            log.info("After update select id={} isDelete={}", id, reloadIsDelete);
        } catch (Exception e) {
            log.warn("Failed to re-select product demand id={} after update: {}", id, e.getMessage());
        }

        if (deleted <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除需求失败");
        }
        return true;
    }
}
