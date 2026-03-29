package com.scube.scubebackend.modules.product.service;

import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.product.model.dto.ProductDemandRequest;
import com.scube.scubebackend.modules.product.model.dto.ProductDemandVO;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;

public interface ProductDemandService {
    ProductDemandVO createDemand(ProductDemandRequest request, LoginUser loginUser);
    PageResult<ProductDemandVO> getMyDemands(LoginUser loginUser, int page, int pageSize);
    PageResult<ProductDemandVO> getAllDemands(int page, int pageSize);
    ProductDemandVO updateDemand(Long id, ProductDemandRequest request, LoginUser loginUser);
    boolean deleteDemand(Long id, LoginUser loginUser);
}
