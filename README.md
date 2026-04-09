# 即刻开饭（JiKekaiFan）

基于 **Spring Boot 3** 与 **Vue 3** 的前后端分离外卖点餐演示项目：商家浏览与搜索、购物车、订单与支付状态、商家秒杀券、评价与头像、个人中心等。仓库内附带与代码同步的 **Wiki**（数据库 ER、API 映射、业务流程图）。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 3.2、MyBatis-Plus、Flyway、JWT（`Authorization: Bearer`） |
| 数据 | MySQL、Redis（秒杀 Lua 闸门等） |
| 消息 | Kafka（秒杀异步落库，可在配置中关闭改为同步写库） |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios |
| API | 统一前缀 `/api/v1`；静态/上传文件访问见 `application.yml` 中 `app.upload` |

---

## 环境要求

- **JDK 17+**（Maven 构建与运行）
- **Node.js**（建议 LTS，用于前端）
- **MySQL**（数据库；连接信息见 `backend/src/main/resources/application-dev.yml`）
- **Redis**（秒杀等能力需要；仅本地调试可结合配置关闭 Lua 路径）
- **Kafka + Zookeeper**（若开启 `app.seckill.kafka-enabled: true`；本地可改为 `false` 同步落库）

---

## 快速开始

### 1. 后端

```bash
cd backend
mvn spring-boot:run
```

默认服务端口：**8081**（见 `application.yml`）。

### 2. 前端

```bash
cd frontend
npm install
npm run dev
```

开发地址：**http://localhost:5173**

### 3.（可选）本地启动 Kafka（Windows 示例）

详见仓库内 `start.md`：先启 Zookeeper，再启 Kafka。

---

## 功能说明与界面预览

> 请将截图放在仓库根目录 `images/` 下（与下列路径一致），推送到 GitHub 后即可在 README 中正常显示。

### 登录与账号

- **登录页**：手机号 + 密码登录。
- **无账号**：进入 **注册** 页面完成注册。
- **忘记密码**：进入 **重置密码** 页面。

![登录](images/14.png)

### 首页

- 浏览全部商家，支持 **推荐、距离、评分、月售** 排序。
- 支持 **关键词搜索** 商家名称。

![首页](images/1.png)

### 商家详情

- **商家限时秒杀券**：展示可抢优惠券及剩余库存等（与后端秒杀/券逻辑一致）。
- **商品列表**：按分类浏览，可将商品 **加入购物车**，或进入 **下单（草稿）** 流程。

![商家限时优惠券](images/8.png)

![商品列表与加购 / 下单](images/9.png)

### 草稿下单

- 点击 **下单** 后可查看当前待结算商品列表（含金额试算）。
- 不需要的商品可从草稿中 **删除**。

![草稿商品列表](images/5.png)

![删除草稿商品](images/10.png)

### 商家评价

- 展示商家 **评价列表**；已下单用户可 **发布评价**（含图片上传等能力，见后端上传配置）。

![评价列表与发布](images/11.png)

### 订单

- **订单列表**：展示全部订单及状态（待支付、进行中、已完成、已取消等）。
- 支持 **取消订单**；展示 **成功交易**、**待支付** 等状态。

![订单列表](images/2.png)

![订单取消](images/3.png)

![成功交易](images/4.png)

![待支付](images/6.png)

### 优惠券

- **我的优惠券**：查看已领取券及 **使用状态**（未使用 / 已使用 / 锁定等）。

![我的优惠券](images/7.png)

### 购物车

- 按商家分组展示；支持 **清空购物车**、**删除单品**、**勾选/数量调整** 后进入结算。

![购物车](images/12.png)

### 个人主页

- 展示用户 **基本信息**（含头像等）及 **该用户发布的评价**。

![个人主页](images/13.png)

---

## Wiki 文档（仓库内）

与代码同步的说明文档，建议从根目录 [wiki/README.md](wiki/README.md) 进入：

| 内容 | 路径 |
|------|------|
| 总索引与约定 | [wiki/README.md](wiki/README.md) |
| 数据库 ER 图（Mermaid） | [wiki/database/er-diagram.md](wiki/database/er-diagram.md) |
| 表清单与字段（Flyway V1–V18） | [wiki/database/tables.md](wiki/database/tables.md) |
| 功能与 REST API、前端路由 | [wiki/api/features-and-apis.md](wiki/api/features-and-apis.md) |
| 分层与 Redis / Kafka 说明 | [wiki/features/infra-and-layers.md](wiki/features/infra-and-layers.md) |
| 业务流程（登录、购物车、订单、秒杀等） | [wiki/features/flows/](wiki/features/flows/) |

### 文档配图预览

![秒杀流程](images/15.png)

![登录流程](images/16.png)

![数据库 E-R 图](images/17.png)

### API 文档示例（商家）

以商家模块为例，列出接口路径与前端调用关系，完整映射见 [wiki/api/features-and-apis.md](wiki/api/features-and-apis.md)。

![API 文档示例](images/18.png)

---

## 仓库结构（简要）

```text
backend/          Spring Boot 服务、Flyway 迁移、秒杀 Lua 脚本等
frontend/         Vue3 + Vite 前端
wiki/             与代码同步的 Wiki
design.md         架构与领域设计草案
start.md          本地启动命令与 Kafka 说明
```

---

## 说明

- 本项目用于学习 / 演示，**JWT 密钥、数据库密码等请在生产环境务必修改**。
- 若 README 中图片不显示，请检查是否已将 `images/*.png` 提交到仓库根目录。

---

*VibeCoding Project · [VibeCode-JiKekaiFan](https://github.com/leslie1992-dqp/VibeCode-JiKekaiFan)*
