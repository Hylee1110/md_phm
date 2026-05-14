<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import Papa from "papaparse";
import DataSourceEditDialog from "../components/DataSourceEditDialog.vue";
import { dataSourceApi } from "../services/dataSourceApi";

const loading = ref(false), uploading = ref(false), inspectingCsv = ref(false);
const statusChangingId = ref(null), deletingSourceId = ref(null), sourceDialogOpen = ref(false);
const errorMsg = ref(""), successMsg = ref(""), overview = ref(null), sources = ref([]), tasks = ref([]);
const selectedSourceId = ref(null), importCategory = ref("health_metric"), selectedFile = ref(null), editingSource = ref(null);
const previewHeaders = ref([]), previewRows = ref([]), csvCheck = ref({ headerCount: 0, dataRowCount: 0, missingHeaders: [], parseErrors: [] });
const watchLoading = ref(false), watchError = ref(""), watchSnapshot = ref(emptyWatch());
let watchTimer = null;

const importConfigs = {
  health_metric: { label: "健康指标 CSV", headers: ["measureDate", "steps", "restingHeartRate", "sleepHours", "systolic", "diastolic", "stressLevel"], rows: [["2026-03-15", "8650", "68", "7.2", "121", "78", "46"]] },
  exercise_record: { label: "运动记录 CSV", headers: ["recordTime", "sportName", "durationMin", "caloriesKcal", "note", "externalId"], rows: [["2026-03-15 18:30", "慢跑", "45", "320", "晚间慢跑", "run-001"]] }
};

const selectedSource = computed(() => sources.value.find((item) => item.sourceId === selectedSourceId.value) || null);
const isFileSource = computed(() => selectedSource.value?.sourceType === "file");
const isDeviceSource = computed(() => selectedSource.value?.sourceType === "device");
const currentImportConfig = computed(() => importConfigs[importCategory.value]);
const canSubmitImport = computed(() => Boolean(selectedFile.value && isFileSource.value && !uploading.value && !inspectingCsv.value && csvCheck.value.dataRowCount > 0 && !csvCheck.value.missingHeaders.length && !csvCheck.value.parseErrors.length));
const watchData = computed(() => isDeviceSource.value && Number(watchSnapshot.value.sourceId) === Number(selectedSource.value?.sourceId) && watchSnapshot.value.receivedAt > 0 ? watchSnapshot.value : null);

function emptyWatch() { return { sourceId: null, heartRate: 0, avgHeartRate: 0, restingHeartRate: null, maxHeartRate: 0, mappedRestingHeartRate: null, stressLevel: null, bodyBattery: null, sleepDuration: null, mappedSleepHours: null, steps: 0, calories: 0, distanceCm: 0, device: "Unknown", timestamp: 0, receivedAt: 0, measureDate: null, rawQuery: {} }; }
function clearMessages() { errorMsg.value = ""; successMsg.value = ""; }
function clearCsv() { selectedFile.value = null; previewHeaders.value = []; previewRows.value = []; csvCheck.value = { headerCount: 0, dataRowCount: 0, missingHeaders: [], parseErrors: [] }; }
function formatDateTime(value) { return value ? String(value).replace("T", " ") : "--"; }
function formatMs(value) { return value ? new Date(Number(value)).toLocaleString("zh-CN") : "--"; }
function formatTs(value) { return value ? new Date((Number(value) > 1e12 ? Number(value) : Number(value) * 1000)).toLocaleString("zh-CN") : "--"; }
function formatDistance(value) { return Number.isFinite(Number(value)) ? `${(Number(value) / 100000).toFixed(2)} km` : "--"; }
function sourceTypeText(type) { return type === "device" ? "设备同步" : type === "manual" ? "手动录入" : type === "platform" ? "第三方平台" : "文件导入"; }
function sourceStatusText(status) { return status === 1 ? "已暂停" : status === 2 ? "异常" : "启用中"; }
function statusClass(status) { return status === 1 ? "badge-warn" : status === 2 ? "badge-error" : "badge-ok"; }
function taskStatusText(status) { return status === 1 ? "部分成功" : status === 2 ? "失败" : status === 3 ? "执行中" : "成功"; }
function taskClass(status) { return status === 1 ? "badge-warn" : status === 2 ? "badge-error" : status === 3 ? "badge-neutral" : "badge-ok"; }
function taskCategoryText(category) { return category === "exercise_record" ? "运动记录" : category === "health_metric" ? "健康指标" : "未分类"; }
function actionText(source) { return source.sourceType === "device" ? "查看手表数据" : source.sourceType === "file" ? "打开导入面板" : "查看详情"; }
function normalizeHeader(raw) { return String(raw ?? "").trim().toLowerCase().replace(/[-\s]/g, "_"); }
function canonicalHeader(raw) {
  const h = normalizeHeader(raw);
  if (["measuredate", "measure_date", "date", "日期", "测量日期"].includes(h)) return "measureDate";
  if (["steps", "step", "步数"].includes(h)) return "steps";
  if (["restingheartrate", "resting_heart_rate", "restinghr", "heart_rate", "静息心率", "心率"].includes(h)) return "restingHeartRate";
  if (["sleephours", "sleep_hours", "sleep", "睡眠时长", "睡眠"].includes(h)) return "sleepHours";
  if (["systolic", "收缩压"].includes(h)) return "systolic";
  if (["diastolic", "舒张压"].includes(h)) return "diastolic";
  if (["stresslevel", "stress_level", "stress", "压力指数", "压力"].includes(h)) return "stressLevel";
  if (["recordtime", "record_time", "datetime", "记录时间", "运动时间", "开始时间"].includes(h)) return "recordTime";
  if (["recorddate", "record_date", "date", "记录日期", "运动日期"].includes(h)) return "recordDate";
  if (["sportname", "sport_name", "sport", "课程名称", "运动名称", "项目名称"].includes(h)) return "sportName";
  if (["sportid", "sport_id", "courseid", "course_id", "课程id", "运动id"].includes(h)) return "sportId";
  if (["durationmin", "duration_min", "duration", "minutes", "时长", "分钟"].includes(h)) return "durationMin";
  return null;
}
function buildTemplateCsv() { return Papa.unparse({ fields: currentImportConfig.value.headers, data: currentImportConfig.value.rows }); }
function normalizeWatch(raw) {
  const data = raw && typeof raw === "object" ? raw : {};
  const num = (value) => Number.isFinite(Number(value)) ? Number(value) : 0;
  return { sourceId: data.sourceId == null ? null : Math.floor(num(data.sourceId)), heartRate: Math.floor(num(data.heartRate)), avgHeartRate: Math.floor(num(data.avgHeartRate)), restingHeartRate: data.restingHeartRate == null ? null : Math.floor(num(data.restingHeartRate)), maxHeartRate: Math.floor(num(data.maxHeartRate)), mappedRestingHeartRate: data.mappedRestingHeartRate == null ? null : Math.floor(num(data.mappedRestingHeartRate)), stressLevel: data.stressLevel == null ? null : Math.floor(num(data.stressLevel)), bodyBattery: data.bodyBattery == null ? null : Math.floor(num(data.bodyBattery)), sleepDuration: data.sleepDuration == null ? null : Math.floor(num(data.sleepDuration)), mappedSleepHours: data.mappedSleepHours == null ? null : Number(data.mappedSleepHours), steps: Math.floor(num(data.steps)), calories: Math.floor(num(data.calories)), distanceCm: Math.floor(num(data.distanceCm)), device: data.device || "Unknown", timestamp: Math.floor(num(data.timestamp)), receivedAt: Math.floor(num(data.receivedAt)), measureDate: data.measureDate || null, rawQuery: data.rawQuery && typeof data.rawQuery === "object" ? data.rawQuery : {} };
}
function pickDefaultSource(preferredId = null) {
  if (!sources.value.length) return (selectedSourceId.value = null);
  if (preferredId != null && sources.value.some((item) => item.sourceId === preferredId)) return (selectedSourceId.value = preferredId);
  if (selectedSourceId.value != null && sources.value.some((item) => item.sourceId === selectedSourceId.value)) return;
  selectedSourceId.value = (sources.value.find((item) => item.sourceType === "device") || sources.value.find((item) => item.sourceType === "file") || sources.value[0]).sourceId;
}

async function loadPage({ keepMessages = false, preferredSourceId = null } = {}) {
  loading.value = true;
  if (!keepMessages) clearMessages();
  try {
    const [overviewData, sourceData, taskData] = await Promise.all([dataSourceApi.getOverview(), dataSourceApi.listSources(), dataSourceApi.listSyncTasks({ limit: 12 })]);
    overview.value = overviewData; sources.value = sourceData; tasks.value = taskData; pickDefaultSource(preferredSourceId);
  } catch (error) { errorMsg.value = error.message; } finally { loading.value = false; }
}

async function loadWatchData({ silent = false } = {}) {
  if (!isDeviceSource.value) return;
  if (!silent) watchLoading.value = true;
  try { watchSnapshot.value = normalizeWatch(await dataSourceApi.getWatchStreamLatest()); watchError.value = ""; }
  catch (error) { watchError.value = error.message; }
  finally { if (!silent) watchLoading.value = false; }
}

function startWatchPolling() { stopWatchPolling(); if (!isDeviceSource.value) return; void loadWatchData(); watchTimer = window.setInterval(() => void loadWatchData({ silent: true }), 3000); }
function stopWatchPolling() { if (watchTimer != null) { window.clearInterval(watchTimer); watchTimer = null; } }
function handleFileChange(event) {
  const file = event.target.files?.[0] || null; event.target.value = ""; selectedFile.value = file; clearMessages();
  if (!file) return clearCsv();
  inspectingCsv.value = true; previewHeaders.value = []; previewRows.value = [];
  Papa.parse(file, {
    skipEmptyLines: "greedy",
    complete(results) {
      const rows = Array.isArray(results.data) ? results.data : [];
      const headers = Array.isArray(rows[0]) ? rows[0].map((item) => String(item ?? "").trim()) : [];
      const dataRows = rows.slice(1).filter((row) => Array.isArray(row) && row.some((cell) => String(cell ?? "").trim()));
      const set = new Set(headers.map(canonicalHeader).filter(Boolean));
      const missing = importCategory.value === "exercise_record" ? [["recordTime", "recordDate"], ["durationMin"], ["sportName", "sportId"]].filter((group) => !group.some((key) => set.has(key))).map((group) => group.join(" / ")) : [["measureDate"]].filter((group) => !group.some((key) => set.has(key))).map((group) => group.join(" / "));
      csvCheck.value = { headerCount: headers.length, dataRowCount: dataRows.length, missingHeaders: missing, parseErrors: (results.errors || []).slice(0, 5).map((item) => `第 ${(item.row ?? 0) + 1} 行：${item.message}`) };
      previewHeaders.value = headers; previewRows.value = dataRows.slice(0, 5).map((row) => headers.map((_, index) => String(row[index] ?? "").trim())); inspectingCsv.value = false;
      if (!headers.length) errorMsg.value = "CSV 表头为空，请检查文件格式。"; else if (missing.length) errorMsg.value = `缺少必填列：${missing.join("、")}`;
    },
    error(error) { inspectingCsv.value = false; clearCsv(); errorMsg.value = `CSV 解析失败：${error.message}`; }
  });
}
function downloadTemplate() { const blob = new Blob([buildTemplateCsv()], { type: "text/csv;charset=utf-8;" }); const url = window.URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = `${currentImportConfig.value.label}.csv`; link.click(); window.URL.revokeObjectURL(url); }
function selectSource(source) { selectedSourceId.value = source.sourceId; }
function openCreateDialog() { editingSource.value = null; sourceDialogOpen.value = true; clearMessages(); }
function openEditDialog(source) { editingSource.value = { ...source }; sourceDialogOpen.value = true; clearMessages(); }
function closeSourceDialog() { sourceDialogOpen.value = false; editingSource.value = null; }
async function handleSourceSaved(updated) { const isEdit = Boolean(editingSource.value?.sourceId); closeSourceDialog(); await loadPage({ keepMessages: true, preferredSourceId: updated.sourceId }); successMsg.value = `${isEdit ? "已更新" : "已创建"}数据源：${updated.sourceName}`; }
async function updateSourceStatus(source, nextStatus) { statusChangingId.value = source.sourceId; clearMessages(); try { await dataSourceApi.updateSourceStatus(source.sourceId, nextStatus); await loadPage({ keepMessages: true, preferredSourceId: source.sourceId }); successMsg.value = `已更新数据源状态：${source.sourceName}`; } catch (error) { errorMsg.value = error.message; } finally { statusChangingId.value = null; } }
async function removeSource(source) { if (!window.confirm(`确定删除数据源“${source.sourceName}”吗？已导入的数据会保留，但不再关联该来源。`)) return; deletingSourceId.value = source.sourceId; clearMessages(); try { await dataSourceApi.deleteSource(source.sourceId); if (selectedSourceId.value === source.sourceId) { selectedSourceId.value = null; clearCsv(); } await loadPage({ keepMessages: true }); successMsg.value = `已删除数据源：${source.sourceName}`; } catch (error) { errorMsg.value = error.message; } finally { deletingSourceId.value = null; } }
async function submitImport() {
  if (!isFileSource.value) return (errorMsg.value = "当前仅支持文件类型数据源的 CSV 导入。");
  if (!selectedFile.value) return (errorMsg.value = "请先选择要导入的 CSV 文件。");
  if (csvCheck.value.missingHeaders.length) return (errorMsg.value = `请先补齐必填列：${csvCheck.value.missingHeaders.join("、")}`);
  if (csvCheck.value.parseErrors.length) return (errorMsg.value = "CSV 解析存在错误，请修正后再导入。");
  uploading.value = true; clearMessages();
  try {
    const task = importCategory.value === "exercise_record" ? await dataSourceApi.importExerciseRecords(selectedSource.value.sourceId, selectedFile.value) : await dataSourceApi.importHealthMetrics(selectedSource.value.sourceId, selectedFile.value);
    clearCsv(); await loadPage({ keepMessages: true, preferredSourceId: selectedSource.value.sourceId });
    successMsg.value = task.taskStatus === 2 ? "导入已完成，但存在失败记录，请查看同步日志。" : `已完成${taskCategoryText(task.metricCategory)}导入：${selectedSource.value.sourceName}`;
  } catch (error) { errorMsg.value = error.message; } finally { uploading.value = false; }
}

watch(() => [selectedSource.value?.sourceId, selectedSource.value?.sourceType], ([, type]) => { stopWatchPolling(); watchError.value = ""; if (type !== "file") clearCsv(); if (type === "device") startWatchPolling(); else watchLoading.value = false; });
onMounted(() => void loadPage());
onBeforeUnmount(stopWatchPolling);
</script>

<template>
  <section class="ink-card">
    <div class="row-head"><div><h3>数据源概览</h3><p class="muted summary-copy">集中查看数据源、同步状态和最近日志。</p></div><button @click="loadPage" :disabled="loading">{{ loading ? "刷新中..." : "刷新" }}</button></div>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p><p v-if="successMsg" class="success">{{ successMsg }}</p>
    <div class="metric-grid"><article class="metric-cell"><span>数据源总数</span><strong>{{ overview?.totalSources ?? 0 }}</strong></article><article class="metric-cell"><span>启用中的数据源</span><strong>{{ overview?.activeSources ?? 0 }}</strong></article><article class="metric-cell"><span>已导入指标天数</span><strong>{{ overview?.importedMetricDays ?? 0 }}</strong></article><article class="metric-cell"><span>已导入运动记录</span><strong>{{ overview?.importedExerciseRecords ?? 0 }}</strong></article><article class="metric-cell"><span>同步任务总数</span><strong>{{ overview?.totalTasks ?? 0 }}</strong></article></div>
  </section>

  <section class="ink-card"><div class="row-head"><div><h3>创建数据源</h3><p class="muted summary-copy">文件型用于 CSV 导入，设备型用于接收手表推送。</p></div><button @click="openCreateDialog">创建数据源</button></div></section>

  <section class="ink-card">
    <div class="row-head"><div><h3>我的数据源</h3><p class="muted summary-copy">选择不同类型的数据源，下方会切换成对应面板。</p></div></div>
    <div v-if="sources.length" class="source-grid">
      <article v-for="source in sources" :key="source.sourceId" class="source-card" :class="{ active: selectedSourceId === source.sourceId }">
        <div class="source-head"><div><h4>{{ source.sourceName }}</h4><p class="muted">{{ sourceTypeText(source.sourceType) }}</p></div><span class="status-badge" :class="statusClass(source.sourceStatus)">{{ sourceStatusText(source.sourceStatus) }}</span></div>
        <p class="source-desc">{{ source.description || "暂未填写数据源说明。" }}</p>
        <div class="source-meta"><span>指标覆盖天数：{{ source.metricCount ?? 0 }}</span><span>同步任务数：{{ source.taskCount ?? 0 }}</span><span>最近同步时间：{{ formatDateTime(source.lastSyncTime) }}</span></div>
        <div class="source-actions">
          <button class="ghost-btn" :disabled="deletingSourceId === source.sourceId" @click="selectSource(source)">{{ actionText(source) }}</button>
          <button class="ghost-btn" :disabled="deletingSourceId === source.sourceId" @click="openEditDialog(source)">编辑</button>
          <button v-if="source.sourceStatus === 0" class="ghost-btn" :disabled="statusChangingId === source.sourceId || deletingSourceId === source.sourceId" @click="updateSourceStatus(source, 1)">{{ statusChangingId === source.sourceId ? "保存中..." : "暂停" }}</button>
          <button v-else :disabled="statusChangingId === source.sourceId || deletingSourceId === source.sourceId" @click="updateSourceStatus(source, 0)">{{ statusChangingId === source.sourceId ? "保存中..." : "启用" }}</button>
          <button class="danger-btn" :disabled="deletingSourceId === source.sourceId || statusChangingId === source.sourceId" @click="removeSource(source)">{{ deletingSourceId === source.sourceId ? "删除中..." : "删除" }}</button>
        </div>
      </article>
    </div>
    <div v-else class="empty-state"><h4>还没有数据源</h4><p>请先创建一个数据源，再继续导入文件或接收手表数据。</p></div>
  </section>

  <section class="ink-card">
    <div class="row-head"><div><h3>{{ isDeviceSource ? "手表实时数据" : isFileSource ? "CSV 导入" : "数据源面板" }}</h3><p class="muted summary-copy">{{ selectedSource ? `当前选中：${selectedSource.sourceName}` : "先从上方选择一个数据源。" }}</p></div><button v-if="isDeviceSource" @click="loadWatchData" :disabled="watchLoading">{{ watchLoading ? "刷新中..." : "刷新设备数据" }}</button></div>

    <div v-if="!selectedSource" class="empty-state"><h4>先选择一个数据源</h4><p>文件型会显示 CSV 导入面板，设备型会显示手表最近推送的数据。</p></div>

    <template v-else-if="isDeviceSource">
      <p v-if="watchError" class="error">{{ watchError }}</p>
      <div v-if="watchData" class="watch-grid">
        <article class="metric-cell"><span>实时心率</span><strong>{{ watchData.heartRate || "--" }}</strong></article>
        <article class="metric-cell"><span>平均心率</span><strong>{{ watchData.avgHeartRate || "--" }}</strong></article>
        <article class="metric-cell"><span>最大心率</span><strong>{{ watchData.maxHeartRate || "--" }}</strong></article>
        <article class="metric-cell"><span>手表静息心率</span><strong>{{ watchData.restingHeartRate ?? "--" }}</strong></article>
        <article class="metric-cell"><span>入库静息心率</span><strong>{{ watchData.mappedRestingHeartRate ?? "--" }}</strong></article>
        <article class="metric-cell"><span>步数</span><strong>{{ watchData.steps || 0 }}</strong></article>
        <article class="metric-cell"><span>压力指数</span><strong>{{ watchData.stressLevel ?? "--" }}</strong></article>
        <article class="metric-cell"><span>Body Battery</span><strong>{{ watchData.bodyBattery ?? "--" }}</strong></article>
        <article class="metric-cell"><span>热量</span><strong>{{ watchData.calories || 0 }} kcal</strong></article>
        <article class="metric-cell"><span>距离</span><strong>{{ formatDistance(watchData.distanceCm) }}</strong></article>
        <article class="metric-cell"><span>设备名</span><strong>{{ watchData.device || "--" }}</strong></article>
        <article class="metric-cell"><span>测量日期</span><strong>{{ watchData.measureDate || "--" }}</strong></article>
        <article class="metric-cell"><span>手表时间</span><strong>{{ formatTs(watchData.timestamp) }}</strong></article>
        <article class="metric-cell"><span>接收时间</span><strong>{{ formatMs(watchData.receivedAt) }}</strong></article>
      </div>
      <div v-else class="empty-state"><h4>还没有收到这台设备的数据</h4><p>保持页面打开，手表向系统推送数据后，这里会自动刷新显示。</p></div>
    </template>

    <template v-else-if="isFileSource">
      <div class="import-shell">
        <div class="import-panel">
          <div class="mode-switch"><button v-for="(option, key) in importConfigs" :key="key" class="mode-btn" :class="{ active: importCategory === key }" @click="importCategory = key; clearCsv(); clearMessages();">{{ option.label }}</button></div>
          <label class="upload-field"><span>上传 CSV 文件</span><input type="file" accept=".csv,text/csv" :disabled="uploading" @change="handleFileChange" /></label>
          <div class="file-meta"><span>当前文件：{{ selectedFile?.name || "未选择" }}</span><div class="inline-actions"><button class="ghost-btn" @click="downloadTemplate">下载模板</button><button class="ghost-btn" @click="clearCsv" :disabled="!selectedFile">清空文件</button><button @click="submitImport" :disabled="!canSubmitImport">{{ uploading ? "导入中..." : "开始导入" }}</button></div></div>
          <p v-if="inspectingCsv" class="muted">正在校验 CSV 文件...</p>
          <div v-if="csvCheck.headerCount || csvCheck.dataRowCount" class="check-grid"><article class="check-cell"><span>表头列数</span><strong>{{ csvCheck.headerCount }}</strong></article><article class="check-cell"><span>数据行数</span><strong>{{ csvCheck.dataRowCount }}</strong></article><article class="check-cell"><span>缺失列</span><strong>{{ csvCheck.missingHeaders.length }}</strong></article><article class="check-cell"><span>解析错误</span><strong>{{ csvCheck.parseErrors.length }}</strong></article></div>
          <div v-if="csvCheck.missingHeaders.length" class="check-alert warn">缺少必填列：{{ csvCheck.missingHeaders.join("、") }}</div>
          <div v-if="csvCheck.parseErrors.length" class="check-alert error-box"><p v-for="item in csvCheck.parseErrors" :key="item">{{ item }}</p></div>
          <div v-if="previewHeaders.length" class="table-wrap preview-wrap"><table class="preview-table"><thead><tr><th v-for="(header, index) in previewHeaders" :key="`${header}-${index}`">{{ header || `列 ${index + 1}` }}</th></tr></thead><tbody><tr v-for="(row, rowIndex) in previewRows" :key="rowIndex"><td v-for="(cell, cellIndex) in row" :key="`${rowIndex}-${cellIndex}`">{{ cell || "--" }}</td></tr></tbody></table></div>
        </div>
        <pre>{{ buildTemplateCsv() }}</pre>
      </div>
    </template>

    <div v-else class="empty-state"><h4>当前类型暂无专属面板</h4><p>你仍然可以在上方查看状态，并在下方查看最近同步日志。</p></div>
  </section>

  <section class="ink-card">
    <div class="row-head"><div><h3>最近同步日志</h3><p class="muted summary-copy">展示当前用户最近的导入与同步任务。</p></div></div>
    <div class="table-wrap"><table><thead><tr><th>分类</th><th>来源</th><th>状态</th><th>文件</th><th>总行数</th><th>新增</th><th>更新</th><th>失败</th><th>完成时间</th><th>摘要</th></tr></thead><tbody><tr v-for="task in tasks" :key="task.taskId"><td>{{ taskCategoryText(task.metricCategory) }}</td><td>{{ task.sourceName || "--" }}</td><td><span class="status-badge" :class="taskClass(task.taskStatus)">{{ taskStatusText(task.taskStatus) }}</span></td><td>{{ task.fileName || "--" }}</td><td>{{ task.totalCount ?? 0 }}</td><td>{{ task.insertCount ?? 0 }}</td><td>{{ task.updateCount ?? 0 }}</td><td>{{ task.failCount ?? 0 }}</td><td>{{ formatDateTime(task.finishedTime || task.startedTime) }}</td><td class="summary-cell">{{ task.summaryMessage || "--" }}</td></tr><tr v-if="tasks.length === 0"><td colspan="10" class="muted">暂无同步日志</td></tr></tbody></table></div>
  </section>

  <DataSourceEditDialog :open="sourceDialogOpen" :source="editingSource" @close="closeSourceDialog" @saved="handleSourceSaved" />
</template>

<style scoped>
.summary-copy{margin:6px 0 0}.source-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:14px}.source-card{border:1px solid #e2e4e7;border-radius:16px;background:#fff;padding:16px}.source-card.active{border-color:#111315;box-shadow:0 14px 28px rgba(17,19,21,.08)}.source-head{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.source-head h4{margin:0;font-size:22px}.source-head p{margin:6px 0 0}.source-desc{margin:14px 0 0;color:#586068;min-height:44px}.source-meta{margin-top:14px;display:grid;gap:8px;color:#747a80;font-size:13px}.source-actions{margin-top:16px;display:flex;gap:10px;flex-wrap:wrap}.ghost-btn{background:#fff;color:#2a2c2e;border:1px solid #c7c7c7}.ghost-btn:hover{background:#f2f3f5}.danger-btn{background:#fff;color:#963838;border:1px solid #e1b2b2}.danger-btn:hover{background:#fff3f3}.status-badge{display:inline-flex;align-items:center;border-radius:999px;padding:5px 10px;font-size:12px}.badge-ok{background:#e8f4ea;color:#27653c}.badge-warn{background:#fff2e8;color:#91571c}.badge-error{background:#fdeaea;color:#9c3030}.badge-neutral{background:#f1f3f5;color:#5f6871}.empty-state{border:1px dashed #d7dbe0;border-radius:16px;background:#fcfcfb;padding:20px}.empty-state h4{margin:0 0 10px;font-size:20px}.empty-state p{margin:0;color:#6e757d}.watch-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px}.import-shell{display:grid;grid-template-columns:1.15fr .85fr;gap:16px}.import-panel{border:1px solid #e3e4e6;border-radius:16px;background:#fff;padding:16px}.upload-field{display:grid;gap:8px;margin-top:14px}.mode-switch{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px}.mode-btn{background:#fff;color:#3b4249;border:1px solid #cfd5db}.mode-btn.active{background:#111315;border-color:#111315;color:#fff}.file-meta{margin-top:14px;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:12px}.check-grid{margin-top:14px;display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:10px}.check-cell{border:1px solid #eceef1;border-radius:14px;padding:12px;background:#fafbfc}.check-cell span{display:block;color:#79818a;font-size:12px}.check-cell strong{display:block;margin-top:8px;font-size:22px}.check-alert{margin-top:14px;border-radius:14px;background:#f6f7f8;padding:14px}.check-alert.warn{background:#fff7ef;color:#8a5316}.check-alert.error-box{background:#fdeeee;color:#9a2e2e}.preview-wrap{max-height:260px;overflow:auto;margin-top:14px}.preview-table th,.preview-table td{white-space:nowrap}pre{margin:0;padding:14px;border-radius:12px;background:#121518;color:#fff;white-space:pre-wrap;word-break:break-word;font-family:"Consolas","Monaco",monospace;font-size:13px}.summary-cell{max-width:360px;white-space:normal}@media (max-width:960px){.import-shell{grid-template-columns:1fr}}
</style>
