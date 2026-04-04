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

## MySQL（唯一持久化存储）

所有业务实体落库；Mapper 方法最终通过 MyBatis-Plus 访问 JDBC → MySQL。

## Redis

- `application-dev.yml` / `application-prod.yml` 中可出现 `spring.data.redis` 配置。
- **`src/main/java` 中无任何 `RedisTemplate` / `StringRedisTemplate` / `@Cacheable` 等使用**。  
  流程图中统一标注：**Redis：未接入业务**。

## Kafka / 消息队列

- 依赖与代码中**未引入 Kafka**。  
  流程图中统一标注：**Kafka：未使用**。

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
- **MySQL / File**：真实 IO；**Redis、Kafka** 若出现则标注为「未使用」或省略。
