CREATE TABLE IF NOT EXISTS `user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `mobile` varchar(20) NOT NULL,
    `password_hash` varchar(128) NOT NULL,
    `nickname` varchar(64) DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT '1',
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_address` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `contact_name` varchar(64) NOT NULL,
    `contact_phone` varchar(20) NOT NULL,
    `province` varchar(32) NOT NULL,
    `city` varchar(32) NOT NULL,
    `district` varchar(32) NOT NULL,
    `detail` varchar(255) NOT NULL,
    `latitude` decimal(10,6) DEFAULT NULL,
    `longitude` decimal(10,6) DEFAULT NULL,
    `is_default` tinyint NOT NULL DEFAULT '0',
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
