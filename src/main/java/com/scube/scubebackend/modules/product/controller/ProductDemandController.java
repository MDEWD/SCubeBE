package com.scube.scubebackend.modules.product.controller;

import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductDemandRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductDemandVO;
import com.scube.scubebackend.modules.product.service.ProductDemandService;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/product/demand")
public class ProductDemandController extends BaseController {

    @Autowired
    private ProductDemandService demandService;

    /**
     * 发布需求（普通用户或合作伙伴）
     */
    @PostMapping
    public BaseResponse<ProductDemandVO> createDemand(@RequestBody @Valid ProductDemandRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductDemandVO vo = demandService.createDemand(request, loginUser);
        return BaseResponse.success("发布成功", vo);
    }

    /**
     * 用户/合作伙伴查询自己的需求
     */
    @GetMapping("/my")
    public BaseResponse<PageResult<ProductDemandVO>> getMyDemands(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LoginUser loginUser = getLoginUser();
        PageResult<ProductDemandVO> result = demandService.getMyDemands(loginUser, page, pageSize);
        return BaseResponse.success(result);
    }

    /**
     * 管理员查询所有需求
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<PageResult<ProductDemandVO>> getAllDemands(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<ProductDemandVO> result = demandService.getAllDemands(page, pageSize);
        return BaseResponse.success(result);
    }

    /**
     * 更新需求（仅发布者或管理员）
     */
    @PutMapping("/{id}")
    public BaseResponse<ProductDemandVO> updateDemand(@PathVariable Long id, @RequestBody @Valid ProductDemandRequest request) {
        LoginUser loginUser = getLoginUser();
        ProductDemandVO vo = demandService.updateDemand(id, request, loginUser);
        return BaseResponse.success("更新成功", vo);
    }

    /**
     * 删除需求（逻辑删除，仅发布者或管理员）
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteDemand(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        boolean ok = demandService.deleteDemand(id, loginUser);
        return BaseResponse.success(ok);
    }
}
