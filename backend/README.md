# HomeRecipe 后端 API 文档

家常菜谱管理后端，提供菜谱（含步骤/原料/调料）的增删改查、收藏、AI 识菜草稿生成，以及膳食计划管理。

> 本文件供前端开发对接使用。所有接口均已基于源码核对。

## 技术栈

- Java 8 + Spring Boot 2.7.18
- MyBatis-Plus 3.5.5（ORM）
- MySQL 8（`com.mysql.cj.jdbc.Driver`）
- Lombok
- 构建工具：Maven

## 基础信息

- **服务地址**：`http://localhost:4993`
- **API 前缀**：所有接口以 `/api` 开头
- **数据格式**：请求与响应均为 `application/json`（个别接口用 query 参数，见下文）
- **鉴权**：**无**。接口通过 `userId` query 参数区分用户（默认 `0`），目前没有用户表，也没有登录体系。
- **OpenAPI/Swagger**：未集成，本文件即接口文档。

### 运行（本地）

```bash
mvn spring-boot:run
```

数据库配置见 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/home_recipe?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

> ⚠️ 注意：`docker/docker-compose.yml` 起的是 MySQL **3307 端口 / 库名 `eng-cloud` / 密码 `eng123`**，与 `application.yml` 默认不一致。用 Docker 起库时需手动改 `application.yml` 或 compose 文件对齐。

## 数据模型

### Recipe（菜谱）`recipe`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键，自增 |
| title | String | 标题 |
| imageUrl | String | 封面图 |
| sourceType | String | 来源类型（如 link/video/image） |
| sourceUrl | String | 来源链接 |
| createdAt | String(DateTime) | 创建时间 |

### RecipeStep（步骤）`recipe_step`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| recipeId | Long | 关联菜谱 |
| stepNo | Integer | 步骤序号（从 0 开始，列表下标即顺序） |
| content | String | 步骤内容 |
| imageUrl | String | 步骤图 |

### Ingredient / Seasoning（原料 / 调料 字典）`ingredient` / `seasoning`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| name | String | 名称 |
| createdAt | String(DateTime) | 创建时间 |

### MealPlan（膳食计划）`meal_plan`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| recipeId | Long | 关联菜谱 |
| remark | String | 备注 |
| status | String | 状态 |
| review | String | 评价/评分 |
| imageUrl | String | 计划图 |
| planDate | String(Date) | 计划日期 |
| createdAt | String(DateTime) | 创建时间 |

### 其他表
- `recipe_ingredient`（recipe_id, ingredient_id, quantity, unit）
- `recipe_seasoning`（recipe_id, seasoning_id, quantity, unit）
- `recipe_favorite`（recipe_id, user_id）

## 通用 DTO

**ItemInput / ItemView**（原料、调料的输入/输出项）
```json
{ "name": "盐", "quantity": "5", "unit": "g" }
```

**StepInput**（步骤输入，顺序即列表下标）
```json
{ "content": "热锅下油", "imageUrl": "https://..." }
```

---

## API 接口

### 菜谱 Recipe `/api/recipes`

#### 创建菜谱
`POST /api/recipes`

请求体：
```json
{
  "title": "番茄炒蛋",
  "imageUrl": "https://...",
  "sourceType": "link",
  "sourceUrl": "https://...",
  "steps": [
    { "content": "打蛋", "imageUrl": "" },
    { "content": "下锅炒", "imageUrl": "" }
  ],
  "ingredients": [
    { "name": "番茄", "quantity": "2", "unit": "个" }
  ],
  "seasonings": [
    { "name": "盐", "quantity": "5", "unit": "g" }
  ]
}
```
响应：`RecipeDetail`（见下）

#### 获取菜谱详情
`GET /api/recipes/{id}`

响应：`RecipeDetail`
```json
{
  "recipe": { "id": 1, "title": "番茄炒蛋", "imageUrl": "...", "sourceType": "link", "sourceUrl": "...", "createdAt": "..." },
  "steps": [ { "id": 1, "recipeId": 1, "stepNo": 0, "content": "打蛋", "imageUrl": "" } ],
  "ingredients": [ { "name": "番茄", "quantity": "2", "unit": "个" } ],
  "seasonings": [ { "name": "盐", "quantity": "5", "unit": "g" } ]
}
```

#### 更新菜谱
`PUT /api/recipes/{id}`

请求体：同「创建菜谱」的 `RecipeCreateRequest`。响应：`RecipeDetail`

#### 删除菜谱
`DELETE /api/recipes/{id}`

响应：HTTP 200，空 body

#### 菜谱列表
`GET /api/recipes`

响应：`Recipe[]`（仅菜谱基础字段，不含步骤/原料）
```json
[ { "id": 1, "title": "番茄炒蛋", "imageUrl": "...", "sourceType": "link", "sourceUrl": "...", "createdAt": "..." } ]
```

#### 收藏 / 取消收藏
`POST /api/recipes/{id}/favorite?userId=0`
`DELETE /api/recipes/{id}/favorite?userId=0`

响应：HTTP 200，空 body

#### 我的收藏列表
`GET /api/recipes/favorites?userId=0`

响应：`Recipe[]`

#### AI 识菜生成草稿
`POST /api/recipes/ai-recognize`

请求体：
```json
{ "sourceType": "link", "content": "https://.../video-or-image" }
```
> 后端转发到 mock 服务 `http://localhost:5000/recognize`（配置项 `ai.mock-service.url`）。本 demo 仅作透传，需另起 Flask 服务。

响应：`RecipeDraft`（可用来预填创建表单）
```json
{
  "title": "识别出的菜名",
  "imageUrl": "https://...",
  "steps": [ { "content": "...", "imageUrl": "" } ],
  "ingredients": [ { "name": "番茄", "quantity": "2", "unit": "个" } ],
  "seasonings": [ { "name": "盐", "quantity": "5", "unit": "g" } ]
}
```

---

### 原料 Ingredient `/api/ingredients`

#### 原料列表
`GET /api/ingredients`

响应：`Ingredient[]` → `[ { "id": 1, "name": "番茄", "createdAt": "..." } ]`

#### 按名解析 / 新建原料
`POST /api/ingredients?name=番茄`

> `name` 是 **query 参数**（非 JSON body）。名称不存在时自动新建，返回其 id（精确、区分大小写）。

响应：`Long`（原料 id）

---

### 调料 Seasoning `/api/seasonings`

#### 调料列表
`GET /api/seasonings`

响应：`Seasoning[]` → `[ { "id": 1, "name": "盐", "createdAt": "..." } ]`

#### 按名解析 / 新建调料
`POST /api/seasonings?name=盐`

响应：`Long`（调料 id）

---

### 膳食计划 MealPlan `/api/meal-plans`

#### 创建计划
`POST /api/meal-plans`

请求体（即 `MealPlan` 实体字段）：
```json
{ "recipeId": 1, "remark": "今晚做", "status": "planned", "review": "", "imageUrl": "", "planDate": "2026-07-27" }
```
响应：`MealPlan`（含 id、createdAt）

#### 计划列表
`GET /api/meal-plans`

响应：`MealPlan[]`

#### 计划详情
`GET /api/meal-plans/{id}`

响应：`MealPlan`

#### 更新计划
`PUT /api/meal-plans/{id}`

请求体：同创建（完整 `MealPlan`）。响应：`MealPlan`

#### 删除计划
`DELETE /api/meal-plans/{id}`

响应：HTTP 200，空 body

#### 计划关联的完整菜谱
`GET /api/meal-plans/{id}/recipe`

响应：`PlanRecipeDetailVO`
```json
{
  "planId": 1, "remark": "今晚做", "status": "planned", "review": "", "planImageUrl": "", "planDate": "2026-07-27",
  "recipeId": 1, "title": "番茄炒蛋", "recipeImageUrl": "https://...",
  "steps": [ { "id": 1, "recipeId": 1, "stepNo": 0, "content": "打蛋", "imageUrl": "" } ],
  "ingredients": [ { "name": "番茄", "quantity": "2", "unit": "个" } ],
  "seasonings": [ { "name": "盐", "quantity": "5", "unit": "g" } ]
}
```

---

## 前端对接注意事项

1. **跨域（CORS）**：后端**未配置 CORS**。前端若跑在另一个端口（如 3000/5173），浏览器会被拦截。开发期建议加 `WebMvcConfigurer` 放开 CORS，或用前端 dev server 代理把 `/api` 转到 `localhost:4993`。
2. **无鉴权**：`userId` 直接当 query 参数传（默认 `0`），没有登录/Token。收藏、我的收藏等接口需自行在前端维护当前用户 id。
3. **空响应**：删除、收藏、取消收藏类接口返回 **HTTP 200 + 空 body**（不是 204），前端不要解析 JSON。
4. **`/api/ingredients` 与 `/api/seasonings` 的新建**用 `?name=` query 参数，不是 POST body。
5. **AI 识菜**依赖外部 mock 服务（默认 `localhost:5000/recognize`），未启动时该接口不可用。
6. **日期格式**：`planDate` 为 `yyyy-MM-dd`；`createdAt` 为 `yyyy-MM-dd'T'HH:mm:ss`（LocalDateTime 默认序列化）。

## 数据库表一览

`recipe`、`recipe_step`、`recipe_ingredient`、`recipe_seasoning`、`recipe_favorite`、`ingredient`、`seasoning`、`meal_plan`

字段详情见上方「数据模型」。Java 字段为驼峰，数据库列为下划线（MyBatis-Plus 已开启 `map-underscore-to-camel-case`）。
