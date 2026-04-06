SET NAMES utf8mb4;

USE md_phms;

-- 演示账号
-- 所有演示账号密码均为 123456
INSERT INTO t_user (
  account,
  password_hash,
  nickname,
  realname,
  gender,
  age,
  account_level,
  account_status
)
VALUES
  ('admin', MD5('123456'), '管理员', '系统管理员', 0, NULL, 1, 0),
  ('demo_user', MD5('123456'), '小米', '李小米', 2, 27, 0, 0),
  ('demo_friend', MD5('123456'), '阿乐', '张乐', 1, 29, 0, 0),
  ('readonly_user', MD5('123456'), '只读用户', '赵宁', 2, 25, 0, 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  realname = VALUES(realname),
  gender = VALUES(gender),
  age = VALUES(age),
  account_level = VALUES(account_level),
  account_status = VALUES(account_status);

SET @admin_id := (SELECT user_id FROM t_user WHERE account = 'admin' LIMIT 1);
SET @demo_user_id := (SELECT user_id FROM t_user WHERE account = 'demo_user' LIMIT 1);
SET @demo_friend_id := (SELECT user_id FROM t_user WHERE account = 'demo_friend' LIMIT 1);
SET @readonly_user_id := (SELECT user_id FROM t_user WHERE account = 'readonly_user' LIMIT 1);

-- 确保基础健康目标为中文展示
INSERT INTO t_health_goal
  (goal_code, goal_name, goal_description, metric_type, unit, default_target_min, default_target_max, default_target_text, sort_no, goal_status, created_by)
VALUES
  ('BMI', '健康 BMI', '保持健康的 BMI 范围。', 1, 'kg/m2', 18.50, 24.90, NULL, 1, 0, @admin_id),
  ('RESTING_HEART_RATE', '静息心率', '持续跟踪静息心率变化。', 1, 'bpm', 60.00, 100.00, NULL, 2, 0, @admin_id)
ON DUPLICATE KEY UPDATE
  goal_name = VALUES(goal_name),
  goal_description = VALUES(goal_description),
  metric_type = VALUES(metric_type),
  unit = VALUES(unit),
  default_target_min = VALUES(default_target_min),
  default_target_max = VALUES(default_target_max),
  default_target_text = VALUES(default_target_text),
  sort_no = VALUES(sort_no),
  goal_status = VALUES(goal_status),
  created_by = VALUES(created_by);

SET @goal_bmi_id := (SELECT goal_id FROM t_health_goal WHERE goal_code = 'BMI' LIMIT 1);
SET @goal_rhr_id := (SELECT goal_id FROM t_health_goal WHERE goal_code = 'RESTING_HEART_RATE' LIMIT 1);

INSERT IGNORE INTO sport_audience(name) VALUES ('全部人群'), ('青少年'), ('成年人'), ('老年人');
INSERT IGNORE INTO sport_equipment(name) VALUES ('无需器械'), ('跳绳'), ('哑铃'), ('瑜伽垫');
INSERT IGNORE INTO sport_benefit(name) VALUES ('燃脂'), ('增肌'), ('缓解压力'), ('提升柔韧性');

INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT sca.course_id, target.id
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
JOIN sport_audience target ON target.name = '全部人群'
WHERE source.name = 'all';
DELETE sca
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
WHERE source.name = 'all';

INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT sca.course_id, target.id
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
JOIN sport_audience target ON target.name = '全部人群'
WHERE source.name = '所有人';
DELETE sca
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
WHERE source.name = '所有人';

INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT sca.course_id, target.id
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
JOIN sport_audience target ON target.name = '青少年'
WHERE source.name = 'teenagers';
DELETE sca
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
WHERE source.name = 'teenagers';

INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT sca.course_id, target.id
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
JOIN sport_audience target ON target.name = '成年人'
WHERE source.name = 'adults';
DELETE sca
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
WHERE source.name = 'adults';

INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT sca.course_id, target.id
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
JOIN sport_audience target ON target.name = '老年人'
WHERE source.name = 'seniors';
DELETE sca
FROM sport_course_audience sca
JOIN sport_audience source ON source.id = sca.audience_id
WHERE source.name = 'seniors';

INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT sce.course_id, target.id
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
JOIN sport_equipment target ON target.name = '无需器械'
WHERE source.name = 'none';
DELETE sce
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
WHERE source.name = 'none';

INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT sce.course_id, target.id
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
JOIN sport_equipment target ON target.name = '无需器械'
WHERE source.name = '无';
DELETE sce
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
WHERE source.name = '无';

INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT sce.course_id, target.id
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
JOIN sport_equipment target ON target.name = '跳绳'
WHERE source.name = 'jump rope';
DELETE sce
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
WHERE source.name = 'jump rope';

INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT sce.course_id, target.id
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
JOIN sport_equipment target ON target.name = '哑铃'
WHERE source.name = 'dumbbell';
DELETE sce
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
WHERE source.name = 'dumbbell';

INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT sce.course_id, target.id
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
JOIN sport_equipment target ON target.name = '瑜伽垫'
WHERE source.name = 'yoga mat';
DELETE sce
FROM sport_course_equipment sce
JOIN sport_equipment source ON source.id = sce.equipment_id
WHERE source.name = 'yoga mat';

INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT scb.course_id, target.id, scb.sort_order
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
JOIN sport_benefit target ON target.name = '燃脂'
WHERE source.name = 'fat burn';
DELETE scb
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
WHERE source.name = 'fat burn';

INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT scb.course_id, target.id, scb.sort_order
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
JOIN sport_benefit target ON target.name = '增肌'
WHERE source.name = 'muscle gain';
DELETE scb
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
WHERE source.name = 'muscle gain';

INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT scb.course_id, target.id, scb.sort_order
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
JOIN sport_benefit target ON target.name = '缓解压力'
WHERE source.name = 'stress relief';
DELETE scb
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
WHERE source.name = 'stress relief';

INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT scb.course_id, target.id, scb.sort_order
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
JOIN sport_benefit target ON target.name = '缓解压力'
WHERE source.name = '减压';
DELETE scb
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
WHERE source.name = '减压';

INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT scb.course_id, target.id, scb.sort_order
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
JOIN sport_benefit target ON target.name = '提升柔韧性'
WHERE source.name = 'flexibility';
DELETE scb
FROM sport_course_benefit scb
JOIN sport_benefit source ON source.id = scb.benefit_id
WHERE source.name = 'flexibility';

DELETE FROM sport_audience WHERE name IN ('all', 'teenagers', 'adults', 'seniors', '所有人');
DELETE FROM sport_equipment WHERE name IN ('none', 'jump rope', 'dumbbell', 'yoga mat', '无');
DELETE FROM sport_benefit WHERE name IN ('fat burn', 'muscle gain', 'stress relief', 'flexibility', '减压');

-- 清理旧演示数据，支持重复导入
DELETE FROM health_metric
WHERE user_id = @demo_user_id
  AND measure_date >= DATE_SUB(CURDATE(), INTERVAL 20 DAY);

DELETE FROM t_sync_task
WHERE user_id = @demo_user_id
  AND file_name IN (
    'demo_device_sync_last7.json',
    'demo_clinic_import_weekly.csv',
    '演示设备同步_近7天.json',
    '演示体检导入_周报.csv'
  );

DELETE FROM t_data_source
WHERE user_id = @demo_user_id
  AND source_name IN (
    'Demo Manual Journal',
    'Demo FitBand S1',
    'Demo Clinic Upload',
    '演示手动日志',
    '演示手环 S1',
    '演示体检导入'
  );

DELETE FROM t_health_record
WHERE user_id = @demo_user_id
  AND (remark LIKE 'demo_seed:%' OR remark LIKE '演示种子:%');

DELETE FROM t_diet_record
WHERE user_id = @demo_user_id
  AND (remark LIKE 'demo_seed:%' OR remark LIKE '演示种子:%');

DELETE FROM t_exercise_record
WHERE user_id = @demo_user_id
  AND (external_id LIKE 'DEMO_%' OR note LIKE 'demo_seed:%' OR note LIKE '演示种子:%');

DELETE FROM t_recipe_rating_log
WHERE recipe_id IN (
  SELECT recipe_id
  FROM t_recipe
  WHERE food_name IN (
    'Demo Oatmeal Bowl',
    'Demo Chicken Salad',
    'Demo Salmon Plate',
    'Demo Yogurt Cup',
    '演示燕麦能量碗',
    '演示鸡胸肉沙拉',
    '演示三文鱼套餐',
    '演示酸奶水果杯'
  )
);

DELETE FROM t_recipe_rating
WHERE recipe_id IN (
  SELECT recipe_id
  FROM t_recipe
  WHERE food_name IN (
    'Demo Oatmeal Bowl',
    'Demo Chicken Salad',
    'Demo Salmon Plate',
    'Demo Yogurt Cup',
    '演示燕麦能量碗',
    '演示鸡胸肉沙拉',
    '演示三文鱼套餐',
    '演示酸奶水果杯'
  )
);

DELETE FROM t_recipe
WHERE food_name IN (
  'Demo Oatmeal Bowl',
  'Demo Chicken Salad',
  'Demo Salmon Plate',
  'Demo Yogurt Cup',
  '演示燕麦能量碗',
  '演示鸡胸肉沙拉',
  '演示三文鱼套餐',
  '演示酸奶水果杯'
);

DELETE FROM sport_course
WHERE name IN (
  'Demo Brisk Walking',
  'Demo Yoga Flow',
  'Demo HIIT Basics',
  '演示快走训练',
  '演示舒缓瑜伽',
  '演示 HIIT 入门'
);

-- 演示食谱
INSERT INTO t_recipe (
  food_name,
  meal_type,
  portion,
  unit,
  calories,
  image_url,
  description,
  created_by,
  is_admin_recommend,
  recipe_status
)
VALUES
  ('演示燕麦能量碗', 1, 280.00, 'g', 95, 'https://example.com/demo-oatmeal.jpg', '早餐示例：燕麦、香蕉与牛奶组合。', @admin_id, 1, 0),
  ('演示鸡胸肉沙拉', 2, 320.00, 'g', 145, 'https://example.com/demo-chicken-salad.jpg', '午餐示例：高蛋白低负担轻食。', @admin_id, 1, 0),
  ('演示三文鱼套餐', 3, 350.00, 'g', 180, 'https://example.com/demo-salmon-plate.jpg', '晚餐示例：鱼类、杂粮和蔬菜搭配。', @admin_id, 1, 0),
  ('演示酸奶水果杯', 4, 180.00, 'g', 82, 'https://example.com/demo-yogurt-cup.jpg', '加餐示例：酸奶搭配水果。', @admin_id, 0, 0);

SET @recipe_oatmeal_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示燕麦能量碗' LIMIT 1);
SET @recipe_salad_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示鸡胸肉沙拉' LIMIT 1);
SET @recipe_salmon_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示三文鱼套餐' LIMIT 1);
SET @recipe_yogurt_id := (SELECT recipe_id FROM t_recipe WHERE food_name = '演示酸奶水果杯' LIMIT 1);

INSERT INTO t_recipe_rating_log (recipe_id, user_id, score, created_time, last_change_time)
VALUES
  (@recipe_oatmeal_id, @demo_user_id, 5, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
  (@recipe_oatmeal_id, @demo_friend_id, 4, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
  (@recipe_salad_id, @demo_user_id, 5, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@recipe_salad_id, @demo_friend_id, 5, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@recipe_salmon_id, @demo_user_id, 4, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (@recipe_yogurt_id, @demo_friend_id, 4, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  created_time = VALUES(created_time),
  last_change_time = VALUES(last_change_time);

INSERT INTO t_recipe_rating (
  recipe_id,
  rating_avg,
  rating_count,
  created_time,
  last_change_time
)
SELECT
  recipe_id,
  ROUND(AVG(score), 2) AS rating_avg,
  COUNT(*) AS rating_count,
  NOW(),
  NOW()
FROM t_recipe_rating_log
WHERE recipe_id IN (@recipe_oatmeal_id, @recipe_salad_id, @recipe_salmon_id, @recipe_yogurt_id)
GROUP BY recipe_id
ON DUPLICATE KEY UPDATE
  rating_avg = VALUES(rating_avg),
  rating_count = VALUES(rating_count),
  last_change_time = VALUES(last_change_time);

-- 演示运动课程
INSERT INTO sport_course (
  name,
  cover_url,
  summary,
  description,
  recommend_duration_min,
  calories_per_hour,
  recommend_frequency_per_week,
  level,
  status,
  is_deleted,
  sort_weight
)
VALUES
  ('演示快走训练', 'https://example.com/demo-walk.jpg', '适合日常激活身体的低冲击有氧训练。', '面向初学者的快走课程，帮助提升耐力并逐步进入稳定燃脂节奏。', 45, 320, 5, 'beginner', 'published', 0, 100),
  ('演示舒缓瑜伽', 'https://example.com/demo-yoga.jpg', '用于拉伸放松和舒缓压力的瑜伽练习。', '通过呼吸、拉伸和轻度核心动作，帮助改善柔韧性和恢复状态。', 35, 220, 4, 'all', 'published', 0, 90),
  ('演示 HIIT 入门', 'https://example.com/demo-hiit.jpg', '适合忙碌人群的短时高效训练。', '通过简单的间歇组合提升心肺能力，适合已有基础的用户。', 20, 520, 3, 'intermediate', 'published', 0, 80);

SET @course_walk_id := (SELECT id FROM sport_course WHERE name = '演示快走训练' LIMIT 1);
SET @course_yoga_id := (SELECT id FROM sport_course WHERE name = '演示舒缓瑜伽' LIMIT 1);
SET @course_hiit_id := (SELECT id FROM sport_course WHERE name = '演示 HIIT 入门' LIMIT 1);

INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT @course_walk_id, id FROM sport_audience WHERE name IN ('全部人群', '成年人');
INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT @course_yoga_id, id FROM sport_audience WHERE name IN ('全部人群', '成年人', '老年人');
INSERT IGNORE INTO sport_course_audience (course_id, audience_id)
SELECT @course_hiit_id, id FROM sport_audience WHERE name IN ('成年人');

INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT @course_walk_id, id FROM sport_equipment WHERE name IN ('无需器械');
INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT @course_yoga_id, id FROM sport_equipment WHERE name IN ('瑜伽垫');
INSERT IGNORE INTO sport_course_equipment (course_id, equipment_id)
SELECT @course_hiit_id, id FROM sport_equipment WHERE name IN ('无需器械', '哑铃');

INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT @course_walk_id, id, 1 FROM sport_benefit WHERE name = '燃脂';
INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT @course_walk_id, id, 2 FROM sport_benefit WHERE name = '缓解压力';
INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT @course_yoga_id, id, 1 FROM sport_benefit WHERE name = '提升柔韧性';
INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT @course_yoga_id, id, 2 FROM sport_benefit WHERE name = '缓解压力';
INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT @course_hiit_id, id, 1 FROM sport_benefit WHERE name = '燃脂';
INSERT IGNORE INTO sport_course_benefit (course_id, benefit_id, sort_order)
SELECT @course_hiit_id, id, 2 FROM sport_benefit WHERE name = '增肌';

INSERT INTO sport_course_rating_log (
  course_id,
  user_id,
  score,
  comment,
  created_at
)
VALUES
  (@course_walk_id, @demo_user_id, 5, '非常适合日常坚持。', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (@course_walk_id, @demo_friend_id, 4, '节奏清晰，容易跟练。', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (@course_yoga_id, @demo_user_id, 5, '下班后练很放松。', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (@course_hiit_id, @demo_friend_id, 4, '时间短，效果不错。', DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  comment = VALUES(comment),
  created_at = VALUES(created_at);

INSERT INTO sport_course_rating (
  course_id,
  rating_avg,
  rating_count,
  updated_at
)
SELECT
  course_id,
  ROUND(AVG(score), 2) AS rating_avg,
  COUNT(*) AS rating_count,
  NOW()
FROM sport_course_rating_log
WHERE course_id IN (@course_walk_id, @course_yoga_id, @course_hiit_id)
GROUP BY course_id
ON DUPLICATE KEY UPDATE
  rating_avg = VALUES(rating_avg),
  rating_count = VALUES(rating_count),
  updated_at = VALUES(updated_at);

-- 演示健康目标与检查记录
INSERT INTO t_user_health_goal (
  user_id,
  goal_id,
  target_min,
  target_max,
  target_text,
  start_date,
  end_date,
  user_goal_status
)
VALUES
  (@demo_user_id, @goal_bmi_id, 20.50, 23.90, NULL, DATE_SUB(CURDATE(), INTERVAL 30 DAY), NULL, 0),
  (@demo_user_id, @goal_rhr_id, 58.00, 78.00, NULL, DATE_SUB(CURDATE(), INTERVAL 30 DAY), NULL, 0)
ON DUPLICATE KEY UPDATE
  target_min = VALUES(target_min),
  target_max = VALUES(target_max),
  target_text = VALUES(target_text),
  start_date = VALUES(start_date),
  end_date = VALUES(end_date),
  user_goal_status = VALUES(user_goal_status);

SET @demo_bmi_goal_id := (
  SELECT user_goal_id
  FROM t_user_health_goal
  WHERE user_id = @demo_user_id AND goal_id = @goal_bmi_id
  LIMIT 1
);

SET @demo_rhr_goal_id := (
  SELECT user_goal_id
  FROM t_user_health_goal
  WHERE user_id = @demo_user_id AND goal_id = @goal_rhr_id
  LIMIT 1
);

INSERT INTO t_health_record (
  user_goal_id,
  user_id,
  goal_id,
  record_value,
  record_text,
  record_time,
  record_source,
  evaluation_result,
  remark
)
VALUES
  (@demo_bmi_goal_id, @demo_user_id, @goal_bmi_id, 24.80, NULL, DATE_SUB(NOW(), INTERVAL 21 DAY), 0, 2, '演示种子:BMI_21天'),
  (@demo_bmi_goal_id, @demo_user_id, @goal_bmi_id, 24.10, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), 1, 2, '演示种子:BMI_10天'),
  (@demo_bmi_goal_id, @demo_user_id, @goal_bmi_id, 23.60, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), 1, 2, '演示种子:BMI_1天'),
  (@demo_rhr_goal_id, @demo_user_id, @goal_rhr_id, 76.00, NULL, DATE_SUB(NOW(), INTERVAL 21 DAY), 1, 2, '演示种子:静息心率_21天'),
  (@demo_rhr_goal_id, @demo_user_id, @goal_rhr_id, 71.00, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), 1, 2, '演示种子:静息心率_10天'),
  (@demo_rhr_goal_id, @demo_user_id, @goal_rhr_id, 68.00, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), 1, 2, '演示种子:静息心率_1天');

-- 演示数据源与同步任务
INSERT INTO t_data_source (
  user_id,
  source_name,
  source_type,
  source_status,
  description,
  last_sync_time
)
VALUES
  (@demo_user_id, '演示手动日志', 'manual', 0, '用户每日自主补录的健康记录。', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
  (@demo_user_id, '演示手环 S1', 'device', 0, '用于同步步数与心率的可穿戴设备。', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
  (@demo_user_id, '演示体检导入', 'file', 0, '用于导入睡眠与血压数据的 CSV 文件。', DATE_SUB(NOW(), INTERVAL 5 HOUR));

SET @source_manual_id := (
  SELECT source_id FROM t_data_source
  WHERE user_id = @demo_user_id AND source_name = '演示手动日志'
  LIMIT 1
);

SET @source_band_id := (
  SELECT source_id FROM t_data_source
  WHERE user_id = @demo_user_id AND source_name = '演示手环 S1'
  LIMIT 1
);

SET @source_file_id := (
  SELECT source_id FROM t_data_source
  WHERE user_id = @demo_user_id AND source_name = '演示体检导入'
  LIMIT 1
);

INSERT INTO t_sync_task (
  user_id,
  source_id,
  task_type,
  task_status,
  file_name,
  metric_category,
  total_count,
  insert_count,
  update_count,
  fail_count,
  summary_message,
  started_time,
  finished_time
)
VALUES
  (@demo_user_id, @source_band_id, 'sync', 0, '演示设备同步_近7天.json', 'health_metric', 7, 5, 2, 0, '演示手环同步完成，数据已合并。', DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR)),
  (@demo_user_id, @source_file_id, 'import', 1, '演示体检导入_周报.csv', 'health_metric', 7, 4, 2, 1, '演示体检导入完成，其中 1 行数据格式异常。', DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR));

SET @task_band_id := (
  SELECT task_id FROM t_sync_task
  WHERE user_id = @demo_user_id AND file_name = '演示设备同步_近7天.json'
  ORDER BY task_id DESC
  LIMIT 1
);

SET @task_file_id := (
  SELECT task_id FROM t_sync_task
  WHERE user_id = @demo_user_id AND file_name = '演示体检导入_周报.csv'
  ORDER BY task_id DESC
  LIMIT 1
);

-- 演示趋势数据
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
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 13 DAY), 6200, 75, 6.20, 126, 84, 5, @source_manual_id, 'manual', '演示手动日志', NULL),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 12 DAY), 6800, 74, 6.40, 124, 83, 5, @source_manual_id, 'manual', '演示手动日志', NULL),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 11 DAY), 7100, 73, 6.60, 123, 82, 4, @source_manual_id, 'manual', '演示手动日志', NULL),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 7600, 72, 6.80, 122, 81, 4, @source_manual_id, 'manual', '演示手动日志', NULL),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 7900, 71, 6.90, 121, 80, 4, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 8400, 70, 7.10, 121, 80, 3, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 8800, 70, 7.20, 120, 79, 3, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 9100, 69, 7.00, 120, 79, 3, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 9400, 69, 7.30, 119, 78, 3, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 9700, 68, 7.40, 118, 78, 2, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 9900, 68, 7.50, 118, 77, 2, @source_band_id, 'device', '演示手环 S1', @task_band_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 10200, 67, 7.60, 117, 77, 2, @source_file_id, 'file', '演示体检导入', @task_file_id),
  (@demo_user_id, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 10800, 67, 7.80, 116, 76, 2, @source_file_id, 'file', '演示体检导入', @task_file_id),
  (@demo_user_id, CURDATE(), 11200, 66, 7.90, 116, 76, 1, @source_file_id, 'file', '演示体检导入', @task_file_id)
ON DUPLICATE KEY UPDATE
  steps = VALUES(steps),
  resting_heart_rate = VALUES(resting_heart_rate),
  sleep_hours = VALUES(sleep_hours),
  systolic = VALUES(systolic),
  diastolic = VALUES(diastolic),
  stress_level = VALUES(stress_level),
  source_id = VALUES(source_id),
  source_type = VALUES(source_type),
  source_name = VALUES(source_name),
  sync_task_id = VALUES(sync_task_id);

-- 演示运动记录
INSERT INTO t_exercise_record (
  user_id,
  sport_id,
  sport_name,
  record_time,
  duration_min,
  calories_kcal,
  note,
  data_source,
  external_id
)
VALUES
  (@demo_user_id, @course_walk_id, '演示快走训练', DATE_SUB(TIMESTAMP(CURDATE(), '06:40:00'), INTERVAL 3 DAY), 45, 250.00, '演示种子:晨间快走', 'device', 'DEMO_WALK_D3'),
  (@demo_user_id, @course_yoga_id, '演示舒缓瑜伽', DATE_SUB(TIMESTAMP(CURDATE(), '20:10:00'), INTERVAL 2 DAY), 35, 140.00, '演示种子:晚间瑜伽', 'manual', 'DEMO_YOGA_D2'),
  (@demo_user_id, @course_hiit_id, '演示 HIIT 入门', DATE_SUB(TIMESTAMP(CURDATE(), '18:30:00'), INTERVAL 1 DAY), 20, 180.00, '演示种子:间歇训练', 'manual', 'DEMO_HIIT_D1'),
  (@demo_user_id, @course_walk_id, '演示快走训练', TIMESTAMP(CURDATE(), '07:05:00'), 50, 280.00, '演示种子:今日快走', 'device', 'DEMO_WALK_D0')
ON DUPLICATE KEY UPDATE
  sport_id = VALUES(sport_id),
  sport_name = VALUES(sport_name),
  record_time = VALUES(record_time),
  duration_min = VALUES(duration_min),
  calories_kcal = VALUES(calories_kcal),
  note = VALUES(note);

-- 演示饮食记录
INSERT INTO t_diet_record (
  user_id,
  recipe_id,
  food_name,
  meal_type,
  dining_time,
  intake_amount,
  calories_per_100g,
  estimated_calories,
  remark,
  record_status
)
VALUES
  (@demo_user_id, @recipe_oatmeal_id, '演示燕麦能量碗', 1, DATE_SUB(TIMESTAMP(CURDATE(), '07:30:00'), INTERVAL 2 DAY), 280.00, 95.00, 266.00, '演示种子:早餐_两天前', 0),
  (@demo_user_id, @recipe_salad_id, '演示鸡胸肉沙拉', 2, DATE_SUB(TIMESTAMP(CURDATE(), '12:20:00'), INTERVAL 2 DAY), 320.00, 145.00, 464.00, '演示种子:午餐_两天前', 0),
  (@demo_user_id, @recipe_salmon_id, '演示三文鱼套餐', 3, DATE_SUB(TIMESTAMP(CURDATE(), '18:40:00'), INTERVAL 1 DAY), 350.00, 180.00, 630.00, '演示种子:晚餐_一天前', 0),
  (@demo_user_id, @recipe_yogurt_id, '演示酸奶水果杯', 4, TIMESTAMP(CURDATE(), '15:30:00'), 180.00, 82.00, 147.60, '演示种子:加餐_今天', 0);

SET @meal_breakfast_d2_id := (
  SELECT record_id
  FROM t_diet_record
  WHERE user_id = @demo_user_id AND remark = '演示种子:早餐_两天前'
  LIMIT 1
);

SET @meal_lunch_d2_id := (
  SELECT record_id
  FROM t_diet_record
  WHERE user_id = @demo_user_id AND remark = '演示种子:午餐_两天前'
  LIMIT 1
);

SET @meal_dinner_d1_id := (
  SELECT record_id
  FROM t_diet_record
  WHERE user_id = @demo_user_id AND remark = '演示种子:晚餐_一天前'
  LIMIT 1
);

SET @meal_snack_d0_id := (
  SELECT record_id
  FROM t_diet_record
  WHERE user_id = @demo_user_id AND remark = '演示种子:加餐_今天'
  LIMIT 1
);

INSERT INTO t_meal_item (
  meal_id,
  food_id,
  food_name,
  amount,
  unit,
  kcal
)
VALUES
  (@meal_breakfast_d2_id, @recipe_oatmeal_id, '燕麦', 80.00, 'g', 300.00),
  (@meal_breakfast_d2_id, @recipe_oatmeal_id, '香蕉', 100.00, 'g', 89.00),
  (@meal_breakfast_d2_id, @recipe_oatmeal_id, '牛奶', 100.00, 'ml', 61.00),
  (@meal_lunch_d2_id, @recipe_salad_id, '鸡胸肉', 150.00, 'g', 248.00),
  (@meal_lunch_d2_id, @recipe_salad_id, '生菜', 80.00, 'g', 12.00),
  (@meal_lunch_d2_id, @recipe_salad_id, '牛油果', 90.00, 'g', 144.00),
  (@meal_dinner_d1_id, @recipe_salmon_id, '三文鱼', 180.00, 'g', 374.00),
  (@meal_dinner_d1_id, @recipe_salmon_id, '糙米饭', 120.00, 'g', 133.00),
  (@meal_dinner_d1_id, @recipe_salmon_id, '西兰花', 90.00, 'g', 31.00),
  (@meal_snack_d0_id, @recipe_yogurt_id, '希腊酸奶', 120.00, 'g', 88.00),
  (@meal_snack_d0_id, @recipe_yogurt_id, '蓝莓', 60.00, 'g', 34.00);
