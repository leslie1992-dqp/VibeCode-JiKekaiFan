package com.food.delivery.impl.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.mapper.coupon.MerchantSeckillCouponMapper;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import com.food.delivery.service.coupon.MerchantSeckillCouponService;
import com.food.delivery.vo.coupon.SeckillCouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantSeckillCouponServiceImpl implements MerchantSeckillCouponService {

    private final MerchantSeckillCouponMapper seckillCouponMapper;
    private final UserCouponMapper userCouponMapper;

    public MerchantSeckillCouponServiceImpl(
            MerchantSeckillCouponMapper seckillCouponMapper,
            UserCouponMapper userCouponMapper
    ) {
        this.seckillCouponMapper = seckillCouponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public List<SeckillCouponVO> listSeckillCoupons(Long merchantId) {
        if (merchantId == null || merchantId < 1) {
            return Collections.emptyList();
        }
        LocalDateTime now = LocalDateTime.now();
        List<MerchantSeckillCouponEntity> rows = seckillCouponMapper.selectList(
                new LambdaQueryWrapper<MerchantSeckillCouponEntity>()
                        .eq(MerchantSeckillCouponEntity::getMerchantId, merchantId)
                        .eq(MerchantSeckillCouponEntity::getStatus, 1)
                        .gt(MerchantSeckillCouponEntity::getStockRemain, 0)
                        .le(MerchantSeckillCouponEntity::getValidFrom, now)
                        .ge(MerchantSeckillCouponEntity::getValidUntil, now)
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long userId, Long couponId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (couponId == null || couponId < 1) {
            throw new BizException(40001, "invalid request");
        }

        MerchantSeckillCouponEntity tpl = seckillCouponMapper.selectById(couponId);
        if (tpl == null || tpl.getStatus() == null || tpl.getStatus() != 1) {
            throw new BizException(40406, "coupon not found");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(tpl.getValidFrom()) || now.isAfter(tpl.getValidUntil())) {
            throw new BizException(40011, "coupon not in valid period");
        }
        if (tpl.getStockRemain() == null || tpl.getStockRemain() <= 0) {
            throw new BizException(40012, "coupon sold out");
        }

        Long merchantId = tpl.getMerchantId();
        UserCouponEntity existed = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getUserId, userId)
                        .eq(UserCouponEntity::getSeckillCouponId, couponId)
        );
        if (existed != null) {
            throw new BizException(40903, "already claimed");
        }

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

    private SeckillCouponVO toVo(MerchantSeckillCouponEntity e) {
        SeckillCouponVO v = new SeckillCouponVO();
        v.setId(e.getId());
        v.setMerchantId(e.getMerchantId());
        v.setTitle(e.getTitle());
        v.setThresholdAmount(e.getThresholdAmount());
        v.setDiscountAmount(e.getDiscountAmount());
        v.setStockRemain(e.getStockRemain());
        v.setValidFrom(e.getValidFrom());
        v.setValidUntil(e.getValidUntil());
        return v;
    }
}
