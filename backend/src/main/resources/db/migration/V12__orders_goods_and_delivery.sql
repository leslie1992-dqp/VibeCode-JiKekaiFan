-- 订单商品小计与配送费（用于列表展示；历史数据从明细反推）
ALTER TABLE `orders`
    ADD COLUMN `goods_amount` decimal(10,2) NULL COMMENT '商品小计' AFTER `total_amount`;

ALTER TABLE `orders`
    ADD COLUMN `delivery_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费' AFTER `goods_amount`;

UPDATE `orders` o
SET `goods_amount` = (
    SELECT COALESCE(SUM(oi.subtotal), 0)
    FROM `order_item` oi
    WHERE oi.order_id = o.id
);

UPDATE `orders`
SET `delivery_fee` = GREATEST(0, `total_amount` - COALESCE(`goods_amount`, 0));

UPDATE `orders` SET `goods_amount` = `total_amount` WHERE `goods_amount` IS NULL;

ALTER TABLE `orders`
    MODIFY COLUMN `goods_amount` decimal(10,2) NOT NULL COMMENT '商品小计';
