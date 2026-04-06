CREATE TABLE IF NOT EXISTS t_diet_record (
  record_id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '饮食记录ID',
  user_id                BIGINT NOT NULL COMMENT '用户ID，关联 t_user.user_id',
  recipe_id              BIGINT NOT NULL COMMENT '食谱ID，关联 t_recipe.recipe_id',

  food_name              VARCHAR(100) NOT NULL COMMENT '食物名称（冗余保存）',
  meal_type              TINYINT NOT NULL DEFAULT 0 COMMENT '餐次 0-未指定 1-早餐 2-午餐 3-晚餐 4-加餐',
  dining_time            DATETIME NOT NULL COMMENT '用餐时间',

  intake_amount          DECIMAL(10,2) NOT NULL COMMENT '摄入份量(g)',
  calories_per_100g      DECIMAL(10,2) DEFAULT NULL COMMENT '每100g热量(kcal)',
  estimated_calories     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预计热量(kcal)',

  remark                 VARCHAR(500) DEFAULT NULL COMMENT '用餐备注',
  record_status          TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-正常 1-删除',

  created_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  last_change_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  CONSTRAINT fk_diet_record_user
    FOREIGN KEY (user_id) REFERENCES t_user(user_id),

  CONSTRAINT fk_diet_record_recipe
    FOREIGN KEY (recipe_id) REFERENCES t_recipe(recipe_id),

  INDEX idx_diet_record_user(user_id),
  INDEX idx_diet_record_recipe(recipe_id),
  INDEX idx_diet_record_dining_time(dining_time),
  INDEX idx_diet_record_meal_type(meal_type),
  INDEX idx_diet_record_status(record_status)
) COMMENT='饮食记录表';
