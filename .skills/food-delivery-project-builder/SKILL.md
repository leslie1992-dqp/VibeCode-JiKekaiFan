---
name: food-delivery-project-builder
description: 基于 design.md 生成并实现外卖系统项目，使用固定执行流程与完整模块映射。适用于需要进行代码脚手架搭建、API 实现、表与实体映射、模块功能实现与业务流程落地的任务。
---

# 外卖项目构建技能指引

请按以下顺序读取并执行引用文档。

## Required References

1. `references/01-unified-execution-process.md`
   - 定义统一执行顺序与实现检查清单，任何开发任务先读此文件。
2. `references/02-api-mapping.md`
   - 定义全部 REST API 路径、方法、参数、返回与错误码映射。
3. `references/03-db-entity-mapping.md`
   - 定义数据库表到实体类字段映射、类型映射、主键/索引/唯一约束规则。
4. `references/04-module-features.md`
   - 定义各业务模块功能边界与功能清单（user/merchant/product/cart/order/coupon/payment）。
5. `references/05-feature-workflows.md`
   - 定义核心功能业务流程（下单、秒杀、支付超时取消、幂等、购物车计价等）。

## Usage Rule

不得跳过引用文档。除非用户明确要求，否则不得超出映射契约自行新增接口或表结构。
