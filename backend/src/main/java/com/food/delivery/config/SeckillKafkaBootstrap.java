package com.food.delivery.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 仅在开启 Kafka 异步落库时注册 Kafka 监听基础设施。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.seckill", name = "kafka-enabled", havingValue = "true")
@EnableKafka
public class SeckillKafkaBootstrap {
}
