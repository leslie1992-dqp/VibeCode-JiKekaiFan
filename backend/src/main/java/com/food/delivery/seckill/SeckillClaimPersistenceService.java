package com.food.delivery.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.dto.messaging.SeckillClaimEvent;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.mapper.coupon.MerchantSeckillCouponMapper;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 将「已通过 Redis 闸门」的领券请求落库：先插入用户券（唯一约束防重复），再扣减 DB 库存，保证与消息幂等。
 */
@Service
public class SeckillClaimPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(SeckillClaimPersistenceService.class);

    private final UserCouponMapper userCouponMapper;
    private final MerchantSeckillCouponMapper seckillCouponMapper;

    public SeckillClaimPersistenceService(
            UserCouponMapper userCouponMapper,
            MerchantSeckillCouponMapper seckillCouponMapper
    ) {
        this.userCouponMapper = userCouponMapper;
        this.seckillCouponMapper = seckillCouponMapper;
    }

    /**
     * Kafka 消费者或同步降级路径调用。幂等：已存在 (userId, seckillCouponId) 则直接返回。
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistClaim(SeckillClaimEvent event) {
        Long userId = event.getUserId();
        Long couponId = event.getCouponId();
        if (userId == null || couponId == null) {
            return;
        }

        UserCouponEntity existed = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getUserId, userId)
                        .eq(UserCouponEntity::getSeckillCouponId, couponId)
        );
        if (existed != null) {
            return;
        }

        LocalDateTime now = event.getClaimedAt() != null ? event.getClaimedAt() : LocalDateTime.now();
        UserCouponEntity uc = new UserCouponEntity();
        uc.setUserId(userId);
        uc.setMerchantId(event.getMerchantId());
        uc.setSeckillCouponId(couponId);
        uc.setStatus(UserCouponStatus.UNUSED);
        uc.setClaimedAt(now);
        uc.setExpireAt(event.getValidUntil());
        uc.setLockedOrderId(null);
        uc.setCreatedAt(now);
        uc.setUpdatedAt(now);

        try {
            userCouponMapper.insert(uc);
        } catch (DataIntegrityViolationException ex) {
            log.debug("seckill claim duplicate insert ignored user={} coupon={}", userId, couponId);
            return;
        }

        LocalDateTime updateAt = LocalDateTime.now();
        int rows = seckillCouponMapper.update(
                null,
                new LambdaUpdateWrapper<MerchantSeckillCouponEntity>()
                        .setSql("stock_remain = stock_remain - 1")
                        .set(MerchantSeckillCouponEntity::getUpdatedAt, updateAt)
                        .eq(MerchantSeckillCouponEntity::getId, couponId)
                        .eq(MerchantSeckillCouponEntity::getStatus, 1)
                        .gt(MerchantSeckillCouponEntity::getStockRemain, 0)
        );
        if (rows == 0) {
            throw new IllegalStateException(
                    "seckill DB stock not decremented after insert user=" + userId + " coupon=" + couponId
            );
        }
    }
}
