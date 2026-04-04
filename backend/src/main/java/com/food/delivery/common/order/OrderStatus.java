package com.food.delivery.common.order;

public final class OrderStatus {

    /** 已支付（成功交易） */
    public static final int PAID_SUCCESS = 1;
    /** 待支付（倒计时内） */
    public static final int PENDING_PAYMENT = 2;
    /** 用户取消（待支付订单） */
    public static final int CANCELLED = 3;

    private OrderStatus() {
    }
}
