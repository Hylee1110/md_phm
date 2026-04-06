SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS md_phms
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE md_phms;

CREATE TABLE IF NOT EXISTS t_user (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(64),
  realname VARCHAR(64),
  idcard VARCHAR(32),
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0-unknown 1-male 2-female',
  age INT,
  account_level TINYINT NOT NULL DEFAULT 0 COMMENT '0-user 1-admin',
  account_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-normal 1-readonly 2-disabled',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status_level(account_status, account_level)
) COMMENT='system user table';

CREATE TABLE IF NOT EXISTS t_health_goal (
  goal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  goal_code VARCHAR(64) NOT NULL UNIQUE,
  goal_name VARCHAR(128) NOT NULL,
  goal_description VARCHAR(500),
  metric_type TINYINT NOT NULL DEFAULT 1 COMMENT '1-number 2-text 3-boolean',
  unit VARCHAR(32),
  default_target_min DECIMAL(10,2),
  default_target_max DECIMAL(10,2),
  default_target_text VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  goal_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-enabled 1-disabled',
  created_by BIGINT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_health_goal_created_by FOREIGN KEY (created_by) REFERENCES t_user(user_id),
  INDEX idx_health_goal_status(goal_status),
  INDEX idx_health_goal_sort(sort_no)
) COMMENT='health goal definition';

CREATE TABLE IF NOT EXISTS t_user_health_goal (
  user_goal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  goal_id BIGINT NOT NULL,
  target_min DECIMAL(10,2),
  target_max DECIMAL(10,2),
  target_text VARCHAR(255),
  start_date DATE,
  end_date DATE,
  user_goal_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-active 1-done 2-cancelled',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_health_goal_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_user_health_goal_goal FOREIGN KEY (goal_id) REFERENCES t_health_goal(goal_id),
  CONSTRAINT uk_user_goal UNIQUE (user_id, goal_id),
  INDEX idx_uhg_user(user_id),
  INDEX idx_uhg_goal(goal_id),
  INDEX idx_uhg_status(user_goal_status)
) COMMENT='selected health goals';

CREATE TABLE IF NOT EXISTS t_health_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_goal_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  goal_id BIGINT NOT NULL,
  record_value DECIMAL(10,2),
  record_text VARCHAR(255),
  record_time DATETIME NOT NULL,
  record_source TINYINT NOT NULL DEFAULT 0 COMMENT '0-manual 1-device 2-system',
  evaluation_result TINYINT NOT NULL DEFAULT 0 COMMENT '0-none 1-low 2-ok 3-high',
  remark VARCHAR(500),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_health_record_user_goal FOREIGN KEY (user_goal_id) REFERENCES t_user_health_goal(user_goal_id),
  CONSTRAINT fk_health_record_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_health_record_goal FOREIGN KEY (goal_id) REFERENCES t_health_goal(goal_id),
  INDEX idx_hr_user_goal_time(user_goal_id, record_time DESC),
  INDEX idx_hr_user_goal(user_id, goal_id),
  INDEX idx_hr_record_time(record_time)
) COMMENT='health record table';

CREATE TABLE IF NOT EXISTS t_recipe (
  recipe_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  food_name VARCHAR(100) NOT NULL,
  meal_type TINYINT NOT NULL DEFAULT 0 COMMENT '0-unspecified 1-breakfast 2-lunch 3-dinner 4-snack',
  portion DECIMAL(10,2) DEFAULT NULL,
  unit VARCHAR(20) NOT NULL DEFAULT 'g',
  calories INT DEFAULT NULL COMMENT 'kcal per 100g',
  image_url VARCHAR(512) DEFAULT NULL,
  description TEXT,
  created_by BIGINT NOT NULL,
  is_admin_recommend TINYINT NOT NULL DEFAULT 0,
  recipe_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-normal 1-deleted 2-offline',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_recipe_user FOREIGN KEY (created_by) REFERENCES t_user(user_id),
  INDEX idx_recipe_creator(created_by),
  INDEX idx_recipe_meal_type(meal_type),
  INDEX idx_recipe_status(recipe_status),
  INDEX idx_recipe_admin_recommend(is_admin_recommend)
) COMMENT='recipe table';

CREATE TABLE IF NOT EXISTS t_recipe_rating (
  recipe_id BIGINT PRIMARY KEY,
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  rating_count INT NOT NULL DEFAULT 0,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_recipe_rating_recipe FOREIGN KEY (recipe_id) REFERENCES t_recipe(recipe_id),
  INDEX idx_rating_avg(rating_avg),
  INDEX idx_rating_count(rating_count)
) COMMENT='recipe rating summary';

CREATE TABLE IF NOT EXISTS t_recipe_rating_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recipe_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score TINYINT NOT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_rating_log_recipe FOREIGN KEY (recipe_id) REFERENCES t_recipe(recipe_id),
  CONSTRAINT fk_rating_log_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  UNIQUE KEY uk_recipe_user(recipe_id, user_id),
  INDEX idx_rating_log_recipe(recipe_id),
  INDEX idx_rating_log_user(user_id)
) COMMENT='recipe rating log';

CREATE TABLE IF NOT EXISTS t_diet_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  recipe_id BIGINT NOT NULL,
  food_name VARCHAR(100) NOT NULL,
  meal_type TINYINT NOT NULL DEFAULT 0,
  dining_time DATETIME NOT NULL,
  intake_amount DECIMAL(10,2) NOT NULL,
  calories_per_100g DECIMAL(10,2) DEFAULT NULL,
  estimated_calories DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  remark VARCHAR(500) DEFAULT NULL,
  record_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-normal 1-deleted',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_diet_record_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_diet_record_recipe FOREIGN KEY (recipe_id) REFERENCES t_recipe(recipe_id),
  INDEX idx_diet_record_user(user_id),
  INDEX idx_diet_record_recipe(recipe_id),
  INDEX idx_diet_record_dining_time(dining_time),
  INDEX idx_diet_record_meal_type(meal_type),
  INDEX idx_diet_record_status(record_status)
) COMMENT='diet record';

CREATE TABLE IF NOT EXISTS t_meal_item (
  item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  meal_id BIGINT NOT NULL,
  food_id BIGINT NULL,
  food_name VARCHAR(100) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  unit VARCHAR(20) NOT NULL DEFAULT 'g',
  kcal DECIMAL(10,2) NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_meal_item_meal_id (meal_id),
  KEY idx_meal_item_food_name (food_name),
  CONSTRAINT fk_meal_item_meal FOREIGN KEY (meal_id) REFERENCES t_diet_record(record_id) ON DELETE CASCADE
) COMMENT='meal item detail';

DROP VIEW IF EXISTS t_meal_record;

CREATE VIEW t_meal_record AS
SELECT
  record_id AS meal_id,
  user_id,
  dining_time AS meal_time,
  food_name,
  meal_type,
  remark,
  record_status,
  created_time,
  last_change_time
FROM t_diet_record;

CREATE TABLE IF NOT EXISTS t_exercise_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sport_id BIGINT NULL,
  sport_name VARCHAR(64) NULL,
  record_time DATETIME NOT NULL,
  duration_min INT NOT NULL,
  calories_kcal DECIMAL(10,2) NULL,
  note VARCHAR(255),
  data_source VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT 'manual/device/3rd/import',
  external_id VARCHAR(128) NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exercise_source_ext(data_source, external_id),
  INDEX idx_exercise_user_time(user_id, record_time),
  CONSTRAINT fk_exercise_user FOREIGN KEY (user_id) REFERENCES t_user(user_id)
) COMMENT='exercise record';

CREATE TABLE IF NOT EXISTS t_data_source (
  source_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  source_name VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL COMMENT 'file/manual/device/platform',
  source_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-active 1-paused 2-error',
  description VARCHAR(255),
  last_sync_time DATETIME,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_data_source_user_name (user_id, source_name),
  KEY idx_data_source_user_status (user_id, source_status),
  CONSTRAINT fk_data_source_user FOREIGN KEY (user_id) REFERENCES t_user(user_id)
) COMMENT='user data source';

CREATE TABLE IF NOT EXISTS t_sync_task (
  task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  task_type VARCHAR(32) NOT NULL COMMENT 'import/sync',
  task_status TINYINT NOT NULL DEFAULT 3 COMMENT '0-success 1-partial 2-failed 3-running',
  file_name VARCHAR(255),
  metric_category VARCHAR(32) NOT NULL DEFAULT 'health_metric',
  total_count INT NOT NULL DEFAULT 0,
  insert_count INT NOT NULL DEFAULT 0,
  update_count INT NOT NULL DEFAULT 0,
  fail_count INT NOT NULL DEFAULT 0,
  summary_message VARCHAR(500),
  started_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_time DATETIME NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sync_task_user_time (user_id, started_time),
  KEY idx_sync_task_source_time (source_id, started_time),
  CONSTRAINT fk_sync_task_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_sync_task_source FOREIGN KEY (source_id) REFERENCES t_data_source(source_id)
) COMMENT='sync task log';

CREATE TABLE IF NOT EXISTS health_metric (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  measure_date DATE NOT NULL,
  steps INT NULL,
  resting_heart_rate INT NULL,
  sleep_hours DECIMAL(4,2) NULL,
  systolic INT NULL,
  diastolic INT NULL,
  stress_level INT NULL,
  source_id BIGINT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
  source_name VARCHAR(64) NULL,
  sync_task_id BIGINT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_metric_user_date (user_id, measure_date),
  KEY idx_metric_date (measure_date),
  KEY idx_metric_source (source_id),
  KEY idx_metric_sync_task (sync_task_id),
  CONSTRAINT fk_metric_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_metric_source FOREIGN KEY (source_id) REFERENCES t_data_source(source_id) ON DELETE SET NULL,
  CONSTRAINT fk_metric_sync_task FOREIGN KEY (sync_task_id) REFERENCES t_sync_task(task_id) ON DELETE SET NULL
) COMMENT='daily health metric';

CREATE TABLE IF NOT EXISTS sport_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  cover_url VARCHAR(512) NULL,
  summary VARCHAR(255) NULL,
  description TEXT NULL,
  recommend_duration_min INT NOT NULL DEFAULT 60,
  calories_per_hour INT NOT NULL DEFAULT 0,
  recommend_frequency_per_week TINYINT NOT NULL DEFAULT 3,
  level ENUM('beginner', 'intermediate', 'advanced', 'all') NOT NULL DEFAULT 'all',
  status ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'published',
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  sort_weight INT NOT NULL DEFAULT 0,
  name_unique_active VARCHAR(64)
    GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN name ELSE NULL END) STORED,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_course_name_active (name_unique_active),
  KEY idx_course_search (name),
  KEY idx_course_status (status, is_deleted),
  KEY idx_course_sort (sort_weight, updated_at)
) COMMENT='sport course';

CREATE TABLE IF NOT EXISTS sport_course_rating (
  course_id BIGINT PRIMARY KEY,
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  rating_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_rating_course FOREIGN KEY (course_id) REFERENCES sport_course(id) ON DELETE CASCADE,
  KEY idx_rating_avg (rating_avg),
  KEY idx_rating_count (rating_count)
) COMMENT='sport course rating summary';

CREATE TABLE IF NOT EXISTS sport_audience (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_audience_name (name)
) COMMENT='sport audience dictionary';

CREATE TABLE IF NOT EXISTS sport_course_audience (
  course_id BIGINT NOT NULL,
  audience_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, audience_id),
  CONSTRAINT fk_ca_course FOREIGN KEY (course_id) REFERENCES sport_course(id) ON DELETE CASCADE,
  CONSTRAINT fk_ca_audience FOREIGN KEY (audience_id) REFERENCES sport_audience(id) ON DELETE RESTRICT,
  KEY idx_ca_audience (audience_id)
) COMMENT='course audience relation';

CREATE TABLE IF NOT EXISTS sport_equipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_equipment_name (name)
) COMMENT='sport equipment dictionary';

CREATE TABLE IF NOT EXISTS sport_course_equipment (
  course_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, equipment_id),
  CONSTRAINT fk_ce_course FOREIGN KEY (course_id) REFERENCES sport_course(id) ON DELETE CASCADE,
  CONSTRAINT fk_ce_equipment FOREIGN KEY (equipment_id) REFERENCES sport_equipment(id) ON DELETE RESTRICT,
  KEY idx_ce_equipment (equipment_id)
) COMMENT='course equipment relation';

CREATE TABLE IF NOT EXISTS sport_benefit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_benefit_name (name)
) COMMENT='sport benefit dictionary';

CREATE TABLE IF NOT EXISTS sport_course_benefit (
  course_id BIGINT NOT NULL,
  benefit_id BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (course_id, benefit_id),
  CONSTRAINT fk_cb_course FOREIGN KEY (course_id) REFERENCES sport_course(id) ON DELETE CASCADE,
  CONSTRAINT fk_cb_benefit FOREIGN KEY (benefit_id) REFERENCES sport_benefit(id) ON DELETE RESTRICT,
  KEY idx_cb_benefit (benefit_id),
  KEY idx_cb_sort (course_id, sort_order)
) COMMENT='course benefit relation';

CREATE TABLE IF NOT EXISTS sport_course_rating_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score TINYINT NOT NULL,
  comment VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rl_course FOREIGN KEY (course_id) REFERENCES sport_course(id) ON DELETE CASCADE,
  CONSTRAINT fk_rl_user FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
  UNIQUE KEY uk_user_course (course_id, user_id),
  KEY idx_rl_course_time (course_id, created_at),
  KEY idx_rl_user_time (user_id, created_at),
  CHECK (score BETWEEN 1 AND 5)
) COMMENT='sport course rating log';

INSERT INTO t_health_goal
  (goal_code, goal_name, goal_description, metric_type, unit, default_target_min, default_target_max, default_target_text, sort_no, goal_status, created_by)
VALUES
  ('BMI', '健康 BMI', '保持健康的 BMI 范围。', 1, 'kg/m2', 18.50, 24.90, NULL, 1, 0, NULL),
  ('RESTING_HEART_RATE', '静息心率', '持续跟踪静息心率变化。', 1, 'bpm', 60.00, 100.00, NULL, 2, 0, NULL)
ON DUPLICATE KEY UPDATE
  goal_name = VALUES(goal_name),
  goal_description = VALUES(goal_description),
  metric_type = VALUES(metric_type),
  unit = VALUES(unit),
  default_target_min = VALUES(default_target_min),
  default_target_max = VALUES(default_target_max),
  default_target_text = VALUES(default_target_text),
  sort_no = VALUES(sort_no),
  goal_status = VALUES(goal_status);

INSERT IGNORE INTO sport_audience(name) VALUES
  ('全部人群'),
  ('青少年'),
  ('成年人'),
  ('老年人');

INSERT IGNORE INTO sport_equipment(name) VALUES
  ('无需器械'),
  ('跳绳'),
  ('哑铃'),
  ('瑜伽垫');

INSERT IGNORE INTO sport_benefit(name) VALUES
  ('燃脂'),
  ('增肌'),
  ('缓解压力'),
  ('提升柔韧性');
