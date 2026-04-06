CREATE TABLE IF NOT EXISTS t_health_goal (
  goal_id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  goal_code            VARCHAR(64) NOT NULL UNIQUE COMMENT '目标编码，如 BMI / RESTING_HEART_RATE',
  goal_name            VARCHAR(128) NOT NULL COMMENT '目标名称，如 健康BMI / 静息心率',
  goal_description     VARCHAR(500) COMMENT '目标说明',
  metric_type          TINYINT NOT NULL DEFAULT 1 COMMENT '指标类型：1-数值型 2-文本型 3-布尔型',
  unit                 VARCHAR(32) COMMENT '单位，如 kg/m²、bpm、mmHg',
  default_target_min   DECIMAL(10,2) COMMENT '默认目标最小值',
  default_target_max   DECIMAL(10,2) COMMENT '默认目标最大值',
  default_target_text  VARCHAR(255) COMMENT '默认目标文本描述，适合文本型目标',
  sort_no              INT NOT NULL DEFAULT 0 COMMENT '排序号',
  goal_status          TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-停用',
  created_by           BIGINT COMMENT '创建人（管理员user_id）',
  created_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_health_goal_created_by FOREIGN KEY (created_by) REFERENCES t_user(user_id),
  INDEX idx_health_goal_status(goal_status),
  INDEX idx_health_goal_sort(sort_no)
) COMMENT='健康目标定义表';

CREATE TABLE IF NOT EXISTS t_user_health_goal (
  user_goal_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id              BIGINT NOT NULL COMMENT '用户ID',
  goal_id              BIGINT NOT NULL COMMENT '健康目标ID',
  target_min           DECIMAL(10,2) COMMENT '用户目标最小值',
  target_max           DECIMAL(10,2) COMMENT '用户目标最大值',
  target_text          VARCHAR(255) COMMENT '用户目标文本描述',
  start_date           DATE COMMENT '开始日期',
  end_date             DATE COMMENT '结束日期',
  user_goal_status     TINYINT NOT NULL DEFAULT 0 COMMENT '0-进行中 1-已完成 2-已取消',
  created_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_health_goal_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_user_health_goal_goal FOREIGN KEY (goal_id) REFERENCES t_health_goal(goal_id),
  CONSTRAINT uk_user_goal UNIQUE (user_id, goal_id),
  INDEX idx_uhg_user(user_id),
  INDEX idx_uhg_goal(goal_id),
  INDEX idx_uhg_status(user_goal_status)
) COMMENT='用户已选择的健康目标表';

CREATE TABLE IF NOT EXISTS t_health_record (
  record_id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_goal_id         BIGINT NOT NULL COMMENT '用户健康目标ID',
  user_id              BIGINT NOT NULL COMMENT '用户ID，冗余字段，便于查询',
  goal_id              BIGINT NOT NULL COMMENT '健康目标ID，冗余字段，便于查询',
  record_value         DECIMAL(10,2) COMMENT '数值型记录值',
  record_text          VARCHAR(255) COMMENT '文本型记录值',
  record_time          DATETIME NOT NULL COMMENT '记录时间',
  record_source        TINYINT NOT NULL DEFAULT 0 COMMENT '0-手动录入 1-设备同步 2-系统计算',
  evaluation_result    TINYINT NOT NULL DEFAULT 0 COMMENT '0-未评价 1-偏低 2-达标 3-偏高',
  remark               VARCHAR(500) COMMENT '备注',
  created_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_health_record_user_goal FOREIGN KEY (user_goal_id) REFERENCES t_user_health_goal(user_goal_id),
  CONSTRAINT fk_health_record_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_health_record_goal FOREIGN KEY (goal_id) REFERENCES t_health_goal(goal_id),
  INDEX idx_hr_user_goal_time(user_goal_id, record_time DESC),
  INDEX idx_hr_user_goal(user_id, goal_id),
  INDEX idx_hr_record_time(record_time)
) COMMENT='健康记录表';

INSERT INTO t_health_goal
(goal_code, goal_name, goal_description, metric_type, unit, default_target_min, default_target_max, default_target_text, sort_no, goal_status, created_by)
VALUES
('BMI', '健康BMI', '维持健康的BMI值（18.5-24.9）可帮助降低慢性疾病风险。通过合理饮食和适度锻炼来达到和维持该目标。', 1, 'kg/m²', 18.50, 24.90, NULL, 1, 0, NULL),
('RESTING_HEART_RATE', '静息心率', '定期监测心率，特别是在锻炼时，可以帮助评估心脏健康和运动强度。', 1, 'bpm', 60.00, 100.00, NULL, 2, 0, NULL)
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
