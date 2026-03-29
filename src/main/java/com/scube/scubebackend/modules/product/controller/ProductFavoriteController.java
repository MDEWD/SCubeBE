package com.scube.scubebackend.modules.product.controller;

import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.product.model.dto.FavoriteRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductVO;
import com.scube.scubebackend.modules.product.service.ProductFavoriteService;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/product/favorites")
public class ProductFavoriteController extends BaseController {

    @Autowired
    private ProductFavoriteService favoriteService;

    /**
     * 添加/取消收藏，幂等
     */
    @PostMapping
    public BaseResponse<String> toggleFavorite(@RequestBody @Valid FavoriteRequest request) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null ) {
            return BaseResponse.error(40100, "未登录");
        }
        boolean result = favoriteService.toggleFavorite(loginUser.getDisplayId(), request.getProductId());
        if (result) {
            // need to determine whether it was add or removed? Service returns true for both success cases
            // We can check exists to respond with proper message
            boolean exists = favoriteService.existsFavorite(loginUser.getDisplayId(), request.getProductId());
            if (exists) {
                return BaseResponse.success("收藏成功", null);
            } else {
                return BaseResponse.success("已取消收藏", null);
            }
        }
        return BaseResponse.error(50001, "操作失败");
    }

    /**
     * 查询收藏列表（支持分页）
     */
    @GetMapping
    public BaseResponse<com.scube.scubebackend.common.model.dto.PageResult<ProductVO>> listFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getDisplayId() == null || loginUser.getDisplayId().isBlank()) {
            return BaseResponse.error(40100, "未登录");
        }
        com.scube.scubebackend.common.model.dto.PageResult<ProductVO> result = favoriteService.listFavoritesPaged(loginUser.getDisplayId(), page, pageSize);
        return BaseResponse.success(result);
    }

    /**
     * 删除指定收藏
     */
    @DeleteMapping("/{productId}")
    public BaseResponse<String> deleteFavorite(@PathVariable String productId) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getDisplayId() == null || loginUser.getDisplayId().isBlank()) {
            return BaseResponse.error(40100, "未登录");
        }
        boolean deleted = favoriteService.deleteFavorite(loginUser.getDisplayId(), productId);
        if (deleted) {
            return BaseResponse.success("删除成功", null);
        }
        return BaseResponse.error(40400, "该商品未收藏");
    }

    /**
     * 检查是否收藏
     */
    @GetMapping("/exist")
    public BaseResponse<Boolean> existFavorite(@RequestParam String productId) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getDisplayId() == null || loginUser.getDisplayId().isBlank()) {
            return BaseResponse.error(40100, "未登录");
        }
        boolean exists = favoriteService.existsFavorite(loginUser.getDisplayId(), productId);
        return BaseResponse.success(exists);
    }
}
