-- HomeRecipe 后端建库建表 + 假数据种子
-- 执行方式：mysql -u<user> -p < schema.sql
-- 或直接在 MySQL 客户端 source 本文件

CREATE DATABASE IF NOT EXISTS home_recipe
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE home_recipe;

-- 食材
DROP TABLE IF EXISTS ingredient;
CREATE TABLE ingredient (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 调料
DROP TABLE IF EXISTS seasoning;
CREATE TABLE seasoning (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜谱
DROP TABLE IF EXISTS recipe;
CREATE TABLE recipe (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(128) NOT NULL,
  image_url   VARCHAR(1024) DEFAULT NULL,
  source_type VARCHAR(16)  DEFAULT 'manual' COMMENT 'manual / ai',
  source_url  VARCHAR(512) DEFAULT NULL,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜谱-食材明细（含数量）
DROP TABLE IF EXISTS recipe_ingredient;
CREATE TABLE recipe_ingredient (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  recipe_id     BIGINT       NOT NULL,
  ingredient_id BIGINT       NOT NULL,
  quantity      VARCHAR(32)  DEFAULT NULL,
  unit          VARCHAR(16)  DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_recipe (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜谱-调料明细（含数量）
DROP TABLE IF EXISTS recipe_seasoning;
CREATE TABLE recipe_seasoning (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  recipe_id    BIGINT       NOT NULL,
  seasoning_id BIGINT       NOT NULL,
  quantity     VARCHAR(32)  DEFAULT NULL,
  unit         VARCHAR(16)  DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_recipe (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜谱步骤（每步独立一行，可带自己的图片）
DROP TABLE IF EXISTS recipe_step;
CREATE TABLE recipe_step (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  recipe_id  BIGINT       NOT NULL,
  step_no    INT          NOT NULL,
  content    TEXT         NOT NULL,
  image_url  VARCHAR(512) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_recipe (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏
DROP TABLE IF EXISTS recipe_favorite;
CREATE TABLE recipe_favorite (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  recipe_id  BIGINT   NOT NULL,
  user_id    BIGINT   DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_recipe_user (recipe_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 膳食计划
DROP TABLE IF EXISTS meal_plan;
CREATE TABLE meal_plan (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  recipe_id  BIGINT       DEFAULT NULL,
  remark     VARCHAR(512) DEFAULT NULL,
  status     VARCHAR(16)  DEFAULT 'planned' COMMENT 'planned / cooking / done',
  review     VARCHAR(512) DEFAULT NULL,
  image_url  VARCHAR(512) DEFAULT NULL,
  plan_date  DATE         DEFAULT NULL,
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_recipe (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ 假数据种子 ============
INSERT INTO ingredient (name) VALUES
  ('西红柿'),('鸡蛋'),('土豆'),('青椒'),('牛肉'),('洋葱'),('大蒜'),('生姜');

INSERT INTO seasoning (name) VALUES
  ('盐'),('糖'),('生抽'),('老抽'),('料酒'),('食用油'),('胡椒粉'),('醋');

INSERT INTO recipe (title, image_url, source_type) VALUES
  ('番茄炒蛋', 'https://example.com/img/tomato-egg.jpg', 'manual'),
  ('青椒炒牛肉', 'https://example.com/img/pepper-beef.jpg', 'manual');

INSERT INTO recipe_step (recipe_id, step_no, content, image_url) VALUES
  (1, 1, '鸡蛋打散加盐', 'https://example.com/img/step1.jpg'),
  (1, 2, '番茄切块', NULL),
  (1, 3, '热油炒蛋盛出', NULL),
  (1, 4, '炒番茄出汁后回锅鸡蛋翻炒均匀', NULL),
  (2, 1, '牛肉切片腌制', NULL),
  (2, 2, '青椒切段', NULL),
  (2, 3, '热油爆香蒜姜', NULL),
  (2, 4, '下牛肉快炒，加青椒调味出锅', NULL);

INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit) VALUES
  (1, 1, 2, '个'), (1, 2, 3, '个'),
  (2, 4, 2, '个'), (2, 5, 200, 'g'), (2, 6, 1, '个'), (2, 7, 2, '瓣'), (2, 8, 1, '块');

INSERT INTO recipe_seasoning (recipe_id, seasoning_id, quantity, unit) VALUES
  (1, 1, 2, 'g'), (1, 7, 1, 'g'), (1, 6, 15, 'ml'),
  (2, 1, 3, 'g'), (2, 3, 10, 'ml'), (2, 5, 5, 'ml'), (2, 6, 20, 'ml');

INSERT INTO recipe_favorite (recipe_id, user_id) VALUES (1, 0);

INSERT INTO meal_plan (recipe_id, remark, status, review, plan_date) VALUES
  (1, '午餐', 'done', '很下饭', '2026-07-27'),
  (2, '晚餐', 'not_started', NULL, '2026-07-28');

-- ============ 烹饪日志（cooking_log）============
-- 当膳食计划状态变为 done 时，自动生成一条日志记录
DROP TABLE IF EXISTS cooking_log;
CREATE TABLE cooking_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  plan_id       BIGINT       NOT NULL COMMENT '关联的膳食计划ID',
  recipe_id     BIGINT       NOT NULL COMMENT '关联的菜谱ID',
  recipe_title  VARCHAR(200) DEFAULT NULL COMMENT '菜谱名称（冗余）',
  plan_date     DATE         DEFAULT NULL COMMENT '计划日期',
  completed_at  DATETIME     DEFAULT NULL COMMENT '完成时间',
  image_url     VARCHAR(500) DEFAULT NULL COMMENT '成果图片',
  review        VARCHAR(1000) DEFAULT NULL COMMENT '评价/心得',
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
