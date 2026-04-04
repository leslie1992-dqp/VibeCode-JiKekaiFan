-- 补齐商家 3 演示秒杀券（V13 仅含商家 1、2）
INSERT INTO `merchant_seckill_coupon`
(`merchant_id`, `title`, `threshold_amount`, `discount_amount`, `stock_total`, `stock_remain`, `valid_from`, `valid_until`, `status`)
VALUES
(3, '满25减4', 25.00, 4.00, 600, 600, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1);
