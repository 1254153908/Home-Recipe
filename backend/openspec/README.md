# OpenSpec 使用与规约记录指南（HomeRecipe 后端）

本文件是**操作指南 + 规约记录入口**，不重复承载约定内容——约定权威源是 `openspec/config.yaml`，落点是 `openspec/specs/`。

---

## 1. OpenSpec 各文件的作用

| 路径 | 作用 | 谁来读/写 |
|---|---|---|
| `openspec/config.yaml` | 全局上下文（技术栈、依赖约束、变更铁律）+ 各 artifact 的生成规则。AI 在**生成 proposal/spec/tasks** 时读取。 | 已生成，少改 |
| `openspec/specs/<能力>/spec.md` | **主规格 / 规约库**：当前已批准的设计与约定，是"活文档"。每次 change 归档后，其增量 spec 会合并进来。**规约记录的最终落点就在这里。** | AI 归档时自动更新 |
| `openspec/changes/<变更名>/proposal.md` | 提案：为什么改、改什么、**影响评估**、非目标。先评审再动手。 | 新建变更时生成 |
| `openspec/changes/<变更名>/tasks.md` | 实现任务清单（带 `- [ ]/[x]` 勾选）。 | 新建变更时生成 |
| `openspec/changes/<变更名>/spec.md` | 本次变更的**增量规格**（新增/修改的 `### Requirement` 与 Scenario）。 | 新建变更时生成 |
| `openspec/archive/<日期>-<变更>/` | 已完成变更的归档，保留历史可追溯。 | `openspec archive` 自动生成 |

---

## 2. 与"直接让 AI 写代码"的具体区别

以"给 RecipeController 加一个创建菜谱的接口"为例：

| 维度 | 直接 AICoding | 走 OpenSpec |
|---|---|---|
| 起点 | 你一句话"加个接口"，AI 立刻改文件。 | 先 `openspec new change`，AI 先产出 proposal 供你评审。 |
| 影响评估 | 容易忽略：现有 Controller 是 `@Controller` 空壳、包名写错、无统一返回格式。 | proposal 必须含"影响评估"小节（由 config 规则强制），把上述坑点暴露出来。 |
| 数据库改动 | 直接改表，常漏迁移/回滚。 | 涉及 DB 的 proposal 必须含"数据迁移方案"小节（config 规则强制）。 |
| 约定沉淀 | 改完即忘，下次会话丢失上下文，AI 重新瞎猜。 | 实现后 `archive`，增量 spec 合并进 `specs/`，成为**持久化规约**，后续 AI 自动遵循。 |
| 可追溯性 | 无。为什么这么改、谁批准的，查不到。 | 每个 change 有 proposal/tasks/spec + 归档，形成变更史。 |
| 老项目护栏 | 容易顺手"顺手重构"核心逻辑。 | config 规则禁止夹带无关重构，重构须单独 proposal。 |

**一句话**：直接 AICoding 是"改了再说"，OpenSpec 是"先定义再改、改完沉淀为规约"。对运行 3 年的老项目，后者能挡住破坏性改动。

---

## 3. 规约记录怎么落地（本项目当前状态）

- 真实约定应当写在 `openspec/specs/<能力>/spec.md`，随 change 归档自动累积。
- **当前项目是空壳**：尚无任何业务能力 spec，因此 `specs/` 目录待首个 change 创建。
- 已确认的硬事实（依赖版本、Java 8 等）与"待补全"项（错误码、API 返回格式、命名细节）全部在 `openspec/config.yaml`，不在此复述。
- 下方"规约/决策记录"表用于**人工沉淀**那些跨 change 的共识（例如"统一返回用 Result<T>"一旦确定就记下）。

### 规约 / 决策记录（Decision Log）

> 格式：日期 | 决策 | 落点文件 | 状态
> 当前为空，待首个业务 change 确定约定后填写。

| 日期 | 决策 | 落点 | 状态 |
|---|---|---|---|
| — | 错误码规则（待业务代码确定） | specs/ | 待补全 |
| — | API 统一返回格式（待确定 Result<T>） | specs/ | 待补全 |
| — | 命名规范细节（待补全） | specs/ | 待补全 |

---

## 4. 常用命令

```bash
openspec new change <变更名>     # 新建变更，生成 proposal/tasks/spec 骨架
openspec list                    # 列出当前进行中的 change
openspec status --change <名>    # 查看某 change 的 artifact / task 完成度
openspec instructions <artifact> # 输出生成某 artifact 的强化指令（含 config 上下文）
openspec validate <名>           # 校验 change / spec 是否合规
openspec archive <变更名>        # 归档已完成变更，delta spec 同步进 specs/
openspec spec                    # 查看/管理主规格库
openspec show <名>               # 查看某个 change 或 spec
```

---

## 5. 典型工作流（老项目加功能）

1. `openspec new change add-recipe-api` → AI 生成 proposal/tasks/spec 骨架。
2. 补全 proposal 的**影响评估**与（若涉及 DB）**迁移方案**，人工评审。
3. 按 tasks 实现，遵守 `AGENTS.md` 的模块边界与历史坑点。
4. `openspec validate` 自检 → 实现完成后 `openspec archive`。
5. 增量约定自动沉淀到 `specs/`，并在上方 Decision Log 记录关键共识。
