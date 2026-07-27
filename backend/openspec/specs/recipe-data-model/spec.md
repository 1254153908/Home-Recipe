# recipe-data-model Specification

## Purpose
HomeRecipe 菜谱步骤的数据模型约定：做法不再用 `recipe.method` 单列文本，而是拆到独立的 `recipe_step` 表（每步一行，含 step_no / content / image_url，每步可自带图片）。由变更 `recipe-step-table`（演示期做法步骤化重构）沉淀而来。
## Requirements
### Requirement: 菜谱步骤独立存储
菜谱的每一步 MUST 作为 `recipe_step` 表中的独立一行存储，字段至少包含 `recipe_id`、`step_no`、`content`、`image_url`，步骤顺序由 `step_no` 表达。

#### Scenario: 创建带步骤的菜谱
- **WHEN** 通过 `POST /api/recipes` 提交包含 `steps` 数组的菜谱
- **THEN** 每条 `steps` 元素 MUST 写入 `recipe_step` 一行，其 `step_no` 按数组下标从 1 递增

#### Scenario: 查询菜谱详情
- **WHEN** 查询某菜谱详情或计划联查
- **THEN** 返回结果 MUST 包含按 `step_no` 升序排列的 `steps` 列表

### Requirement: 做法不再使用单列文本
`recipe` 表 MUST NOT 保留 `method` 单列文本字段；做法内容 MUST 仅存在于 `recipe_step`。

#### Scenario: 读取做法
- **WHEN** 任何接口需要返回菜谱做法
- **THEN** 返回结构 MUST 为 `steps` 数组，而非 `method` 字符串

### Requirement: 步骤可独立配图
`recipe_step` 的 `image_url` 字段 MUST 允许为空，以表达"该步骤无独立图片"。

#### Scenario: 步骤图片可选
- **WHEN** 提交某个步骤时未提供 `imageUrl`
- **THEN** 该 `recipe_step` 行的 `image_url` MUST 为 NULL，且不影响其他步骤

