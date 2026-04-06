-- =============================================================================
-- 从数据库仅查询「运动课程名」「食谱名」（无其它字段）
-- 用法：见 scripts/generate_catalog_document.mjs 或客户端单独执行下列 SELECT。
-- =============================================================================

USE md_phms;

-- 一、运动课程（未软删）
SELECT sc.name AS 课程名称
FROM sport_course sc
WHERE sc.is_deleted = 0
ORDER BY sc.sort_weight DESC, sc.updated_at DESC, sc.id DESC;

-- 二、食谱（有效）
SELECT r.food_name AS 食谱名称
FROM t_recipe r
WHERE r.recipe_status = 0
ORDER BY r.is_admin_recommend DESC, r.food_name ASC, r.recipe_id ASC;
