-- 骑手配送域：骑手、班次、配送任务、事件与轨迹

CREATE TABLE IF NOT EXISTS `rider` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '映射 user.id',
    `display_name` varchar(64) NOT NULL COMMENT '骑手显示名',
    `phone` varchar(20) DEFAULT NULL,
    `vehicle_type` varchar(32) NOT NULL DEFAULT 'E_BIKE' COMMENT '交通工具',
    `rating` decimal(3,2) NOT NULL DEFAULT 4.80 COMMENT '评分',
    `max_concurrent_tasks` int NOT NULL DEFAULT 3 COMMENT '最大并发配送单',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 在职可用 0 停用',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rider_user_id` (`user_id`),
    KEY `idx_rider_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手';

CREATE TABLE IF NOT EXISTS `rider_shift` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `rider_id` bigint NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 在线可接单 2 忙碌 0 下线',
    `current_task_count` int NOT NULL DEFAULT 0 COMMENT '当前配送中数量',
    `online_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `offline_at` datetime DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rider_shift_rider_id` (`rider_id`),
    KEY `idx_rider_shift_status_online` (`status`, `online_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手班次状态';

CREATE TABLE IF NOT EXISTS `delivery_task` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL,
    `merchant_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `rider_id` bigint DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT 10 COMMENT '10 待派单 20 已派单待接单 30 取餐中 40 配送中 50 已送达 60 取消 70 配送失败',
    `dispatch_attempt` int NOT NULL DEFAULT 0 COMMENT '派单尝试次数',
    `assign_token` varchar(64) DEFAULT NULL COMMENT '幂等令牌',
    `expected_arrive_at` datetime DEFAULT NULL,
    `accepted_at` datetime DEFAULT NULL,
    `picked_up_at` datetime DEFAULT NULL,
    `delivered_at` datetime DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_delivery_task_order_id` (`order_id`),
    KEY `idx_delivery_task_rider_status` (`rider_id`, `status`),
    KEY `idx_delivery_task_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送任务';

CREATE TABLE IF NOT EXISTS `delivery_event` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `task_id` bigint NOT NULL,
    `order_id` bigint NOT NULL,
    `event_type` varchar(32) NOT NULL COMMENT 'ASSIGNED/ACCEPTED/PICKED_UP/DELIVERED/REASSIGNED/FAILED',
    `from_status` tinyint DEFAULT NULL,
    `to_status` tinyint DEFAULT NULL,
    `operator_type` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/RIDER/MERCHANT/USER',
    `operator_id` bigint DEFAULT NULL,
    `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `payload_json` varchar(1024) DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_delivery_event_task_created` (`task_id`, `created_at`),
    KEY `idx_delivery_event_order_time` (`order_id`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送事件日志';

CREATE TABLE IF NOT EXISTS `delivery_location` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `task_id` bigint NOT NULL,
    `order_id` bigint NOT NULL,
    `rider_id` bigint NOT NULL,
    `latitude` decimal(10,6) NOT NULL,
    `longitude` decimal(10,6) NOT NULL,
    `speed_kmh` decimal(6,2) DEFAULT NULL,
    `heading` decimal(6,2) DEFAULT NULL,
    `client_time` datetime DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_delivery_location_task_time` (`task_id`, `created_at`),
    KEY `idx_delivery_location_order_time` (`order_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送轨迹点';
