package com.scube.scubebackend.modules.order.controller;

import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse;
import com.scube.scubebackend.modules.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partner")
public class PartnerController extends BaseController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public BaseResponse<PartnerOrdersResponse> getPartnerOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PartnerOrdersResponse response = orderService.getPartnerOrders(page, pageSize);
        return BaseResponse.success(response);
    }
}

