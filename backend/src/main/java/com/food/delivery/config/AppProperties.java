package com.food.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 自定义配置前缀 {@code app}，供 IDE 与 spring-configuration-metadata 识别。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Seckill seckill = new Seckill();
    private Delivery delivery = new Delivery();

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

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
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

    public static class Delivery {
        /** 全局开关：关闭后仅保留基础订单，不创建配送任务 */
        private boolean enabled = true;
        /** 允许 SSE 推送；关闭后前端降级为轮询 */
        private boolean realtimeEnabled = true;
        /** 轨迹点持久化开关（高频写入可按环境关闭） */
        private boolean locationStoreEnabled = true;
        /** 骑手位置上报最小间隔毫秒，低于该值可由网关/业务层限流 */
        private int locationMinIntervalMs = 1000;
        /**
         * 演示/测试：将 ETA 与自动化推进间隔按该系数缩短（如 0.1 约为十分之一时长）。
         */
        private double demoTimeScale = 0.1d;
        /** 预计送达：基础分钟（与 minutes-per-km 相加后再乘 demo-time-scale） */
        private double etaBaseMinutes = 8d;
        /** 预计送达：每公里增加分钟（与商家 distance_km 相乘） */
        private double etaMinutesPerKm = 3d;
        /** 无 distance_km 时使用的默认公里数 */
        private double etaDefaultDistanceKm = 3d;
        /** 自动化：已派单 → 已到店（分钟，会再乘 demo-time-scale） */
        private double demoAssignedToArriveMinutes = 2d;
        /** 自动化：已到店 → 已取餐 */
        private double demoArriveToPickupMinutes = 1d;
        /** 自动化：已取餐 → 配送中 */
        private double demoPickupToShipMinutes = 0.5d;
        /** 自动化：送达触发：预计送达前多少分钟（演示自动点送达） */
        private double demoDeliverBeforeEtaMinutes = 2d;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRealtimeEnabled() {
            return realtimeEnabled;
        }

        public void setRealtimeEnabled(boolean realtimeEnabled) {
            this.realtimeEnabled = realtimeEnabled;
        }

        public boolean isLocationStoreEnabled() {
            return locationStoreEnabled;
        }

        public void setLocationStoreEnabled(boolean locationStoreEnabled) {
            this.locationStoreEnabled = locationStoreEnabled;
        }

        public int getLocationMinIntervalMs() {
            return locationMinIntervalMs;
        }

        public void setLocationMinIntervalMs(int locationMinIntervalMs) {
            this.locationMinIntervalMs = locationMinIntervalMs;
        }

        public double getDemoTimeScale() {
            return demoTimeScale;
        }

        public void setDemoTimeScale(double demoTimeScale) {
            this.demoTimeScale = demoTimeScale;
        }

        public double getEtaBaseMinutes() {
            return etaBaseMinutes;
        }

        public void setEtaBaseMinutes(double etaBaseMinutes) {
            this.etaBaseMinutes = etaBaseMinutes;
        }

        public double getEtaMinutesPerKm() {
            return etaMinutesPerKm;
        }

        public void setEtaMinutesPerKm(double etaMinutesPerKm) {
            this.etaMinutesPerKm = etaMinutesPerKm;
        }

        public double getEtaDefaultDistanceKm() {
            return etaDefaultDistanceKm;
        }

        public void setEtaDefaultDistanceKm(double etaDefaultDistanceKm) {
            this.etaDefaultDistanceKm = etaDefaultDistanceKm;
        }

        public double getDemoAssignedToArriveMinutes() {
            return demoAssignedToArriveMinutes;
        }

        public void setDemoAssignedToArriveMinutes(double demoAssignedToArriveMinutes) {
            this.demoAssignedToArriveMinutes = demoAssignedToArriveMinutes;
        }

        public double getDemoArriveToPickupMinutes() {
            return demoArriveToPickupMinutes;
        }

        public void setDemoArriveToPickupMinutes(double demoArriveToPickupMinutes) {
            this.demoArriveToPickupMinutes = demoArriveToPickupMinutes;
        }

        public double getDemoPickupToShipMinutes() {
            return demoPickupToShipMinutes;
        }

        public void setDemoPickupToShipMinutes(double demoPickupToShipMinutes) {
            this.demoPickupToShipMinutes = demoPickupToShipMinutes;
        }

        public double getDemoDeliverBeforeEtaMinutes() {
            return demoDeliverBeforeEtaMinutes;
        }

        public void setDemoDeliverBeforeEtaMinutes(double demoDeliverBeforeEtaMinutes) {
            this.demoDeliverBeforeEtaMinutes = demoDeliverBeforeEtaMinutes;
        }

        /**
         * 预计送达总分钟（已含 demo 缩放）：(base + km * perKm) * scale
         */
        public double resolveEtaTotalMinutes(BigDecimal distanceKm) {
            double km = distanceKm == null ? etaDefaultDistanceKm : Math.max(0.1d, distanceKm.doubleValue());
            return (etaBaseMinutes + km * etaMinutesPerKm) * demoTimeScale;
        }
    }
}
