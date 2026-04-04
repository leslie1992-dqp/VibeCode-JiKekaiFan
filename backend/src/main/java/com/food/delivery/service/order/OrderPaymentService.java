package com.food.delivery.service.order;

public interface OrderPaymentService {

    void payPendingOrder(Long userId, Long orderId);

    void cancelPendingOrder(Long userId, Long orderId);
}
