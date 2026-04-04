# Wiki 目录与文件约定

## 推荐根目录

在项目根目录下使用 `wiki/` 作为 Wiki 根目录（与业务代码分离，便于检索与静态站点生成）。

```text
wiki/
  README.md                    # Wiki 入口：导航、最近更新、约定说明
  database/
    er-diagram.md              # 全库 ER 图（Mermaid）
    tables.md                  # 全量表清单与字段说明
  features/
    <module>/                  # 与业务模块一致，如 user、order、payment
      <feature-slug>.md        # 单功能一页，内含该功能流程图
  api/
    features-and-apis.md       # 功能 ↔ API 映射总表
    openapi.md                 # 可选：汇总或链接到 OpenAPI
```

## 命名规则

- **模块目录**：小写英文，与后端包 `module` 或 `controller` 下分包名一致。
- **功能文件**：`kebab-case`，如 `order-create.md`、`payment-callback.md`。
- **禁止**：在文件名中使用空格；中文可用于 `README.md` 内标题，文件名建议英文。

## 索引维护

- `wiki/README.md` 中维护到各模块的链接，并在每次较大变更后增加「更新日志」小节（日期 + 简述）。
- 新增功能文件后，在对应模块下增加一条链接，或在 `README.md` 中增加模块入口。

## 与代码仓库的关系

- Wiki 与代码 **同仓库** 存储，随 PR 一起评审。
- 若项目使用多仓库，在 `wiki/README.md` 注明主仓库与文档仓库的同步策略。
