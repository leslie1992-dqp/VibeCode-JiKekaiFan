package com.food.delivery.common.coupon;

public final class UserCouponStatus {

    public static final int UNUSED = 1;
    public static final int USED = 2;
    /** 待支付订单占用 */
    public static final int LOCKED = 3;

    private UserCouponStatus() {
    }
}
