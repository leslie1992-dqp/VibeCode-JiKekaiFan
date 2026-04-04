-- 状态 3：用户主动取消（仍保留在订单列表中展示）
ALTER TABLE `orders`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 成功交易 2 待支付 3 订单取消';
