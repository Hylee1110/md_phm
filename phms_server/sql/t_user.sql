CREATE TABLE IF NOT EXISTS t_user (
  user_id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  account           VARCHAR(64) NOT NULL UNIQUE,
  password_hash     VARCHAR(255) NOT NULL,
  nickname          VARCHAR(64),
  realname          VARCHAR(64),
  idcard            VARCHAR(32),
  gender            TINYINT NOT NULL DEFAULT 0 COMMENT '0-未知 1-男 2-女',
  age               INT,
  account_level     TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通用户 1-管理员',
  account_status    TINYINT NOT NULL DEFAULT 0 COMMENT '0-正常 1-异常 2-禁用',
  created_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_change_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status_level(account_status, account_level)
);
