package com.scube.scubebackend.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.modules.order.model.dto.AdminOrderVO;
import com.scube.scubebackend.modules.order.model.dto.CustomerOrdersResponse;
import com.scube.scubebackend.modules.order.model.dto.PartnerOrdersResponse;
import com.scube.scubebackend.modules.order.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    IPage<PartnerOrdersResponse.PartnerOrderVO> selectPartnerOrders(Page<?> page, @Param("supplierDisplayId") String supplierDisplayId);

    IPage<CustomerOrdersResponse.CustomerOrderVO> selectUserOrdersByDisplayId(Page<?> page, @Param("userDisplayId") String userDisplayId);

    IPage<AdminOrderVO> selectAdminOrders(Page<?> page);

}
