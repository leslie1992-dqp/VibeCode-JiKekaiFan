package com.food.delivery.service.order;

import com.food.delivery.vo.order.OrderListItemVO;

import java.util.List;

public interface OrderQueryService {

    List<OrderListItemVO> listOrders(Long userId);
}
