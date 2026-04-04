# 功能与 API 映射

> 与 `backend/src/main/java/com/food/delivery/controller` 及 `frontend/src` 对齐。  
> 鉴权：除标明「否」外，需在请求头携带 `Authorization: Bearer <JWT>`。

**从用户点击到 Service/Mapper 的逐步调用链**（含 Redis/Kafka 实况说明）见：[../features/flows/README.md](../features/flows/README.md)。

## 前端路由（`frontend/src/router/index.ts`）

| 路径 | 页面 | 需登录 |
|------|------|--------|
| `/` | 首页商家列表 | 否 |
| `/merchants/:id` | 商家详情（菜品、评价、待下单、购物车） | 否（部分操作需登录） |
| `/auth/login` | 登录 | 否 |
| `/auth/register` | 注册 | 否 |
| `/auth/forgot-password` | 找回密码 | 否 |
| `/orders` | 订单列表 / 支付 / 取消 | 是 |
| `/cart` | 购物车 / 结算预览 | 是 |
| `/coupons` | 我的优惠券 | 是 |
| `/profile` | 个人主页（资料、头像、我的评价入口等） | 是 |

---

## 认证与用户

| 功能 | 方法 | 路径 | 鉴权 | 前端 API 模块 |
|------|------|------|------|----------------|
| 注册 | POST | `/api/v1/auth/register` | 否 | `auth.ts` |
| 登录 | POST | `/api/v1/auth/login` | 否 | `auth.ts` |
| 重置密码 | POST | `/api/v1/auth/reset-password` | 否 | `auth.ts` |
| 当前用户 | GET | `/api/v1/users/me` | 是 | `auth.ts` / `userProfile.ts` |
| 上传头像 | POST | `/api/v1/users/me/avatar` | 是，`multipart/form-data` | `userProfile.ts` |
| 地址列表 | GET | `/api/v1/users/addresses` | 是 | `userProfile.ts` |
| 新增地址 | POST | `/api/v1/users/addresses` | 是 | `userProfile.ts` |
| 更新地址 | PUT | `/api/v1/users/addresses/{id}` | 是 | `userProfile.ts` |
| 删除地址 | DELETE | `/api/v1/users/addresses/{id}` | 是 | `userProfile.ts` |
| 我的优惠券 | GET | `/api/v1/users/me/coupons` | 是 | `userCoupon.ts` |
| 已抢秒杀券 id 列表 | GET | `/api/v1/users/me/claimed-seckill-ids` | 是 | `userCoupon.ts` |
| 我的评价（分页） | GET | `/api/v1/users/me/reviews` | 是 | `userProfile.ts` |
| 提交商家评价 | POST | `/api/v1/users/me/merchant-reviews` | 是 | `merchantReview.ts` |
| 上传评价配图 | POST | `/api/v1/users/me/merchant-review-images` | 是，`multipart` | `merchantReview.ts` |

---

## 静态文件（无需 JWT）

| 功能 | 方法 | 路径 |
|------|------|------|
| 用户头像文件 | GET | `/api/v1/files/user-avatars/{filename}` |
| 评价图片文件 | GET | `/api/v1/files/review-images/{filename}` |

---

## 商家与目录

| 功能 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| 商家分页 | GET | `/api/v1/merchants` | 否 | `keyword`、`pageNo`、`pageSize`、`sort`（默认 `recommend`，含 `distance`/`rating`/`sales` 等） |
| 商家详情 | GET | `/api/v1/merchants/{merchantId}` | 否 | |
| 分类列表 | GET | `/api/v1/merchants/{merchantId}/categories` | 否 | |
| 商品列表 | GET | `/api/v1/merchants/{merchantId}/products` | 否 | 可选 `categoryId` |
| 秒杀券列表 | GET | `/api/v1/merchants/{merchantId}/seckill-coupons` | 否 | |
| 评价分页 | GET | `/api/v1/merchants/{merchantId}/reviews` | 否 | `pageNo`、`pageSize` |

前端：`merchant.ts`、`merchantReview.ts`。

---

## 秒杀领取

| 功能 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 抢券 | POST | `/api/v1/merchant-seckill-coupons/{couponId}/claim` | 是 |

前端：`userCoupon.ts`（与商家页联动）。

---

## 购物车

| 功能 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 加入购物车 | POST | `/api/v1/cart/items` | 是 |
| 购物车列表 | GET | `/api/v1/cart/items` | 是 |
| 结算预览 | POST | `/api/v1/cart/checkout-preview` | 是 |
| 购物车结算下单 | POST | `/api/v1/cart/checkout` | 是 |
| 清空购物车 | DELETE | `/api/v1/cart/clear` | 是 |
| 删除某商品 | DELETE | `/api/v1/cart/items/{productId}` | 是 |
| 数量 +1 | POST | `/api/v1/cart/items/{productId}/increase` | 是 |
| 数量 −1 | POST | `/api/v1/cart/items/{productId}/decrease` | 是 |

前端：`cart.ts`，页面 `CartView.vue`。

---

## 待下单草稿（按商家）

| 功能 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 加入草稿 | POST | `/api/v1/merchant-drafts/items` | 是 |
| 拉取某店草稿 | GET | `/api/v1/merchant-drafts/merchants/{merchantId}` | 是 |
| 草稿行 +1 | POST | `/api/v1/merchant-drafts/merchants/{merchantId}/items/{productId}/increase` | 是 |
| 草稿行 −1 | POST | `/api/v1/merchant-drafts/merchants/{merchantId}/items/{productId}/decrease` | 是 |
| 删除草稿行 | DELETE | `/api/v1/merchant-drafts/merchants/{merchantId}/items/{productId}` | 是 |
| 结算（模拟支付成功） | POST | `/api/v1/merchant-drafts/merchants/{merchantId}/checkout` | 是 |
| 生成待支付订单 | POST | `/api/v1/merchant-drafts/merchants/{merchantId}/checkout-pending` | 是 |

前端：`merchantDraft.ts`，页面 `MerchantDetailView.vue`。

---

## 订单

| 功能 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 我的订单列表 | GET | `/api/v1/orders` | 是 |
| 支付（待支付 → 成功） | POST | `/api/v1/orders/{orderId}/pay` | 是 |
| 取消订单 | POST | `/api/v1/orders/{orderId}/cancel` | 是 |

前端：`order.ts`，页面 `OrderListView.vue`。

---

## 页面 ↔ API 速查

| 页面 / 模块 | 主要接口 |
|-------------|----------|
| `HomeView.vue` | `GET /merchants` |
| `MerchantDetailView.vue` | `GET /merchants/{id}`、`categories`、`products`、`seckill-coupons`、`reviews`；登录后 `merchant-drafts/*`、`cart/*`、抢券 `POST .../claim`、写评价 `POST /users/me/merchant-reviews` |
| `CartView.vue` | `GET/POST/DELETE /cart/*`、`checkout-preview`、`checkout` |
| `OrderListView.vue` | `GET /orders`、`POST .../pay`、`.../cancel` |
| `UserCouponsView.vue` | `GET /users/me/coupons` |
| `ProfileView.vue` | `GET /users/me`、`POST /users/me/avatar`、`GET /users/me/reviews` 等 |
| `LoginView` / `RegisterView` / `ForgotPasswordView` | `/api/v1/auth/*` |
