package com.food.delivery.common.order;

public final class OrderStatus {

    /** 已支付（成功交易） */
    public static final int PAID_SUCCESS = 1;
    /** 待支付（倒计时内） */
    public static final int PENDING_PAYMENT = 2;
    /** 用户取消（待支付订单） */
    public static final int CANCELLED = 3;
    /** 已派单 */
    public static final int DELIVERY_ASSIGNED = 4;
    /** 骑手已到店 */
    public static final int DELIVERY_ARRIVED_AT_MERCHANT = 9;
    /** 已取餐（前往用户前） */
    public static final int DELIVERY_PICKING_UP = 5;
    /** 配送中 */
    public static final int DELIVERY_IN_TRANSIT = 6;
    /** 已送达 */
    public static final int DELIVERY_DONE = 7;
    /** 配送失败 */
    public static final int DELIVERY_FAILED = 8;

    private OrderStatus() {
    }
}
