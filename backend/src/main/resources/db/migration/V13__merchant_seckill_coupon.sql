-- 商家秒杀优惠券（模板）
CREATE TABLE IF NOT EXISTS `merchant_seckill_coupon` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `merchant_id` bigint NOT NULL,
    `title` varchar(64) NOT NULL,
    `threshold_amount` decimal(10,2) NOT NULL COMMENT '满减门槛（商品小计）',
    `discount_amount` decimal(10,2) NOT NULL COMMENT '减免金额',
    `stock_total` int NOT NULL,
    `stock_remain` int NOT NULL,
    `valid_from` datetime NOT NULL,
    `valid_until` datetime NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 上架',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户领取的券
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `merchant_id` bigint NOT NULL,
    `seckill_coupon_id` bigint NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1未使用 2已使用 3待支付占用',
    `claimed_at` datetime NOT NULL,
    `expire_at` datetime NOT NULL,
    `locked_order_id` bigint NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_seckill` (`user_id`, `seckill_coupon_id`),
    KEY `idx_user_merchant` (`user_id`, `merchant_id`),
    KEY `idx_lock` (`locked_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `orders`
    ADD COLUMN `user_coupon_id` bigint NULL COMMENT 'user_coupon.id' AFTER `coupon_code`;

-- 演示数据（商家 1、2 各若干张）
INSERT INTO `merchant_seckill_coupon`
(`merchant_id`, `title`, `threshold_amount`, `discount_amount`, `stock_total`, `stock_remain`, `valid_from`, `valid_until`, `status`)
VALUES
(1, '满30减5', 30.00, 5.00, 1000, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1),
(1, '满50减12', 50.00, 12.00, 500, 500, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1),
(2, '满20减3', 20.00, 3.00, 800, 800, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1);
