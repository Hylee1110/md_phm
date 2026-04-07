<!--
  健康趋势：按天数拉取指标序列并可视化（步数、心率、睡眠、血压等）。
-->
<script setup>
import { computed, onMounted, ref } from "vue";
import { healthApi } from "../services/healthApi";

const loading = ref(false);
const errorMsg = ref("");
const queryDays = ref(14);
const metrics = ref([]);

const rangeOptions = [
  { value: 7, label: "近7天" },
  { value: 14, label: "近14天" },
  { value: 30, label: "近30天" }
];

function toNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function formatDecimal(value, digits = 1) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "--";
  }
  return Number.isInteger(number) ? String(number) : number.toFixed(digits).replace(/\.?0+$/, "");
}

function formatValue(value, suffix = "", digits = 1) {
  const number = toNumber(value);
  if (number == null) {
    return "--";
  }
  return `${formatDecimal(number, digits)}${suffix}`;
}

function formatDate(value) {
  if (!value) {
    return "--";
  }
  return String(value);
}

function formatShortDate(value) {
  if (!value) {
    return "--";
  }
  return String(value).slice(5);
}

function sourceTypeText(type) {
  if (type === "manual") return "手动录入";
  if (type === "device") return "设备同步";
  if (type === "platform") return "平台接入";
  if (type === "file") return "文件导入";
  if (type === "mock") return "模拟数据";
  return "未知来源";
}

function sourceNameText(item) {
  const sourceName = String(item?.sourceName ?? "").trim();
  if (sourceName) {
    return sourceName;
  }
  return sourceTypeText(item?.sourceType);
}

function formatBloodPressure(item) {
  if (!item) {
    return "--";
  }
  const systolic = toNumber(item.systolic);
  const diastolic = toNumber(item.diastolic);
  if (systolic == null && diastolic == null) {
    return "--";
  }
  return `${systolic ?? "--"}/${diastolic ?? "--"} mmHg`;
}

const orderedMetrics = computed(() =>
  [...metrics.value].sort((left, right) => String(left.measureDate ?? "").localeCompare(String(right.measureDate ?? "")))
);

const latestMetric = computed(() => {
  const items = orderedMetrics.value;
  return items.length ? items[items.length - 1] : null;
});

const overviewStats = computed(() => [
  { label: "记录天数", value: `${orderedMetrics.value.length}` },
  { label: "查询范围", value: `${queryDays.value} 天` },
  { label: "最新日期", value: formatDate(latestMetric.value?.measureDate) },
  { label: "最新来源", value: sourceNameText(latestMetric.value) }
]);

const latestSnapshots = computed(() => [...orderedMetrics.value].reverse().slice(0, 5));

const hoveredPoint = ref(null);

function buildChartDomain(values, floorAtZero = false) {
  let min = Math.min(...values);
  let max = Math.max(...values);
  if (min === max) {
    const padding = min === 0 ? 1 : Math.max(Math.abs(min) * 0.08, 1);
    min -= padding;
    max += padding;
  } else {
    const padding = (max - min) * 0.12;
    min -= padding;
    max += padding;
  }
  if (floorAtZero && min > 0) {
    min = 0;
  }
  return { min, max };
}

function pickLabelIndexes(length) {
  if (!length) {
    return [];
  }
  return [
    ...new Set([
      0,
      Math.floor((length - 1) * 0.25),
      Math.floor((length - 1) * 0.5),
      Math.floor((length - 1) * 0.75),
      length - 1
    ])
  ];
}

function buildSmoothPath(points) {
  if (!points.length) {
    return "";
  }
  if (points.length === 1) {
    return `M ${points[0].x} ${points[0].y}`;
  }

  const smoothing = 0.18;

  function controlPoint(current, previous, next, reverse = false) {
    const source = previous || current;
    const target = next || current;
    const angle = Math.atan2(target.y - source.y, target.x - source.x) + (reverse ? Math.PI : 0);
    const length = Math.hypot(target.x - source.x, target.y - source.y) * smoothing;
    return {
      x: current.x + Math.cos(angle) * length,
      y: current.y + Math.sin(angle) * length
    };
  }

  return points.reduce((path, point, index, list) => {
    if (index === 0) {
      return `M ${point.x} ${point.y}`;
    }
    const previous = list[index - 1];
    const previousPrevious = list[index - 2];
    const next = list[index + 1];
    const cps = controlPoint(previous, previousPrevious, point, false);
    const cpe = controlPoint(point, previous, next, true);
    return `${path} C ${cps.x} ${cps.y}, ${cpe.x} ${cpe.y}, ${point.x} ${point.y}`;
  }, "");
}

function buildSmoothAreaPath(points, baselineY) {
  if (points.length < 2) {
    return "";
  }
  const linePath = buildSmoothPath(points);
  const first = points[0];
  const last = points[points.length - 1];
  return `M ${first.x} ${baselineY} L ${first.x} ${first.y} ${linePath.slice(linePath.indexOf("C"))} L ${last.x} ${baselineY} Z`;
}

function buildPointMeta(chartId, chart, point, label, value, color, extra = "") {
  return {
    chartId,
    label,
    value,
    color,
    extra,
    left: `${(point.x / chart.width) * 100}%`,
    top: `${(point.y / chart.height) * 100}%`
  };
}

function showTooltip(payload) {
  hoveredPoint.value = payload;
}

function hideTooltip(chartId) {
  if (!hoveredPoint.value || hoveredPoint.value.chartId !== chartId) {
    return;
  }
  hoveredPoint.value = null;
}

function buildSingleLineChart(items, getter, { floorAtZero = true } = {}) {
  const values = items.map((item) => toNumber(getter(item))).filter((value) => value != null);
  if (!values.length) {
    return null;
  }

  const width = 640;
  const height = 220;
  const paddingLeft = 44;
  const paddingRight = 18;
  const paddingTop = 18;
  const paddingBottom = 34;
  const innerWidth = width - paddingLeft - paddingRight;
  const innerHeight = height - paddingTop - paddingBottom;
  const { min, max } = buildChartDomain(values, floorAtZero);
  const baselineY = height - paddingBottom;

  const points = items
    .map((item, index) => {
      const value = toNumber(getter(item));
      if (value == null) {
        return null;
      }
      const x = items.length === 1 ? paddingLeft + innerWidth / 2 : paddingLeft + (innerWidth * index) / (items.length - 1);
      const y = paddingTop + ((max - value) * innerHeight) / (max - min);
      return {
        x,
        y,
        value,
        label: formatShortDate(item.measureDate),
        measureDate: item.measureDate,
        item
      };
    })
    .filter(Boolean);

  const ticks = Array.from({ length: 4 }, (_, index) => {
    const ratio = index / 3;
    const value = max - (max - min) * ratio;
    return {
      y: paddingTop + innerHeight * ratio,
      label: formatDecimal(value)
    };
  });

  const labels = pickLabelIndexes(items.length).map((index) => {
    const x = items.length === 1 ? paddingLeft + innerWidth / 2 : paddingLeft + (innerWidth * index) / (items.length - 1);
    return {
      x,
      label: formatShortDate(items[index]?.measureDate)
    };
  });

  const polyline = points.map((point) => `${point.x},${point.y}`).join(" ");
  const path = buildSmoothPath(points);
  const areaPath = buildSmoothAreaPath(points, baselineY);

  return {
    width,
    height,
    baselineY,
    ticks,
    labels,
    points,
    path,
    polyline,
    areaPath
  };
}

function buildDualLineChart(items, leftGetter, rightGetter) {
  const values = items.flatMap((item) => {
    const left = toNumber(leftGetter(item));
    const right = toNumber(rightGetter(item));
    return [left, right].filter((value) => value != null);
  });
  if (!values.length) {
    return null;
  }

  const width = 960;
  const height = 260;
  const paddingLeft = 44;
  const paddingRight = 20;
  const paddingTop = 18;
  const paddingBottom = 34;
  const innerWidth = width - paddingLeft - paddingRight;
  const innerHeight = height - paddingTop - paddingBottom;
  const { min, max } = buildChartDomain(values, false);

  function buildPoints(getter) {
    return items
      .map((item, index) => {
        const value = toNumber(getter(item));
        if (value == null) {
          return null;
        }
        const x = items.length === 1 ? paddingLeft + innerWidth / 2 : paddingLeft + (innerWidth * index) / (items.length - 1);
        const y = paddingTop + ((max - value) * innerHeight) / (max - min);
        return {
          x,
          y,
          value,
          measureDate: item.measureDate,
          item
        };
      })
      .filter(Boolean);
  }

  const systolicPoints = buildPoints(leftGetter);
  const diastolicPoints = buildPoints(rightGetter);
  const ticks = Array.from({ length: 4 }, (_, index) => {
    const ratio = index / 3;
    const value = max - (max - min) * ratio;
    return {
      y: paddingTop + innerHeight * ratio,
      label: formatDecimal(value)
    };
  });

  const labels = pickLabelIndexes(items.length).map((index) => {
    const x = items.length === 1 ? paddingLeft + innerWidth / 2 : paddingLeft + (innerWidth * index) / (items.length - 1);
    return {
      x,
      label: formatShortDate(items[index]?.measureDate)
    };
  });

  return {
    width,
    height,
    ticks,
    labels,
    systolicPoints,
    systolicPath: buildSmoothPath(systolicPoints),
    systolicPolyline: systolicPoints.map((point) => `${point.x},${point.y}`).join(" "),
    diastolicPoints,
    diastolicPath: buildSmoothPath(diastolicPoints),
    diastolicPolyline: diastolicPoints.map((point) => `${point.x},${point.y}`).join(" ")
  };
}

function findLatestBy(items, getter) {
  for (let index = items.length - 1; index >= 0; index -= 1) {
    const value = toNumber(getter(items[index]));
    if (value != null) {
      return {
        item: items[index],
        value
      };
    }
  }
  return null;
}

function describeDelta(items, getter, suffix = "", digits = 1) {
  const values = items
    .map((item) => ({ item, value: toNumber(getter(item)) }))
    .filter((item) => item.value != null);
  if (values.length < 2) {
    return "当前时间范围内记录较少，先持续积累数据。";
  }
  const start = values[0].value;
  const end = values[values.length - 1].value;
  const delta = end - start;
  if (Math.abs(delta) < 0.01) {
    return `与 ${formatDate(values[0].item.measureDate)} 相比基本持平。`;
  }
  return delta > 0
    ? `较 ${formatDate(values[0].item.measureDate)} 上升 ${formatDecimal(delta, digits)}${suffix}。`
    : `较 ${formatDate(values[0].item.measureDate)} 下降 ${formatDecimal(Math.abs(delta), digits)}${suffix}。`;
}

const trendCards = computed(() => {
  const items = orderedMetrics.value;
  const configs = [
    {
      id: "steps",
      title: "步数",
      subtitle: "活动量",
      color: "#315efb",
      fill: "rgba(49, 94, 251, 0.14)",
      getter: (item) => item.steps,
      suffix: " 步",
      digits: 0
    },
    {
      id: "heart-rate",
      title: "静息心率",
      subtitle: "恢复状态",
      color: "#d74d55",
      fill: "rgba(215, 77, 85, 0.14)",
      getter: (item) => item.restingHeartRate,
      suffix: " bpm",
      digits: 0
    },
    {
      id: "sleep",
      title: "睡眠时长",
      subtitle: "休息质量",
      color: "#2f9d88",
      fill: "rgba(47, 157, 136, 0.14)",
      getter: (item) => item.sleepHours,
      suffix: " h",
      digits: 1
    },
    {
      id: "stress",
      title: "压力指数",
      subtitle: "身心负荷",
      color: "#9b6bce",
      fill: "rgba(155, 107, 206, 0.14)",
      getter: (item) => item.stressLevel,
      suffix: "",
      digits: 0
    }
  ];

  return configs
    .map((config) => {
      const latest = findLatestBy(items, config.getter);
      const chart = buildSingleLineChart(items, config.getter);
      if (!chart) {
        return null;
      }
      return {
        ...config,
        latestValue: latest ? formatValue(latest.value, config.suffix, config.digits) : "--",
        latestDate: latest ? formatDate(latest.item.measureDate) : "--",
        copy: describeDelta(items, config.getter, config.suffix, config.digits),
        chart,
        style: {
          "--trend-accent": config.color,
          "--trend-fill": config.fill
        }
      };
    })
    .filter(Boolean);
});

const bloodPressureCard = computed(() => {
  const items = orderedMetrics.value;
  const chart = buildDualLineChart(items, (item) => item.systolic, (item) => item.diastolic);
  if (!chart) {
    return null;
  }

  let latest = null;
  for (let index = items.length - 1; index >= 0; index -= 1) {
    const systolic = toNumber(items[index].systolic);
    const diastolic = toNumber(items[index].diastolic);
    if (systolic != null || diastolic != null) {
      latest = items[index];
      break;
    }
  }

  return {
    chart,
    latestValue: formatBloodPressure(latest),
    latestDate: formatDate(latest?.measureDate),
    copy: "收缩压与舒张压共用一张折线图，便于观察波动和间距变化。"
  };
});

async function loadMetrics() {
  loading.value = true;
  errorMsg.value = "";
  try {
    // 后端按「最近 N 天」返回每日一条聚合指标（步数、心率、睡眠、血压等）
    metrics.value = await healthApi.getMetrics(queryDays.value);
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

function changeRange(days) {
  if (queryDays.value === days && metrics.value.length) {
    return;
  }
  queryDays.value = days;
  // 切换天数后重新拉序列；已有数据且天数未变则跳过避免无意义请求
  loadMetrics();
}

onMounted(loadMetrics);
</script>

<template>
  <section class="ink-card trends-shell">
    <div class="trends-head">
      <div>
        <h3>趋势总览</h3>
        <p class="muted">用折线图查看最近一段时间的步数、静息心率、睡眠、血压和压力变化。</p>
      </div>
      <div class="trends-actions">
        <div class="range-tabs">
          <button
            v-for="item in rangeOptions"
            :key="item.value"
            type="button"
            class="range-tab"
            :class="{ active: queryDays === item.value }"
            @click="changeRange(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <button type="button" @click="loadMetrics" :disabled="loading">
          {{ loading ? "刷新中..." : "刷新数据" }}
        </button>
      </div>
    </div>

    <p v-if="loading" class="muted">正在生成趋势图...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>

    <template v-else-if="orderedMetrics.length">
      <div class="overview-grid">
        <article v-for="item in overviewStats" :key="item.label" class="overview-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>

      <div class="trend-grid">
        <article
          v-for="card in trendCards"
          :key="card.id"
          class="trend-card"
          :style="card.style"
        >
          <div class="trend-card-head">
            <div>
              <p class="trend-card-tag">{{ card.subtitle }}</p>
              <h4>{{ card.title }}</h4>
            </div>
            <div class="trend-value">
              <strong>{{ card.latestValue }}</strong>
              <span>{{ card.latestDate }}</span>
            </div>
          </div>

          <div class="trend-chart-box">
            <div class="trend-chart-wrap" @mouseleave="hideTooltip(card.id)">
              <svg :viewBox="`0 0 ${card.chart.width} ${card.chart.height}`" class="trend-chart" preserveAspectRatio="none">
                <g v-for="tick in card.chart.ticks" :key="`${card.id}-${tick.y}`">
                  <line
                    :x1="44"
                    :x2="card.chart.width - 18"
                    :y1="tick.y"
                    :y2="tick.y"
                    class="chart-grid"
                  />
                  <text :x="14" :y="tick.y + 4" class="chart-tick">{{ tick.label }}</text>
                </g>

                <path v-if="card.chart.areaPath" :d="card.chart.areaPath" class="chart-area" />
                <path :d="card.chart.path" class="chart-line" />

                <circle
                  v-for="point in card.chart.points"
                  :key="`${card.id}-${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="4.5"
                  class="chart-point"
                />
                <circle
                  v-for="point in card.chart.points"
                  :key="`${card.id}-hit-${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="10"
                  class="chart-hit-point"
                  @mouseenter="showTooltip(buildPointMeta(card.id, card.chart, point, formatDate(point.measureDate), `${card.title} ${formatValue(point.value, card.suffix, card.digits)}`, card.color, sourceNameText(point.item)))"
                />

                <text
                  v-for="label in card.chart.labels"
                  :key="`${card.id}-${label.x}`"
                  :x="label.x"
                  :y="card.chart.height - 10"
                  text-anchor="middle"
                  class="chart-label"
                >
                  {{ label.label }}
                </text>
              </svg>

              <div
                v-if="hoveredPoint?.chartId === card.id"
                class="chart-tooltip"
                :style="{ left: hoveredPoint.left, top: hoveredPoint.top, '--tooltip-color': hoveredPoint.color }"
              >
                <strong>{{ hoveredPoint.label }}</strong>
                <p>{{ hoveredPoint.value }}</p>
                <span>{{ hoveredPoint.extra }}</span>
              </div>
            </div>
          </div>

          <p class="trend-card-copy">{{ card.copy }}</p>
        </article>

        <article v-if="bloodPressureCard" class="trend-card trend-card-wide blood-card">
          <div class="trend-card-head">
            <div>
              <p class="trend-card-tag">双轴对照</p>
              <h4>血压变化</h4>
            </div>
            <div class="trend-value">
              <strong>{{ bloodPressureCard.latestValue }}</strong>
              <span>{{ bloodPressureCard.latestDate }}</span>
            </div>
          </div>

          <div class="trend-legend">
            <span><i class="legend-dot systolic-dot"></i>收缩压</span>
            <span><i class="legend-dot diastolic-dot"></i>舒张压</span>
          </div>

          <div class="trend-chart-box trend-chart-box-wide">
            <div class="trend-chart-wrap" @mouseleave="hideTooltip('blood-pressure')">
              <svg
                :viewBox="`0 0 ${bloodPressureCard.chart.width} ${bloodPressureCard.chart.height}`"
                class="trend-chart"
                preserveAspectRatio="none"
              >
                <g v-for="tick in bloodPressureCard.chart.ticks" :key="`pressure-${tick.y}`">
                  <line
                    :x1="44"
                    :x2="bloodPressureCard.chart.width - 20"
                    :y1="tick.y"
                    :y2="tick.y"
                    class="chart-grid"
                  />
                  <text :x="14" :y="tick.y + 4" class="chart-tick">{{ tick.label }}</text>
                </g>

                <path :d="bloodPressureCard.chart.systolicPath" class="chart-line line-systolic" />
                <path :d="bloodPressureCard.chart.diastolicPath" class="chart-line line-diastolic" />

                <circle
                  v-for="point in bloodPressureCard.chart.systolicPoints"
                  :key="`sys-${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="4"
                  class="chart-point point-systolic"
                />
                <circle
                  v-for="point in bloodPressureCard.chart.diastolicPoints"
                  :key="`dia-${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="4"
                  class="chart-point point-diastolic"
                />
                <circle
                  v-for="point in bloodPressureCard.chart.systolicPoints"
                  :key="`sys-hit-${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="10"
                  class="chart-hit-point"
                  @mouseenter="showTooltip(buildPointMeta('blood-pressure', bloodPressureCard.chart, point, formatDate(point.measureDate), `收缩压 ${formatValue(point.value, ' mmHg', 0)}`, '#315efb', sourceNameText(point.item)))"
                />
                <circle
                  v-for="point in bloodPressureCard.chart.diastolicPoints"
                  :key="`dia-hit-${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="10"
                  class="chart-hit-point"
                  @mouseenter="showTooltip(buildPointMeta('blood-pressure', bloodPressureCard.chart, point, formatDate(point.measureDate), `舒张压 ${formatValue(point.value, ' mmHg', 0)}`, '#2f9d88', sourceNameText(point.item)))"
                />

                <text
                  v-for="label in bloodPressureCard.chart.labels"
                  :key="`pressure-label-${label.x}`"
                  :x="label.x"
                  :y="bloodPressureCard.chart.height - 10"
                  text-anchor="middle"
                  class="chart-label"
                >
                  {{ label.label }}
                </text>
              </svg>

              <div
                v-if="hoveredPoint?.chartId === 'blood-pressure'"
                class="chart-tooltip"
                :style="{ left: hoveredPoint.left, top: hoveredPoint.top, '--tooltip-color': hoveredPoint.color }"
              >
                <strong>{{ hoveredPoint.label }}</strong>
                <p>{{ hoveredPoint.value }}</p>
                <span>{{ hoveredPoint.extra }}</span>
              </div>
            </div>
          </div>

          <p class="trend-card-copy">{{ bloodPressureCard.copy }}</p>
        </article>
      </div>

      <section class="snapshot-shell">
        <div class="snapshot-head">
          <h4>最近记录</h4>
          <p class="muted">快速查看最近几天的综合指标与数据来源。</p>
        </div>
        <div class="snapshot-grid">
          <article v-for="item in latestSnapshots" :key="item.measureDate" class="snapshot-card">
            <div class="snapshot-top">
              <strong>{{ formatDate(item.measureDate) }}</strong>
              <span>{{ sourceNameText(item) }}</span>
            </div>
            <p>步数 {{ formatValue(item.steps, " 步", 0) }}</p>
            <p>静息心率 {{ formatValue(item.restingHeartRate, " bpm", 0) }}</p>
            <p>睡眠 {{ formatValue(item.sleepHours, " h", 1) }}</p>
            <p>血压 {{ formatBloodPressure(item) }}</p>
          </article>
        </div>
      </section>
    </template>

    <div v-else class="empty-state">
      <h4>暂无趋势数据</h4>
      <p>先在数据源管理中导入健康指标 CSV，趋势页才会生成折线图。</p>
    </div>
  </section>
</template>

<style scoped>
.trends-shell {
  padding-top: 24px;
}

.trends-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.trends-head h3,
.snapshot-head h4 {
  margin: 0;
  font-size: 24px;
  letter-spacing: 0.06em;
}

.trends-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.range-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.range-tab {
  min-width: 88px;
  min-height: 42px;
  border-radius: 12px;
  background: #f2f4f6;
  color: #586068;
  font-size: 14px;
}

.range-tab.active {
  background: #121518;
  color: #fff;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.overview-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid #e4e6e8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfa 100%);
}

.overview-card span {
  color: #778089;
  font-size: 13px;
}

.overview-card strong {
  display: block;
  margin-top: 10px;
  font-size: 26px;
  color: #1c2024;
}

.trend-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.trend-card {
  --trend-accent: #1f2326;
  --trend-fill: rgba(20, 24, 28, 0.1);
  padding: 20px;
  border-radius: 24px;
  border: 1px solid #e5e7ea;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbf9 100%);
  box-shadow: 0 14px 30px rgba(16, 18, 20, 0.05);
}

.trend-card-wide {
  grid-column: 1 / -1;
}

.trend-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.trend-card-tag {
  margin: 0;
  color: #81878c;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.trend-card-head h4 {
  margin: 10px 0 0;
  font-size: 30px;
  font-weight: 600;
}

.trend-value {
  text-align: right;
}

.trend-value strong {
  display: block;
  font-size: 26px;
  color: var(--trend-accent);
}

.trend-value span {
  display: block;
  margin-top: 6px;
  color: #81878c;
  font-size: 13px;
}

.trend-chart-box {
  margin-top: 18px;
  padding: 12px 12px 8px;
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(248, 249, 250, 0.96) 100%),
    radial-gradient(circle at top right, var(--trend-fill), transparent 42%);
  border: 1px solid #eceef1;
}

.trend-chart-wrap {
  position: relative;
}

.trend-chart-box-wide {
  padding-inline: 10px;
}

.trend-chart {
  width: 100%;
  height: 220px;
  display: block;
}

.blood-card .trend-chart {
  height: 260px;
}

.chart-grid {
  stroke: #e7eaee;
  stroke-width: 1;
}

.chart-tick,
.chart-label {
  fill: #8a9095;
  font-size: 12px;
}

.chart-area {
  fill: var(--trend-fill);
}

.chart-line {
  fill: none;
  stroke: var(--trend-accent);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chart-point {
  fill: #fff;
  stroke: var(--trend-accent);
  stroke-width: 2;
}

.chart-hit-point {
  fill: transparent;
  stroke: transparent;
  cursor: pointer;
}

.chart-tooltip {
  position: absolute;
  min-width: 132px;
  max-width: 200px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(17, 19, 21, 0.94);
  color: #fff;
  box-shadow: 0 16px 30px rgba(10, 12, 14, 0.2);
  transform: translate(-50%, calc(-100% - 14px));
  pointer-events: none;
  z-index: 2;
}

.chart-tooltip::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -6px;
  width: 12px;
  height: 12px;
  background: rgba(17, 19, 21, 0.94);
  transform: translateX(-50%) rotate(45deg);
}

.chart-tooltip strong {
  display: block;
  color: var(--tooltip-color, #fff);
  font-size: 13px;
}

.chart-tooltip p,
.chart-tooltip span {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
}

.chart-tooltip span {
  display: block;
  color: rgba(255, 255, 255, 0.78);
}

.trend-card-copy {
  margin: 14px 0 0;
  color: #61686f;
  line-height: 1.7;
}

.trend-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 14px;
  color: #60676e;
  font-size: 14px;
}

.trend-legend span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.systolic-dot {
  background: #315efb;
}

.diastolic-dot {
  background: #2f9d88;
}

.line-systolic {
  stroke: #315efb;
}

.line-diastolic {
  stroke: #2f9d88;
}

.point-systolic {
  stroke: #315efb;
}

.point-diastolic {
  stroke: #2f9d88;
}

.snapshot-shell {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #eceef1;
}

.snapshot-head p {
  margin: 8px 0 0;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.snapshot-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e4e6e8;
  background: #fff;
}

.snapshot-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.snapshot-top strong {
  font-size: 18px;
}

.snapshot-top span {
  color: #7b8289;
  font-size: 13px;
  text-align: right;
}

.snapshot-card p {
  margin: 10px 0 0;
  color: #5e656c;
}

@media (max-width: 980px) {
  .trends-head,
  .trend-card-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .trends-actions,
  .trend-value {
    width: 100%;
    justify-content: flex-start;
    text-align: left;
  }

  .trend-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .trends-actions,
  .range-tabs {
    width: 100%;
  }

  .range-tab,
  .trends-actions > button {
    width: 100%;
  }

  .overview-grid,
  .snapshot-grid {
    grid-template-columns: 1fr;
  }

  .trend-card {
    padding: 18px 16px;
  }

  .trend-card-head h4 {
    font-size: 26px;
  }
}
</style>
