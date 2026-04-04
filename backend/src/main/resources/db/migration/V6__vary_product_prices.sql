-- 让演示商品价格有差异（解决价格全都一样的问题）
-- 仅调整原先演示店铺（1/2/3）下的数据，避免影响新增店铺的相对合理价格

UPDATE `product`
SET `price` = ROUND(
  CASE
    -- 老王炒饭
    WHEN `merchant_id` = 1 AND `category_id` = 1 THEN 16.00 + MOD(`id`, 10) * 0.50
    WHEN `merchant_id` = 1 AND `category_id` = 2 THEN 18.00 + MOD(`id`, 10) * 0.50

    -- 鲜果茶饮
    WHEN `merchant_id` = 2 AND `category_id` = 3 THEN 12.00 + MOD(`id`, 10) * 0.30
    WHEN `merchant_id` = 2 AND `category_id` = 4 THEN 15.00 + MOD(`id`, 10) * 0.30

    -- 川味小馆
    WHEN `merchant_id` = 3 AND `category_id` = 5 THEN 22.00 + MOD(`id`, 10) * 0.40

    ELSE `price`
  END
, 2)
WHERE `merchant_id` IN (1, 2, 3);

