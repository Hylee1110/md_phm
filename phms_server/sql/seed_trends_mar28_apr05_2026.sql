-- 趋势演示数据：健康指标（每日一条）、饮食记录、运动记录
-- 日期固定为 2026-03-28 ～ 2026-04-05（共 9 天）。
-- 目标用户：修改下方 @seed_account 为要灌入的账号（默认 lee123）。
-- 依赖：t_user 中存在该账号；若缺少演示食谱/课程会自动插入（created_by 优先用 admin，否则用目标用户）。
--       健康指标里的 source_id 取自该用户下「演示手动日志」「演示手环 S1」，若无则为 NULL。
-- 用法：须整脚本一次性执行（会话变量跨语句）；mysql -u... -p md_phms < seed_trends_mar28_apr05_2026.sql

SET NAMES utf8mb4;
USE md_phms;

SET @seed_account := 'lee123';
SET @seed_user_id := (SELECT user_id FROM t_user WHERE account = @seed_account LIMIT 1);
SET @admin_id := (SELECT user_id FROM t_user WHERE account = 'admin' LIMIT 1);
SET @recipe_creator_id := COALESCE(@admin_id, @seed_user_id);

-- 若无演示食谱则补齐（否则 t_diet_record.recipe_id NOT NULL 会报错）
INSERT INTO t_recipe (
  food_name, meal_type, portion, unit, calories, image_url, description,
  created_by, is_admin_recommend, recipe_status
)
SELECT '演示燕麦能量碗', 1, 280.00, 'g', 95, 'https://example.com/demo-oatmeal.jpg', '早餐示例：燕麦、香蕉与牛奶组合。', @recipe_creator_id, 1, 0
FROM DUAL
WHERE @recipe_creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM t_recipe r WHERE r.food_name = '演示燕麦能量碗' LIMIT 1);

INSERT INTO t_recipe (
  food_name, meal_type, portion, unit, calories, image_url, description,
  created_by, is_admin_recommend, recipe_status
)
SELECT '演示鸡胸肉沙拉', 2, 320.00, 'g', 145, 'https://example.com/demo-chicken-salad.jpg', '午餐示例：高蛋白低负担轻食。', @recipe_creator_id, 1, 0
FROM DUAL
WHERE @recipe_creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM t_recipe r WHERE r.food_name = '演示鸡胸肉沙拉' LIMIT 1);

INSERT INTO t_recipe (
  food_name, meal_type, portion, unit, calories, image_url, description,
  created_by, is_admin_recommend, recipe_status
)
SELECT '演示三文鱼套餐', 3, 350.00, 'g', 180, 'https://example.com/demo-salmon-plate.jpg', '晚餐示例：鱼类、杂粮和蔬菜搭配。', @recipe_creator_id, 1, 0
FROM DUAL
WHERE @recipe_creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM t_recipe r WHERE r.food_name = '演示三文鱼套餐' LIMIT 1);

INSERT INTO t_recipe (
  food_name, meal_type, portion, unit, calories, image_url, description,
  created_by, is_admin_recommend, recipe_status
)
SELECT '演示酸奶水果杯', 4, 180.00, 'g', 82, 'https://example.com/demo-yogurt-cup.jpg', '加餐示例：酸奶搭配水果。', @recipe_creator_id, 0, 0
FROM DUAL
WHERE @recipe_creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM t_recipe r WHERE r.food_name = '演示酸奶水果杯' LIMIT 1);

-- 若无演示课程则补齐（运动记录需要 sport_id）
INSERT INTO sport_course (
  name, cover_url, summary, description,
  recommend_duration_min, calories_per_hour, recommend_frequency_per_week,
  level, status, is_deleted, sort_weight
)
SELECT '演示快走训练', 'https://example.com/demo-walk.jpg', '适合日常激活身体的低冲击有氧训练。', '面向初学者的快走课程，帮助提升耐力并逐步进入稳定燃脂节奏。', 45, 320, 5, 'beginner', 'published', 0, 100
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sport_course c WHERE c.name = '演示快走训练' AND c.is_deleted = 0 LIMIT 1);

INSERT INTO sport_course (
  name, cover_url, summary, description,
  recommend_duration_min, calories_per_hour, recommend_frequency_per_week,
  level, status, is_deleted, sort_weight
)
SELECT '演示舒缓瑜伽', 'https://example.com/demo-yoga.jpg', '用于拉伸放松和舒缓压力的瑜伽练习。', '通过呼吸、拉伸和轻度核心动作，帮助改善柔韧性和恢复状态。', 35, 220, 4, 'all', 'published', 0, 90
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sport_course c WHERE c.name = '演示舒缓瑜伽' AND c.is_deleted = 0 LIMIT 1);

INSERT INTO sport_course (
  name, cover_url, summary, description,
  recommend_duration_min, calories_per_hour, recommend_frequency_per_week,
  level, status, is_deleted, sort_weight
)
SELECT '演示 HIIT 入门', 'https://example.com/demo-hiit.jpg', '适合忙碌人群的短时高效训练。', '通过简单的间歇组合提升心肺能力，适合已有基础的用户。', 20, 520, 3, 'intermediate', 'published', 0, 80
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sport_course c WHERE c.name = '演示 HIIT 入门' AND c.is_deleted = 0 LIMIT 1);

SET @recipe_oatmeal_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示燕麦能量碗' LIMIT 1);
SET @recipe_salad_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示鸡胸肉沙拉' LIMIT 1);
SET @recipe_salmon_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示三文鱼套餐' LIMIT 1);
SET @recipe_yogurt_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示酸奶水果杯' LIMIT 1);

SET @course_walk_id := (SELECT id FROM sport_course WHERE name = '演示快走训练' AND is_deleted = 0 ORDER BY id DESC LIMIT 1);
SET @course_yoga_id := (SELECT id FROM sport_course WHERE name = '演示舒缓瑜伽' AND is_deleted = 0 ORDER BY id DESC LIMIT 1);
SET @course_hiit_id := (SELECT id FROM sport_course WHERE name = '演示 HIIT 入门' AND is_deleted = 0 ORDER BY id DESC LIMIT 1);

SET @source_manual_id := (
  SELECT source_id FROM t_data_source
  WHERE user_id = @seed_user_id AND source_name = '演示手动日志'
  LIMIT 1
);
SET @source_band_id := (
  SELECT source_id FROM t_data_source
  WHERE user_id = @seed_user_id AND source_name = '演示手环 S1'
  LIMIT 1
);

-- 清理同批次饮食（子项随外键级联删除）
DELETE FROM t_diet_record
WHERE user_id = @seed_user_id
  AND remark LIKE '趋势演示%';

-- external_id 带 user_id，避免与全库 uk_exercise_source_ext(data_source, external_id) 下其他用户冲突
DELETE FROM t_exercise_record
WHERE user_id = @seed_user_id
  AND external_id LIKE CONCAT('TREND_DEMO_', @seed_user_id, '_%');

-- 健康指标：步数↑、静息心率↓、睡眠↑、血压略降、压力略降
INSERT INTO health_metric (
  user_id,
  measure_date,
  steps,
  resting_heart_rate,
  sleep_hours,
  systolic,
  diastolic,
  stress_level,
  source_id,
  source_type,
  source_name,
  sync_task_id
)
VALUES
  (@seed_user_id, '2026-03-28', 5200, 78, 6.35, 128, 86, 6, @source_manual_id, 'manual', '趋势演示', NULL),
  (@seed_user_id, '2026-03-29', 5750, 77, 6.42, 127, 85, 6, @source_manual_id, 'manual', '趋势演示', NULL),
  (@seed_user_id, '2026-03-30', 6100, 76, 6.50, 126, 85, 5, @source_band_id, 'device', '趋势演示', NULL),
  (@seed_user_id, '2026-03-31', 6580, 75, 6.58, 125, 84, 5, @source_band_id, 'device', '趋势演示', NULL),
  (@seed_user_id, '2026-04-01', 7020, 74, 6.68, 124, 83, 5, @source_band_id, 'device', '趋势演示', NULL),
  (@seed_user_id, '2026-04-02', 7480, 73, 6.78, 123, 82, 4, @source_band_id, 'device', '趋势演示', NULL),
  (@seed_user_id, '2026-04-03', 7950, 72, 6.88, 122, 81, 4, @source_band_id, 'device', '趋势演示', NULL),
  (@seed_user_id, '2026-04-04', 8320, 71, 6.95, 121, 80, 3, @source_band_id, 'device', '趋势演示', NULL),
  (@seed_user_id, '2026-04-05', 8800, 70, 7.05, 120, 79, 3, @source_band_id, 'device', '趋势演示', NULL)
ON DUPLICATE KEY UPDATE
  steps = VALUES(steps),
  resting_heart_rate = VALUES(resting_heart_rate),
  sleep_hours = VALUES(sleep_hours),
  systolic = VALUES(systolic),
  diastolic = VALUES(diastolic),
  stress_level = VALUES(stress_level),
  source_id = VALUES(source_id),
  source_type = VALUES(source_type),
  source_name = VALUES(source_name);

-- 饮食：每日早+午+晚，热量逐日略升，便于「每日卡路里摄入」折线
INSERT INTO t_diet_record (
  user_id, recipe_id, food_name, meal_type, dining_time,
  intake_amount, calories_per_100g, estimated_calories, remark, record_status
) VALUES
  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-03-28 07:30:00', 260, 95, 247.00, '趋势演示0328早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-03-28 12:15:00', 300, 145, 435.00, '趋势演示0328午', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-03-28 18:30:00', 320, 180, 576.00, '趋势演示0328晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-03-29 07:28:00', 270, 95, 256.50, '趋势演示0329早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-03-29 12:10:00', 310, 145, 449.50, '趋势演示0329午', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-03-29 18:35:00', 330, 180, 594.00, '趋势演示0329晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-03-30 07:32:00', 275, 95, 261.25, '趋势演示0330早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-03-30 12:18:00', 315, 145, 456.75, '趋势演示0330午', 0),
  (@seed_user_id, @recipe_yogurt_id, '演示酸奶水果杯', 4, '2026-03-30 15:40:00', 160, 82, 131.20, '趋势演示0330加', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-03-30 18:25:00', 340, 180, 612.00, '趋势演示0330晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-03-31 07:25:00', 280, 95, 266.00, '趋势演示0331早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-03-31 12:20:00', 320, 145, 464.00, '趋势演示0331午', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-03-31 18:40:00', 350, 180, 630.00, '趋势演示0331晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-04-01 07:35:00', 285, 95, 270.75, '趋势演示0401早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-04-01 12:12:00', 325, 145, 471.25, '趋势演示0401午', 0),
  (@seed_user_id, @recipe_yogurt_id, '演示酸奶水果杯', 4, '2026-04-01 15:25:00', 170, 82, 139.40, '趋势演示0401加', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-04-01 18:45:00', 360, 180, 648.00, '趋势演示0401晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-04-02 07:22:00', 290, 95, 275.50, '趋势演示0402早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-04-02 12:22:00', 330, 145, 478.50, '趋势演示0402午', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-04-02 18:32:00', 365, 180, 657.00, '趋势演示0402晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-04-03 07:30:00', 295, 95, 280.25, '趋势演示0403早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-04-03 12:18:00', 335, 145, 485.75, '趋势演示0403午', 0),
  (@seed_user_id, @recipe_yogurt_id, '演示酸奶水果杯', 4, '2026-04-03 15:50:00', 185, 82, 151.70, '趋势演示0403加', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-04-03 18:28:00', 370, 180, 666.00, '趋势演示0403晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-04-04 07:27:00', 300, 95, 285.00, '趋势演示0404早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-04-04 12:25:00', 340, 145, 493.00, '趋势演示0404午', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-04-04 18:38:00', 375, 180, 675.00, '趋势演示0404晚', 0),

  (@seed_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, '2026-04-05 07:33:00', 305, 95, 289.75, '趋势演示0405早', 0),
  (@seed_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, '2026-04-05 12:15:00', 345, 145, 500.25, '趋势演示0405午', 0),
  (@seed_user_id, @recipe_yogurt_id, '演示酸奶水果杯', 4, '2026-04-05 15:30:00', 190, 82, 155.80, '趋势演示0405加', 0),
  (@seed_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, '2026-04-05 18:50:00', 380, 180, 684.00, '趋势演示0405晚', 0);

-- 运动：每日一次，消耗逐日略升
INSERT INTO t_exercise_record (
  user_id, sport_id, sport_name, record_time, duration_min, calories_kcal, note, data_source, external_id
) VALUES
  (@seed_user_id, @course_walk_id, '演示快走训练', '2026-03-28 06:45:00', 32, 168.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260328')),
  (@seed_user_id, @course_walk_id, '演示快走训练', '2026-03-29 06:50:00', 35, 182.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260329')),
  (@seed_user_id, @course_yoga_id, '演示舒缓瑜伽', '2026-03-30 19:10:00', 38, 198.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260330')),
  (@seed_user_id, @course_walk_id, '演示快走训练', '2026-03-31 06:40:00', 40, 215.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260331')),
  (@seed_user_id, @course_hiit_id, '演示 HIIT 入门', '2026-04-01 18:20:00', 22, 228.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260401')),
  (@seed_user_id, @course_walk_id, '演示快走训练', '2026-04-02 06:55:00', 44, 248.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260402')),
  (@seed_user_id, @course_yoga_id, '演示舒缓瑜伽', '2026-04-03 20:00:00', 42, 262.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260403')),
  (@seed_user_id, @course_hiit_id, '演示 HIIT 入门', '2026-04-04 18:35:00', 24, 285.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260404')),
  (@seed_user_id, @course_walk_id, '演示快走训练', '2026-04-05 07:00:00', 48, 308.00, '趋势演示', 'manual', CONCAT('TREND_DEMO_', @seed_user_id, '_20260405'));
