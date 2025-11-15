package com.scube.scubebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scube.scubebackend.mapper.CarouselMapper;
import com.scube.scubebackend.mapper.ProductMapper;
import com.scube.scubebackend.mapper.ServiceMapper;
import com.scube.scubebackend.model.dto.CarouselVO;
import com.scube.scubebackend.model.dto.ProductListVO;
import com.scube.scubebackend.model.dto.ServiceVO;
import com.scube.scubebackend.model.entity.Carousel;
import com.scube.scubebackend.model.entity.Product;
import com.scube.scubebackend.model.entity.Service;
import com.scube.scubebackend.service.HomeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomeServiceImpl implements HomeService {
    
    @Autowired
    private CarouselMapper carouselMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private ServiceMapper serviceMapper;
    
    @Override
    public List<CarouselVO> getCarousel() {
        QueryWrapper<Carousel> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("order");
        List<Carousel> carousels = carouselMapper.selectList(queryWrapper);
        return carousels.stream().map(carousel -> {
            CarouselVO vo = new CarouselVO();
            BeanUtils.copyProperties(carousel, vo);
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<ProductListVO> getHotProducts(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 4;
        }
        
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getIsHot, 1)
                .eq(Product::getIsDelete, 0)
                .eq(Product::getStatus, "ACTIVE")
                .orderByDesc(Product::getViewCount)
                .last("LIMIT " + limit);
        
        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream().map(product -> {
            ProductListVO vo = new ProductListVO();
            BeanUtils.copyProperties(product, vo);
            vo.setIsHot(product.getIsHot() == 1);
            vo.setIsNew(product.getIsNew() == 1);
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<ServiceVO> getServices() {
        List<Service> services = serviceMapper.selectList(null);
        return services.stream().map(service -> {
            ServiceVO vo = new ServiceVO();
            BeanUtils.copyProperties(service, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}

