# 功能与 API 映射

> 与代码同步：用户模块已实现接口见下表。

## API 清单（用户模块）


| 模块   | 功能   | 方法     | 路径                           | 鉴权  | 说明           |
| ---- | ---- | ------ | ---------------------------- | --- | ------------ |
| user | 用户注册 | POST   | /api/v1/auth/register        | 否   | 手机号注册        |
| user | 用户登录 | POST   | /api/v1/auth/login           | 否   | 登录并获取 JWT    |
| user | 当前用户 | GET    | /api/v1/users/me             | 是   | Bearer Token |
| user | 地址列表 | GET    | /api/v1/users/addresses      | 是   | Bearer Token |
| user | 新增地址 | POST   | /api/v1/users/addresses      | 是   | Bearer Token |
| user | 更新地址 | PUT    | /api/v1/users/addresses/{id} | 是   | Bearer Token |
| user | 删除地址 | DELETE | /api/v1/users/addresses/{id} | 是   | Bearer Token |

## API 清单（商家模块，首页）

| 模块 | 功能 | 方法 | 路径 | 鉴权 | 说明 |
| ---- | ---- | ---- | ---- | --- | ---- |
| merchant | 商家分页列表 | GET | /api/v1/merchants | 否 | keyword、pageNo、pageSize |
| merchant | 商家详情 | GET | /api/v1/merchants/{merchantId} | 否 | 营业中商家 |

## 功能对应 API（占位扩展）


| 功能页面                          | 相关 API                                              |
| ----------------------------- | --------------------------------------------------- |
| features/user/auth-login.md   | POST /api/v1/auth/login, POST /api/v1/auth/register |
| features/user/user-profile.md | GET /api/v1/users/me                                |
| features/user/user-address.md | GET/POST/PUT/DELETE /api/v1/users/addresses         |
| views/home/HomeView.vue（首页商家） | GET /api/v1/merchants |


