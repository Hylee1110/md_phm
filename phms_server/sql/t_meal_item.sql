CREATE TABLE IF NOT EXISTS t_meal_item (
  item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  meal_id BIGINT NOT NULL COMMENT 'references t_diet_record.record_id',
  food_id BIGINT NULL COMMENT 'optional external food id',
  food_name VARCHAR(100) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  unit VARCHAR(20) NOT NULL DEFAULT 'g',
  kcal DECIMAL(10,2) NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_meal_item_meal_id (meal_id),
  KEY idx_meal_item_food_name (food_name),
  CONSTRAINT fk_meal_item_meal
    FOREIGN KEY (meal_id) REFERENCES t_diet_record(record_id)
    ON DELETE CASCADE
) COMMENT='meal item detail table';

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
