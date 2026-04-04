package com.food.delivery.impl.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.coupon.UserCouponStatus;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.coupon.UserCouponEntity;
import com.food.delivery.mapper.coupon.MerchantSeckillCouponMapper;
import com.food.delivery.mapper.coupon.UserCouponMapper;
import com.food.delivery.vo.draft.DraftMerchantVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DraftCouponSupport {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final UserCouponMapper userCouponMapper;
    private final MerchantSeckillCouponMapper seckillCouponMapper;

    public DraftCouponSupport(UserCouponMapper userCouponMapper, MerchantSeckillCouponMapper seckillCouponMapper) {
        this.userCouponMapper = userCouponMapper;
        this.seckillCouponMapper = seckillCouponMapper;
    }

    public void applyToDraftVo(
            DraftMerchantVO vo,
            Long userId,
            Long merchantId,
            BigDecimal goodsSum,
            BigDecimal delivery
    ) {
        BigDecimal orderTotal = goodsSum.add(delivery);
        vo.setCouponAmount(ZERO);
        vo.setCouponCode(null);
        vo.setCouponLineStatus("none");
        vo.setCouponRuleText(null);
        vo.setCouponHint("无");

        if (userId == null || userId < 1) {
            vo.setCouponLineStatus("none");
            vo.setOriginalAmount(orderTotal);
            vo.setPayableAmount(orderTotal.max(ZERO));
            return;
        }

        CouponPick pick = pickCoupon(userId, merchantId, goodsSum, LocalDateTime.now());
        if (pick.ucList().isEmpty()) {
            vo.setCouponLineStatus("no_coupon");
            vo.setCouponHint("无");
            vo.setOriginalAmount(orderTotal);
            vo.setPayableAmount(orderTotal.max(ZERO));
            return;
        }

        if (!pick.anyEligible()) {
            MerchantSeckillCouponEntity t = pick.firstTemplate();
            vo.setCouponLineStatus("ineligible");
            vo.setCouponRuleText(formatRule(t));
            vo.setCouponHint("暂不可用");
            vo.setCouponCode(t.getId() == null ? null : String.valueOf(t.getId()));
            vo.setOriginalAmount(orderTotal);
            vo.setPayableAmount(orderTotal.max(ZERO));
            return;
        }

        UserCouponEntity uc = pick.bestUc();
        MerchantSeckillCouponEntity tpl = pick.bestTpl();
        BigDecimal discount = capDiscount(tpl.getDiscountAmount(), orderTotal);
        vo.setCouponLineStatus("applied");
        vo.setCouponRuleText(formatRule(tpl));
        vo.setCouponHint("已减¥" + discount.toPlainString());
        vo.setCouponAmount(discount);
        vo.setCouponCode("UC-" + uc.getId());
        vo.setOriginalAmount(orderTotal);
        vo.setPayableAmount(orderTotal.subtract(discount).max(ZERO));
    }

    public CouponApplyResult resolveForCheckout(
            Long userId,
            Long merchantId,
            BigDecimal goodsSum,
            BigDecimal orderTotal
    ) {
        if (userId == null || userId < 1) {
            return null;
        }
        CouponPick pick = pickCoupon(userId, merchantId, goodsSum, LocalDateTime.now());
        if (pick == null || !pick.anyEligible() || pick.bestUc() == null) {
            return null;
        }
        BigDecimal discount = capDiscount(pick.bestTpl().getDiscountAmount(), orderTotal);
        if (discount.compareTo(ZERO) <= 0) {
            return null;
        }
        return new CouponApplyResult(pick.bestUc().getId(), discount, pick.bestTpl());
    }

    private BigDecimal capDiscount(BigDecimal raw, BigDecimal orderTotal) {
        BigDecimal d = raw == null ? ZERO : raw.setScale(2, RoundingMode.HALF_UP);
        if (orderTotal == null) {
            return ZERO;
        }
        return d.min(orderTotal).max(ZERO);
    }

    private String formatRule(MerchantSeckillCouponEntity t) {
        if (t == null) {
            return "";
        }
        BigDecimal th = t.getThresholdAmount() == null ? ZERO : t.getThresholdAmount();
        BigDecimal di = t.getDiscountAmount() == null ? ZERO : t.getDiscountAmount();
        return "满¥" + th.toPlainString() + "减¥" + di.toPlainString();
    }

    private CouponPick pickCoupon(Long userId, Long merchantId, BigDecimal goodsSum, LocalDateTime now) {
        List<UserCouponEntity> ucs = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCouponEntity>()
                        .eq(UserCouponEntity::getUserId, userId)
                        .eq(UserCouponEntity::getMerchantId, merchantId)
                        .eq(UserCouponEntity::getStatus, UserCouponStatus.UNUSED)
                        .isNull(UserCouponEntity::getLockedOrderId)
        );
        if (ucs == null || ucs.isEmpty()) {
            return new CouponPick(Collections.emptyList(), false, null, null, null, null);
        }

        List<UserCouponEntity> validUc = new ArrayList<>();
        List<MerchantSeckillCouponEntity> validTpl = new ArrayList<>();
        for (UserCouponEntity uc : ucs) {
            if (uc == null || uc.getExpireAt() != null && uc.getExpireAt().isBefore(now)) {
                continue;
            }
            MerchantSeckillCouponEntity tpl = seckillCouponMapper.selectById(uc.getSeckillCouponId());
            if (tpl == null || tpl.getStatus() == null || tpl.getStatus() != 1) {
                continue;
            }
            if (now.isBefore(tpl.getValidFrom()) || now.isAfter(tpl.getValidUntil())) {
                continue;
            }
            validUc.add(uc);
            validTpl.add(tpl);
        }
        if (validUc.isEmpty()) {
            return new CouponPick(Collections.emptyList(), false, null, null, null, null);
        }

        boolean anyEligible = false;
        UserCouponEntity bestUc = null;
        MerchantSeckillCouponEntity bestTpl = null;
        BigDecimal bestDisc = ZERO;

        for (int i = 0; i < validUc.size(); i++) {
            UserCouponEntity uc = validUc.get(i);
            MerchantSeckillCouponEntity tpl = validTpl.get(i);
            BigDecimal thresh = tpl.getThresholdAmount() == null ? ZERO : tpl.getThresholdAmount();
            BigDecimal disc = tpl.getDiscountAmount() == null ? ZERO : tpl.getDiscountAmount();
            if (goodsSum.compareTo(thresh) >= 0) {
                anyEligible = true;
                if (disc.compareTo(bestDisc) > 0) {
                    bestDisc = disc;
                    bestUc = uc;
                    bestTpl = tpl;
                }
            }
        }

        return new CouponPick(validUc, anyEligible, bestUc, bestTpl, validUc.get(0), validTpl.get(0));
    }

    public static final class CouponApplyResult {
        private final Long userCouponId;
        private final BigDecimal discountAmount;
        private final MerchantSeckillCouponEntity template;

        public CouponApplyResult(Long userCouponId, BigDecimal discountAmount, MerchantSeckillCouponEntity template) {
            this.userCouponId = userCouponId;
            this.discountAmount = discountAmount;
            this.template = template;
        }

        public Long getUserCouponId() {
            return userCouponId;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public MerchantSeckillCouponEntity getTemplate() {
            return template;
        }
    }

    private record CouponPick(
            List<UserCouponEntity> ucList,
            boolean anyEligible,
            UserCouponEntity bestUc,
            MerchantSeckillCouponEntity bestTpl,
            UserCouponEntity firstUc,
            MerchantSeckillCouponEntity firstTpl
    ) {
        MerchantSeckillCouponEntity firstTemplate() {
            return firstTpl;
        }
    }
}

