package com.food.delivery.common.delivery;

public final class DeliveryTaskStatus {

    public static final int WAITING_DISPATCH = 10;
    public static final int ASSIGNED = 20;
    /** 骑手已到店，待取餐 */
    public static final int ARRIVED_AT_MERCHANT = 25;
    public static final int PICKING_UP = 30;
    public static final int DELIVERING = 40;
    public static final int DELIVERED = 50;
    public static final int CANCELLED = 60;
    public static final int FAILED = 70;

    private DeliveryTaskStatus() {
    }
}
