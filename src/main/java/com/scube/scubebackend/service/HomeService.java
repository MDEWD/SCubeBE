package com.scube.scubebackend.service;

import com.scube.scubebackend.model.dto.CarouselVO;
import com.scube.scubebackend.model.dto.ProductListVO;
import com.scube.scubebackend.model.dto.ServiceVO;

import java.util.List;

public interface HomeService {
    List<CarouselVO> getCarousel();
    List<ProductListVO> getHotProducts(Integer limit);
    List<ServiceVO> getServices();
}

