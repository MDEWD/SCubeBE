package com.scube.scubebackend.modules.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderVO;
import com.scube.scubebackend.modules.admin.model.dto.AdminUserVO;
import com.scube.scubebackend.modules.admin.model.dto.AuditDecision;
import com.scube.scubebackend.modules.admin.model.dto.AuditRequest;
import com.scube.scubebackend.modules.order.service.OrderService;
import com.scube.scubebackend.modules.product.service.ProductService;
import com.scube.scubebackend.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public BaseResponse<List<AdminUserVO>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<AdminUserVO> userPage = userService.getAllUsers(page, pageSize);
        return BaseResponse.success(userPage.getRecords());
    }

    /**
     * 获取全平台订单（双边台账）
     */
    @GetMapping("/orders")
    public BaseResponse<List<AdminOrderVO>> getAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<AdminOrderVO> orderPage = orderService.getAdminOrders(page, pageSize);
        return BaseResponse.success(orderPage.getRecords());
    }

    /**
     * 获取审核列表
     */
    @GetMapping("/audits")
    public BaseResponse<List<AuditRequest>> getAudits() {
        List<AuditRequest> audits = productService.getPendingAudits();
        return BaseResponse.success(audits);
    }

    /**
     * 处理审核 (通过/驳回)
     */
    @PostMapping("/audits/{auditId}/decision")
    public BaseResponse<String> auditDecision(
            @PathVariable String auditId,
            @RequestBody AuditDecision decision) {
        productService.auditProduct(auditId, decision);
        return BaseResponse.success("Operation successful");
    }
}

