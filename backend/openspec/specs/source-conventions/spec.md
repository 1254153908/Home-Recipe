# source-conventions Specification

## Purpose
HomeRecipe 后端的源码基线约定：Java 包名必须与目录一致（根包 `org.huhu.recipe`），以及 Web/Service 层的 Spring 注解规范。由首个变更 `fix-package-and-restcontroller`（修正历史包名 bug 并补全注解）沉淀而来。
## Requirements
### Requirement: 源码包名规范
所有 Java 源文件的 `package` 声明 MUST 与物理目录路径一致，且 MUST 使用项目根包 `org.huhu.recipe`。历史错误前缀 `java.` MUST NOT 出现。

#### Scenario: 新增或修改 Java 文件
- **WHEN** 在 `src/main/java/org/huhu/recipe/**` 下新增或修改 Java 文件
- **THEN** 其 `package` 声明为 `org.huhu.recipe.<layer>`（如 `org.huhu.recipe.Controller`），不含 `java.` 前缀，且与所在目录一致

### Requirement: Web 与 Service 层注解规范
HTTP 接口类 MUST 使用 `@RestController`，Service 实现类 MUST 使用 `@Service`，以保证接口可返回 JSON 且被 Spring 容器托管。

#### Scenario: 定义 HTTP 接口
- **WHEN** 定义一个对外 HTTP 接口类
- **THEN** 该类标注 `@RestController`（而非仅 `@Controller`）

#### Scenario: 实现 Service 接口
- **WHEN** 编写 `Service` 接口的实现类
- **THEN** 该类标注 `@Service` 且 `implements` 对应接口

