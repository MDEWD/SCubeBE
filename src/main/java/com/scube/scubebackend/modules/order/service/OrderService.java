package com.scube.scubebackend.modules.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderVO;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrdersResponse;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse;
import com.scube.scubebackend.modules.order.model.entity.Order;

public interface OrderService extends IService<Order> {

    PartnerOrdersResponse getPartnerOrders(int page, int pageSize);

    IPage<AdminOrderVO> getAdminOrders(int page, int pageSize);

    CustomerOrdersResponse getCustomerOrders(int page, int pageSize);
}
