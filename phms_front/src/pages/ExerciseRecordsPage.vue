<!--
  运动记录：按时间范围查询、查看详情、删除记录（当前用户数据）。
-->
<script setup>
import { computed, onMounted, ref } from "vue";
import { healthApi } from "../services/healthApi";

const loading = ref(false);
const trendLoading = ref(false);
const deletingId = ref(null);
const errorMsg = ref("");
const successMsg = ref("");
const trendErrorMsg = ref("");
const records = ref([]);
const trendRecords = ref([]);
const trendRange = ref("14d");
const hoveredDate = ref(null);
const courseNameMap = ref({});

const query = ref({
  startTime: "",
  endTime: "",
  limit: 50
});

const trendRangeOptions = [
  { value: "7d", label: "近7天" },
  { value: "14d", label: "近14天" },
  { value: "30d", label: "近30天" },
  { value: "90d", label: "近90天" },
  { value: "180d", label: "近半年" },
  { value: "365d", label: "近一年" },
  { value: "all", label: "全部" }
];

const sourceLabelMap = {
  manual: "手动记录",
  device: "设备记录",
  "3rd": "第三方",
  import: "导入数据"
};

const numberFormatter = new Intl.NumberFormat("zh-CN", {
  maximumFractionDigits: 1
});

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length > 0 ? text : null;
}

function toNonNegativeInt(value, fallback = 0) {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return fallback;
  }
  return Math.max(0, Math.floor(num));
}

function parseCalories(value) {
  const num = Number(value);
  return Number.isFinite(num) && num > 0 ? num : 0;
}

function normalizeHourMinute(hours, minutes) {
  let normalizedHours = toNonNegativeInt(hours, 0);
  let normalizedMinutes = toNonNegativeInt(minutes, 0);
  if (normalizedMinutes >= 60) {
    normalizedHours += Math.floor(normalizedMinutes / 60);
    normalizedMinutes %= 60;
  }
  return {
    hours: normalizedHours,
    minutes: normalizedMinutes
  };
}

function splitDurationMinutes(totalMinutes) {
  const total = toNonNegativeInt(totalMinutes, 0);
  return {
    hours: Math.floor(total / 60),
    minutes: total % 60
  };
}

function formatHourMinute(hours, minutes) {
  const normalized = normalizeHourMinute(hours, minutes);
  if (normalized.hours === 0 && normalized.minutes === 0) {
    return "--";
  }
  if (normalized.hours === 0) {
    return `${normalized.minutes} 分钟`;
  }
  if (normalized.minutes === 0) {
    return `${normalized.hours} 小时`;
  }
  return `${normalized.hours} 小时 ${normalized.minutes} 分钟`;
}

function formatDuration(totalMinutes) {
  const parts = splitDurationMinutes(totalMinutes);
  return formatHourMinute(parts.hours, parts.minutes);
}

function formatCalories(value) {
  const num = Number(value);
  if (!Number.isFinite(num) || num < 0) {
    return "--";
  }
  if (Number.isInteger(num)) {
    return `${num} 卡`;
  }
  return `${num.toFixed(1)} 卡`;
}

function formatRecordDate(value) {
  if (!value) {
    return "--";
  }
  const text = String(value);
  return text.length >= 10 ? text.slice(0, 10) : text;
}

function formatDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseDateKey(key) {
  const [year, month, day] = String(key).split("-").map((item) => Number(item));
  return new Date(year, (month || 1) - 1, day || 1);
}

function addDays(dateKey, days) {
  const date = parseDateKey(dateKey);
  date.setDate(date.getDate() + days);
  return formatDateKey(date);
}

function todayDateKey() {
  return formatDateKey(new Date());
}

function toDateKey(value) {
  if (!value) {
    return null;
  }
  const text = String(value);
  return text.length >= 10 ? text.slice(0, 10) : null;
}

function sourceLabel(source) {
  const key = String(source ?? "").trim().toLowerCase();
  return sourceLabelMap[key] || "运动记录";
}

function resolveRecordName(record) {
  const name = normalizeText(record.sportName);
  if (name) {
    return name;
  }
  if (record.sportId != null) {
    const courseName = courseNameMap.value[String(record.sportId)];
    return courseName || `运动 #${record.sportId}`;
  }
  return "未命名运动";
}

function recordNote(record) {
  return normalizeText(record.note) || "暂无笔记";
}

function buildListParams() {
  return {
    startTime: normalizeText(query.value.startTime),
    endTime: normalizeText(query.value.endTime),
    limit: Number(query.value.limit || 50)
  };
}

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function trendDays(rangeKey) {
  if (rangeKey === "7d") return 7;
  if (rangeKey === "14d") return 14;
  if (rangeKey === "30d") return 30;
  if (rangeKey === "90d") return 90;
  if (rangeKey === "180d") return 180;
  if (rangeKey === "365d") return 365;
  return null;
}

function trendWindow(rangeKey) {
  const days = trendDays(rangeKey);
  if (days === null) {
    return null;
  }
  const endKey = todayDateKey();
  const startKey = addDays(endKey, -(days - 1));
  return {
    startKey,
    endKey
  };
}

function buildDateKeys(startKey, endKey) {
  const dates = [];
  let current = startKey;
  while (current <= endKey) {
    dates.push(current);
    current = addDays(current, 1);
  }
  return dates;
}

function formatKcalNumber(value) {
  return numberFormatter.format(value);
}

function buildSmoothPath(points) {
  if (points.length === 0) {
    return "";
  }
  if (points.length === 1) {
    return `M ${points[0].x} ${points[0].y}`;
  }
  let path = `M ${points[0].x} ${points[0].y}`;
  for (let index = 1; index < points.length; index += 1) {
    const prev = points[index - 1];
    const curr = points[index];
    const controlX = (prev.x + curr.x) / 2;
    path += ` C ${controlX} ${prev.y}, ${controlX} ${curr.y}, ${curr.x} ${curr.y}`;
  }
  return path;
}

function buildSmoothAreaPath(points, baseY) {
  if (points.length === 0) {
    return "";
  }
  if (points.length === 1) {
    const point = points[0];
    return `M ${point.x} ${baseY} L ${point.x} ${point.y} L ${point.x} ${baseY} Z`;
  }
  let path = `M ${points[0].x} ${baseY} L ${points[0].x} ${points[0].y}`;
  for (let index = 1; index < points.length; index += 1) {
    const prev = points[index - 1];
    const curr = points[index];
    const controlX = (prev.x + curr.x) / 2;
    path += ` C ${controlX} ${prev.y}, ${controlX} ${curr.y}, ${curr.x} ${curr.y}`;
  }
  const last = points[points.length - 1];
  path += ` L ${last.x} ${baseY} Z`;
  return path;
}

function xLabelStep(totalCount) {
  if (totalCount <= 8) {
    return 1;
  }
  return Math.ceil(totalCount / 8);
}

function shouldShowXLabel(index, totalCount) {
  const step = xLabelStep(totalCount);
  return index === 0 || index === totalCount - 1 || index % step === 0;
}

function formatXAxisLabel(dateKey, totalCount) {
  if (totalCount <= 8) {
    return dateKey;
  }
  return String(dateKey).slice(5);
}

async function loadRecords({ keepMessages = false } = {}) {
  loading.value = true;
  if (!keepMessages) {
    clearMessages();
  }
  try {
    // 主列表：时间范围 + limit，与趋势图数据源分离
    records.value = await healthApi.listExerciseRecords(buildListParams());
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function loadTrendRecords({ keepError = false } = {}) {
  trendLoading.value = true;
  if (!keepError) {
    trendErrorMsg.value = "";
  }
  try {
    const range = trendWindow(trendRange.value);
    const params = {
      limit: 200
    };
    if (range) {
      params.startTime = `${range.startKey}T00:00:00`;
      params.endTime = `${range.endKey}T23:59:59`;
    }
    trendRecords.value = await healthApi.listExerciseRecords(params);
  } catch (error) {
    trendErrorMsg.value = error.message;
  } finally {
    trendLoading.value = false;
  }
}

async function loadCourseNames() {
  try {
    const courses = await healthApi.listSportCourses({ limit: 200 });
    const map = {};
    for (const course of courses) {
      map[String(course.id)] = String(course.name ?? "").trim();
    }
    courseNameMap.value = map;
  } catch (error) {
    // 课程名拉取失败时仍可用 resolveRecordName 回退为「运动 #ID」
    courseNameMap.value = {};
  }
}

function changeTrendRange(value) {
  if (trendRange.value === value) {
    return;
  }
  trendRange.value = value;
  hoveredDate.value = null;
  loadTrendRecords();
}

async function deleteRecord(recordId) {
  // 与饮食记录页一致：删后双刷列表与趋势，保证 UI 一致
  if (!window.confirm(`确认删除记录 #${recordId} 吗？`)) {
    return;
  }
  deletingId.value = recordId;
  clearMessages();
  try {
    await healthApi.deleteExerciseRecord(recordId);
    await Promise.all([
      loadRecords({ keepMessages: true }),
      loadTrendRecords({ keepError: true })
    ]);
    successMsg.value = "记录已删除";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    deletingId.value = null;
  }
}

// 将 trendRecords 按日汇总卡路里，供 SVG 折线/柱图使用
const trendSeries = computed(() => {
  const map = new Map();
  for (const item of trendRecords.value) {
    const dateKey = toDateKey(item.recordTime);
    if (!dateKey) {
      continue;
    }
    map.set(dateKey, (map.get(dateKey) || 0) + parseCalories(item.caloriesKcal));
  }

  const window = trendWindow(trendRange.value);
  if (!window) {
    const allKeys = [...map.keys()].sort();
    return allKeys.map((date) => ({
      date,
      kcal: map.get(date) || 0
    }));
  }

  const dates = buildDateKeys(window.startKey, window.endKey);
  return dates.map((date) => ({
    date,
    kcal: map.get(date) || 0
  }));
});

const trendTotalKcal = computed(() => {
  return trendSeries.value.reduce((sum, item) => sum + item.kcal, 0);
});

const chartState = computed(() => {
  if (trendSeries.value.length === 0) {
    return null;
  }

  const width = 980;
  const height = 360;
  const left = 72;
  const right = 20;
  const top = 24;
  const bottom = 50;
  const plotWidth = width - left - right;
  const plotHeight = height - top - bottom;
  const maxKcal = Math.max(...trendSeries.value.map((item) => item.kcal), 0);
  const yMax = Math.max(100, Math.ceil(maxKcal / 100) * 100);
  const baseY = top + plotHeight;

  const points = trendSeries.value.map((item, index) => {
    const x = trendSeries.value.length === 1
      ? left + plotWidth / 2
      : left + (index / (trendSeries.value.length - 1)) * plotWidth;
    const y = baseY - (item.kcal / yMax) * plotHeight;
    return {
      ...item,
      x,
      y
    };
  });

  const linePath = buildSmoothPath(points);
  const areaPath = buildSmoothAreaPath(points, baseY);
  const ticks = Array.from({ length: 5 }, (_, index) => {
    const ratio = index / 4;
    const value = yMax * (1 - ratio);
    return {
      y: top + ratio * plotHeight,
      value
    };
  });

  return {
    width,
    height,
    left,
    right,
    top,
    bottom,
    baseY,
    xEnd: left + plotWidth,
    points,
    ticks,
    linePath,
    areaPath
  };
});

function setHoveredDate(dateKey) {
  hoveredDate.value = dateKey;
}

function clearHoveredDate() {
  hoveredDate.value = null;
}

const hoveredPoint = computed(() => {
  if (!hoveredDate.value || !chartState.value) {
    return null;
  }
  return chartState.value.points.find((point) => point.date === hoveredDate.value) || null;
});

const hoveredTooltip = computed(() => {
  if (!hoveredPoint.value || !chartState.value) {
    return null;
  }
  const chart = chartState.value;
  const point = hoveredPoint.value;
  const label = `${point.date}：${formatKcalNumber(point.kcal)} kcal`;
  const width = Math.max(140, label.length * 7.2 + 18);
  const centerX = Math.min(
    chart.xEnd - width / 2,
    Math.max(chart.left + width / 2, point.x)
  );
  const rectY = Math.max(chart.top + 2, point.y - 36);
  return {
    label,
    width,
    rectX: centerX - width / 2,
    rectY,
    textX: centerX,
    textY: rectY + 17,
    guideX: point.x,
    guideY: point.y
  };
});

onMounted(() => {
  loadRecords();
  loadTrendRecords();
  loadCourseNames();
});
</script>

<template>
  <section class="ink-card trend-card">
    <div class="trend-head">
      <div>
        <p class="trend-caption">数据分析</p>
        <h3 class="trend-title">每日卡路里消耗趋势</h3>
      </div>
      <div class="trend-range">
        <button
          v-for="option in trendRangeOptions"
          :key="option.value"
          type="button"
          class="range-btn"
          :class="{ active: trendRange === option.value }"
          @click="changeTrendRange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <p v-if="trendErrorMsg" class="error">{{ trendErrorMsg }}</p>
    <p v-else-if="trendLoading" class="muted">图表加载中...</p>

    <div v-else class="chart-shell">
      <svg
        v-if="chartState"
        :viewBox="`0 0 ${chartState.width} ${chartState.height}`"
        class="trend-svg"
        @mouseleave="clearHoveredDate"
      >
        <defs>
          <linearGradient id="trendAreaGradient" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stop-color="rgba(18, 22, 26, 0.22)" />
            <stop offset="100%" stop-color="rgba(18, 22, 26, 0.04)" />
          </linearGradient>
        </defs>

        <g class="grid">
          <line
            v-for="tick in chartState.ticks"
            :key="`grid-${tick.y}`"
            :x1="chartState.left"
            :y1="tick.y"
            :x2="chartState.xEnd"
            :y2="tick.y"
            stroke="#d9dde1"
            stroke-dasharray="4 4"
          />
        </g>

        <line
          :x1="chartState.left"
          :y1="chartState.baseY"
          :x2="chartState.xEnd"
          :y2="chartState.baseY"
          stroke="#b8bec6"
        />

        <path :d="chartState.areaPath" fill="url(#trendAreaGradient)" />
        <path :d="chartState.linePath" fill="none" stroke="#111315" stroke-width="2.6" />

        <g v-if="hoveredTooltip" class="tooltip-layer" pointer-events="none">
          <line
            :x1="hoveredTooltip.guideX"
            :y1="hoveredTooltip.guideY - 2"
            :x2="hoveredTooltip.guideX"
            :y2="chartState.baseY"
            class="tooltip-guide"
          />
          <rect
            :x="hoveredTooltip.rectX"
            :y="hoveredTooltip.rectY"
            :width="hoveredTooltip.width"
            height="24"
            rx="7"
            class="tooltip-box"
          />
          <text :x="hoveredTooltip.textX" :y="hoveredTooltip.textY" text-anchor="middle" class="tooltip-text">
            {{ hoveredTooltip.label }}
          </text>
        </g>

        <circle
          v-for="point in chartState.points"
          :key="`point-${point.date}`"
          :cx="point.x"
          :cy="point.y"
          :r="hoveredDate === point.date ? 5.2 : 3.8"
          :class="['trend-point', { active: hoveredDate === point.date }]"
          fill="#111315"
          @mouseenter="setHoveredDate(point.date)"
          @mousemove="setHoveredDate(point.date)"
          @focus="setHoveredDate(point.date)"
          @blur="clearHoveredDate"
        />

        <text
          v-for="tick in chartState.ticks"
          :key="`y-label-${tick.y}`"
          :x="chartState.left - 10"
          :y="tick.y + 4"
          text-anchor="end"
          class="y-label"
        >
          {{ formatKcalNumber(tick.value) }}
        </text>

        <text
          v-for="(point, index) in chartState.points"
          v-show="shouldShowXLabel(index, chartState.points.length)"
          :key="`x-label-${point.date}`"
          :x="point.x"
          :y="chartState.baseY + 20"
          text-anchor="middle"
          class="x-label"
        >
          {{ formatXAxisLabel(point.date, chartState.points.length) }}
        </text>

        <text :x="chartState.left" :y="16" class="axis-title">卡路里 (kcal)</text>
      </svg>

      <p v-else class="muted">暂无可用于统计的运动消耗数据</p>
    </div>

    <p v-if="trendSeries.length > 0" class="trend-summary">
      当前区间累计消耗 {{ formatKcalNumber(trendTotalKcal) }} kcal，按天自动汇总同日运动消耗。
    </p>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <h3>历史记录</h3>
      <div class="inline-actions query-actions">
        <label>
          开始时间
          <input type="datetime-local" v-model="query.startTime" />
        </label>
        <label>
          结束时间
          <input type="datetime-local" v-model="query.endTime" />
        </label>
        <label>
          条数
          <input type="number" min="1" max="200" v-model.number="query.limit" />
        </label>
        <button @click="loadRecords" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
      </div>
    </div>

    <p class="muted">运动记录仅支持在“运动课程”详情中创建。</p>
    <h3 class="history-title">所有运动记录</h3>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>
    <p v-if="loading" class="muted">运动记录加载中...</p>

    <div v-else class="record-list">
      <article v-for="record in records" :key="record.recordId" class="record-card">
        <div class="record-head">
          <div class="record-title-wrap">
            <h4>{{ resolveRecordName(record) }}</h4>
            <span class="record-badge">{{ sourceLabel(record.dataSource) }}</span>
          </div>
          <button
            class="danger-btn"
            type="button"
            @click="deleteRecord(record.recordId)"
            :disabled="deletingId === record.recordId"
          >
            {{ deletingId === record.recordId ? "删除中..." : "删除" }}
          </button>
        </div>

        <div class="record-meta">
          <div class="meta-item">
            <span class="meta-label">时长</span>
            <strong>{{ formatDuration(record.durationMin) }}</strong>
          </div>
          <div class="meta-item">
            <span class="meta-label">消耗</span>
            <strong>{{ formatCalories(record.caloriesKcal) }}</strong>
          </div>
          <div class="meta-item">
            <span class="meta-label">日期</span>
            <strong>{{ formatRecordDate(record.recordTime) }}</strong>
          </div>
        </div>

        <p class="record-note">笔记: {{ recordNote(record) }}</p>
      </article>

      <p v-if="records.length === 0" class="muted">暂无运动记录</p>
    </div>
  </section>
</template>

<style scoped>
.trend-card {
  padding-top: 16px;
}

.trend-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.trend-caption {
  margin: 0;
  color: #6e7781;
  font-size: 13px;
}

.trend-title {
  margin: 8px 0 0;
  font-size: 48px;
  font-weight: 500;
}

.trend-range {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.range-btn {
  background: #fff;
  color: #3a4048;
  border: 1px solid #ced3d8;
  border-radius: 8px;
  padding: 7px 14px;
}

.range-btn:hover {
  background: #f3f5f7;
}

.range-btn.active {
  background: #111315;
  border-color: #111315;
  color: #fff;
}

.chart-shell {
  margin-top: 14px;
  border: 2px solid #d7dce1;
  border-radius: 14px;
  padding: 12px 10px 6px;
  background: #fff;
}

.trend-svg {
  width: 100%;
  height: auto;
  display: block;
}

.trend-point {
  cursor: pointer;
  transition: r 0.15s ease, opacity 0.15s ease;
}

.trend-point.active {
  opacity: 1;
}

.tooltip-guide {
  stroke: #8a939e;
  stroke-dasharray: 4 4;
}

.tooltip-box {
  fill: rgba(17, 19, 21, 0.94);
}

.tooltip-text {
  fill: #fff;
  font-size: 12px;
}

.axis-title {
  font-size: 12px;
  fill: #6d7680;
}

.x-label,
.y-label {
  font-size: 11px;
  fill: #6d7680;
}

.trend-summary {
  margin: 10px 0 0;
  color: #5b626c;
}

.query-actions {
  flex-wrap: wrap;
}

.query-actions label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 180px;
}

.history-title {
  margin: 8px 0 16px;
  font-size: 42px;
  font-weight: 500;
}

.record-list {
  display: grid;
  gap: 14px;
}

.record-card {
  border: 2px solid #e4e6e8;
  border-radius: 16px;
  padding: 18px;
  background: #fff;
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.record-title-wrap h4 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-badge {
  border-radius: 6px;
  background: #f1f3f5;
  color: #6f7781;
  font-size: 11px;
  padding: 3px 8px;
  flex: none;
}

.danger-btn {
  background: #a22727;
  color: #fff;
}

.danger-btn:hover {
  background: #861c1c;
}

.record-meta {
  margin-top: 14px;
  border-top: 1px solid #eceef1;
  border-bottom: 1px solid #eceef1;
  padding: 12px 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.meta-item {
  display: grid;
  gap: 6px;
}

.meta-label {
  color: #848b94;
  font-size: 12px;
}

.meta-item strong {
  font-size: 26px;
  font-weight: 600;
}

.record-note {
  margin: 14px 0 0;
  background: #f6f7f8;
  border-radius: 8px;
  padding: 10px 12px;
  color: #505760;
}

@media (max-width: 1100px) {
  .trend-title {
    font-size: 38px;
  }
}

@media (max-width: 920px) {
  .history-title {
    font-size: 34px;
  }

  .record-title-wrap h4 {
    font-size: 24px;
  }
}

@media (max-width: 820px) {
  .trend-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .record-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .record-meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .trend-title {
    font-size: 30px;
  }

  .history-title {
    font-size: 28px;
  }

  .record-title-wrap h4 {
    font-size: 20px;
  }
}
</style>
