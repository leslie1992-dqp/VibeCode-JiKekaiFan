package com.food.delivery.impl.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.config.AppProperties;
import com.food.delivery.dto.messaging.SeckillClaimEvent;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.mapper.coupon.MerchantSeckillCouponMapper;
import com.food.delivery.seckill.SeckillClaimPersistenceService;
import com.food.delivery.seckill.SeckillLuaClaimService;
import com.food.delivery.service.coupon.MerchantSeckillCouponService;
import com.food.delivery.vo.coupon.SeckillCouponVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class MerchantSeckillCouponServiceImpl implements MerchantSeckillCouponService {

    private static final Logger log = LoggerFactory.getLogger(MerchantSeckillCouponServiceImpl.class);

    private static final int KAFKA_SEND_TIMEOUT_SEC = 10;

    private final MerchantSeckillCouponMapper seckillCouponMapper;
    private final AppProperties appProperties;
    private final SeckillLuaClaimService seckillLuaClaimService;
    private final SeckillClaimPersistenceService seckillClaimPersistenceService;
    private final KafkaTemplate<String, SeckillClaimEvent> seckillKafkaTemplate;
    private final SeckillDbOnlyClaimService seckillDbOnlyClaimService;

    public MerchantSeckillCouponServiceImpl(
            MerchantSeckillCouponMapper seckillCouponMapper,
            AppProperties appProperties,
            SeckillLuaClaimService seckillLuaClaimService,
            SeckillClaimPersistenceService seckillClaimPersistenceService,
            @Qualifier("seckillKafkaTemplate") KafkaTemplate<String, SeckillClaimEvent> seckillKafkaTemplate,
            SeckillDbOnlyClaimService seckillDbOnlyClaimService
    ) {
        this.seckillCouponMapper = seckillCouponMapper;
        this.appProperties = appProperties;
        this.seckillLuaClaimService = seckillLuaClaimService;
        this.seckillClaimPersistenceService = seckillClaimPersistenceService;
        this.seckillKafkaTemplate = seckillKafkaTemplate;
        this.seckillDbOnlyClaimService = seckillDbOnlyClaimService;
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
    public void claim(Long userId, Long couponId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (couponId == null || couponId < 1) {
            throw new BizException(40001, "invalid request");
        }

        MerchantSeckillCouponEntity tpl = loadAndValidateTemplate(couponId);
        if (!appProperties.getSeckill().isRedisLuaEnabled()) {
            seckillDbOnlyClaimService.claim(userId, couponId, tpl);
            return;
        }

        int remain = tpl.getStockRemain() == null ? 0 : tpl.getStockRemain();
        seckillLuaClaimService.warmStockIfAbsent(couponId, remain);
        long lua = seckillLuaClaimService.tryClaim(userId, couponId);
        if (lua == SeckillLuaClaimService.LUA_NOT_INITIALIZED) {
            seckillLuaClaimService.warmStockIfAbsent(couponId, remain);
            lua = seckillLuaClaimService.tryClaim(userId, couponId);
        }

        if (lua == SeckillLuaClaimService.LUA_ALREADY_CLAIMED) {
            throw new BizException(40903, "already claimed");
        }
        if (lua == SeckillLuaClaimService.LUA_SOLD_OUT) {
            throw new BizException(40012, "coupon sold out");
        }
        if (lua != SeckillLuaClaimService.LUA_OK) {
            throw new BizException(50002, "seckill redis gate error");
        }

        SeckillClaimEvent event = buildEvent(userId, tpl);

        if (appProperties.getSeckill().isKafkaEnabled()) {
            try {
                CompletableFuture<SendResult<String, SeckillClaimEvent>> future = seckillKafkaTemplate.send(
                        appProperties.getSeckill().getKafkaTopic(),
                        String.valueOf(couponId),
                        event
                );
                future.get(KAFKA_SEND_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                seckillLuaClaimService.rollbackRedisClaim(userId, couponId);
                throw new BizException(50003, "领取被中断，请重试");
            } catch (ExecutionException | TimeoutException e) {
                log.warn("seckill kafka send failed user={} coupon={}", userId, couponId, e);
                seckillLuaClaimService.rollbackRedisClaim(userId, couponId);
                throw new BizException(50003, "领取提交失败，请稍后重试");
            }
        } else {
            seckillClaimPersistenceService.persistClaim(event);
        }
    }

    private MerchantSeckillCouponEntity loadAndValidateTemplate(Long couponId) {
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
        return tpl;
    }

    private static SeckillClaimEvent buildEvent(Long userId, MerchantSeckillCouponEntity tpl) {
        LocalDateTime now = LocalDateTime.now();
        SeckillClaimEvent event = new SeckillClaimEvent();
        event.setUserId(userId);
        event.setCouponId(tpl.getId());
        event.setMerchantId(tpl.getMerchantId());
        event.setValidUntil(tpl.getValidUntil());
        event.setClaimedAt(now);
        return event;
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
