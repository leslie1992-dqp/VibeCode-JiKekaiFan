-- 首页排序：演示用「相对距离」与「推荐权重」
ALTER TABLE `merchant`
    ADD COLUMN `distance_km` DECIMAL(6,2) NOT NULL DEFAULT 1.00 COMMENT '演示：相对用户距离(km)' AFTER `monthly_sales`,
    ADD COLUMN `recommend_score` INT NOT NULL DEFAULT 0 COMMENT '推荐排序权重，越大越靠前' AFTER `distance_km`;

UPDATE `merchant` SET `distance_km` = 0.80, `recommend_score` = 92 WHERE `id` = 1;
UPDATE `merchant` SET `distance_km` = 2.50, `recommend_score` = 88 WHERE `id` = 2;
UPDATE `merchant` SET `distance_km` = 4.20, `recommend_score` = 95 WHERE `id` = 3;
