-- 商品名中不再保留“商家名 + 空格”的前缀
-- 例如：'老王炒饭 招牌蛋炒饭' -> '招牌蛋炒饭'

UPDATE `product`
SET `name` =
  CASE
    WHEN `name` LIKE '老王炒饭 %' THEN SUBSTRING(`name`, CHAR_LENGTH('老王炒饭 ') + 1)
    WHEN `name` LIKE '鲜果茶饮 %' THEN SUBSTRING(`name`, CHAR_LENGTH('鲜果茶饮 ') + 1)
    WHEN `name` LIKE '川味小馆 %' THEN SUBSTRING(`name`, CHAR_LENGTH('川味小馆 ') + 1)
    ELSE `name`
  END
WHERE `merchant_id` IN (1, 2, 3)
  AND (
    `name` LIKE '老王炒饭 %'
    OR `name` LIKE '鲜果茶饮 %'
    OR `name` LIKE '川味小馆 %'
  );

