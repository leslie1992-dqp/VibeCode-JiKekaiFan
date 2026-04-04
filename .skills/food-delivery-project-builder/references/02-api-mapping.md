# API 映射

基础路径：`/api/v1`  
鉴权方式：`Authorization: Bearer <jwt>`  
统一返回：`Result<T> { code, message, data, requestId, timestamp }`

## 认证与用户模块

- `POST /auth/register`
  - 请求参数：`mobile,password,nickname`
  - 返回：`userId`
- `POST /auth/login`
  - 请求参数：`mobile,password`
  - 返回：`token,expireAt,userInfo`
- `GET /users/me`
  - 返回：当前用户信息
- `GET /users/addresses`
  - 返回：地址列表
- `POST /users/addresses`
  - 请求参数：`contactName,contactPhone,province,city,district,detail,isDefault`
  - 返回：地址 ID
- `PUT /users/addresses/{id}`
  - 请求参数：同新增接口
- `DELETE /users/addresses/{id}`

## 商家与商品模块

- `GET /merchants`
  - 查询参数：`keyword,pageNo,pageSize`
- `GET /merchants/{merchantId}`
- `GET /merchants/{merchantId}/categories`
- `GET /merchants/{merchantId}/products`
  - 查询参数：`categoryId,keyword,pageNo,pageSize`
- `GET /products/{productId}`

## 购物车模块

- `GET /cart/items`
- `POST /cart/items`
  - 请求参数：`merchantId,productId,quantity`
- `PUT /cart/items/{id}`
  - 请求参数：`quantity,checked`
- `DELETE /cart/items/{id}`
- `POST /cart/clear`
  - 请求参数：`merchantId`

## 订单模块

- `POST /orders/preview`
  - 请求参数：`merchantId,addressId,cartItemIds,couponId?`
  - 返回：价格试算结果
- `POST /orders`
  - 请求参数：`merchantId,addressId,cartItemIds,remark,couponId?`
  - 返回：`orderId,orderNo,payAmount,timeoutAt`
- `GET /orders/{orderId}`
- `GET /orders`
  - 查询参数：`status,pageNo,pageSize`
- `POST /orders/{orderId}/cancel`
  - 请求参数：`reason`

## 优惠券秒杀模块

- `GET /coupons/seckill`
- `POST /coupons/{couponId}/seckill`
  - 请求参数：`requestId`
  - 返回：`queueNo` 或 `couponOrderId`
- `GET /coupons/seckill/result`
  - 查询参数：`couponId`
  - 返回：`processing|success|fail`

## 支付模块

- `POST /payments/create`
  - 请求参数：`orderId,channel`
  - 返回：`payNo,payUrl,expireAt`
- `POST /payments/mock-callback`
  - 请求参数：`payNo,status,paidAt,signature`
- `GET /payments/{orderId}/status`

## 公共接口

- `GET /health`
- `GET /dicts/order-status`

## 错误码映射

- `0` 成功
- `40001` 参数错误
- `40003` 未认证或鉴权失败
- `40404` 资源不存在
- `40901` 库存不足
- `40902` 重复请求
- `50000` 系统内部错误
