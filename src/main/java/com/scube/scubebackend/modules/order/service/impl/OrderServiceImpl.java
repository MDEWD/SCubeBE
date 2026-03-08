package com.scube.scubebackend.modules.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.order.mapper.OrderMapper;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderVO;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrdersResponse.CustomerOrderVO;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrdersResponse;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse.PartnerOrderVO;
import com.scube.scubebackend.modules.order.model.entity.Order;
import com.scube.scubebackend.modules.order.service.OrderService;
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.modules.user.model.entity.User;
import com.scube.scubebackend.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PartnerOrdersResponse getPartnerOrders(int page, int pageSize) {
        LoginUser currentUser = UserContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        String supplierDisplayId = currentUser.getDisplayId();
        if (supplierDisplayId == null || supplierDisplayId.isBlank()) {
            if (currentUser.getId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id缺失");
            }
            User user = userMapper.selectById(currentUser.getId());
            if (user == null || (user.getIsDelete() != null && user.getIsDelete() == 1)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            supplierDisplayId = user.getDisplayId();
            // 回写，避免同一次请求/后续逻辑重复查库
            currentUser.setDisplayId(supplierDisplayId);
        }

        if (supplierDisplayId == null || supplierDisplayId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户displayId缺失");
        }

        Page<PartnerOrderVO> pageParam = new Page<>(page, pageSize);
        IPage<PartnerOrderVO> ordersPage = orderMapper.selectPartnerOrders(pageParam, supplierDisplayId);

        PartnerOrdersResponse response = new PartnerOrdersResponse();
        response.setList(ordersPage.getRecords());

        return response;
    }

    @Override
    public CustomerOrdersResponse getCustomerOrders(int page, int pageSize) {
        LoginUser currentUser = UserContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        String userDisplayId = currentUser.getDisplayId();
        if (userDisplayId == null || userDisplayId.isBlank()) {
            if (currentUser.getId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id缺失");
            }
            User user = userMapper.selectById(currentUser.getId());
            if (user == null || (user.getIsDelete() != null && user.getIsDelete() == 1)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            userDisplayId = user.getDisplayId();
            currentUser.setDisplayId(userDisplayId);
        }

        if (userDisplayId == null || userDisplayId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户displayId缺失");
        }

        Page<CustomerOrdersResponse> customerParam = new Page<>(page, pageSize);
        IPage<CustomerOrderVO> ordersPage = orderMapper.selectUserOrdersByDisplayId(customerParam, userDisplayId);

        CustomerOrdersResponse response = new CustomerOrdersResponse();
        response.setList(ordersPage.getRecords());
        return response;
    }

    @Override
    public IPage<AdminOrderVO> getAdminOrders(int page, int pageSize) {
        Page<AdminOrderVO> pageParam = new Page<>(page, pageSize);
        return orderMapper.selectAdminOrders(pageParam);
    }
}
