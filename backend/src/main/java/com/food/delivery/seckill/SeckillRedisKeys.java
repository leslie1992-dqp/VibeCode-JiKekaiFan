package com.food.delivery.seckill;

/**
 * Redis Key 约定：秒杀券库存与已领用户集合。
 */
public final class SeckillRedisKeys {

    private SeckillRedisKeys() {
    }

    public static String stockKey(long couponId) {
        return "seckill:stock:" + couponId;
    }

    public static String claimedSetKey(long couponId) {
        return "seckill:claimed:" + couponId;
    }
}
