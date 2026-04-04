# 数据库与实体映射

## 通用规则

- 数据库表名：snake_case
- Java 实体名：PascalCase
- 字段映射：snake_case -> camelCase
- 金额字段：`decimal(10,2)` -> `BigDecimal`
- 时间字段：`datetime` -> `LocalDateTime`
- 主键字段：`bigint` -> `Long`

## 表到实体映射

- `user` -> `UserEntity`
- `user_address` -> `UserAddressEntity`
- `merchant` -> `MerchantEntity`
- `product_category` -> `ProductCategoryEntity`
- `product` -> `ProductEntity`
- `cart_item` -> `CartItemEntity`
- `order` -> `OrderEntity`
- `order_item` -> `OrderItemEntity`
- `coupon` -> `CouponEntity`
- `coupon_stock` -> `CouponStockEntity`
- `coupon_order` -> `CouponOrderEntity`
- `payment_record` -> `PaymentRecordEntity`

## 核心字段映射

### user

- id -> id
- mobile -> mobile
- password_hash -> passwordHash
- nickname -> nickname
- status -> status
- created_at -> createdAt
- updated_at -> updatedAt

### user_address

- id -> id
- user_id -> userId
- contact_name -> contactName
- contact_phone -> contactPhone
- province -> province
- city -> city
- district -> district
- detail -> detail
- latitude -> latitude
- longitude -> longitude
- is_default -> isDefault
- created_at -> createdAt
- updated_at -> updatedAt

### merchant

- id -> id
- name -> name
- logo_url -> logoUrl
- min_delivery_amount -> minDeliveryAmount
- delivery_fee -> deliveryFee
- status -> status
- rating -> rating
- monthly_sales -> monthlySales
- created_at -> createdAt
- updated_at -> updatedAt

### product_category

- id -> id
- merchant_id -> merchantId
- name -> name
- sort -> sort
- created_at -> createdAt
- updated_at -> updatedAt

### product

- id -> id
- merchant_id -> merchantId
- category_id -> categoryId
- name -> name
- description -> description
- price -> price
- stock -> stock
- sales -> sales
- status -> status
- version -> version
- created_at -> createdAt
- updated_at -> updatedAt

### cart_item

- id -> id
- user_id -> userId
- merchant_id -> merchantId
- product_id -> productId
- quantity -> quantity
- checked -> checked
- unit_price_snapshot -> unitPriceSnapshot
- created_at -> createdAt
- updated_at -> updatedAt

### order

- id -> id
- order_no -> orderNo
- user_id -> userId
- merchant_id -> merchantId
- address_id -> addressId
- total_amount -> totalAmount
- discount_amount -> discountAmount
- delivery_fee -> deliveryFee
- pay_amount -> payAmount
- status -> status
- cancel_reason -> cancelReason
- paid_at -> paidAt
- timeout_at -> timeoutAt
- created_at -> createdAt
- updated_at -> updatedAt

### order_item

- id -> id
- order_id -> orderId
- product_id -> productId
- product_name -> productName
- unit_price -> unitPrice
- quantity -> quantity
- amount -> amount
- created_at -> createdAt
- updated_at -> updatedAt

### coupon

- id -> id
- name -> name
- type -> type
- amount -> amount
- threshold_amount -> thresholdAmount
- start_time -> startTime
- end_time -> endTime
- status -> status
- created_at -> createdAt
- updated_at -> updatedAt

### coupon_stock

- coupon_id -> couponId
- total_stock -> totalStock
- available_stock -> availableStock
- version -> version
- updated_at -> updatedAt

### coupon_order

- id -> id
- coupon_id -> couponId
- user_id -> userId
- order_id -> orderId
- status -> status
- created_at -> createdAt
- updated_at -> updatedAt

### payment_record

- id -> id
- order_id -> orderId
- pay_no -> payNo
- channel -> channel
- amount -> amount
- status -> status
- callback_payload -> callbackPayload
- paid_at -> paidAt
- created_at -> createdAt
- updated_at -> updatedAt

## 约束与索引规则

- 一人一券唯一约束：`coupon_order(coupon_id,user_id)`
- 购物车商品唯一约束：`cart_item(user_id,product_id)`
- 订单号唯一约束：`order(order_no)`
- 支付流水号唯一约束：`payment_record(pay_no)`
- 超时扫描索引：`order(status,timeout_at)`
