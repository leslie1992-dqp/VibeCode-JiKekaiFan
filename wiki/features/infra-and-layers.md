# 基础设施与分层约定（本仓库实况）

本文档是各条「全链路流程」的**前置说明**：与 `backend`、`frontend` 代码一致。

## 分层与命名

| 层次 | 位置 | 说明 |
|------|------|------|
| 浏览器 / 用户 | — | 点击、表单提交、路由跳转 |
| 前端视图 | `frontend/src/views/**/*.vue` | 模板事件 → 调用 `script` 中的方法 |
| 前端状态 / API | `frontend/src/store/*.ts`、`frontend/src/api/*.ts` | Pinia action 或封装 `axios` |
| HTTP 客户端 | `frontend/src/api/request.ts` | `axios.create`，请求拦截器附加 `Authorization: Bearer`；响应拦截器把 `code≠0` 转为 `reject` |
| Spring MVC | `backend/.../controller/**/*Controller.java` | `@RestController`，入参校验、`Result` 包装 |
| 安全过滤器 | `backend/.../common/security/JwtAuthenticationFilter.java` | 对特定前缀路径校验 JWT，解析后 `request.setAttribute(ATTR_USER_ID, userId)` |
| 业务服务 | `backend/.../service/**/*.java`（接口）+ `backend/.../impl/**/*.java`（实现） | `@Service`，`@Transactional` 多在实现类方法上 |
| 数据访问 | `backend/.../mapper/**/*Mapper.java` | **MyBatis-Plus `BaseMapper`**，无单独 DAO 接口层；XML 极少，以 Wrapper / 默认 CRUD 为主 |
| SQL 表 | MySQL | Flyway：`backend/src/main/resources/db/migration/V*.sql` |
| 支撑组件 | `backend/.../support/*.java` 等 | 如 `OrderNoAllocator`（查库防重生成订单号） |
| 定时任务 | `backend/.../schedule/*.java` | `@Scheduled` |
| 秒杀闸门 | `SeckillLuaClaimService` + `scripts/seckill_claim.lua` | `StringRedisTemplate` 执行 Lua，键 `seckill:stock:*` / `seckill:claimed:*` |
| 秒杀异步落库 | `SeckillKafkaProducerConfiguration`、`SeckillClaimConsumer` | `KafkaTemplate<String,SeckillClaimEvent>`；`app.seckill.kafka-enabled` 控制 |

## MySQL（唯一持久化存储）

所有业务实体落库；Mapper 方法最终通过 MyBatis-Plus 访问 JDBC → MySQL。

## Redis

- 配置：`spring.data.redis`（`application-dev.yml` / `application-prod.yml`）。
- **秒杀领券**：`SeckillLuaClaimService` 使用 **`StringRedisTemplate`** 执行 **`classpath:scripts/seckill_claim.lua`**，在应用层实现**一人一单**（`SISMEMBER` + `SADD`）与**防超卖**（`DECR` 原子扣减，不足则回滚 `INCR`）。
- 可通过 `app.seckill.redis-lua-enabled=false` 关闭 Redis 路径，退回纯 DB（`SeckillDbOnlyClaimService`）。

## Kafka

- 依赖：`spring-kafka`；配置：`spring.kafka.bootstrap-servers`、消费者 `group-id` 等（见 `application-dev.yml`）。
- **秒杀领券**：`app.seckill.kafka-enabled=true` 时，`MerchantSeckillCouponServiceImpl` 在 Lua 成功后 **`seckillKafkaTemplate.send`** 到 **`app.seckill.kafka-topic`**（默认 `seckill-claim`），由 **`SeckillClaimConsumer`** 调用 **`SeckillClaimPersistenceService.persistClaim`** 幂等写 MySQL。
- `kafka-enabled=false` 时在同一线程内直接 `persistClaim`，不经过 Broker。
- **业务 Topic 自动创建**：`kafka-enabled=true` 时 **`SeckillKafkaTopicConfiguration`** 注册 **`NewTopic`**（默认名同 `kafka-topic`，**3 分区、1 副本**），由 Spring **`KafkaAdmin`** 在**应用启动时**向 broker 建 topic（broker 需可达且具备建 topic 权限；与 `spring.kafka.bootstrap-servers` 指向的集群一致）。
- 集群中还会出现 Kafka **内部 topic** **`__consumer_offsets`**（记录 consumer group 的位移），**不是**本仓库代码创建的业务 topic。
- 根配置 `application.yml` 当前默认 `kafka-enabled: true`；本地无 Kafka 时可改为 `false` 或仅在 profile 中覆盖。

## 本地文件存储（非 DB）

| 能力 | 写入 | 读取 |
|------|------|------|
| 用户头像 | `UserAvatarStorageService.saveAndBuildUrl` | `UserAvatarFileController` → `GET /api/v1/files/user-avatars/{filename}` |
| 评价配图 | `MerchantReviewImageStorageService.saveAndBuildUrls` | `ReviewImageFileController` → `GET /api/v1/files/review-images/{filename}` |

文件目录由配置指定（如项目下 `uploads/`），**不经过 Redis/Kafka**。

## JWT（非 Redis 会话）

- 签发与解析：`com.food.delivery.common.util.JwtUtil`（`generateToken` / `parseUserId`）。
- 登录成功后前端仅存 **localStorage `token`**，服务端**无服务端 Session 存储**。

## 流程图阅读说明

各 `flows/*.md` 中的 Mermaid 图使用下列参与者约定：

- **Vue**：具体页面组件中的处理函数（会写出函数名）。
- **axios**：`request.ts` 发出的 HTTP 调用。
- **Filter**：`JwtAuthenticationFilter`（若该路径需登录）。
- **Controller / Service / Mapper**：Java 类与方法名。
- **MySQL / File**：真实 IO；**Redis / Kafka** 以秒杀流程为准，其余业务仍不经消息队列。
