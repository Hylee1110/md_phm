-- Course name uniqueness should only apply to non-deleted rows (soft delete).
-- Without this, renaming/creating a course to a name still held by a deleted row
-- fails with Duplicate entry on uk_course_name, while Java countByName() allows it.
--
-- Run once against existing databases that still have UNIQUE KEY uk_course_name (name).

ALTER TABLE sport_course
  ADD COLUMN name_unique_active VARCHAR(64)
    GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN name ELSE NULL END) STORED
  AFTER sort_weight;

ALTER TABLE sport_course DROP INDEX uk_course_name;

ALTER TABLE sport_course
  ADD UNIQUE KEY uk_course_name_active (name_unique_active);
