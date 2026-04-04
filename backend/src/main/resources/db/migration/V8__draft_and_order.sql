-- 商家待下单草稿（按用户 + 商家隔离）
CREATE TABLE IF NOT EXISTS `merchant_order_draft` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `merchant_id` bigint NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 进行中 0 已关闭',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_merchant` (`user_id`, `merchant_id`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `merchant_order_draft_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `draft_id` bigint NOT NULL,
    `merchant_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `quantity` int NOT NULL DEFAULT 1,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 有效 0 已删除',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_draft_product` (`draft_id`, `product_id`),
    KEY `idx_draft_status` (`draft_id`, `status`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单（模拟支付后直接为已支付）
CREATE TABLE IF NOT EXISTS `orders` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `merchant_id` bigint NOT NULL,
    `total_amount` decimal(10,2) NOT NULL,
    `coupon_code` varchar(32) NOT NULL DEFAULT 'DEMO0',
    `coupon_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
    `pay_amount` decimal(10,2) NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 已支付',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `product_name` varchar(128) NOT NULL,
    `quantity` int NOT NULL,
    `unit_price` decimal(10,2) NOT NULL,
    `subtotal` decimal(10,2) NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
