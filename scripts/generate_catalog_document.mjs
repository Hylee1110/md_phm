/**
 * 从本地 MySQL（md_phms）执行 query_export_sport_courses_and_recipes.sql，
 * 生成仅含「课程名称」「食谱名称」列表的 UTF-8 Markdown。
 *
 * 用法（在项目根目录）:
 *   node scripts/generate_catalog_document.mjs
 */
import { spawnSync } from "node:child_process";
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, "..");
const sqlPath = join(root, "phms_server", "sql", "query_export_sport_courses_and_recipes.sql");
const outDir = join(root, "phms_server", "docs");
const outMd = join(outDir, "运动课程与食谱目录.md");

const host = process.env.PHMS_DB_HOST ?? "127.0.0.1";
const port = process.env.PHMS_DB_PORT ?? "3306";
const user = process.env.PHMS_DB_USER ?? "root";
const password = process.env.PHMS_DB_PASSWORD ?? "123456";
const database = process.env.PHMS_DB_NAME ?? "md_phms";

let sql = readFileSync(sqlPath, "utf8");
sql = sql
  .split("\n")
  .filter((line) => !/^\s*--/.test(line))
  .join("\n")
  .replace(/^\s*USE\s+\w+\s*;/im, "")
  .trim();

const args = [
  `-h${host}`,
  `-P${port}`,
  `-u${user}`,
  `-p${password}`,
  "--default-character-set=utf8mb4",
  database
];

const result = spawnSync("mysql", args, {
  input: sql,
  encoding: "utf8",
  maxBuffer: 64 * 1024 * 1024
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}
if (result.status !== 0) {
  console.error(result.stderr || result.stdout || "mysql failed");
  process.exit(result.status ?? 1);
}

const raw = (result.stdout || "").trimEnd();
const now = new Date().toISOString().replace("T", " ").slice(0, 19);

const { courses, recipes } = splitNameLists(raw);

const md = `# PHMS 运动课程与食谱（仅名称）

> 自动生成时间：${now} · 数据库 \`${database}\` @ \`${host}\`

## 一、运动课程

${bulletList(courses)}

## 二、食谱

${bulletList(recipes)}
`;

mkdirSync(outDir, { recursive: true });
writeFileSync(outMd, md, "utf8");
console.log("已生成:", outMd);

function splitNameLists(text) {
  const lines = text.split(/\r?\n/).map((l) => l.trimEnd());
  const idxRecipeHeader = lines.findIndex((l) => l === "食谱名称" || l.startsWith("食谱名称\t"));
  if (idxRecipeHeader < 0) {
    const courseLines = lines.filter((l) => l.length > 0);
    const header = courseLines[0] === "课程名称" ? 1 : 0;
    return {
      courses: courseLines.slice(header),
      recipes: []
    };
  }
  const courseBlock = lines.slice(0, idxRecipeHeader).filter((l) => l.length > 0);
  const recipeBlock = lines.slice(idxRecipeHeader).filter((l) => l.length > 0);
  const cHeader = courseBlock[0] === "课程名称" ? 1 : 0;
  const rHeader = recipeBlock[0] === "食谱名称" ? 1 : 0;
  return {
    courses: courseBlock.slice(cHeader),
    recipes: recipeBlock.slice(rHeader)
  };
}

function bulletList(names) {
  if (!names.length) {
    return "_（无）_";
  }
  return names.map((n) => `- ${n}`).join("\n");
}
