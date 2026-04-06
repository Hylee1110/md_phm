<script setup>
import { onMounted, ref } from "vue";
import { dataSourceApi } from "../services/dataSourceApi";
import { healthApi } from "../services/healthApi";

const loading = ref(false);
const errorMsg = ref("");
const dashboard = ref(null);
const syncOverview = ref(null);

function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return String(value).replace("T", " ");
}

function displayValue(value, suffix = "") {
  if (value == null || value === "") {
    return "--";
  }
  return `${value}${suffix}`;
}

function displayBloodPressure(metric) {
  if (!metric) {
    return "--";
  }
  const systolic = metric.systolic;
  const diastolic = metric.diastolic;
  if (systolic == null && diastolic == null) {
    return "--";
  }
  return `${systolic ?? "--"}/${diastolic ?? "--"}`;
}

function metricSourceLabel(metric) {
  if (!metric) {
    return "--";
  }
  const sourceName = String(metric.sourceName ?? "").trim();
  const sourceType = String(metric.sourceType ?? "").trim();
  if (sourceName) {
    return sourceName;
  }
  if (!sourceType) {
    return "未知来源";
  }
  if (sourceType === "manual") return "手动录入";
  if (sourceType === "device") return "设备同步";
  if (sourceType === "platform") return "第三方平台";
  if (sourceType === "file") return "文件导入";
  return sourceType;
}

function syncStatusLabel(status) {
  if (status === 1) return "部分成功";
  if (status === 2) return "失败";
  if (status === 3) return "执行中";
  return "成功";
}

async function loadDashboard() {
  loading.value = true;
  errorMsg.value = "";
  try {
    const [dashboardData, overviewData] = await Promise.all([
      healthApi.getDashboard(),
      dataSourceApi.getOverview()
    ]);
    dashboard.value = dashboardData;
    syncOverview.value = overviewData;
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>

<template>
  <section class="ink-card">
    <div class="row-head">
      <h3>今日健康指标</h3>
      <button @click="loadDashboard">刷新</button>
    </div>
    <p v-if="loading" class="muted">正在加载健康总览...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>

    <template v-else-if="dashboard?.latestMetric">
      <div class="metric-grid">
        <article class="metric-cell">
          <span>步数</span>
          <strong>{{ displayValue(dashboard.latestMetric.steps) }}</strong>
        </article>
        <article class="metric-cell">
          <span>静息心率</span>
          <strong>{{ displayValue(dashboard.latestMetric.restingHeartRate, " bpm") }}</strong>
        </article>
        <article class="metric-cell">
          <span>睡眠时长</span>
          <strong>{{ displayValue(dashboard.latestMetric.sleepHours, " h") }}</strong>
        </article>
        <article class="metric-cell">
          <span>血压</span>
          <strong>{{ displayBloodPressure(dashboard.latestMetric) }}</strong>
        </article>
      </div>

      <div class="ink-note">
        <h4>数据来源</h4>
        <p>{{ metricSourceLabel(dashboard.latestMetric) }}</p>
        <p>{{ dashboard.latestMetric.measureDate }}</p>
      </div>

      <div class="ink-note">
        <h4>健康建议</h4>
        <p>{{ dashboard.advice }}</p>
      </div>
    </template>

    <div v-else class="empty-state">
      <h4>暂无健康指标数据</h4>
      <p>可以前往“数据源管理”页面导入 CSV 数据后，再回来查看健康总览。</p>
    </div>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <h3>同步概览</h3>
    </div>

    <div class="metric-grid">
      <article class="metric-cell">
        <span>数据源总数</span>
        <strong>{{ syncOverview?.totalSources ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>启用中的数据源</span>
        <strong>{{ syncOverview?.activeSources ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>已导入指标天数</span>
        <strong>{{ syncOverview?.importedMetricDays ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>同步任务数</span>
        <strong>{{ syncOverview?.totalTasks ?? 0 }}</strong>
      </article>
    </div>

    <div class="ink-note" v-if="syncOverview?.latestTask">
      <h4>最近任务</h4>
      <p>来源：{{ syncOverview.latestTask.sourceName || "--" }}</p>
      <p>状态：{{ syncStatusLabel(syncOverview.latestTask.taskStatus) }}</p>
      <p>{{ formatDateTime(syncOverview.latestTask.finishedTime || syncOverview.latestTask.startedTime) }}</p>
      <p>{{ syncOverview.latestTask.summaryMessage || "--" }}</p>
    </div>
    <div v-else class="ink-note">
      <h4>暂无同步任务</h4>
      <p>请先创建一个文件类型数据源，并上传第一份 CSV，激活多源数据流程。</p>
    </div>
  </section>
</template>

<style scoped>
.empty-state {
  padding: 24px 0 4px;
  color: #69717a;
}

.empty-state h4 {
  margin: 0 0 8px;
  color: #24282c;
  font-size: 24px;
}
</style>
