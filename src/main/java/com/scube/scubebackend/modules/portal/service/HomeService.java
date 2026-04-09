package com.scube.scubebackend.modules.portal.service;

import com.scube.scubebackend.modules.portal.model.dto.CarouselVO;
import com.scube.scubebackend.modules.product.model.dto.ProductListVO;
import com.scube.scubebackend.modules.portal.model.dto.ServiceVO;

import java.util.List;

public interface HomeService {
    List<CarouselVO> getCarousel();
    List<ProductListVO> getHotProducts(Integer limit);
    List<ServiceVO> getServices();
}

