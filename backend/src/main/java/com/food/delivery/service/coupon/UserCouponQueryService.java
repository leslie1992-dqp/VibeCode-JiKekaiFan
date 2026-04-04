package com.food.delivery.service.coupon;

import com.food.delivery.vo.coupon.UserCouponListItemVO;

import java.util.List;

public interface UserCouponQueryService {

    List<UserCouponListItemVO> listMine(Long userId);

    /**
     * 当前用户在该商家已领取的秒杀券模板 id（用于前端「抢」按钮置灰）
     */
    List<Long> listClaimedSeckillCouponIds(Long userId, Long merchantId);
}
