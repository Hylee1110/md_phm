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
);

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
  finished_time DATETIME,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sync_task_user_time (user_id, started_time),
  KEY idx_sync_task_source_time (source_id, started_time),
  CONSTRAINT fk_sync_task_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_sync_task_source FOREIGN KEY (source_id) REFERENCES t_data_source(source_id)
);
