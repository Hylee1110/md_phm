CREATE TABLE IF NOT EXISTS t_recipe (
  recipe_id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'recipe id',
  food_name          VARCHAR(100) NOT NULL COMMENT 'food name',
  meal_type          TINYINT NOT NULL DEFAULT 0 COMMENT 'meal type 0-unspecified 1-breakfast 2-lunch 3-dinner 4-snack',
  portion            DECIMAL(10,2) DEFAULT NULL COMMENT 'reference portion',
  unit               VARCHAR(20) NOT NULL DEFAULT 'g' COMMENT 'reference unit',
  calories           INT DEFAULT NULL COMMENT 'kcal per 100g',
  image_url          VARCHAR(512) DEFAULT NULL COMMENT 'recipe image url',
  description        TEXT COMMENT 'recipe description',
  created_by         BIGINT NOT NULL COMMENT 'creator user id',
  is_admin_recommend TINYINT NOT NULL DEFAULT 0 COMMENT 'admin recommendation flag 0-no 1-yes',
  recipe_status      TINYINT NOT NULL DEFAULT 0 COMMENT 'status 0-normal 1-deleted 2-offline',
  created_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  last_change_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  CONSTRAINT fk_recipe_user FOREIGN KEY (created_by) REFERENCES t_user(user_id),
  INDEX idx_recipe_creator(created_by),
  INDEX idx_recipe_meal_type(meal_type),
  INDEX idx_recipe_status(recipe_status),
  INDEX idx_recipe_admin_recommend(is_admin_recommend)
) COMMENT='recipe table';

CREATE TABLE IF NOT EXISTS t_recipe_rating (
  recipe_id          BIGINT PRIMARY KEY COMMENT 'recipe id',
  rating_avg         DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT 'average rating',
  rating_count       INT NOT NULL DEFAULT 0 COMMENT 'rating count',
  created_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  last_change_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  CONSTRAINT fk_recipe_rating_recipe
    FOREIGN KEY (recipe_id) REFERENCES t_recipe(recipe_id),
  INDEX idx_rating_avg(rating_avg),
  INDEX idx_rating_count(rating_count)
) COMMENT='recipe rating summary table';

CREATE TABLE IF NOT EXISTS t_recipe_rating_log (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'log id',
  recipe_id          BIGINT NOT NULL COMMENT 'recipe id',
  user_id            BIGINT NOT NULL COMMENT 'user id',
  score              TINYINT NOT NULL COMMENT 'score 1-5',
  created_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  last_change_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  CONSTRAINT fk_rating_log_recipe
    FOREIGN KEY (recipe_id) REFERENCES t_recipe(recipe_id),
  CONSTRAINT fk_rating_log_user
    FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  UNIQUE KEY uk_recipe_user(recipe_id, user_id),
  INDEX idx_rating_log_recipe(recipe_id),
  INDEX idx_rating_log_user(user_id)
) COMMENT='recipe rating log table';
