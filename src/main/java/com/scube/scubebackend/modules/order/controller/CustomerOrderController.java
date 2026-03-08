package com.scube.scubebackend.modules.order.controller;

import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrdersResponse;
import com.scube.scubebackend.modules.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通用户订单查询（与合作伙伴查询逻辑一致：使用登录用户displayId匹配order表中的supplier_display_id）
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerOrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public BaseResponse<CustomerOrdersResponse> getUserOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        CustomerOrdersResponse response = orderService.getCustomerOrders(page, pageSize);
        return BaseResponse.success(response);
    }
}
