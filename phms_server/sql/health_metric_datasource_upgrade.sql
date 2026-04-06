SET @db_name = DATABASE();

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND column_name = 'source_id'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD COLUMN source_id BIGINT NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND column_name = 'source_type'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT ''manual'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND column_name = 'source_name'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD COLUMN source_name VARCHAR(64) NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND column_name = 'sync_task_id'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD COLUMN sync_task_id BIGINT NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND index_name = 'idx_metric_source'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD KEY idx_metric_source (source_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND index_name = 'idx_metric_sync_task'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD KEY idx_metric_sync_task (sync_task_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND constraint_name = 'fk_metric_source'
      AND constraint_type = 'FOREIGN KEY'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD CONSTRAINT fk_metric_source FOREIGN KEY (source_id) REFERENCES t_data_source(source_id) ON DELETE SET NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
      AND constraint_name = 'fk_metric_sync_task'
      AND constraint_type = 'FOREIGN KEY'
  ),
  'SELECT 1',
  'ALTER TABLE health_metric ADD CONSTRAINT fk_metric_sync_task FOREIGN KEY (sync_task_id) REFERENCES t_sync_task(task_id) ON DELETE SET NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
