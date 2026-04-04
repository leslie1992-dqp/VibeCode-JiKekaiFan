CREATE TABLE IF NOT EXISTS `merchant` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(128) NOT NULL,
    `logo_url` varchar(255) DEFAULT NULL,
    `min_delivery_amount` decimal(10,2) NOT NULL,
    `delivery_fee` decimal(10,2) NOT NULL,
    `status` tinyint NOT NULL DEFAULT '1',
    `rating` decimal(3,2) DEFAULT '5.00',
    `monthly_sales` int DEFAULT '0',
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `product_category` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `merchant_id` bigint NOT NULL,
    `name` varchar(64) NOT NULL,
    `sort` int NOT NULL DEFAULT '0',
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_merchant_sort` (`merchant_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `product` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `merchant_id` bigint NOT NULL,
    `category_id` bigint NOT NULL,
    `name` varchar(128) NOT NULL,
    `description` varchar(512) DEFAULT NULL,
    `price` decimal(10,2) NOT NULL,
    `stock` int NOT NULL DEFAULT '0',
    `sales` int NOT NULL DEFAULT '0',
    `status` tinyint NOT NULL DEFAULT '1',
    `version` int NOT NULL DEFAULT '0',
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_merchant_status` (`merchant_id`, `status`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `merchant` (`id`, `name`, `logo_url`, `min_delivery_amount`, `delivery_fee`, `status`, `rating`, `monthly_sales`, `created_at`, `updated_at`)
VALUES
    (1, '老王炒饭', NULL, 20.00, 5.00, 1, 4.80, 1200, NOW(), NOW()),
    (2, '鲜果茶饮', NULL, 25.00, 3.00, 1, 4.90, 856, NOW(), NOW()),
    (3, '川味小馆', NULL, 30.00, 6.00, 1, 4.70, 2100, NOW(), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `product_category` (`id`, `merchant_id`, `name`, `sort`, `created_at`, `updated_at`)
VALUES
    (1, 1, '热销', 0, NOW(), NOW()),
    (2, 1, '主食', 1, NOW(), NOW()),
    (3, 2, '奶茶', 0, NOW(), NOW()),
    (4, 2, '果茶', 1, NOW(), NOW()),
    (5, 3, '招牌', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `product` (`id`, `merchant_id`, `category_id`, `name`, `description`, `price`, `stock`, `sales`, `status`, `version`, `created_at`, `updated_at`)
VALUES
    (1, 1, 1, '招牌蛋炒饭', '粒粒分明', 16.00, 100, 320, 1, 0, NOW(), NOW()),
    (2, 1, 2, '扬州炒饭', '配料丰富', 18.00, 80, 180, 1, 0, NOW(), NOW()),
    (3, 2, 3, '珍珠奶茶', '少糖可选', 12.00, 200, 500, 1, 0, NOW(), NOW()),
    (4, 2, 4, '满杯百香果', '清爽解腻', 15.00, 150, 400, 1, 0, NOW(), NOW()),
    (5, 3, 5, '麻婆豆腐饭', '微辣', 22.00, 60, 90, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
