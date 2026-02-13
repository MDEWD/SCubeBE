package com.scube.scubebackend.modules.portal.controller;

import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.portal.model.dto.CarouselVO;
import com.scube.scubebackend.modules.product.model.dto.ProductListVO;
import com.scube.scubebackend.modules.portal.model.dto.ServiceVO;
import com.scube.scubebackend.modules.portal.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    
    @Autowired
    private HomeService homeService;
    
    @GetMapping("/carousel")
    public BaseResponse<List<CarouselVO>> getCarousel() {
        List<CarouselVO> carousels = homeService.getCarousel();
        return BaseResponse.success(carousels);
    }
    
    @GetMapping("/hot-products")
    public BaseResponse<List<ProductListVO>> getHotProducts(@RequestParam(required = false, defaultValue = "4") Integer limit) {
        List<ProductListVO> products = homeService.getHotProducts(limit);
        return BaseResponse.success(products);
    }
    
    @GetMapping("/services")
    public BaseResponse<List<ServiceVO>> getServices() {
        List<ServiceVO> services = homeService.getServices();
        return BaseResponse.success(services);
    }
}

