package com.food.delivery.impl.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.mapper.coupon.MerchantSeckillCouponMapper;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.service.coupon.UserCouponQueryService;
import com.food.delivery.vo.coupon.UserCouponListItemVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserCouponQueryServiceImpl implements UserCouponQueryService {

    private final UserCouponMapper userCouponMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantSeckillCouponMapper seckillCouponMapper;

    public UserCouponQueryServiceImpl(
            UserCouponMapper userCouponMapper,
            MerchantMapper merchantMapper,
            MerchantSeckillCouponMapper seckillCouponMapper
    ) {
        this.userCouponMapper = userCouponMapper;
        this.merchantMapper = merchantMapper;
        this.seckillCouponMapper = seckillCouponMapper;
    }

    @Override
    public List<UserCouponListItemVO> listMine(Long userId) {
        if (userId == null || userId < 1) {
            return Collections.emptyList();
        }
        List<UserCouponEntity> rows = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getUserId, userId)
                        .ne(UserCouponEntity::getStatus, UserCouponStatus.USED)
                        .orderByDesc(UserCouponEntity::getExpireAt)
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserCouponListItemVO> out = new ArrayList<>();
        for (UserCouponEntity uc : rows) {
            if (uc == null) {
                continue;
            }
            MerchantEntity m = merchantMapper.selectById(uc.getMerchantId());
            MerchantSeckillCouponEntity t = seckillCouponMapper.selectById(uc.getSeckillCouponId());
            UserCouponListItemVO vo = new UserCouponListItemVO();
            vo.setId(uc.getId());
            vo.setMerchantId(uc.getMerchantId());
            vo.setMerchantName(m == null ? "—" : m.getName());
            vo.setExpireAt(uc.getExpireAt());
            vo.setStatus(uc.getStatus());
            if (t != null) {
                vo.setTitle(t.getTitle());
                vo.setThresholdAmount(t.getThresholdAmount());
                vo.setDiscountAmount(t.getDiscountAmount());
            } else {
                vo.setTitle("—");
            }
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<Long> listClaimedSeckillCouponIds(Long userId, Long merchantId) {
        if (userId == null || userId < 1 || merchantId == null || merchantId < 1) {
            return Collections.emptyList();
        }
        List<UserCouponEntity> rows = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getUserId, userId)
                        .eq(UserCouponEntity::getMerchantId, merchantId)
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(UserCouponEntity::getSeckillCouponId)
                .distinct()
                .collect(Collectors.toList());
    }
}
