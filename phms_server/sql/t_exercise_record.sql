CREATE TABLE IF NOT EXISTS t_exercise_record (
  record_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id           BIGINT NOT NULL,
  sport_id          BIGINT COMMENT 'nullable for custom exercise',
  sport_name        VARCHAR(64) COMMENT 'required text when sport_id is null',
  record_time       DATETIME NOT NULL COMMENT 'exercise start time / event time',
  duration_min      INT NOT NULL COMMENT 'duration in minutes',
  calories_kcal     DECIMAL(10,2) COMMENT 'estimated or from device',
  note              VARCHAR(255),
  data_source       VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT 'manual/device/3rd/import',
  external_id       VARCHAR(128) COMMENT 'external record id for dedupe',
  created_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exercise_source_ext(data_source, external_id),
  INDEX idx_exercise_user_time(user_id, record_time),
  CONSTRAINT fk_exercise_user FOREIGN KEY (user_id) REFERENCES t_user(user_id)
);
