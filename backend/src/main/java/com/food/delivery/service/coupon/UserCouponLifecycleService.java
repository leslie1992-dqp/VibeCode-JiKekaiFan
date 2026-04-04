package com.food.delivery.service.coupon;

public interface UserCouponLifecycleService {

    void lockForPendingOrder(Long userCouponId, Long orderId);

    void releaseByOrderId(Long orderId);

    void markUsed(Long userCouponId);
}
