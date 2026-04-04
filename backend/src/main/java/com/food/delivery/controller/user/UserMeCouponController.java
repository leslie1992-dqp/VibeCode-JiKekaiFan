package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.coupon.UserCouponQueryService;
import com.food.delivery.vo.coupon.UserCouponListItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserMeCouponController {

    private final UserCouponQueryService userCouponQueryService;

    public UserMeCouponController(UserCouponQueryService userCouponQueryService) {
        this.userCouponQueryService = userCouponQueryService;
    }

    @GetMapping("/coupons")
    public Result<List<UserCouponListItemVO>> listCoupons(@RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId) {
        return Result.success(userCouponQueryService.listMine(userId));
    }

    @GetMapping("/claimed-seckill-ids")
    public Result<List<Long>> claimedSeckillIds(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @RequestParam Long merchantId
    ) {
        return Result.success(userCouponQueryService.listClaimedSeckillCouponIds(userId, merchantId));
    }
}
