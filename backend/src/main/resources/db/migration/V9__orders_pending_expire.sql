-- 待支付订单：超时前需完成支付；超时后由任务删除
ALTER TABLE `orders`
    ADD COLUMN `expire_at` datetime NULL DEFAULT NULL COMMENT '待支付截止时间' AFTER `status`;

ALTER TABLE `orders`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 成功交易 2 待支付';
