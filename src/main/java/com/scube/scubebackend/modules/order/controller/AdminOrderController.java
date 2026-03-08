package com.scube.scubebackend.modules.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.order.mapper.OrderMapper;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderCreateRequest;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderDetailVO;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderUpdateRequest;
import com.scube.scubebackend.modules.order.model.entity.Order;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController extends BaseController {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 新增订单（管理员录入）
     */
    @PostMapping
    public BaseResponse<AdminOrderDetailVO> createOrder(@RequestBody @Valid AdminOrderCreateRequest request) {
        Order order = new Order();
        // 基础字段
        order.setProductId(request.getProductId());

        order.setUserDisplayId(request.getUserDisplayId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerProduct(request.getCustomerProduct());
        order.setCustomerProductQuantity(request.getCustomerProductQuantity());
        order.setCustomerAmount(request.getCustomerAmount());
        order.setCustomerTotalAmount(request.getCustomerTotalAmount());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setContact(request.getContact());
        order.setSupplierName(request.getSupplierName());
        order.setSupplierDisplayId(request.getSupplierDisplayId());
        order.setRemark(request.getRemark());

        // 兼容已有字段
        order.setStartTime(parseStartTime(request.getStartDate()));
        order.setEndTime(parseEndTime(request.getEndDate()));
        order.setAmount(request.getCustomerAmount());
        order.setStatus("ACTIVE");

        // 新增：生成订单号、创建/更新时间、软删除标记
        order.setOrderNo("O" + generateOrderNo());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setIsDelete(0);

        orderMapper.insert(order);
        return BaseResponse.success("创建成功", toDetailVO(order));
    }

    /**
     * 查询订单列表（分页）
     */
    @GetMapping
    public BaseResponse<PageResult<AdminOrderDetailVO>> listOrders(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;

        Page<Order> pageParam = new Page<>(safePage, safePageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getIsDelete, 0);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        List<AdminOrderDetailVO> items = orderPage.getRecords().stream()
                .map(this::toDetailVO)
                .collect(Collectors.toList());

        return BaseResponse.success(new PageResult<>(items, orderPage.getTotal(), (long) safePage, (long) safePageSize));
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{id}")
    public BaseResponse<AdminOrderDetailVO> getOrder(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || (order.getIsDelete() != null && order.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        return BaseResponse.success(toDetailVO(order));
    }

    /**
     * 更新订单
     */
    @PutMapping("/{id}")
    public BaseResponse<AdminOrderDetailVO> updateOrder(
            @PathVariable Long id,
            @RequestBody @Valid AdminOrderUpdateRequest request) {
        Order order = orderMapper.selectById(id);
        if (order == null || (order.getIsDelete() != null && order.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }

        if (request.getProductId() != null) {
            order.setProductId(request.getProductId());
        }
        if (request.getUserDisplayId() != null) {
            order.setUserDisplayId(request.getUserDisplayId());
        }
        if (request.getCustomerName() != null) {
            order.setCustomerName(request.getCustomerName());
        }
        if (request.getCustomerProduct() != null) {
            order.setCustomerProduct(request.getCustomerProduct());
        }
        if (request.getCustomerProductQuantity() != null) {
            order.setCustomerProductQuantity(request.getCustomerProductQuantity());
        }
        if (request.getCustomerAmount() != null) {
            order.setCustomerAmount(request.getCustomerAmount());
            order.setAmount(request.getCustomerAmount());
        }
        if (request.getCustomerTotalAmount() != null) {
            order.setCustomerTotalAmount(request.getCustomerTotalAmount());
        }
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            order.setStartTime(parseStartTime(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            order.setEndTime(parseEndTime(request.getEndDate()));
        }
        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getContact() != null) {
            order.setContact(request.getContact());
        }
        if (request.getSupplierName() != null) {
            order.setSupplierName(request.getSupplierName());
        }
        if (request.getSupplierDisplayId() != null) {
            order.setSupplierDisplayId(request.getSupplierDisplayId());
        }
        if (request.getRemark() != null) {
            order.setRemark(request.getRemark());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            order.setStatus(request.getStatus());
        }

        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return BaseResponse.success("更新成功", toDetailVO(order));
    }

    /**
     * 删除订单（软删除）
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteOrder(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || (order.getIsDelete() != null && order.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        // 使用 MyBatis-Plus 逻辑删除：会按全局配置/@TableLogic 将 is_delete 更新为 1
        orderMapper.deleteById(id);
        return BaseResponse.success(true);
    }

    private AdminOrderDetailVO toDetailVO(Order order) {
        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setUserDisplayId(order.getUserDisplayId());
        vo.setCustomerName(order.getCustomerName());
        vo.setCustomerProduct(order.getCustomerProduct());
        vo.setProductId(order.getProductId());
        vo.setCustomerProductQuantity(order.getCustomerProductQuantity());
        vo.setCustomerAmount(order.getCustomerAmount());
        vo.setCustomerTotalAmount(order.getCustomerTotalAmount());
        vo.setStartDate(order.getStartTime() == null ? null : order.getStartTime().toLocalDate().toString());
        vo.setEndDate(order.getEndTime() == null ? null : order.getEndTime().toLocalDate().toString());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setContact(order.getContact());
        vo.setSupplierName(order.getSupplierName());
        vo.setSupplierDisplayId(order.getSupplierDisplayId());
        vo.setRemark(order.getRemark());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime() == null ? null : order.getCreateTime().toString());
        vo.setUpdateTime(order.getUpdateTime() == null ? null : order.getUpdateTime().toString());
        return vo;
    }

    private LocalDateTime parseStartTime(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "startDate不能为空");
        }
        try {
            return LocalDate.parse(dateText).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "startDate格式错误，期望yyyy-MM-dd");
        }
    }

    private LocalDateTime parseEndTime(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "endDate不能为空");
        }
        try {
            return LocalDate.parse(dateText).atTime(LocalTime.MAX);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "endDate格式错误，期望yyyy-MM-dd");
        }
    }

    private String generateOrderNo() {
        // 简单生成：ORD + yyyyMMdd + 8位随机
        String date = LocalDate.now().toString().replace("-", "");
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD" + date + rand;
    }
}
