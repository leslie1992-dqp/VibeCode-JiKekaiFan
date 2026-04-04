package com.food.delivery.service.order;

/**
 * 订单成交后对商品销量、商家月售的统计。
 */
public interface OrderSalesService {

    /**
     * 在订单已支付（{@link com.food.delivery.common.order.OrderStatus#PAID_SUCCESS}）且明细已落库后调用。
     */
    void applyPaidOrder(Long orderId);
}
