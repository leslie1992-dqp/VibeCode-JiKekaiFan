# VibeCode-JiKekaiFan
A VibeCoding Project

# 即刻开饭（JiKekaiFan）

基于 **Spring Boot 3** 与 **Vue 3** 的前后端分离外卖点餐演示项目，涵盖账号体系（登录 / 注册 / 重置密码）、商家检索与排序、商家详情与秒杀券、购物车与草稿下单、订单全生命周期（含 **支付后派单、配送状态与演示轨迹**）、优惠券、评价与图片上传、个人主页等。仓库内提供与代码同步的 **Wiki**（ER 图、API 映射、业务流程、秒杀与 Kafka/Redis 说明）。

**关联仓库：** [VibeCode-JiKekaiFan](https://github.com/leslie1992-dqp/VibeCode-JiKekaiFan)

---

## 技术栈

| 层级 | 说明 |
|------|------|
| 后端 | Java 17、Spring Boot 3.2、MyBatis-Plus、Flyway、JWT（请求头 `Authorization: Bearer <token>`） |
| 数据 | MySQL；Redis（秒杀 Lua、缓存等，可按配置降级） |
| 消息 | Kafka（秒杀异步落库；`application.yml` 中 `app.seckill.kafka-enabled` 可关改为同步写库） |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios |
| 接口约定 | REST 前缀 **`/api/v1`**；上传与静态资源目录见 `app.upload.dir`（默认 `food-delivery-uploads`） |

---

## 环境要求

- **JDK 17+**（`backend` 使用 Spring Boot 3）
- **Node.js**（建议 LTS，用于前端构建与开发）
- **MySQL**（表结构由 Flyway 迁移维护，见 `backend/src/main/resources/db/migration/`）
- **Redis**（秒杀 Lua 路径推荐开启；无 Redis 时可按项目配置调整）
- **Kafka + Zookeeper**（仅当开启异步秒杀落库时需要；本地可关闭 Kafka 走同步）

---

## 快速开始

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：**8081**（`application.yml` → `server.port`）。

### 前端

```bash
cd frontend
npm install
npm run dev
```

开发入口：**http://localhost:5173** （Vite 默认端口）。

### Kafka（可选）

Windows 下启动顺序可参考仓库根目录 **`start.md`**（Zookeeper → Kafka）。

---

## Wiki 文档（仓库内 Markdown）

| 说明 | 路径 |
|------|------|
| Wiki 总索引与同步约定 | [wiki/README.md](wiki/README.md) |
| 数据库 ER 图（Mermaid） | [wiki/database/er-diagram.md](wiki/database/er-diagram.md) |
| 表清单与字段（对齐 Flyway） | [wiki/database/tables.md](wiki/database/tables.md) |
| 功能与 API、前端路由映射 | [wiki/api/features-and-apis.md](wiki/api/features-and-apis.md) |
| 分层与 Redis / Kafka | [wiki/features/infra-and-layers.md](wiki/features/infra-and-layers.md) |
| 业务流程（登录、购物车、秒杀等） | [wiki/features/flows/](wiki/features/flows/) |

### 秒杀流程图

![main_page](/images/15.png)

### 登录流程图

![main_page](/images/16.png)

### 数据库 E-R 图

![main_page](/images/17.png)

### API 文档（以商家为例）

接口路径、请求方式与前端 `api/*.ts` 对应关系以 Wiki 为准；以下为文档示例配图。

![main_page](/images/18.png)

---

## 仓库结构（简要）

```text
backend/          Spring Boot、MyBatis-Plus、Flyway、秒杀 Lua、Kafka 消费者等
frontend/         Vue3 + Vite + Pinia
wiki/             与代码同步的 Wiki
design.md         系统架构与领域设计草案
start.md          本地启动与 Kafka 说明
README.md         项目说明（可与本文件择一或合并使用）
```

---

## 功能与界面说明

以下为各模块截图说明；**图片路径按你提供的顺序与写法保留**（`/images/...`）。

### 登录 / 注册 / 重置密码

- **登录页**：手机号 + 密码登录。
- **没有账号**：进入注册页完成注册。
- **忘记密码**：进入重置密码页。

![main_page](/images/14.png)

### 首页

- 浏览全部商家，支持按 **推荐、距离、评分、月售** 排序。
- 支持 **搜索商家名称**。

![main_page](/images/1.png)

### 商家详情

- **商家限时秒杀券**：展示可抢券、库存与规则等。

![main_page](/images/8.png)

- **商品列表**：分类浏览；可将商品 **加入购物车**，或使用 **下单（草稿）** 流程。
![main_page](/images/9.png)


### 下单

- 点击 **下单** 后可查看待结算商品列表（金额试算）。

![main_page](/images/5.png)

- 不需要的商品可在草稿中 **删除**。
![main_page](/images/10.png)


### 商家评价

- 展示 **商家评价列表**；已下单用户可 **发布评价**（支持多图等，见后端上传接口）。

![main_page](/images/11.png)


### 订单

- **订单列表**：展示全部订单及状态（待支付、配送中、已完成、已取消等）；已支付订单可查看 **配送进度**（待派单 / 已派单等与后端 `delivery_task` 一致）。

![main_page](/images/2.png)

- 支持 **取消订单**；可区分 **已支付**、**待支付** 等展示。
![main_page](/images/3.png)

![main_page](/images/4.png)

![main_page](/images/6.png)

### 优惠券

- **我的优惠券**：查看已领取的券及 **使用状态**（未使用 / 已使用 / 锁定等）。

![main_page](/images/7.png)

### 购物车

- 按商家分组；支持 **一键清空**、**删除单品**、**勾选与数量调整** 后进入结算。

![main_page](/images/12.png)

### 个人主页

- 展示用户 **基本信息**（含头像）及 **该用户发布的评价**。

![main_page](/images/13.png)

---

## 安全与部署提示

- 演示用 **JWT 密钥、数据库密码** 等请勿直接用于生产；部署前请修改 `application-*.yml` 与密钥配置。

---

*VibeCoding Project · [VibeCode-JiKekaiFan](https://github.com/leslie1992-dqp/VibeCode-JiKekaiFan)*

