# 全链路流程（用户请求 → 前端 → 后端 → 存储）

每篇文档按**真实代码**展开到 **Controller / Service / Mapper 方法名**，并标明 **MySQL / 本地文件**；**Redis、Kafka 在本项目中未用于业务**（见 [../infra-and-layers.md](../infra-and-layers.md)）。

| 文档 | 场景 |
|------|------|
| [01-auth-login-and-register.md](01-auth-login-and-register.md) | 登录、注册、重置密码 |
| [02-jwt-filter-and-me.md](02-jwt-filter-and-me.md) | JWT 过滤器、`GET /users/me` |
| [03-cart-add-and-list.md](03-cart-add-and-list.md) | 购物车增删改数量、列表 |
| [04-cart-checkout.md](04-cart-checkout.md) | 结算预览、多店下单 |
| [05-merchant-draft-checkout.md](05-merchant-draft-checkout.md) | 待下单草稿、单店结算/待支付 |
| [06-order-list-pay-cancel.md](06-order-list-pay-cancel.md) | 订单列表、支付、取消 |
| [07-merchant-catalog-public.md](07-merchant-catalog-public.md) | 首页商家分页、店内分类/商品（无需登录） |
| [08-seckill-claim.md](08-seckill-claim.md) | 秒杀抢券 |
| [09-review-create-and-images.md](09-review-create-and-images.md) | 评价发表、配图上传、店铺/我的评价列表 |
| [10-avatar-upload.md](10-avatar-upload.md) | 用户头像上传 |
| [11-schedulers.md](11-schedulers.md) | 定时任务（过期订单、券过期处理） |
