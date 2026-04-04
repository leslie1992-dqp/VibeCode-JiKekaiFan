package com.food.delivery.impl.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.mapper.coupon.MerchantSeckillCouponMapper;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 关闭 Redis 时的纯 DB 领券（独立 Bean 以保证 {@link Transactional} 代理生效）。
 */
@Service
public class SeckillDbOnlyClaimService {

    private final MerchantSeckillCouponMapper seckillCouponMapper;
    private final UserCouponMapper userCouponMapper;

    public SeckillDbOnlyClaimService(
            MerchantSeckillCouponMapper seckillCouponMapper,
            UserCouponMapper userCouponMapper
    ) {
        this.seckillCouponMapper = seckillCouponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void claim(Long userId, Long couponId, MerchantSeckillCouponEntity tpl) {
        Long merchantId = tpl.getMerchantId();
        UserCouponEntity existed = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getUserId, userId)
                        .eq(UserCouponEntity::getSeckillCouponId, couponId)
        );
        if (existed != null) {
            throw new BizException(40903, "already claimed");
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = seckillCouponMapper.update(
                null,
                new LambdaUpdateWrapper<MerchantSeckillCouponEntity>()
                        .setSql("stock_remain = stock_remain - 1")
                        .set(MerchantSeckillCouponEntity::getUpdatedAt, now)
                        .eq(MerchantSeckillCouponEntity::getId, couponId)
                        .eq(MerchantSeckillCouponEntity::getStatus, 1)
                        .gt(MerchantSeckillCouponEntity::getStockRemain, 0)
        );
        if (rows == 0) {
            throw new BizException(40012, "coupon sold out");
        }

        UserCouponEntity uc = new UserCouponEntity();
        uc.setUserId(userId);
        uc.setMerchantId(merchantId);
        uc.setSeckillCouponId(couponId);
        uc.setStatus(UserCouponStatus.UNUSED);
        uc.setClaimedAt(now);
        uc.setExpireAt(tpl.getValidUntil());
        uc.setLockedOrderId(null);
        uc.setCreatedAt(now);
        uc.setUpdatedAt(now);
        userCouponMapper.insert(uc);
    }
}
