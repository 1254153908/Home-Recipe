# AGENTS.md — HomeRecipe 后端（编码操作护栏）

> 项目约定、依赖约束、变更铁律的**权威来源是 `openspec/config.yaml`**。本文只补充**编码期**的操作护栏与历史坑点，避免与 config 重复。

## 模块边界
- **禁止直接修改**（属核心边界，改动须先走 proposal 并做影响评估）：
  - `Controller/*`：对外接口层，签名变更影响前端。
  - `Service`（接口）：服务契约，改动影响所有实现与调用方。
- **可迭代优化**：
  - `Service/impl/*`：业务实现，可增量补充，但仍须走变更评估。
- **高风险模块**（处理时额外校验兼容性）：
  - 持久层 MyBatis-Plus 映射与 XML：表结构/字段变更须带迁移方案。
  - 依赖升级（Spring Boot / MyBatis-Plus / Java）：破坏性极高，必须单列风险说明。

## 历史坑点（务必注意，避免生成不符合项目现状的代码）
1. **包名错误**：现有源码包声明为 `java.org.huhu.recipe...`，与目录 `org/huhu/recipe/...` 不符，当前无法编译。新增/修改文件必须写对包名 `org.huhu.recipe.*`。
2. **注解缺失/误用**：`RecipeServiceImpl` 漏了 `@Service`；Controller 现用 `@Controller` 而非 `@RestController`，无 JSON 返回能力。新增接口注意补齐。
3. **配置缺失**：`src/main/resources` 无 `application.yml`、无数据源配置、无 Mapper 扫描配置，启动即报错，需先补齐。
4. **无测试**：`src/test` 为空，无法用测试验证改动。

## 测试（当前状态与要求）
- 测试目录：`src/test/java`（当前为空）。
- 运行：`mvn test`（需先补齐测试配置与用例）。
- 要求：完成代码修改后，若已补充用例，必须先 `mvn test` 校验；若仍为空，须在 proposal 中说明“无测试可校验”及手动验证方式。
