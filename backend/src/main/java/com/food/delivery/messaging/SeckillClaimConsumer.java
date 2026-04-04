package com.food.delivery.messaging;

import com.food.delivery.dto.messaging.SeckillClaimEvent;
import com.food.delivery.seckill.SeckillClaimPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 异步落库：Lua 已通过一人一单与防超卖闸门后，由消费者幂等写入 user_coupon 并扣减 DB 库存。
 */
@Component
@ConditionalOnProperty(prefix = "app.seckill", name = "kafka-enabled", havingValue = "true")
public class SeckillClaimConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillClaimConsumer.class);

    private final SeckillClaimPersistenceService persistenceService;

    public SeckillClaimConsumer(SeckillClaimPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @KafkaListener(topics = "${app.seckill.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onClaim(SeckillClaimEvent event) {
        try {
            persistenceService.persistClaim(event);
        } catch (Exception e) {
            log.error("seckill claim consume failed user={} coupon={}", event.getUserId(), event.getCouponId(), e);
            throw e;
        }
    }
}
