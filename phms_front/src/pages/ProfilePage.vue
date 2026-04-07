<!--
  个人档案：查看与编辑基础信息，支持身份证号辅助解析填表。
-->
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { healthApi } from "../services/healthApi";

const router = useRouter();

const goalsLoading = ref(false);
const recordsLoading = ref(false);
const goalActionLoadingId = ref(null);
const recordSaving = ref(false);
const goalErrorMsg = ref("");
const goalSuccessMsg = ref("");
const userGoals = ref([]);
const records = ref([]);
const selectedUserGoalId = ref(null);
const activeRange = ref("all");
const recordModalOpen = ref(false);

const recordForm = ref(defaultRecordForm());

const rangeOptions = [
  { value: 1, label: "今日" },
  { value: 7, label: "近7天" },
  { value: 30, label: "近30天" },
  { value: 90, label: "近90天" },
  { value: 180, label: "近半年" },
  { value: 365, label: "近一年" },
  { value: "all", label: "全部" }
];

const sourceOptions = [
  { value: 0, label: "手动录入" },
  { value: 1, label: "设备同步" },
  { value: 2, label: "系统计算" }
];

const booleanOptions = [
  { value: "true", label: "是" },
  { value: "false", label: "否" }
];

const selectedGoal = computed(() =>
  userGoals.value.find((item) => item.userGoalId === selectedUserGoalId.value) || null
);

const orderedRecords = computed(() =>
  [...records.value].sort((left, right) => new Date(left.recordTime) - new Date(right.recordTime))
);

const latestRecords = computed(() => [...orderedRecords.value].reverse().slice(0, 6));

const chartState = computed(() => {
  if (!selectedGoal.value || selectedGoal.value.metricType !== 1) {
    return null;
  }
  const series = orderedRecords.value
    .map((item) => ({ ...item, numericValue: Number(item.recordValue) }))
    .filter((item) => Number.isFinite(item.numericValue));
  if (!series.length) {
    return null;
  }

  const width = 860;
  const height = 280;
  const paddingLeft = 46;
  const paddingRight = 18;
  const paddingTop = 18;
  const paddingBottom = 40;
  const innerWidth = width - paddingLeft - paddingRight;
  const innerHeight = height - paddingTop - paddingBottom;

  let rawMin = Math.min(...series.map((item) => item.numericValue));
  let rawMax = Math.max(...series.map((item) => item.numericValue));
  if (rawMin === rawMax) {
    rawMin -= 1;
    rawMax += 1;
  }
  const margin = (rawMax - rawMin) * 0.12 || 1;
  let min = rawMin - margin;
  let max = rawMax + margin;
  if (rawMin >= 0 && min < 0) {
    min = 0;
  }

  const points = series.map((item, index) => {
    const x =
      series.length === 1
        ? paddingLeft + innerWidth / 2
        : paddingLeft + (innerWidth * index) / (series.length - 1);
    const y = paddingTop + ((max - item.numericValue) * innerHeight) / (max - min);
    return {
      x,
      y,
      label: formatShortDate(item.recordTime),
      value: item.numericValue
    };
  });

  const polyline = points.map((point) => `${point.x},${point.y}`).join(" ");
  const area = `${points[0].x},${height - paddingBottom} ${polyline} ${
    points[points.length - 1].x
  },${height - paddingBottom}`;
  const ticks = Array.from({ length: 5 }, (_, index) => {
    const ratio = index / 4;
    const value = max - (max - min) * ratio;
    return {
      y: paddingTop + innerHeight * ratio,
      label: formatDecimal(value)
    };
  });

  const labelIndexes = Array.from(
    new Set([
      0,
      Math.floor((series.length - 1) * 0.25),
      Math.floor((series.length - 1) * 0.5),
      Math.floor((series.length - 1) * 0.75),
      series.length - 1
    ])
  );

  const labels = labelIndexes.map((index) => ({
    x: points[index].x,
    label: points[index].label
  }));

  return {
    width,
    height,
    paddingLeft,
    paddingRight,
    paddingTop,
    paddingBottom,
    points,
    polyline,
    area,
    ticks,
    labels
  };
});

function defaultRecordForm() {
  const now = currentDateTimeParts();
  return {
    remark: "",
    recordValue: "",
    recordText: "",
    recordDate: now.date,
    recordClock: now.time,
    recordSource: 0
  };
}

function currentDateTimeParts() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
  return {
    date: local.slice(0, 10),
    time: local.slice(11, 16)
  };
}

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length ? text : null;
}

function genderText(gender) {
  if (gender === 1) {
    return "男";
  }
  if (gender === 2) {
    return "女";
  }
  return "未知";
}

function accountStatusText(status) {
  if (status === 1) {
    return "异常（只读）";
  }
  if (status === 2) {
    return "禁用";
  }
  return "正常";
}

function clearGoalMessages() {
  goalErrorMsg.value = "";
  goalSuccessMsg.value = "";
}

function formatDecimal(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "--";
  }
  return Number.isInteger(number) ? String(number) : number.toFixed(2).replace(/\.?0+$/, "");
}

function formatShortDate(value) {
  if (!value) {
    return "--";
  }
  return String(value).slice(0, 10);
}

function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return String(value).replace("T", " ");
}

function metricTypeText(metricType) {
  if (metricType === 1) {
    return "数值型";
  }
  if (metricType === 2) {
    return "文本型";
  }
  if (metricType === 3) {
    return "布尔型";
  }
  return "未知";
}

function sourceText(source) {
  const target = sourceOptions.find((item) => item.value === source);
  return target ? target.label : "未知来源";
}

function evaluationText(evaluation) {
  if (evaluation === 1) {
    return "偏低";
  }
  if (evaluation === 2) {
    return "达标";
  }
  if (evaluation === 3) {
    return "偏高";
  }
  return "未评价";
}

function evaluationClass(evaluation) {
  if (evaluation === 2) {
    return "badge-ok";
  }
  if (evaluation === 1 || evaluation === 3) {
    return "badge-warn";
  }
  return "badge-neutral";
}

function goalTargetSummary(goal) {
  if (!goal) {
    return "--";
  }
  if (goal.metricType === 1) {
    if (goal.targetMin == null && goal.targetMax == null) {
      return "暂未设置目标范围";
    }
    return `${goal.targetMin ?? "--"} - ${goal.targetMax ?? "--"}${goal.unit ? ` ${goal.unit}` : ""}`;
  }
  return goal.targetText || "暂未设置文本目标";
}

function latestValueSummary(goal) {
  if (!goal) {
    return "--";
  }
  if (goal.metricType === 1) {
    return goal.latestRecordValue == null
      ? "--"
      : `${formatDecimal(goal.latestRecordValue)}${goal.unit ? ` ${goal.unit}` : ""}`;
  }
  if (goal.metricType === 3) {
    if (!goal.latestRecordText) {
      return "--";
    }
    return goal.latestRecordText === "true" ? "是" : goal.latestRecordText === "false" ? "否" : goal.latestRecordText;
  }
  return goal.latestRecordText || "--";
}

function recordValueSummary(record, goal = selectedGoal.value) {
  if (!goal) {
    return "--";
  }
  if (goal.metricType === 1) {
    return record.recordValue == null
      ? "--"
      : `${formatDecimal(record.recordValue)}${goal.unit ? ` ${goal.unit}` : ""}`;
  }
  if (goal.metricType === 3) {
    return record.recordText === "true" ? "是" : record.recordText === "false" ? "否" : record.recordText || "--";
  }
  return record.recordText || "--";
}

function resetRecordForm() {
  recordForm.value = defaultRecordForm();
}

async function loadProfile() {
  loading.value = true;
  errorMsg.value = "";
  successMsg.value = "";
  try {
    // 档案 GET 后同步表单与「上次已识别身份证」缓存，避免重复打识别接口
    const data = await healthApi.getProfile();
    profile.value = data;
    lastRecognizedIdCard.value = (data?.idcard ?? "").trim().toUpperCase();
    form.value = mapProfileToForm(data);
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function recognizeIdCard(silent = true) {
  // silent=true 用于保存前自动补全；false 时在用户主动触发场景展示错误
  const idcard = normalizedIdCard();
  if (!idcard) {
    if (!silent) {
      errorMsg.value = "请先输入身份证号";
    }
    return false;
  }
  if (!isCompleteIdCard(idcard)) {
    return false;
  }
  if (idcard === lastRecognizedIdCard.value) {
    return true;
  }

  recognizing.value = true;
  errorMsg.value = "";
  try {
    const result = await healthApi.recognizeIdCard(idcard);
    form.value.idcard = result.idcard;
    form.value.gender = result.gender;
    form.value.age = result.age;
    lastRecognizedIdCard.value = result.idcard;
    successMsg.value = `已自动识别：${genderText(result.gender)}，${result.age} 岁（出生日期 ${result.birthDate}）`;
    return true;
  } catch (error) {
    if (!silent) {
      errorMsg.value = error.message;
    }
    return false;
  } finally {
    recognizing.value = false;
  }
}

function scheduleRecognize() {
  if (recognizeTimer) {
    clearTimeout(recognizeTimer);
  }
  recognizeTimer = setTimeout(() => {
    recognizeIdCard(true);
  }, 350);
}

async function saveProfile() {
  saving.value = true;
  errorMsg.value = "";
  successMsg.value = "";
  try {
    // 先静默识别身份证以回填性别年龄，再提交 updateProfile
    await recognizeIdCard(true);
    const updated = await healthApi.updateProfile(buildPayload());
    profile.value = updated;
    form.value = mapProfileToForm(updated);
    lastRecognizedIdCard.value = (updated.idcard ?? "").trim().toUpperCase();
    successMsg.value = "个人信息保存成功";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    saving.value = false;
  }
}

async function loadRecords() {
  if (!selectedGoal.value) {
    records.value = [];
    return;
  }
  recordsLoading.value = true;
  goalErrorMsg.value = "";
  try {
    // 健康记录按「当前选中的用户目标」拉取；rangeDays 为 null 表示全量（上限由 limit 约束）
    records.value = await healthApi.listHealthRecords(selectedGoal.value.userGoalId, {
      rangeDays: activeRange.value === "all" ? null : Number(activeRange.value),
      limit: activeRange.value === "all" ? 1000 : 400
    });
  } catch (error) {
    goalErrorMsg.value = error.message;
  } finally {
    recordsLoading.value = false;
  }
}

async function loadGoalSection() {
  goalsLoading.value = true;
  clearGoalMessages();
  try {
    // 与 HealthGoalsPage 一致：只展示进行中的关联目标，并校正当前 tab 选中项
    const data = await healthApi.listUserGoals({ status: 0 });
    userGoals.value = data;
    if (!selectedUserGoalId.value || !data.some((item) => item.userGoalId === selectedUserGoalId.value)) {
      selectedUserGoalId.value = data[0]?.userGoalId ?? null;
    }
    await loadRecords();
  } catch (error) {
    goalErrorMsg.value = error.message;
    records.value = [];
  } finally {
    goalsLoading.value = false;
  }
}

async function activateGoal(userGoalId) {
  selectedUserGoalId.value = userGoalId;
  await loadRecords();
}

async function changeRange(range) {
  activeRange.value = range;
  await loadRecords();
}

async function removeGoal(goal) {
  if (!window.confirm(`确认移除目标“${goal.goalName}”吗？`)) {
    return;
  }
  goalActionLoadingId.value = goal.userGoalId;
  clearGoalMessages();
  try {
    await healthApi.cancelHealthGoal(goal.userGoalId);
    goalSuccessMsg.value = `${goal.goalName} 已移除`;
    await loadGoalSection();
  } catch (error) {
    goalErrorMsg.value = error.message;
  } finally {
    goalActionLoadingId.value = null;
  }
}

function openRecordModal() {
  if (!selectedGoal.value) {
    return;
  }
  resetRecordForm();
  recordForm.value.remark = `${selectedGoal.value.goalName}记录`;
  recordModalOpen.value = true;
}

function closeRecordModal() {
  recordModalOpen.value = false;
  resetRecordForm();
}

async function submitRecord() {
  if (!selectedGoal.value) {
    return;
  }
  recordSaving.value = true;
  clearGoalMessages();
  try {
    // 弹窗内新建一条健康记录：数值型走 recordValue，文本/布尔走 recordText
    if (!normalizeText(recordForm.value.remark)) {
      throw new Error("请输入记录标题");
    }
    if (!recordForm.value.recordDate || !recordForm.value.recordClock) {
      throw new Error("请选择完整的记录日期和时间");
    }
    const recordTime = `${recordForm.value.recordDate}T${recordForm.value.recordClock}`;
    const payload = {
      remark: normalizeText(recordForm.value.remark),
      recordValue:
        selectedGoal.value.metricType === 1
          ? recordForm.value.recordValue === "" || recordForm.value.recordValue == null
            ? null
            : Number(recordForm.value.recordValue)
          : null,
      recordText: selectedGoal.value.metricType === 1 ? null : normalizeText(recordForm.value.recordText),
      recordTime,
      recordSource: Number(recordForm.value.recordSource)
    };
    await healthApi.createHealthRecord(selectedGoal.value.userGoalId, payload);
    goalSuccessMsg.value = "健康数据已提交";
    closeRecordModal();
    await loadGoalSection();
  } catch (error) {
    goalErrorMsg.value = error.message;
  } finally {
    recordSaving.value = false;
  }
}

function goToHealthGoals() {
  router.push("/health-goals");
}

onMounted(loadGoalSection);
</script>

<template>
  <section v-if="false" class="ink-card">
    <div class="row-head">
      <h3>个人信息</h3>
      <div class="inline-actions">
        <button @click="loadProfile" :disabled="loading">刷新</button>
        <button @click="saveProfile" :disabled="saving || loading">
          {{ saving ? "保存中..." : "保存" }}
        </button>
      </div>
    </div>

    <p v-if="loading" class="muted">档案加载中...</p>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>

    <div v-if="profile" class="form-grid">
      <label>
        账号（只读）
        <input :value="profile.account" disabled />
      </label>
      <label>
        账号状态（只读）
        <input :value="accountStatusText(profile.accountStatus)" disabled />
      </label>
      <label>
        昵称
        <input v-model.trim="form.nickname" placeholder="请输入昵称" />
      </label>
      <label>
        真实姓名
        <input v-model.trim="form.realname" placeholder="请输入真实姓名" />
      </label>
      <label class="idcard-field">
        身份证号
        <input
          v-model.trim="form.idcard"
          placeholder="输入完整身份证号后自动识别"
          @input="scheduleRecognize"
        />
        <small class="idcard-tip">
          {{ recognizing ? "识别中..." : "支持15位/18位身份证，填完整后自动识别性别和年龄" }}
        </small>
      </label>
      <label>
        性别
        <select v-model.number="form.gender">
          <option :value="0">未知</option>
          <option :value="1">男</option>
          <option :value="2">女</option>
        </select>
      </label>
      <label>
        年龄
        <input type="number" min="0" max="130" v-model="form.age" placeholder="请输入年龄" />
      </label>
    </div>
  </section>

  <section class="ink-card goal-board">
    <div class="row-head">
      <div>
        <h3>我的健康目标</h3>
        <p class="board-subtitle">选择中的目标会在这里展示，并支持上传记录和查看趋势。</p>
      </div>
      <div class="inline-actions">
        <button class="ghost-btn" @click="goToHealthGoals">管理目标</button>
        <button @click="loadGoalSection" :disabled="goalsLoading || recordsLoading">
          {{ goalsLoading ? "刷新中..." : "刷新目标" }}
        </button>
      </div>
    </div>

    <p v-if="goalErrorMsg" class="error">{{ goalErrorMsg }}</p>
    <p v-if="goalSuccessMsg" class="success">{{ goalSuccessMsg }}</p>
    <p v-if="goalsLoading" class="muted">健康目标加载中...</p>

    <template v-else-if="userGoals.length">
      <div class="goal-chip-row">
        <button
          v-for="goal in userGoals"
          :key="goal.userGoalId"
          type="button"
          class="goal-chip"
          :class="{ active: selectedUserGoalId === goal.userGoalId }"
          @click="activateGoal(goal.userGoalId)"
        >
          <span>{{ goal.goalName }}</span>
          <em
            title="移除目标"
            @click.stop="removeGoal(goal)"
          >
            {{ goalActionLoadingId === goal.userGoalId ? "..." : "×" }}
          </em>
        </button>
      </div>

      <section v-if="selectedGoal" class="goal-detail">
        <div class="goal-detail-head">
          <div>
            <p class="detail-tag">健康目标</p>
            <h4>{{ selectedGoal.goalName }}</h4>
            <p class="goal-range-copy">目标范围：{{ goalTargetSummary(selectedGoal) }}</p>
            <p class="goal-meta">
              <span>{{ metricTypeText(selectedGoal.metricType) }}</span>
              <span>记录数 {{ selectedGoal.recordCount }}</span>
              <span>最近记录 {{ latestValueSummary(selectedGoal) }}</span>
            </p>
          </div>
          <div class="inline-actions">
            <button @click="openRecordModal">上传数据</button>
          </div>
        </div>

        <section class="trend-shell">
          <div class="trend-head">
            <h5>
              数据趋势
              <span v-if="selectedGoal.unit">（{{ selectedGoal.unit }}）</span>
            </h5>
            <div class="range-tabs">
              <button
                v-for="item in rangeOptions"
                :key="item.value"
                type="button"
                class="range-tab"
                :class="{ active: activeRange === item.value }"
                @click="changeRange(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>

          <div class="chart-box">
            <p v-if="recordsLoading" class="muted">趋势数据加载中...</p>
            <template v-else-if="selectedGoal.metricType === 1 && chartState">
              <svg
                :viewBox="`0 0 ${chartState.width} ${chartState.height}`"
                class="trend-chart"
                role="img"
                aria-label="健康记录趋势图"
              >
                <g v-for="tick in chartState.ticks" :key="tick.y">
                  <line
                    :x1="chartState.paddingLeft"
                    :x2="chartState.width - chartState.paddingRight"
                    :y1="tick.y"
                    :y2="tick.y"
                    class="chart-grid"
                  />
                  <text :x="16" :y="tick.y + 4" class="chart-tick">{{ tick.label }}</text>
                </g>

                <polygon :points="chartState.area" class="chart-area" />
                <polyline :points="chartState.polyline" class="chart-line" />
                <circle
                  v-for="point in chartState.points"
                  :key="`${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="4"
                  class="chart-point"
                />

                <text
                  v-for="label in chartState.labels"
                  :key="`${label.x}-${label.label}`"
                  :x="label.x"
                  :y="chartState.height - 10"
                  class="chart-label"
                  text-anchor="middle"
                >
                  {{ label.label }}
                </text>
              </svg>
            </template>
            <div v-else-if="selectedGoal.metricType !== 1 && latestRecords.length" class="text-record-grid">
              <article v-for="record in latestRecords" :key="record.recordId" class="text-record-card">
                <strong>{{ recordValueSummary(record) }}</strong>
                <p>{{ record.remark || "无标题" }}</p>
                <span>{{ formatDateTime(record.recordTime) }}</span>
              </article>
            </div>
            <div v-else class="empty-state">
              <h4>暂无记录</h4>
              <p>先上传一条健康数据，档案页才会开始展示趋势。</p>
            </div>
          </div>
        </section>

        <section class="history-shell">
          <div class="history-head">
            <h5>最近记录</h5>
          </div>
          <div v-if="latestRecords.length" class="history-list">
            <article v-for="record in latestRecords" :key="record.recordId" class="history-card">
              <div class="history-main">
                <strong>{{ recordValueSummary(record) }}</strong>
                <span>{{ record.remark || "未填写标题" }}</span>
              </div>
              <div class="history-side">
                <em class="status-badge" :class="evaluationClass(record.evaluationResult)">
                  {{ evaluationText(record.evaluationResult) }}
                </em>
                <span>{{ formatDateTime(record.recordTime) }}</span>
                <span>{{ sourceText(record.recordSource) }}</span>
              </div>
            </article>
          </div>
          <p v-else class="muted">暂无记录</p>
        </section>
      </section>
    </template>

    <div v-else class="empty-state">
      <h4>还没有选中的健康目标</h4>
      <p>先去健康目标页添加 BMI、静息心率等目标，之后再回到档案页记录数据。</p>
      <button @click="goToHealthGoals">去选择目标</button>
    </div>
  </section>

  <div v-if="recordModalOpen && selectedGoal" class="detail-mask" @click.self="closeRecordModal">
    <section class="record-modal">
      <div class="record-modal-head">
        <div class="record-modal-icon">◎</div>
        <h4>上传健康数据</h4>
        <p>{{ selectedGoal.goalName }}</p>
        <span>{{ goalTargetSummary(selectedGoal) }}</span>
      </div>

      <form class="record-form" @submit.prevent="submitRecord">
        <label class="record-field">
          <span><i>*</i> 标题</span>
          <input v-model.trim="recordForm.remark" maxlength="500" placeholder="输入记录标题" />
        </label>

        <label v-if="selectedGoal.metricType === 1" class="record-field">
          <span><i>*</i> 数值</span>
          <input
            type="number"
            step="0.01"
            v-model="recordForm.recordValue"
            placeholder="请输入数值"
          />
        </label>

        <label v-else-if="selectedGoal.metricType === 3" class="record-field">
          <span><i>*</i> 结果</span>
          <select v-model="recordForm.recordText">
            <option value="">请选择</option>
            <option v-for="item in booleanOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <label v-else class="record-field">
          <span><i>*</i> 文本结果</span>
          <input v-model.trim="recordForm.recordText" maxlength="255" placeholder="请输入记录结果" />
        </label>

        <div class="record-datetime">
          <label class="record-field">
            <span><i>*</i> 日期</span>
            <input type="date" v-model="recordForm.recordDate" />
          </label>
          <label class="record-field">
            <span><i>*</i> 时间</span>
            <input type="time" v-model="recordForm.recordClock" />
          </label>
        </div>

        <label class="record-field">
          <span>来源</span>
          <select v-model.number="recordForm.recordSource">
            <option v-for="item in sourceOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <div class="record-actions">
          <button type="button" class="ghost-btn" @click="closeRecordModal">取消</button>
          <button type="submit" :disabled="recordSaving">
            {{ recordSaving ? "提交中..." : "提交数据" }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.board-subtitle {
  margin: 8px 0 0;
  color: #767c80;
}

.ghost-btn {
  background: #fff;
  color: #202224;
  border: 1px solid #c8c8c8;
}

.ghost-btn:hover {
  background: #f3f3f3;
}

.goal-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.goal-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid #dadada;
  background: #fff;
  color: #1b1d1f;
  font-size: 15px;
}

.goal-chip.active {
  background: #121518;
  border-color: #121518;
  color: #fff;
}

.goal-chip em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  font-style: normal;
  cursor: pointer;
}

.goal-detail {
  margin-top: 24px;
  border: 1px solid #e5e6e8;
  border-radius: 26px;
  padding: 24px;
  background: #fff;
}

.goal-detail-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.detail-tag {
  margin: 0;
  color: #8a9094;
  font-size: 13px;
}

.goal-detail-head h4 {
  margin: 10px 0 0;
  font-size: 46px;
  font-weight: 600;
}

.goal-range-copy {
  margin: 14px 0 0;
  color: #7b8084;
  font-size: 18px;
}

.goal-meta {
  margin: 14px 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #7c8287;
  font-size: 14px;
}

.trend-shell,
.history-shell {
  margin-top: 28px;
  border-top: 1px solid #e8eaec;
  padding-top: 22px;
}

.trend-head,
.history-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.trend-head h5,
.history-head h5 {
  margin: 0;
  font-size: 20px;
}

.range-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.range-tab {
  min-width: 72px;
  min-height: 42px;
  border-radius: 12px;
  background: #f4f4f2;
  color: #50575c;
  font-size: 14px;
}

.range-tab.active {
  background: #121518;
  color: #fff;
}

.chart-box {
  margin-top: 18px;
  border: 1px solid #e6e7e9;
  border-radius: 20px;
  background: #fcfcfb;
  padding: 14px;
}

.trend-chart {
  width: 100%;
  display: block;
}

.chart-grid {
  stroke: #e6e8ea;
  stroke-width: 1;
}

.chart-tick,
.chart-label {
  fill: #80858a;
  font-size: 12px;
}

.chart-area {
  fill: rgba(20, 23, 26, 0.08);
}

.chart-line {
  fill: none;
  stroke: #171a1d;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chart-point {
  fill: #fff;
  stroke: #171a1d;
  stroke-width: 2;
}

.text-record-grid,
.history-list {
  display: grid;
  gap: 14px;
}

.text-record-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.text-record-card,
.history-card {
  border: 1px solid #e6e7e9;
  border-radius: 16px;
  background: #fff;
  padding: 14px 16px;
}

.text-record-card strong,
.history-main strong {
  display: block;
  font-size: 20px;
}

.text-record-card p,
.history-main span {
  margin: 8px 0 0;
  color: #596066;
}

.text-record-card span,
.history-side span {
  display: block;
  margin-top: 8px;
  color: #8a9095;
  font-size: 13px;
}

.history-card {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.history-side {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 10px;
  font-style: normal;
  font-size: 12px;
}

.badge-ok {
  background: #e8f3e9;
  color: #23653c;
}

.badge-warn {
  background: #fff1e6;
  color: #8e5b1b;
}

.badge-neutral {
  background: #f2f3f5;
  color: #5a6066;
}

.empty-state {
  padding: 40px 20px;
  border: 1px dashed #d9d8d4;
  border-radius: 20px;
  text-align: center;
  color: #72777b;
}

.empty-state h4 {
  margin: 0 0 10px;
  color: #2b3034;
  font-size: 24px;
}

.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  background: rgba(10, 12, 14, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.record-modal {
  width: min(560px, 96vw);
  border-radius: 24px;
  border: 1px solid #d9dcdf;
  background: #fff;
  padding: 24px;
  box-shadow: 0 24px 50px rgba(10, 12, 14, 0.18);
}

.record-modal-head {
  text-align: center;
  padding-bottom: 18px;
  border-bottom: 1px solid #eceef1;
}

.record-modal-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto;
  border-radius: 50%;
  background: #111315;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.record-modal-head h4 {
  margin: 14px 0 0;
  font-size: 44px;
}

.record-modal-head p {
  margin: 10px 0 0;
  font-size: 20px;
  font-weight: 600;
}

.record-modal-head span {
  display: block;
  margin-top: 8px;
  color: #8a9095;
}

.record-form {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.record-field {
  display: grid;
  gap: 8px;
}

.record-field span {
  color: #5d646d;
  font-size: 14px;
}

.record-field i {
  color: #b83f3f;
  font-style: normal;
}

.record-datetime {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.record-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 6px;
}

@media (max-width: 900px) {
  .goal-detail-head,
  .trend-head,
  .history-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .goal-detail-head h4 {
    font-size: 34px;
  }

  .history-side {
    justify-items: flex-start;
  }
}

@media (max-width: 640px) {
  .record-datetime,
  .record-actions {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .record-actions button,
  .range-tab {
    width: 100%;
  }
}
</style>
