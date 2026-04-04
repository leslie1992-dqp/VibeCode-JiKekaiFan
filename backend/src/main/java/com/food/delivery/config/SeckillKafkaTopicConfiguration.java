package com.food.delivery.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 开启 Kafka 异步落库时，由 Spring {@link org.springframework.kafka.core.KafkaAdmin} 在启动时创建 topic
 * （需 broker 可达且具备建 topic 权限；单节点开发环境副本数固定为 1）。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.seckill", name = "kafka-enabled", havingValue = "true")
public class SeckillKafkaTopicConfiguration {

    @Bean
    public NewTopic seckillClaimTopic(@Value("${app.seckill.kafka-topic}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
