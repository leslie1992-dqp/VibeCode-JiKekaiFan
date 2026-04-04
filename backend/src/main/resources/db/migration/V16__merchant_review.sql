-- 商家评论、评论图片、推荐菜
CREATE TABLE IF NOT EXISTS `merchant_review` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `merchant_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `rating` tinyint NOT NULL COMMENT '1-5',
    `content` varchar(2000) NOT NULL,
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_merchant_created` (`merchant_id`, `created_at`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `merchant_review_image` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `review_id` bigint NOT NULL,
    `image_url` varchar(512) NOT NULL,
    `sort_order` int NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `merchant_review_recommend` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `review_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_product` (`review_id`, `product_id`),
    KEY `idx_review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
