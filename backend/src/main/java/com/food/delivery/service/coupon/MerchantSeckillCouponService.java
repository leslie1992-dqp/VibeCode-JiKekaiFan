package com.food.delivery.service.coupon;

import com.food.delivery.vo.coupon.SeckillCouponVO;

import java.util.List;

public interface MerchantSeckillCouponService {

    List<SeckillCouponVO> listSeckillCoupons(Long merchantId);

    void claim(Long userId, Long couponId);
}
