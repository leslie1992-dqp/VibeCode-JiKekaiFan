# 数据库表清单

> 与 `backend/src/main/resources/db/migration`（V1–V18）及 `com.food.delivery.entity` 下的实体类同步。

## 表一览

| 表名 | 用途 | 实体类 |
|------|------|--------|
| `user` | 用户账号 | `UserEntity` |
| `user_address` | 收货地址 | `UserAddressEntity` |
| `merchant` | 商家 | `MerchantEntity` |
| `product_category` | 店内分类 | `ProductCategoryEntity` |
| `product` | 商品 | `ProductEntity` |
| `cart_item` | 购物车行 | `CartItemEntity` |
| `merchant_order_draft` | 待下单草稿（按用户+商家） | `MerchantOrderDraftEntity` |
| `merchant_order_draft_item` | 草稿行 | `MerchantOrderDraftItemEntity` |
| `orders` | 订单 | `OrderEntity` |
| `order_item` | 订单明细 | `OrderItemEntity` |
| `merchant_seckill_coupon` | 秒杀券模板 | `MerchantSeckillCouponEntity` |
| `user_coupon` | 用户持有的券 | `UserCouponEntity` |
| `merchant_review` | 商家评价 | `MerchantReviewEntity` |
| `merchant_review_image` | 评价配图 | `MerchantReviewImageEntity` |
| `merchant_review_recommend` | 评价关联推荐菜 | `MerchantReviewRecommendEntity` |
| `rider` | 骑手账号映射 | `RiderEntity` |
| `rider_shift` | 骑手在线/负载状态 | `RiderShiftEntity` |
| `delivery_task` | 配送任务主表 | `DeliveryTaskEntity` |
| `delivery_event` | 配送状态事件日志 | `DeliveryEventEntity` |
| `delivery_location` | 配送轨迹点 | `DeliveryLocationEntity` |

---

## user

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| mobile | varchar(20) | 手机号，唯一 |
| password_hash | varchar(128) | 密码哈希 |
| nickname | varchar(64) | 昵称，可空 |
| avatar_url | varchar(512) | 头像 URL（V17），可空 |
| status | tinyint | 1 正常 0 禁用 |
| created_at / updated_at | datetime | 时间 |

---

## user_address

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| user_id | bigint FK | 用户 |
| contact_name | varchar(64) | 联系人 |
| contact_phone | varchar(20) | 电话 |
| province / city / district | varchar(32) | 省市区 |
| detail | varchar(255) | 详细地址 |
| latitude / longitude | decimal(10,6) | 可选 |
| is_default | tinyint | 是否默认 |
| created_at / updated_at | datetime | 时间 |

---

## merchant

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| name | varchar(128) | 店名 |
| logo_url | varchar(255) | Logo |
| min_delivery_amount | decimal(10,2) | 起送价 |
| delivery_fee | decimal(10,2) | 配送费 |
| status | tinyint | 1 营业 0 休息 |
| rating | decimal(3,2) | 评分 |
| monthly_sales | int | 月售 |
| distance_km | decimal(6,2) | 演示：距用户 km（V15），用于排序 |
| recommend_score | int | 推荐权重（V15） |
| created_at / updated_at | datetime | 时间 |

---

## product_category

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| merchant_id | bigint | 商家 |
| name | varchar(64) | 分类名 |
| sort | int | 排序 |
| created_at / updated_at | datetime | 时间 |

---

## product

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| merchant_id | bigint | 商家 |
| category_id | bigint | 分类 |
| name | varchar(128) | 商品名 |
| description | varchar(512) | 描述 |
| price | decimal(10,2) | 售价 |
| stock / sales | int | 库存、销量 |
| status | tinyint | 1 上架 0 下架 |
| version | int | 乐观锁预留 |
| created_at / updated_at | datetime | 时间 |

---

## cart_item

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| user_id | bigint | 用户 |
| product_id | bigint | 商品 |
| merchant_id | bigint | 商家（冗余） |
| quantity | int | 数量 |
| status | tinyint | 有效/逻辑删除等 |
| created_at / updated_at | datetime | 时间 |

唯一约束：`(user_id, product_id)`。

---

## merchant_order_draft

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| user_id | bigint | 用户 |
| merchant_id | bigint | 商家 |
| status | tinyint | 1 进行中 0 已关闭 |
| created_at / updated_at | datetime | 时间 |

唯一约束：`(user_id, merchant_id)`。

---

## merchant_order_draft_item

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| draft_id | bigint | 草稿 |
| merchant_id | bigint | 商家 |
| product_id | bigint | 商品 |
| quantity | int | 数量 |
| status | tinyint | 1 有效 0 已删除 |
| created_at / updated_at | datetime | 时间 |

唯一约束：`(draft_id, product_id)`。

---

## orders

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| order_no | varchar(12) | 12 位数字订单号，唯一 |
| user_id | bigint | 用户 |
| merchant_id | bigint | 商家 |
| total_amount | decimal(10,2) | 应付合计（含配送等） |
| goods_amount | decimal(10,2) | 商品小计（V12） |
| delivery_fee | decimal(10,2) | 配送费（V12） |
| coupon_code | varchar(32) | 演示码，默认如 `DEMO0` |
| user_coupon_id | bigint | 使用的 `user_coupon.id`，可空（V13） |
| coupon_amount | decimal(10,2) | 券抵扣额 |
| pay_amount | decimal(10,2) | 实付 |
| status | tinyint | 1 已支付成功 2 待支付 3 用户取消 |
| expire_at | datetime | 待支付截止时间，可空（V9） |
| created_at / updated_at | datetime | 时间 |

---

## order_item

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| order_id | bigint | 订单 |
| product_id | bigint | 商品 |
| product_name | varchar(128) | 快照名 |
| quantity | int | 数量 |
| unit_price | decimal(10,2) | 单价快照 |
| subtotal | decimal(10,2) | 行小计 |
| created_at / updated_at | datetime | 时间 |

---

## merchant_seckill_coupon

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| merchant_id | bigint | 商家 |
| title | varchar(64) | 标题 |
| threshold_amount | decimal(10,2) | 满减门槛（商品小计） |
| discount_amount | decimal(10,2) | 减免金额 |
| stock_total / stock_remain | int | 库存 |
| valid_from / valid_until | datetime | 有效期 |
| status | tinyint | 1 上架 等 |
| created_at / updated_at | datetime | 时间 |

**演示数据**：V13/V14 为部分商家插入模板；**V18** 对 `merchant` 表中 `status=1` 的**每家商家**再追加 4 条模板（满15减2、满40减8、满80减15、满100减25，高库存），与既有行并存。

---

## user_coupon

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| user_id | bigint | 用户 |
| merchant_id | bigint | 商家 |
| seckill_coupon_id | bigint | 模板 id |
| status | tinyint | 1 未使用 2 已使用 3 待支付占用 |
| claimed_at | datetime | 领取时间 |
| expire_at | datetime | 过期时间 |
| locked_order_id | bigint | 占用订单，可空 |
| created_at / updated_at | datetime | 时间 |

唯一约束：`(user_id, seckill_coupon_id)`。

---

## merchant_review

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| merchant_id | bigint | 商家 |
| user_id | bigint | 用户 |
| rating | tinyint | 1–5 |
| content | varchar(2000) | 正文 |
| created_at / updated_at | datetime | 时间 |

---

## merchant_review_image

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| review_id | bigint | 评价 |
| image_url | varchar(512) | 图片地址 |
| sort_order | int | 排序 |

---

## merchant_review_recommend

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| review_id | bigint | 评价 |
| product_id | bigint | 推荐商品 |

唯一约束：`(review_id, product_id)`。

---

## rider

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| user_id | bigint unique | 绑定用户 |
| display_name | varchar(64) | 展示名 |
| phone | varchar(20) | 手机号 |
| vehicle_type | varchar(32) | 交通工具 |
| rating | decimal(3,2) | 评分 |
| max_concurrent_tasks | int | 最大并发单量 |
| status | tinyint | 1 可用 0 停用 |
| created_at / updated_at | datetime | 时间 |

---

## rider_shift

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| rider_id | bigint unique | 骑手 |
| status | tinyint | 1 在线 2 忙碌 0 下线 |
| current_task_count | int | 当前单量 |
| online_at / offline_at | datetime | 上下线时间 |
| created_at / updated_at | datetime | 时间 |

---

## delivery_task

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| order_id | bigint unique | 订单 |
| merchant_id / user_id | bigint | 商家/用户 |
| rider_id | bigint | 骑手，可空（待派单） |
| status | tinyint | 10 待派单 20 已派单 30 取餐中 40 配送中 50 已送达 60 取消 70 失败 |
| dispatch_attempt | int | 派单重试次数 |
| assign_token | varchar(64) | 幂等令牌 |
| expected_arrive_at | datetime | 预计送达 |
| accepted_at / picked_up_at / delivered_at | datetime | 关键时间 |
| created_at / updated_at | datetime | 时间 |

---

## delivery_event

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| task_id / order_id | bigint | 关联配送任务/订单 |
| event_type | varchar(32) | ASSIGNED / ACCEPTED / PICKED_UP / DELIVERED / REASSIGNED / FAILED |
| from_status / to_status | tinyint | 状态迁移 |
| operator_type | varchar(16) | SYSTEM / RIDER / MERCHANT / USER |
| operator_id | bigint | 操作人 |
| event_time | datetime | 事件发生时间 |
| payload_json | varchar(1024) | 扩展字段 |
| created_at | datetime | 入库时间 |

---

## delivery_location

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 自增 |
| task_id / order_id / rider_id | bigint | 关联信息 |
| latitude / longitude | decimal(10,6) | 坐标 |
| speed_kmh / heading | decimal(6,2) | 速度、朝向 |
| client_time | datetime | 客户端上报时间 |
| created_at | datetime | 服务端时间 |
