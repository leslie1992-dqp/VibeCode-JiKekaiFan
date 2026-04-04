-- 12 位数字订单号（展示用，与自增 id 分离）
ALTER TABLE `orders`
    ADD COLUMN `order_no` varchar(12) NULL COMMENT '12位数字订单号' AFTER `id`;

UPDATE `orders` SET `order_no` = LPAD(`id`, 12, '0') WHERE `order_no` IS NULL;

ALTER TABLE `orders`
    MODIFY COLUMN `order_no` varchar(12) NOT NULL COMMENT '12位数字订单号';

CREATE UNIQUE INDEX `uk_order_no` ON `orders` (`order_no`);
