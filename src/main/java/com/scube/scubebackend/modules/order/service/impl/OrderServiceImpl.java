package com.scube.scubebackend.modules.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.order.mapper.OrderMapper;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderVO;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrderVO;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse.PartnerOrderVO;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse.PartnerStatsVO;
import com.scube.scubebackend.modules.order.model.entity.Order;
import com.scube.scubebackend.modules.order.service.OrderService;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public IPage<CustomerOrderVO> getCustomerOrders(int page, int pageSize, String status) {
        LoginUser currentUser = UserContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<CustomerOrderVO> pageParam = new Page<>(page, pageSize);
        return orderMapper.selectCustomerOrders(pageParam, currentUser.getId(), status);
    }

    @Override
    public PartnerOrdersResponse getPartnerOrders(int page, int pageSize) {
        LoginUser currentUser = UserContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // Ensure user is a partner? The controller/security layer should probably handle this mostly,
        // but no harm checking role here if strictly needed. For now, we trust the caller context or just query.

        Page<PartnerOrderVO> pageParam = new Page<>(page, pageSize);
        IPage<PartnerOrderVO> ordersPage = orderMapper.selectPartnerOrders(pageParam, currentUser.getId());

        PartnerStatsVO stats = orderMapper.selectPartnerStats(currentUser.getId());
        if (stats == null) {
            stats = new PartnerStatsVO();
            stats.setMonthSettlement("0.00");
            stats.setActiveContracts(0);
        }

        PartnerOrdersResponse response = new PartnerOrdersResponse();
        response.setStats(stats);
        response.setList(ordersPage.getRecords());

        return response;
    }

    @Override
    public IPage<AdminOrderVO> getAdminOrders(int page, int pageSize) {
        Page<AdminOrderVO> pageParam = new Page<>(page, pageSize);
        return orderMapper.selectAdminOrders(pageParam);
    }
}

