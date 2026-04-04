# 即刻开饭 Wiki

与仓库代码同步的项目文档（数据库、API、功能流程）。

## 目录导航

| 路径 | 说明 |
|------|------|
| [database/er-diagram.md](database/er-diagram.md) | 数据库 ER 图（Mermaid） |
| [database/tables.md](database/tables.md) | 表清单与字段说明（对齐 Flyway V1–V17） |
| [api/features-and-apis.md](api/features-and-apis.md) | 功能与 REST API 映射、前端路由 |
| [features/README.md](features/README.md) | 功能流程总索引 |
| [features/infra-and-layers.md](features/infra-and-layers.md) | 分层、存储、Redis/Kafka 实况 |
| [features/flows/](features/flows/) | **全链路流程**（用户请求→前端→Controller→Service→Mapper→DB） |

## 同步约定

- **库表**：以 `backend/src/main/resources/db/migration/*.sql` 与 `entity/*Entity.java` 为准。
- **接口**：以 `backend/.../controller/**/*.java` 的 `@RequestMapping` / `@*Mapping` 为准。
- **前端**：路由见 `frontend/src/router/index.ts`，请求封装见 `frontend/src/api/*.ts`。

## 技术栈速览

- 后端：Spring Boot、MyBatis-Plus、Flyway、JWT（Bearer）。
- 前端：Vue 3、Vue Router、Pinia、Vite。
- API 前缀：`/api/v1`；静态文件：`/api/v1/files/*`。
