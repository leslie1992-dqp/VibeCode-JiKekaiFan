# 数据库表清单

> 与迁移脚本、实体类同步。

## 表清单（当前）

| 表名 | 用途 | 对应实体 |
|---|---|---|
| user | 用户信息 | UserEntity |
| user_address | 收货地址 | UserAddressEntity |
| merchant | 商家 | MerchantEntity |
| product_category | 商品分类 | ProductCategoryEntity |
| product | 商品 | ProductEntity |

迁移：`V1__create_user_and_address.sql`、`V2__merchant_product.sql`（含演示数据）。

## user

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|---|---|---|---|---|
| id | bigint | 否 | 自增 | 主键 |
| mobile | varchar(20) | 否 | - | 手机号，唯一 |
| password_hash | varchar(128) | 否 | - | 密码哈希 |
| nickname | varchar(64) | 是 | - | 昵称 |
| status | tinyint | 否 | 1 | 1 正常 0 禁用 |
| created_at | datetime | 否 | - | 创建时间 |
| updated_at | datetime | 否 | - | 更新时间 |

## user_address

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|---|---|---|---|---|
| id | bigint | 否 | 自增 | 主键 |
| user_id | bigint | 否 | - | 用户 ID |
| contact_name | varchar(64) | 否 | - | 联系人 |
| contact_phone | varchar(20) | 否 | - | 联系电话 |
| province | varchar(32) | 否 | - | 省 |
| city | varchar(32) | 否 | - | 市 |
| district | varchar(32) | 否 | - | 区 |
| detail | varchar(255) | 否 | - | 详细地址 |
| latitude | decimal(10,6) | 是 | - | 纬度 |
| longitude | decimal(10,6) | 是 | - | 经度 |
| is_default | tinyint | 否 | 0 | 是否默认地址 |
| created_at | datetime | 否 | - | 创建时间 |
| updated_at | datetime | 否 | - | 更新时间 |

## merchant

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| name | varchar(128) | 商家名称 |
| logo_url | varchar(255) | Logo |
| min_delivery_amount | decimal(10,2) | 起送价 |
| delivery_fee | decimal(10,2) | 配送费 |
| status | tinyint | 1 营业 0 休息 |
| rating | decimal(3,2) | 评分 |
| monthly_sales | int | 月售 |
| created_at / updated_at | datetime | 时间 |

## product_category

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| merchant_id | bigint | 商家 ID |
| name | varchar(64) | 分类名 |
| sort | int | 排序 |
| created_at / updated_at | datetime | 时间 |

## product

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| merchant_id | bigint | 商家 ID |
| category_id | bigint | 分类 ID |
| name | varchar(128) | 商品名 |
| description | varchar(512) | 描述 |
| price | decimal(10,2) | 售价 |
| stock / sales | int | 库存、销量 |
| status | tinyint | 1 上架 0 下架 |
| version | int | 乐观锁预留 |
| created_at / updated_at | datetime | 时间 |
