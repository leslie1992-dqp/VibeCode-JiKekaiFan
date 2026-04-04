# 功能流程文档

## 请先读

- **[infra-and-layers.md](infra-and-layers.md)**：分层命名、MyBatis-Plus Mapper 即 DAO、**Redis/Kafka 在本项目未接入业务**、本地文件存储、JWT 无服务端 Session。

## 全链路流程（推荐）

目录：**[flows/](flows/)**

每条链路从 **用户操作 → Vue 函数 → `api/*.ts` / Pinia → axios →（JWT Filter）→ Controller → Service → Mapper → MySQL / 本地文件**，并写到**具体类与方法名**。

| 入口 | 文档 |
|------|------|
| 登录注册 | [flows/01-auth-login-and-register.md](flows/01-auth-login-and-register.md) |
| JWT、`/users/me` | [flows/02-jwt-filter-and-me.md](flows/02-jwt-filter-and-me.md) |
| 购物车 | [flows/03-cart-add-and-list.md](flows/03-cart-add-and-list.md)、[flows/04-cart-checkout.md](flows/04-cart-checkout.md) |
| 待下单草稿 | [flows/05-merchant-draft-checkout.md](flows/05-merchant-draft-checkout.md) |
| 订单 | [flows/06-order-list-pay-cancel.md](flows/06-order-list-pay-cancel.md) |
| 公开商家目录 | [flows/07-merchant-catalog-public.md](flows/07-merchant-catalog-public.md) |
| 秒杀与我的券 | [flows/08-seckill-claim.md](flows/08-seckill-claim.md) |
| 评价 | [flows/09-review-create-and-images.md](flows/09-review-create-and-images.md) |
| 头像 | [flows/10-avatar-upload.md](flows/10-avatar-upload.md) |
| 定时任务 | [flows/11-schedulers.md](flows/11-schedulers.md) |

完整索引见 [flows/README.md](flows/README.md)。

## 约定（维护 Wiki 时）

- 代码变更后同步更新对应 `flows/*.md` 中的类名、方法名与表名。
- 若将来引入 Redis/Kafka，在 **infra-and-layers.md** 与各流程图中补充参与者与调用点。
