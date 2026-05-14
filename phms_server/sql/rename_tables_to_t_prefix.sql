SET @db_name = DATABASE();
SET @old_fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'health_metric'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_health_metric'
  ),
  'RENAME TABLE health_metric TO t_health_metric',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_course_rating_log'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_course_rating_log'
  ),
  'RENAME TABLE sport_course_rating_log TO t_sport_course_rating_log',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_course_rating'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_course_rating'
  ),
  'RENAME TABLE sport_course_rating TO t_sport_course_rating',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_course_audience'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_course_audience'
  ),
  'RENAME TABLE sport_course_audience TO t_sport_course_audience',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_course_equipment'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_course_equipment'
  ),
  'RENAME TABLE sport_course_equipment TO t_sport_course_equipment',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_course_benefit'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_course_benefit'
  ),
  'RENAME TABLE sport_course_benefit TO t_sport_course_benefit',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_audience'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_audience'
  ),
  'RENAME TABLE sport_audience TO t_sport_audience',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_equipment'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_equipment'
  ),
  'RENAME TABLE sport_equipment TO t_sport_equipment',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_benefit'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_benefit'
  ),
  'RENAME TABLE sport_benefit TO t_sport_benefit',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF (
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 'sport_course'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @db_name
      AND table_name = 't_sport_course'
  ),
  'RENAME TABLE sport_course TO t_sport_course',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = @old_fk_checks;
