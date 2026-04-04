-- 为每个上架商家补充 4 张秒杀券模板（高库存），便于联调 / Redis+Kafka 压测
-- V13/V14 已为 1~3 提供部分券；本脚本对 merchant 表中 status=1 的每一行追加 4 条新模板（含 4~9）

INSERT INTO `merchant_seckill_coupon`
(`merchant_id`, `title`, `threshold_amount`, `discount_amount`, `stock_total`, `stock_remain`, `valid_from`, `valid_until`, `status`)
SELECT m.`id`, t.`title`, t.`threshold_amount`, t.`discount_amount`, t.`stock_total`, t.`stock_remain`, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 1
FROM `merchant` m
CROSS JOIN (
    SELECT '满15减2' AS `title`, 15.00 AS `threshold_amount`, 2.00 AS `discount_amount`, 2000 AS `stock_total`, 2000 AS `stock_remain`
    UNION ALL SELECT '满40减8', 40.00, 8.00, 2000, 2000
    UNION ALL SELECT '满80减15', 80.00, 15.00, 1500, 1500
    UNION ALL SELECT '满100减25', 100.00, 25.00, 1000, 1000
) t
WHERE m.`status` = 1;
