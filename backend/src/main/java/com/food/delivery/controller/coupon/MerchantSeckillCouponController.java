package com.food.delivery.controller.coupon;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.coupon.MerchantSeckillCouponService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MerchantSeckillCouponController {

    private final MerchantSeckillCouponService merchantSeckillCouponService;

    public MerchantSeckillCouponController(MerchantSeckillCouponService merchantSeckillCouponService) {
        this.merchantSeckillCouponService = merchantSeckillCouponService;
    }

    @PostMapping("/api/v1/merchant-seckill-coupons/{couponId}/claim")
    public Result<Void> claim(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long couponId
    ) {
        merchantSeckillCouponService.claim(userId, couponId);
        return Result.success(null);
    }
}
