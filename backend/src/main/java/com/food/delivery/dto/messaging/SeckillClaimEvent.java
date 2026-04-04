package com.food.delivery.dto.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀领券成功后的落库事件，经 Kafka 异步消费（可降级为同步调用同一套持久化逻辑）。
 */
public class SeckillClaimEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long couponId;
    private Long merchantId;
    private LocalDateTime validUntil;
    private LocalDateTime claimedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }
}
