package com.food.delivery.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 过期且未使用的用户券自动删除（未占用订单）
 */
@Component
public class UserCouponExpiredPurgeScheduler {

    private final UserCouponMapper userCouponMapper;

    public UserCouponExpiredPurgeScheduler(UserCouponMapper userCouponMapper) {
        this.userCouponMapper = userCouponMapper;
    }

    @Scheduled(fixedRate = 60_000)
    public void purgeExpiredUnused() {
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.delete(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .lt(UserCouponEntity::getExpireAt, now)
                        .eq(UserCouponEntity::getStatus, UserCouponStatus.UNUSED)
                        .isNull(UserCouponEntity::getLockedOrderId)
        );
    }
}
