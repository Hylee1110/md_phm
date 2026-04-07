<!--
  饮食记录：关联食谱录入用餐、列表与编辑删除。
-->
<script setup>
import { computed, onMounted, ref } from "vue";
import { healthApi } from "../services/healthApi";

const loading = ref(false);
const trendLoading = ref(false);
const recipeLoading = ref(false);
const submitting = ref(false);
const deletingId = ref(null);
const editingRecordId = ref(null);
const hoveredDate = ref(null);
const errorMsg = ref("");
const successMsg = ref("");
const trendErrorMsg = ref("");
const records = ref([]);
const trendRecords = ref([]);
const recipes = ref([]);
const recipeKeyword = ref("");
const trendRange = ref("14d");

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

const mealTypeOptions = [
  { value: 0, label: "未指定" },
  { value: 1, label: "早餐" },
  { value: 2, label: "午餐" },
  { value: 3, label: "晚餐" },
  { value: 4, label: "加餐" }
];

const numberFormatter = new Intl.NumberFormat("zh-CN", {
  maximumFractionDigits: 1
});

const form = ref(defaultForm());

const selectedRecipe = computed(() => {
  const recipeId = Number(form.value.recipeId);
  if (!Number.isInteger(recipeId) || recipeId <= 0) {
    return null;
  }
  return recipes.value.find((item) => item.recipeId === recipeId) || null;
});

// 按「每 100g 热量 × 实际摄入量(g)」估算本餐热量（仅前端预览，最终以服务端为准）
const estimatedCaloriesPreview = computed(() => {
  const amount = toNumber(form.value.intakeAmount);
  const caloriesPer100g = toNumber(selectedRecipe.value?.calories);
  if (amount == null || amount <= 0 || caloriesPer100g == null) {
    return null;
  }
  return ((amount / 100) * caloriesPer100g).toFixed(2);
});

function defaultForm() {
  // datetime-local 需要本地无时区偏移的 ISO 片段，避免显示与提交差 8 小时等问题
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
  return {
    recipeId: "",
    diningTime: local,
    mealType: 0,
    intakeAmount: "",
    remark: ""
  };
}

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length > 0 ? text : null;
}

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

function parseCalories(value) {
  const num = Number(value);
  if (!Number.isFinite(num) || num < 0) {
    return 0;
  }
  return num;
}

function normalizePositiveInt(value) {
  const num = Number(value);
  return Number.isInteger(num) && num > 0 ? num : null;
}

function normalizePositiveDecimal(value) {
  const text = normalizeText(value);
  if (text === null) {
    return null;
  }
  const num = Number(text);
  if (Number.isNaN(num) || num <= 0) {
    return null;
  }
  return num.toFixed(2);
}

function formatDecimal(value) {
  const num = toNumber(value);
  if (num == null) {
    return "--";
  }
  return Number.isInteger(num) ? String(num) : num.toFixed(2).replace(/\.?0+$/, "");
}

function formatKcal(value) {
  const num = toNumber(value);
  return num == null ? "--" : `${formatDecimal(num)} kcal`;
}

function formatKcalPer100g(value) {
  const num = toNumber(value);
  return num == null ? "--" : `${formatDecimal(num)} kcal / 100g`;
}

function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return String(value).replace("T", " ");
}

function mealTypeText(type) {
  const target = mealTypeOptions.find((item) => item.value === Number(type));
  return target ? target.label : "未指定";
}

function editableDecimal(value) {
  const text = formatDecimal(value);
  return text === "--" ? "" : text;
}

function toLocalInputTime(value) {
  if (!value) {
    return "";
  }
  const text = String(value).replace(" ", "T");
  return text.length >= 16 ? text.slice(0, 16) : text;
}

function recipeReferencePortion(recipe) {
  if (!recipe) {
    return "--";
  }
  const portion = formatDecimal(recipe.portion);
  const unit = normalizeText(recipe.unit) || "g";
  return portion === "--" ? "--" : `${portion} ${unit}`;
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
  const text = String(value).replace(" ", "T");
  return text.length >= 10 ? text.slice(0, 10) : null;
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

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function resetForm() {
  form.value = defaultForm();
  editingRecordId.value = null;
}

function buildListParams() {
  return {
    startTime: normalizeText(query.value.startTime),
    endTime: normalizeText(query.value.endTime),
    limit: Number(query.value.limit || 50)
  };
}

function ensureRecipeOption(recipe) {
  if (!recipe || !recipe.recipeId) {
    return;
  }
  const exists = recipes.value.some((item) => item.recipeId === recipe.recipeId);
  if (exists) {
    return;
  }
  recipes.value = [
    {
      recipeId: recipe.recipeId,
      foodName: recipe.foodName,
      mealType: recipe.mealType ?? 0,
      calories: recipe.caloriesPer100g ?? recipe.calories ?? null,
      portion: recipe.portion ?? null,
      unit: recipe.unit ?? "g"
    },
    ...recipes.value
  ];
}

function handleRecipeChange(value) {
  form.value.recipeId = value;
  const recipeId = Number(value);
  const recipe = recipes.value.find((item) => item.recipeId === recipeId);
  if (recipe) {
    form.value.mealType = Number.isInteger(Number(recipe.mealType)) ? Number(recipe.mealType) : 0;
  }
}

function buildPayload() {
  const recipeId = normalizePositiveInt(form.value.recipeId);
  if (recipeId == null) {
    throw new Error("请选择食谱");
  }

  const diningTime = normalizeText(form.value.diningTime);
  if (!diningTime) {
    throw new Error("请选择用餐时间");
  }

  const mealType = Number(form.value.mealType);
  if (!Number.isInteger(mealType) || mealType < 0 || mealType > 4) {
    throw new Error("请选择有效餐次");
  }

  const intakeAmount = normalizePositiveDecimal(form.value.intakeAmount);
  if (intakeAmount == null) {
    throw new Error("摄入份量必须大于 0");
  }

  if (selectedRecipe.value?.calories == null) {
    throw new Error("所选食谱未设置每100g热量，无法生成饮食记录");
  }

  return {
    recipeId,
    diningTime,
    mealType,
    intakeAmount,
    remark: normalizeText(form.value.remark)
  };
}

function startEdit(record) {
  editingRecordId.value = record.recordId;
  ensureRecipeOption(record);
  form.value = {
    recipeId: String(record.recipeId ?? ""),
    diningTime: toLocalInputTime(record.diningTime),
    mealType: record.mealType ?? 0,
    intakeAmount: editableDecimal(record.intakeAmount),
    remark: record.remark ?? ""
  };
  clearMessages();
}

async function loadRecords({ keepMessages = false } = {}) {
  loading.value = true;
  if (!keepMessages) {
    clearMessages();
  }
  try {
    // 主列表：受 query 时间范围与 limit 约束；并为每条记录补全下拉中的食谱选项
    records.value = await healthApi.listMealRecords(buildListParams());
    records.value.forEach((record) => ensureRecipeOption(record));
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
    // 趋势图单独拉宽时间窗的数据，与主列表筛选条件解耦
    const range = trendWindow(trendRange.value);
    const params = {
      limit: 200
    };
    if (range) {
      params.startTime = `${range.startKey}T00:00:00`;
      params.endTime = `${range.endKey}T23:59:59`;
    }
    trendRecords.value = await healthApi.listMealRecords(params);
  } catch (error) {
    trendErrorMsg.value = error.message;
  } finally {
    trendLoading.value = false;
  }
}

async function loadRecipes() {
  recipeLoading.value = true;
  clearMessages();
  try {
    const loaded = await healthApi.listMealRecommendations({
      keyword: normalizeText(recipeKeyword.value),
      limit: 100
    });
    // 合并：保留历史已选食谱，避免编辑旧记录时下拉丢失条目
    const merged = [...loaded];
    recipes.value.forEach((item) => {
      if (!merged.some((candidate) => candidate.recipeId === item.recipeId)) {
        merged.push(item);
      }
    });
    recipes.value = merged;
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    recipeLoading.value = false;
  }
}

async function submitRecord() {
  // 本页仅支持编辑已有记录；新建入口若在其他页需单独对接 create 接口
  if (!editingRecordId.value) {
    return;
  }
  submitting.value = true;
  clearMessages();
  try {
    const payload = buildPayload();
    await healthApi.updateMealRecord(editingRecordId.value, payload);
    await Promise.all([
      loadRecords({ keepMessages: true }),
      loadTrendRecords({ keepError: true })
    ]);
    resetForm();
    successMsg.value = "饮食记录已更新";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    submitting.value = false;
  }
}

async function deleteRecord(recordId) {
  // 删除后同步刷新主列表与趋势数据，避免图表仍含已删点
  if (!window.confirm(`确认删除饮食记录 #${recordId} 吗？`)) {
    return;
  }
  deletingId.value = recordId;
  clearMessages();
  try {
    await healthApi.deleteMealRecord(recordId);
    if (editingRecordId.value === recordId) {
      resetForm();
    }
    await Promise.all([
      loadRecords({ keepMessages: true }),
      loadTrendRecords({ keepError: true })
    ]);
    successMsg.value = "饮食记录已删除";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    deletingId.value = null;
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

function setHoveredDate(dateKey) {
  hoveredDate.value = dateKey;
}

function clearHoveredDate() {
  hoveredDate.value = null;
}

const trendSeries = computed(() => {
  const map = new Map();
  for (const item of trendRecords.value) {
    const dateKey = toDateKey(item.diningTime);
    if (!dateKey) {
      continue;
    }
    map.set(dateKey, (map.get(dateKey) || 0) + parseCalories(item.estimatedCalories));
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
    ticks: Array.from({ length: 5 }, (_, index) => {
      const ratio = index / 4;
      const value = yMax * (1 - ratio);
      return {
        y: top + ratio * plotHeight,
        value
      };
    }),
    linePath: buildSmoothPath(points),
    areaPath: buildSmoothAreaPath(points, baseY)
  };
});

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
  const width = Math.max(144, label.length * 7.2 + 18);
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

onMounted(async () => {
  await Promise.all([
    loadRecipes(),
    loadRecords(),
    loadTrendRecords()
  ]);
});
</script>

<template>
  <section class="ink-card trend-card">
    <div class="trend-head">
      <div>
        <p class="trend-caption">数据分析</p>
        <h3 class="trend-title">每日卡路里摄入趋势</h3>
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
          <linearGradient id="mealTrendAreaGradient" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stop-color="#cf6b25" stop-opacity="0.26" />
            <stop offset="100%" stop-color="#cf6b25" stop-opacity="0.05" />
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
            stroke="#e5d8cb"
            stroke-dasharray="4 4"
          />
        </g>

        <line
          :x1="chartState.left"
          :y1="chartState.baseY"
          :x2="chartState.xEnd"
          :y2="chartState.baseY"
          stroke="#d7c8ba"
        />

        <path :d="chartState.areaPath" fill="url(#mealTrendAreaGradient)" />
        <path :d="chartState.linePath" fill="none" stroke="#b45a1f" stroke-width="2.6" />

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
          fill="#b45a1f"
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

      <p v-else class="muted">暂无可用于统计的饮食热量数据</p>
    </div>

    <p v-if="trendSeries.length > 0" class="trend-summary">
      当前区间累计摄入 {{ formatKcalNumber(trendTotalKcal) }} kcal，按天自动汇总同日饮食记录热量。
    </p>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <h3>饮食记录列表</h3>
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

    <p class="muted">饮食记录仅支持在食谱页通过“添加记录”创建。</p>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>食物</th>
            <th>餐次</th>
            <th>用餐时间</th>
            <th>摄入份量</th>
            <th>每100g热量</th>
            <th>预计热量</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in records" :key="record.recordId">
            <td>{{ record.recordId }}</td>
            <td class="table-food">
              <strong>{{ record.foodName }}</strong>
              <span class="muted">食谱 #{{ record.recipeId }}</span>
            </td>
            <td>{{ mealTypeText(record.mealType) }}</td>
            <td>{{ formatDateTime(record.diningTime) }}</td>
            <td>{{ formatDecimal(record.intakeAmount) }} g</td>
            <td>{{ formatKcalPer100g(record.caloriesPer100g) }}</td>
            <td>{{ formatKcal(record.estimatedCalories) }}</td>
            <td>{{ record.remark || "--" }}</td>
            <td>
              <div class="inline-actions">
                <button @click="startEdit(record)">编辑</button>
                <button
                  @click="deleteRecord(record.recordId)"
                  :disabled="deletingId === record.recordId"
                >
                  {{ deletingId === record.recordId ? "删除中..." : "删除" }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && records.length === 0">
            <td colspan="9" class="muted">暂无饮食记录</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <section v-if="editingRecordId" class="ink-card">
    <div class="row-head">
      <h3>{{ `编辑记录 #${editingRecordId}` }}</h3>
      <div class="inline-actions">
        <button class="ghost-btn" @click="resetForm" :disabled="submitting">
          取消编辑
        </button>
        <button @click="submitRecord" :disabled="submitting">
          {{ submitting ? "保存中..." : "更新记录" }}
        </button>
      </div>
    </div>

    <div class="recipe-search">
      <input
        v-model.trim="recipeKeyword"
        placeholder="搜索食谱名称"
        @keyup.enter="loadRecipes"
      />
      <button @click="loadRecipes" :disabled="recipeLoading">
        {{ recipeLoading ? "加载中..." : "搜索食谱" }}
      </button>
      <span class="muted">当前可选 {{ recipes.length }} 条食谱</span>
    </div>

    <form class="form-grid" @submit.prevent="submitRecord">
      <label class="full-width">
        食谱
        <select :value="form.recipeId" @change="handleRecipeChange($event.target.value)" required>
          <option value="">请选择食谱</option>
          <option v-for="recipe in recipes" :key="recipe.recipeId" :value="recipe.recipeId">
            {{ recipe.foodName }}（{{ recipe.calories == null ? "--" : `${formatDecimal(recipe.calories)} kcal/100g` }}）
          </option>
        </select>
      </label>

      <label>
        餐次
        <select v-model.number="form.mealType">
          <option v-for="item in mealTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>

      <label>
        用餐时间
        <input type="datetime-local" v-model="form.diningTime" required />
      </label>

      <label>
        摄入份量(g)
        <input type="number" min="0.01" step="0.01" v-model="form.intakeAmount" required />
      </label>

      <label>
        每100g热量
        <input :value="formatKcalPer100g(selectedRecipe?.calories)" readonly />
      </label>

      <label>
        预计热量
        <input :value="formatKcal(estimatedCaloriesPreview)" readonly />
      </label>

      <label>
        参考食用分量
        <input :value="recipeReferencePortion(selectedRecipe)" readonly />
      </label>

      <label class="full-width">
        备注
        <textarea
          v-model.trim="form.remark"
          rows="3"
          maxlength="500"
          placeholder="可选，记录本次用餐说明"
        ></textarea>
      </label>
    </form>

    <div class="recipe-preview-grid" v-if="selectedRecipe">
      <div class="preview-cell">
        <span>当前食谱</span>
        <strong>{{ selectedRecipe.foodName }}</strong>
      </div>
      <div class="preview-cell">
        <span>推荐餐次</span>
        <strong>{{ mealTypeText(selectedRecipe.mealType) }}</strong>
      </div>
      <div class="preview-cell">
        <span>热量规则</span>
        <strong>{{ selectedRecipe.calories == null ? "未设置" : "按每100g换算" }}</strong>
      </div>
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
  color: #8c725e;
  font-size: 13px;
  letter-spacing: 0.06em;
}

.trend-title {
  margin: 8px 0 0;
  font-size: 48px;
  font-weight: 500;
  color: #2f1f14;
}

.trend-range {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.range-btn {
  background: #fff9f3;
  color: #71492f;
  border: 1px solid #dfc4ad;
  border-radius: 10px;
  padding: 7px 14px;
}

.range-btn:hover {
  background: #fdf0e4;
}

.range-btn.active {
  background: #2f1f14;
  border-color: #2f1f14;
  color: #fff;
}

.chart-shell {
  margin-top: 14px;
  border: 2px solid #ebdccd;
  border-radius: 18px;
  padding: 12px 10px 6px;
  background: linear-gradient(180deg, #fffdfa 0%, #fff8f1 100%);
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
  stroke: #a1826c;
  stroke-dasharray: 4 4;
}

.tooltip-box {
  fill: rgba(47, 31, 20, 0.94);
}

.tooltip-text {
  fill: #fff;
  font-size: 12px;
}

.axis-title {
  font-size: 12px;
  fill: #8f7967;
}

.x-label,
.y-label {
  font-size: 11px;
  fill: #8f7967;
}

.trend-summary {
  margin: 10px 0 0;
  color: #7a5d49;
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

.recipe-search {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.full-width {
  grid-column: 1 / -1;
}

.table-food {
  min-width: 140px;
}

.table-food strong,
.table-food span {
  display: block;
}

.recipe-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.preview-cell {
  padding: 14px 16px;
  border: 1px solid rgba(120, 75, 44, 0.18);
  border-radius: 16px;
  background: rgba(255, 248, 241, 0.96);
}

.preview-cell span {
  display: block;
  margin-bottom: 6px;
  color: #876850;
  font-size: 13px;
}

.preview-cell strong {
  color: #2f1f14;
}

@media (max-width: 1100px) {
  .trend-title {
    font-size: 38px;
  }
}

@media (max-width: 820px) {
  .trend-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .trend-title {
    font-size: 30px;
  }
}
</style>
