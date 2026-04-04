package com.food.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义配置前缀 {@code app}，供 IDE 与 spring-configuration-metadata 识别。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Seckill seckill = new Seckill();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public Seckill getSeckill() {
        return seckill;
    }

    public void setSeckill(Seckill seckill) {
        this.seckill = seckill;
    }

    public static class Upload {
        /** 相对运行目录或绝对路径 */
        private String dir = "food-delivery-uploads";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }

    public static class Jwt {
        /**
         * HS256 签名密钥，生产环境务必替换为足够长度随机串。
         */
        private String secret = "";
        private int expireHours = 24;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpireHours() {
            return expireHours;
        }

        public void setExpireHours(int expireHours) {
            this.expireHours = expireHours;
        }
    }

    /**
     * 秒杀领券：Redis+Lua 限流闸门；可选 Kafka 异步落库。
     */
    public static class Seckill {
        /** 为 false 时回退为纯 DB 事务（旧实现），便于无 Redis 环境 */
        private boolean redisLuaEnabled = true;
        /** 为 true 时发 Kafka 由消费者落库；为 false 时在请求线程内同步 persistClaim */
        private boolean kafkaEnabled = false;
        private String kafkaTopic = "seckill-claim";
        /** 监听并发数，单 key 顺序消费可保持 1 */
        private String kafkaListenerConcurrency = "1";

        public boolean isRedisLuaEnabled() {
            return redisLuaEnabled;
        }

        public void setRedisLuaEnabled(boolean redisLuaEnabled) {
            this.redisLuaEnabled = redisLuaEnabled;
        }

        public boolean isKafkaEnabled() {
            return kafkaEnabled;
        }

        public void setKafkaEnabled(boolean kafkaEnabled) {
            this.kafkaEnabled = kafkaEnabled;
        }

        public String getKafkaTopic() {
            return kafkaTopic;
        }

        public void setKafkaTopic(String kafkaTopic) {
            this.kafkaTopic = kafkaTopic;
        }

        public String getKafkaListenerConcurrency() {
            return kafkaListenerConcurrency;
        }

        public void setKafkaListenerConcurrency(String kafkaListenerConcurrency) {
            this.kafkaListenerConcurrency = kafkaListenerConcurrency;
        }
    }
}
