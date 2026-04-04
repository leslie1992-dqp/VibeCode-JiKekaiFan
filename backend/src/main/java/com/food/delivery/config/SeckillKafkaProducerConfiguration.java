package com.food.delivery.config;

import com.food.delivery.dto.messaging.SeckillClaimEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀领券专用 Producer（JSON 序列化 {@link SeckillClaimEvent}），与默认 {@link KafkaTemplate} 区分。
 */
@Configuration
public class SeckillKafkaProducerConfiguration {

    @Bean(name = "seckillKafkaTemplate")
    public KafkaTemplate<String, SeckillClaimEvent> seckillKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers:127.0.0.1:9092}") String bootstrapServers
    ) {
        return new KafkaTemplate<>(seckillClaimProducerFactory(bootstrapServers));
    }

    private static ProducerFactory<String, SeckillClaimEvent> seckillClaimProducerFactory(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put("spring.json.add.type.headers", false);
        return new DefaultKafkaProducerFactory<>(props);
    }
}
