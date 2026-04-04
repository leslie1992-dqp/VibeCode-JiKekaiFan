---
name: code-driven-wiki
description: 在每次实现或修改代码后，同步生成或更新项目 Wiki（Mermaid ER 图、功能流程图、全量表与 API 映射）。适用于需要文档与代码一致、以 Markdown+Mermaid 形式维护知识库的场景。
---

# 代码驱动 Wiki 技能

本技能定义「代码变更 → Wiki 同步」的固定做法。执行实现任务时，每完成一段可合并的代码变更，必须按引用文档顺序更新 Wiki。

## 必读引用（按顺序）

1. `references/01-overview-and-triggers.md`
   - 说明 Wiki 的定位、何时触发更新、与代码提交的对应关系。
2. `references/02-wiki-structure.md`
   - 说明 Wiki 根目录、子目录、文件命名与索引页约定。
3. `references/03-mermaid-er-diagrams.md`
   - 说明如何用 Mermaid `erDiagram` 维护数据库 ER 图及与表变更同步规则。
4. `references/04-mermaid-feature-flows.md`
   - 说明如何用 Mermaid `flowchart`/`sequenceDiagram` 维护每个功能的业务逻辑流程图。
5. `references/05-database-tables-wiki.md`
   - 说明全量数据库表清单文档格式及与实体/迁移的同步方式。
6. `references/06-api-and-features-wiki.md`
   - 说明全量 API 清单、功能与 API 的对应表及更新规则。

## 执行规则

- 不得只改代码不更新 Wiki；若本次变更不涉及库表或接口，也需在变更说明中注明「Wiki 无增量」。
- Mermaid 语法需符合引用文档中的约束（节点命名、避免保留字等）。
- Wiki 文件统一使用 UTF-8 编码的 Markdown（`.md`）。
