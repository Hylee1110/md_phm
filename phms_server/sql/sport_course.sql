CREATE TABLE IF NOT EXISTS sport_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL COMMENT 'sport name',
  cover_url VARCHAR(512) NULL COMMENT 'cover image url',
  summary VARCHAR(255) NULL COMMENT 'short summary',
  description TEXT NULL COMMENT 'detailed description',

  recommend_duration_min INT NOT NULL DEFAULT 60 COMMENT 'recommended duration in minutes',
  calories_per_hour INT NOT NULL DEFAULT 0 COMMENT 'calories burned per hour',
  recommend_frequency_per_week TINYINT NOT NULL DEFAULT 3 COMMENT 'recommended times per week',

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

  CONSTRAINT fk_rating_course
    FOREIGN KEY (course_id) REFERENCES sport_course(id)
    ON DELETE CASCADE,

  KEY idx_rating_avg (rating_avg),
  KEY idx_rating_count (rating_count)
) COMMENT='rating summary';

CREATE TABLE IF NOT EXISTS sport_audience (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_audience_name (name)
) COMMENT='audience dictionary';

CREATE TABLE IF NOT EXISTS sport_course_audience (
  course_id BIGINT NOT NULL,
  audience_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, audience_id),

  CONSTRAINT fk_ca_course
    FOREIGN KEY (course_id) REFERENCES sport_course(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_ca_audience
    FOREIGN KEY (audience_id) REFERENCES sport_audience(id)
    ON DELETE RESTRICT,

  KEY idx_ca_audience (audience_id)
) COMMENT='course to audience';

CREATE TABLE IF NOT EXISTS sport_equipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_equipment_name (name)
) COMMENT='equipment dictionary';

CREATE TABLE IF NOT EXISTS sport_course_equipment (
  course_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, equipment_id),

  CONSTRAINT fk_ce_course
    FOREIGN KEY (course_id) REFERENCES sport_course(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_ce_equipment
    FOREIGN KEY (equipment_id) REFERENCES sport_equipment(id)
    ON DELETE RESTRICT,

  KEY idx_ce_equipment (equipment_id)
) COMMENT='course to equipment';

CREATE TABLE IF NOT EXISTS sport_benefit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_benefit_name (name)
) COMMENT='benefit dictionary';

CREATE TABLE IF NOT EXISTS sport_course_benefit (
  course_id BIGINT NOT NULL,
  benefit_id BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (course_id, benefit_id),

  CONSTRAINT fk_cb_course
    FOREIGN KEY (course_id) REFERENCES sport_course(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_cb_benefit
    FOREIGN KEY (benefit_id) REFERENCES sport_benefit(id)
    ON DELETE RESTRICT,

  KEY idx_cb_benefit (benefit_id),
  KEY idx_cb_sort (course_id, sort_order)
) COMMENT='course to benefit';

CREATE TABLE IF NOT EXISTS sport_course_rating_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score TINYINT NOT NULL COMMENT '1-5',
  comment VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_rl_course
    FOREIGN KEY (course_id) REFERENCES sport_course(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_rl_user
    FOREIGN KEY (user_id) REFERENCES t_user(user_id)
    ON DELETE CASCADE,

  UNIQUE KEY uk_user_course (course_id, user_id),
  KEY idx_rl_course_time (course_id, created_at),
  KEY idx_rl_user_time (user_id, created_at),

  CHECK (score BETWEEN 1 AND 5)
) COMMENT='rating log';

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
