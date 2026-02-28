package com.scube.scubebackend.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderVO;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrderVO;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse;
import com.scube.scubebackend.modules.order.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    IPage<CustomerOrderVO> selectCustomerOrders(Page<?> page, @Param("userId") Long userId, @Param("status") String status);

    IPage<PartnerOrdersResponse.PartnerOrderVO> selectPartnerOrders(Page<?> page, @Param("supplierId") Long supplierId);

    IPage<AdminOrderVO> selectAdminOrders(Page<?> page);

    PartnerOrdersResponse.PartnerStatsVO selectPartnerStats(@Param("supplierId") Long supplierId);
}

