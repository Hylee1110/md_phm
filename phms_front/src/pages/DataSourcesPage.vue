<!--
  数据源管理：列表、启停、编辑、删除，以及 CSV 导入健康指标/运动记录与同步任务列表。
-->
<script setup>
import { computed, onMounted, ref } from "vue";
import Papa from "papaparse";
import DataSourceEditDialog from "../components/DataSourceEditDialog.vue";
import { dataSourceApi } from "../services/dataSourceApi";

const loading = ref(false);
const uploading = ref(false);
const inspectingCsv = ref(false);
const statusChangingId = ref(null);
const deletingSourceId = ref(null);
const sourceDialogOpen = ref(false);
const errorMsg = ref("");
const successMsg = ref("");
const overview = ref(null);
const sources = ref([]);
const tasks = ref([]);
const selectedSourceId = ref(null);
const importCategory = ref("health_metric");
const selectedFile = ref(null);
const previewHeaders = ref([]);
const previewRows = ref([]);
const csvCheck = ref(createEmptyCsvCheck());
const editingSource = ref(null);
const creating = ref(false);

const createForm = ref({
  sourceName: "",
  sourceType: "file",
  description: ""
});

// 不同导入类型的“模板与必填字段”配置。用于：
// - 生成 CSV 模板
// - 预览解析结果并提示缺失字段
// - 控制“提交导入”按钮的可用性
const importConfigs = {
  health_metric: {
    label: "健康指标 CSV",
    summary: "按天导入步数、静息心率、睡眠、血压和压力指数。",
    requiredGroups: [
      { label: "measureDate 或 date", anyOf: ["measureDate"] }
    ],
    recommendedKeys: [
      "steps",
      "restingHeartRate",
      "sleepHours",
      "systolic",
      "diastolic",
      "stressLevel"
    ],
    templateHeaders: [
      "measureDate",
      "steps",
      "restingHeartRate",
      "sleepHours",
      "systolic",
      "diastolic",
      "stressLevel"
    ],
    templateRows: [
      ["2026-03-15", "8650", "68", "7.2", "121", "78", "46"],
      ["2026-03-16", "9020", "70", "6.8", "124", "80", "53"]
    ]
  },
  exercise_record: {
    label: "运动记录 CSV",
    summary: "导入单次运动时长、热量和课程/项目名称。",
    requiredGroups: [
      { label: "recordTime 或 recordDate/date", anyOf: ["recordTime", "recordDate"] },
      { label: "durationMin", anyOf: ["durationMin"] },
      { label: "sportName 或 sportId", anyOf: ["sportName", "sportId"] }
    ],
    recommendedKeys: ["caloriesKcal", "note", "externalId", "recordClock"],
    templateHeaders: [
      "recordTime",
      "sportName",
      "durationMin",
      "caloriesKcal",
      "note",
      "externalId"
    ],
    templateRows: [
      ["2026-03-15 18:30", "慢跑", "45", "320", "晚间慢跑", "run-001"],
      ["2026-03-16 19:10", "瑜伽", "35", "160", "拉伸放松", "yoga-002"]
    ]
  }
};

const selectedSource = computed(() =>
  sources.value.find((item) => item.sourceId === selectedSourceId.value) || null
);

const currentImportConfig = computed(() => importConfigs[importCategory.value]);

// 只有在 CSV 校验通过且选中“文件导入”类型的数据源时，才允许提交导入
const canSubmitImport = computed(() =>
  Boolean(
    selectedFile.value &&
      selectedSource.value &&
      selectedSource.value.sourceType === "file" &&
      !uploading.value &&
      !inspectingCsv.value &&
      csvCheck.value.dataRowCount > 0 &&
      csvCheck.value.missingHeaders.length === 0 &&
      csvCheck.value.parseErrors.length === 0
  )
);

function createEmptyCsvCheck() {
  return {
    headerCount: 0,
    dataRowCount: 0,
    delimiter: ",",
    missingHeaders: [],
    warnings: [],
    parseErrors: []
  };
}

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function resetCsvPreview() {
  // 切换导入类型/文件后重置预览与校验结果，避免旧数据误导用户
  previewHeaders.value = [];
  previewRows.value = [];
  csvCheck.value = createEmptyCsvCheck();
}

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length > 0 ? text : null;
}

function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return String(value).replace("T", " ");
}

function sourceTypeText(type) {
  if (type === "manual") return "手动录入";
  if (type === "device") return "设备同步";
  if (type === "platform") return "第三方平台";
  return "文件导入";
}

function sourceStatusText(status) {
  if (status === 1) return "已暂停";
  if (status === 2) return "异常";
  return "启用中";
}

function sourceStatusClass(status) {
  if (status === 1) return "badge-warn";
  if (status === 2) return "badge-error";
  return "badge-ok";
}

function taskStatusText(status) {
  if (status === 1) return "部分成功";
  if (status === 2) return "失败";
  if (status === 3) return "执行中";
  return "成功";
}

function taskStatusClass(status) {
  if (status === 1) return "badge-warn";
  if (status === 2) return "badge-error";
  if (status === 3) return "badge-neutral";
  return "badge-ok";
}

function taskCategoryText(category) {
  if (category === "exercise_record") return "运动记录";
  if (category === "health_metric") return "健康指标";
  return "未分类";
}

function resolveCanonicalHeader(rawHeader) {
  // 兼容不同 CSV 头字段写法：英文大小写、下划线/横线/空格，以及常见中文表头
  const header = String(rawHeader ?? "")
    .trim()
    .toLowerCase()
    .replace(/[-\s]/g, "_");

  if (["measuredate", "measure_date", "date", "日期", "测量日期"].includes(header)) return "measureDate";
  if (["steps", "step", "步数"].includes(header)) return "steps";
  if (["restingheartrate", "resting_heart_rate", "restinghr", "heart_rate", "静息心率", "心率"].includes(header)) {
    return "restingHeartRate";
  }
  if (["sleephours", "sleep_hours", "sleep", "睡眠时长", "睡眠"].includes(header)) return "sleepHours";
  if (["systolic", "收缩压"].includes(header)) return "systolic";
  if (["diastolic", "舒张压"].includes(header)) return "diastolic";
  if (["stresslevel", "stress_level", "stress", "压力指数", "压力"].includes(header)) return "stressLevel";
  if (["recordtime", "record_time", "datetime", "记录时间", "运动时间", "开始时间"].includes(header)) return "recordTime";
  if (["recorddate", "record_date", "date", "记录日期", "运动日期"].includes(header)) return "recordDate";
  if (["recordclock", "record_clock", "time", "时刻", "时间"].includes(header)) return "recordClock";
  if (["sportid", "sport_id", "courseid", "course_id", "课程id", "运动id"].includes(header)) return "sportId";
  if (["sportname", "sport_name", "sport", "coursename", "course_name", "运动名称", "课程名称", "项目名称"].includes(header)) {
    return "sportName";
  }
  if (["durationmin", "duration_min", "duration", "durationminutes", "minutes", "时长", "时长分钟", "分钟"].includes(header)) {
    return "durationMin";
  }
  if (["calorieskcal", "calories_kcal", "calories", "kcal", "热量", "消耗热量", "卡路里"].includes(header)) {
    return "caloriesKcal";
  }
  if (["note", "remark", "memo", "备注", "说明"].includes(header)) return "note";
  if (["externalid", "external_id", "recordid", "外部id", "记录id", "源记录id"].includes(header)) return "externalId";
  return null;
}

function buildTemplateCsv() {
  return Papa.unparse({
    fields: currentImportConfig.value.templateHeaders,
    data: currentImportConfig.value.templateRows
  });
}

function pickDefaultSource() {
  if (selectedSource.value) {
    return;
  }
  const preferred = sources.value.find((item) => item.sourceType === "file") || sources.value[0] || null;
  selectedSourceId.value = preferred?.sourceId ?? null;
}

function clearSelectedFile() {
  selectedFile.value = null;
  resetCsvPreview();
}

function buildCsvCheck(headers, rows, results) {
  const config = currentImportConfig.value;
  const canonicalHeaders = headers.map(resolveCanonicalHeader).filter(Boolean);
  const canonicalHeaderSet = new Set(canonicalHeaders);
  const missingHeaders = config.requiredGroups
    .filter((group) => !group.anyOf.some((key) => canonicalHeaderSet.has(key)))
    .map((group) => group.label);

  const warnings = [];
  if (importCategory.value === "health_metric") {
    const hasMetricColumn = config.recommendedKeys.some((key) => canonicalHeaderSet.has(key));
    if (!hasMetricColumn) {
      warnings.push("当前文件没有识别到健康指标列，导入后可能不会写入任何有效数据。");
    }
  }

  const parseErrors = (results.errors || []).slice(0, 5).map((item) => {
    const rowNo = typeof item.row === "number" ? item.row + 1 : "--";
    return `第 ${rowNo} 行：${item.message}`;
  });

  csvCheck.value = {
    headerCount: headers.length,
    dataRowCount: Math.max(0, rows.length),
    delimiter: results.meta?.delimiter || ",",
    missingHeaders,
    warnings,
    parseErrors
  };
}

function inspectCsvFile(file) {
  inspectingCsv.value = true;
  resetCsvPreview();

  Papa.parse(file, {
    skipEmptyLines: "greedy",
    complete(results) {
      const rows = Array.isArray(results.data) ? results.data : [];
      if (!rows.length) {
        errorMsg.value = "CSV 文件为空，请重新选择。";
        inspectingCsv.value = false;
        return;
      }

      const headers = Array.isArray(rows[0]) ? rows[0].map((item) => String(item ?? "").trim()) : [];
      const dataRows = rows.slice(1).filter((row) => Array.isArray(row) && row.some((cell) => normalizeText(cell)));

      previewHeaders.value = headers;
      previewRows.value = dataRows.slice(0, 5).map((row) =>
        headers.map((_, index) => String(row[index] ?? "").trim())
      );
      buildCsvCheck(headers, dataRows, results);

      if (!headers.length) {
        errorMsg.value = "CSV 表头为空，请检查文件格式。";
      } else if (csvCheck.value.missingHeaders.length > 0) {
        errorMsg.value = `缺少必填列：${csvCheck.value.missingHeaders.join("、")}`;
      }
      inspectingCsv.value = false;
    },
    error(error) {
      inspectingCsv.value = false;
      resetCsvPreview();
      errorMsg.value = `CSV 解析失败：${error.message}`;
    }
  });
}

function handleFileChange(event) {
  const file = event.target.files?.[0] || null;
  event.target.value = "";
  selectedFile.value = file;
  clearMessages();

  if (!file) {
    resetCsvPreview();
    return;
  }
  inspectCsvFile(file);
}

function changeImportCategory(value) {
  if (importCategory.value === value) {
    return;
  }
  importCategory.value = value;
  clearSelectedFile();
  clearMessages();
}

function downloadTemplate() {
  const blob = new Blob([buildTemplateCsv()], {
    type: "text/csv;charset=utf-8;"
  });
  const fileName =
    importCategory.value === "exercise_record"
      ? "运动记录导入模板.csv"
      : "健康指标导入模板.csv";
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
}

async function loadPage({ keepMessages = false } = {}) {
  loading.value = true;
  if (!keepMessages) {
    clearMessages();
  }
  try {
    const [overviewData, sourceData, taskData] = await Promise.all([
      dataSourceApi.getOverview(),
      dataSourceApi.listSources(),
      dataSourceApi.listSyncTasks({ limit: 12 })
    ]);
    overview.value = overviewData;
    sources.value = sourceData;
    tasks.value = taskData;
    pickDefaultSource();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

function openEditDialog(source) {
  editingSource.value = { ...source };
  sourceDialogOpen.value = true;
  clearMessages();
}

function openCreateDialog() {
  editingSource.value = null;
  sourceDialogOpen.value = true;
  clearMessages();
}

function closeSourceDialog() {
  sourceDialogOpen.value = false;
  editingSource.value = null;
}

async function handleSourceSaved(updated) {
  const previousSource = editingSource.value;
  const isEditMode = Boolean(previousSource?.sourceId);
  const wasSelectedForImport = isEditMode && selectedSourceId.value === previousSource.sourceId;
  closeSourceDialog();
  successMsg.value = `已更新数据源：${updated.sourceName}`;
  await loadPage({ keepMessages: true });
  successMsg.value = isEditMode ? `已更新数据源：${updated.sourceName}` : `已创建数据源：${updated.sourceName}`;
  if (!isEditMode) {
    if (updated.sourceType === "file") {
      selectedSourceId.value = updated.sourceId;
    }
    return;
  }
  if (wasSelectedForImport) {
    if (updated.sourceType === "file") {
      selectedSourceId.value = updated.sourceId;
    } else {
      selectedSourceId.value = null;
      clearSelectedFile();
      pickDefaultSource();
    }
  }
}

async function createSource() {
  const payload = {
    sourceName: normalizeText(createForm.value.sourceName),
    sourceType: createForm.value.sourceType,
    description: normalizeText(createForm.value.description)
  };
  if (!payload.sourceName) {
    errorMsg.value = "请输入数据源名称。";
    return;
  }

  creating.value = true;
  clearMessages();
  try {
    const created = await dataSourceApi.createSource(payload);
    successMsg.value = `已创建数据源：${created.sourceName}`;
    resetSourceForm();
    await loadPage({ keepMessages: true });
    if (created.sourceType === "file") {
      selectedSourceId.value = created.sourceId;
    }
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    creating.value = false;
  }
}

async function removeSource(source) {
  const confirmed = window.confirm(
    `确定删除数据源“${source.sourceName}”吗？已导入的健康指标和运动记录会保留，但不会再关联到这个数据源。`
  );
  if (!confirmed) {
    return;
  }

  deletingSourceId.value = source.sourceId;
  clearMessages();
  try {
    await dataSourceApi.deleteSource(source.sourceId);
    successMsg.value = `已删除数据源：${source.sourceName}`;
    if (editingSource.value?.sourceId === source.sourceId) {
      closeSourceDialog();
    }
    if (selectedSourceId.value === source.sourceId) {
      selectedSourceId.value = null;
      clearSelectedFile();
    }
    await loadPage({ keepMessages: true });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    deletingSourceId.value = null;
  }
}

async function updateSourceStatus(source, nextStatus) {
  statusChangingId.value = source.sourceId;
  clearMessages();
  try {
    await dataSourceApi.updateSourceStatus(source.sourceId, nextStatus);
    successMsg.value = `已更新数据源状态：${source.sourceName}`;
    await loadPage({ keepMessages: true });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    statusChangingId.value = null;
  }
}

async function submitImport() {
  if (!selectedSource.value) {
    errorMsg.value = "请先选择一个文件类型的数据源。";
    return;
  }
  if (selectedSource.value.sourceType !== "file") {
    errorMsg.value = "当前仅支持文件类型数据源的 CSV 导入。";
    return;
  }
  if (!selectedFile.value) {
    errorMsg.value = "请先选择要导入的 CSV 文件。";
    return;
  }
  if (csvCheck.value.missingHeaders.length > 0) {
    errorMsg.value = `请先补齐必填列：${csvCheck.value.missingHeaders.join("、")}`;
    return;
  }
  if (csvCheck.value.parseErrors.length > 0) {
    errorMsg.value = "CSV 解析存在错误，请修正后再导入。";
    return;
  }

  uploading.value = true;
  clearMessages();
  try {
    const task =
      importCategory.value === "exercise_record"
        ? await dataSourceApi.importExerciseRecords(selectedSource.value.sourceId, selectedFile.value)
        : await dataSourceApi.importHealthMetrics(selectedSource.value.sourceId, selectedFile.value);
    successMsg.value =
      task.taskStatus === 2
        ? "导入已完成，但存在失败记录，请查看同步日志。"
        : `已完成${taskCategoryText(task.metricCategory)}导入：${selectedSource.value.sourceName}`;
    clearSelectedFile();
    await loadPage({ keepMessages: true });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    uploading.value = false;
  }
}

function useSourceForImport(source) {
  selectedSourceId.value = source.sourceId;
}

onMounted(loadPage);
</script>

<template>
  <section class="ink-card">
    <div class="row-head">
      <div>
        <h3>数据源概览</h3>
        <p class="muted summary-copy">集中查看数据源、CSV 导入结果与最近同步状态。</p>
      </div>
      <button @click="loadPage" :disabled="loading">
        {{ loading ? "刷新中..." : "刷新" }}
      </button>
    </div>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>

    <div class="metric-grid">
      <article class="metric-cell">
        <span>数据源总数</span>
        <strong>{{ overview?.totalSources ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>启用中的数据源</span>
        <strong>{{ overview?.activeSources ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>已导入指标天数</span>
        <strong>{{ overview?.importedMetricDays ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>已导入运动记录</span>
        <strong>{{ overview?.importedExerciseRecords ?? 0 }}</strong>
      </article>
      <article class="metric-cell">
        <span>同步任务总数</span>
        <strong>{{ overview?.totalTasks ?? 0 }}</strong>
      </article>
    </div>

    <div class="ink-note" v-if="overview?.latestTask">
      <h4>最近一次同步</h4>
      <p>来源：{{ overview.latestTask.sourceName || "--" }}</p>
      <p>分类：{{ taskCategoryText(overview.latestTask.metricCategory) }}</p>
      <p>状态：{{ taskStatusText(overview.latestTask.taskStatus) }}</p>
      <p>{{ formatDateTime(overview.latestTask.finishedTime || overview.latestTask.startedTime) }}</p>
      <p>{{ overview.latestTask.summaryMessage || "--" }}</p>
    </div>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <div>
        <h3>创建数据源</h3>
        <p class="muted summary-copy">当前 CSV 导入仅支持文件类型数据源。</p>
      </div>
    </div>

    <div class="inline-actions">
      <button type="button" @click="openCreateDialog">创建数据源</button>
    </div>

    <form v-if="false" class="form-grid" @submit.prevent="createSource">
      <label>
        数据源名称
        <input
          v-model.trim="createForm.sourceName"
          maxlength="64"
          placeholder="例如：小米手环 CSV"
        />
      </label>
      <label>
        数据源类型
        <select v-model="createForm.sourceType">
          <option value="file">文件导入</option>
          <option value="manual">手动录入</option>
          <option value="device">设备同步</option>
          <option value="platform">第三方平台</option>
        </select>
      </label>
      <label class="full-width">
        数据源说明
        <input
          v-model.trim="createForm.description"
          maxlength="255"
          placeholder="可选，用于补充说明该数据源"
        />
      </label>
      <div class="inline-actions form-actions">
        <button type="submit" :disabled="creating">
          {{ creating ? "创建中..." : "创建数据源" }}
        </button>
      </div>
    </form>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <div>
        <h3>我的数据源</h3>
        <p class="muted summary-copy">选择文件类型数据源作为导入目标，也可以暂停或恢复同步。</p>
      </div>
    </div>

    <div v-if="sources.length" class="source-grid">
      <article
        v-for="source in sources"
        :key="source.sourceId"
        class="source-card"
        :class="{ active: selectedSourceId === source.sourceId }"
      >
        <div class="source-head">
          <div>
            <h4>{{ source.sourceName }}</h4>
            <p class="muted">{{ sourceTypeText(source.sourceType) }}</p>
          </div>
          <span class="status-badge" :class="sourceStatusClass(source.sourceStatus)">
            {{ sourceStatusText(source.sourceStatus) }}
          </span>
        </div>

        <p class="source-desc">{{ source.description || "暂未填写数据源说明。" }}</p>

        <div class="source-meta">
          <span>指标覆盖天数：{{ source.metricCount ?? 0 }}</span>
          <span>同步任务数：{{ source.taskCount ?? 0 }}</span>
          <span>最近同步时间：{{ formatDateTime(source.lastSyncTime) }}</span>
        </div>

        <div class="source-actions">
          <button
            type="button"
            class="ghost-btn"
            @click="useSourceForImport(source)"
            :disabled="source.sourceType !== 'file' || deletingSourceId === source.sourceId"
          >
            设为导入来源
          </button>
          <button
            type="button"
            class="ghost-btn"
            :disabled="deletingSourceId === source.sourceId"
            @click="openEditDialog(source)"
          >
            编辑
          </button>
          <button
            v-if="source.sourceStatus === 0"
            type="button"
            class="ghost-btn"
            :disabled="statusChangingId === source.sourceId || deletingSourceId === source.sourceId"
            @click="updateSourceStatus(source, 1)"
          >
            {{ statusChangingId === source.sourceId ? "保存中..." : "暂停" }}
          </button>
          <button
            v-else
            type="button"
            :disabled="statusChangingId === source.sourceId || deletingSourceId === source.sourceId"
            @click="updateSourceStatus(source, 0)"
          >
            {{ statusChangingId === source.sourceId ? "保存中..." : "启用" }}
          </button>
          <button
            type="button"
            class="danger-btn"
            :disabled="deletingSourceId === source.sourceId || statusChangingId === source.sourceId"
            @click="removeSource(source)"
          >
            {{ deletingSourceId === source.sourceId ? "删除中..." : "删除" }}
          </button>
        </div>
      </article>
    </div>
    <div v-else class="empty-state">
      <h4>还没有数据源</h4>
      <p>请先创建一个文件类型数据源，再把 CSV 数据导入系统。</p>
    </div>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <div>
        <h3>CSV 导入</h3>
        <p class="muted summary-copy">
          当前选中的数据源：
          <strong>{{ selectedSource?.sourceName || "未选择" }}</strong>
        </p>
      </div>
    </div>

    <div class="import-shell">
      <div class="import-panel">
        <div class="mode-switch">
          <button
            v-for="option in Object.values(importConfigs)"
            :key="option.label"
            type="button"
            class="mode-btn"
            :class="{ active: currentImportConfig.label === option.label }"
            @click="changeImportCategory(option === importConfigs.exercise_record ? 'exercise_record' : 'health_metric')"
          >
            {{ option.label }}
          </button>
        </div>

        <p class="muted">{{ currentImportConfig.summary }}</p>

        <label class="upload-field">
          <span>上传 CSV 文件</span>
          <input
            type="file"
            accept=".csv,text/csv"
            :disabled="uploading || !selectedSource || selectedSource.sourceType !== 'file'"
            @change="handleFileChange"
          />
        </label>

        <div class="file-meta">
          <span>当前文件：{{ selectedFile?.name || "未选择" }}</span>
          <div class="inline-actions">
            <button type="button" class="ghost-btn" @click="downloadTemplate">下载模板</button>
            <button type="button" class="ghost-btn" @click="clearSelectedFile" :disabled="!selectedFile">
              清空文件
            </button>
            <button type="button" @click="submitImport" :disabled="!canSubmitImport">
              {{ uploading ? "导入中..." : "开始导入" }}
            </button>
          </div>
        </div>

        <p class="muted" v-if="inspectingCsv">正在使用 Papa Parse 校验文件...</p>

        <div v-if="csvCheck.headerCount || csvCheck.dataRowCount" class="check-grid">
          <article class="check-cell">
            <span>表头列数</span>
            <strong>{{ csvCheck.headerCount }}</strong>
          </article>
          <article class="check-cell">
            <span>数据行数</span>
            <strong>{{ csvCheck.dataRowCount }}</strong>
          </article>
          <article class="check-cell">
            <span>分隔符</span>
            <strong>{{ csvCheck.delimiter || "," }}</strong>
          </article>
          <article class="check-cell">
            <span>解析错误</span>
            <strong>{{ csvCheck.parseErrors.length }}</strong>
          </article>
        </div>

        <div v-if="csvCheck.missingHeaders.length" class="check-alert warn">
          <h4>缺少必填列</h4>
          <p>{{ csvCheck.missingHeaders.join("、") }}</p>
        </div>

        <div v-if="csvCheck.warnings.length" class="check-alert">
          <h4>提示</h4>
          <p v-for="warning in csvCheck.warnings" :key="warning">{{ warning }}</p>
        </div>

        <div v-if="csvCheck.parseErrors.length" class="check-alert error-box">
          <h4>解析异常</h4>
          <p v-for="parseError in csvCheck.parseErrors" :key="parseError">{{ parseError }}</p>
        </div>

        <div v-if="previewHeaders.length" class="preview-panel">
          <h4>文件预览</h4>
          <div class="table-wrap preview-wrap">
            <table class="preview-table">
              <thead>
                <tr>
                  <th v-for="(header, index) in previewHeaders" :key="`${header}-${index}`">
                    {{ header || `列 ${index + 1}` }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rowIndex) in previewRows" :key="rowIndex">
                  <td v-for="(cell, cellIndex) in row" :key="`${rowIndex}-${cellIndex}`">
                    {{ cell || "--" }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="template-panel">
        <h4>模板示例</h4>
        <pre>{{ buildTemplateCsv() }}</pre>
      </div>
    </div>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <div>
        <h3>最近同步日志</h3>
        <p class="muted summary-copy">展示当前用户最近的导入与同步任务。</p>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>分类</th>
            <th>来源</th>
            <th>状态</th>
            <th>文件</th>
            <th>总行数</th>
            <th>新增</th>
            <th>更新</th>
            <th>失败</th>
            <th>完成时间</th>
            <th>摘要</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.taskId">
            <td>{{ taskCategoryText(task.metricCategory) }}</td>
            <td>{{ task.sourceName || "--" }}</td>
            <td>
              <span class="status-badge" :class="taskStatusClass(task.taskStatus)">
                {{ taskStatusText(task.taskStatus) }}
              </span>
            </td>
            <td>{{ task.fileName || "--" }}</td>
            <td>{{ task.totalCount ?? 0 }}</td>
            <td>{{ task.insertCount ?? 0 }}</td>
            <td>{{ task.updateCount ?? 0 }}</td>
            <td>{{ task.failCount ?? 0 }}</td>
            <td>{{ formatDateTime(task.finishedTime || task.startedTime) }}</td>
            <td class="summary-cell">{{ task.summaryMessage || "--" }}</td>
          </tr>
          <tr v-if="tasks.length === 0">
            <td colspan="10" class="muted">暂无同步日志</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <DataSourceEditDialog
    :open="sourceDialogOpen"
    :source="editingSource"
    @close="closeSourceDialog"
    @saved="handleSourceSaved"
  />
</template>

<style scoped>
.summary-copy {
  margin: 6px 0 0;
}

.full-width {
  grid-column: 1 / -1;
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.source-card {
  border: 1px solid #e2e4e7;
  border-radius: 16px;
  background: #fff;
  padding: 16px;
}

.source-card.active {
  border-color: #111315;
  box-shadow: 0 14px 28px rgba(17, 19, 21, 0.08);
}

.source-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.source-head h4 {
  margin: 0;
  font-size: 22px;
}

.source-head p {
  margin: 6px 0 0;
}

.source-desc {
  margin: 14px 0 0;
  color: #586068;
  min-height: 44px;
}

.source-meta {
  margin-top: 14px;
  display: grid;
  gap: 8px;
  color: #747a80;
  font-size: 13px;
}

.source-actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ghost-btn {
  background: #fff;
  color: #2a2c2e;
  border: 1px solid #c7c7c7;
}

.ghost-btn:hover {
  background: #f2f3f5;
}

.danger-btn {
  background: #fff;
  color: #963838;
  border: 1px solid #e1b2b2;
}

.danger-btn:hover {
  background: #fff3f3;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
}

.badge-ok {
  background: #e8f4ea;
  color: #27653c;
}

.badge-warn {
  background: #fff2e8;
  color: #91571c;
}

.badge-error {
  background: #fdeaea;
  color: #9c3030;
}

.badge-neutral {
  background: #f1f3f5;
  color: #5f6871;
}

.import-shell {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
}

.import-panel,
.template-panel {
  border: 1px solid #e3e4e6;
  border-radius: 16px;
  background: #fff;
  padding: 16px;
}

.upload-field {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.mode-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.mode-btn {
  background: #fff;
  color: #3b4249;
  border: 1px solid #cfd5db;
}

.mode-btn.active {
  background: #111315;
  border-color: #111315;
  color: #fff;
}

.file-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.check-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}

.check-cell {
  border: 1px solid #eceef1;
  border-radius: 14px;
  padding: 12px;
  background: #fafbfc;
}

.check-cell span {
  display: block;
  color: #79818a;
  font-size: 12px;
}

.check-cell strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
}

.check-alert {
  margin-top: 14px;
  border-radius: 14px;
  background: #f6f7f8;
  padding: 14px;
}

.check-alert h4 {
  margin: 0 0 8px;
}

.check-alert p {
  margin: 0;
}

.check-alert p + p {
  margin-top: 6px;
}

.check-alert.warn {
  background: #fff7ef;
  color: #8a5316;
}

.check-alert.error-box {
  background: #fdeeee;
  color: #9a2e2e;
}

.preview-panel {
  margin-top: 14px;
}

.preview-panel h4,
.template-panel h4 {
  margin: 0 0 12px;
}

.preview-wrap {
  max-height: 260px;
  overflow: auto;
}

.preview-table th,
.preview-table td {
  white-space: nowrap;
}

pre {
  margin: 0;
  padding: 14px;
  border-radius: 12px;
  background: #121518;
  color: #fff;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: "Consolas", "Monaco", monospace;
  font-size: 13px;
}

.summary-cell {
  max-width: 360px;
  white-space: normal;
}

@media (max-width: 860px) {
  .import-shell {
    grid-template-columns: 1fr;
  }
}
</style>
