package com.food.delivery.impl.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import com.food.delivery.service.coupon.UserCouponLifecycleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserCouponLifecycleServiceImpl implements UserCouponLifecycleService {

    private final UserCouponMapper userCouponMapper;

    public UserCouponLifecycleServiceImpl(UserCouponMapper userCouponMapper) {
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockForPendingOrder(Long userCouponId, Long orderId) {
        if (userCouponId == null || orderId == null) {
            return;
        }
        UserCouponEntity uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            throw new BizException(40406, "user coupon not found");
        }
        if (uc.getStatus() == null || uc.getStatus() != UserCouponStatus.UNUSED) {
            throw new BizException(40013, "coupon unavailable");
        }
        if (uc.getLockedOrderId() != null) {
            throw new BizException(40013, "coupon unavailable");
        }
        LocalDateTime now = LocalDateTime.now();
        uc.setStatus(UserCouponStatus.LOCKED);
        uc.setLockedOrderId(orderId);
        uc.setUpdatedAt(now);
        userCouponMapper.updateById(uc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseByOrderId(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<UserCouponEntity> list = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getLockedOrderId, orderId)
        );
        if (list == null || list.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (UserCouponEntity uc : list) {
            if (uc == null || uc.getId() == null) {
                continue;
            }
            if (uc.getStatus() != null && uc.getStatus() == UserCouponStatus.LOCKED) {
                // updateById 默认不更新 null 字段，locked_order_id 无法清空，会导致后续无法再次核销
                userCouponMapper.update(
                        null,
                        new LambdaUpdateWrapper<UserCouponEntity>()
                                .eq(UserCouponEntity::getId, uc.getId())
                                .set(UserCouponEntity::getStatus, UserCouponStatus.UNUSED)
                                .set(UserCouponEntity::getUpdatedAt, now)
                                .setSql("locked_order_id = NULL")
                );
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markUsed(Long userCouponId) {
        if (userCouponId == null) {
            return;
        }
        UserCouponEntity uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.update(
                null,
                new LambdaUpdateWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getId, userCouponId)
                        .set(UserCouponEntity::getStatus, UserCouponStatus.USED)
                        .set(UserCouponEntity::getUpdatedAt, now)
                        .setSql("locked_order_id = NULL")
        );
    }
}
