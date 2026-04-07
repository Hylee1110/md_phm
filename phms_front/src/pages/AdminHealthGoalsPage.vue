<!--
  管理端 - 健康目标：列表筛选、新建与编辑目标模板。
-->
<script setup>
import { onMounted, ref } from "vue";
import { adminApi } from "../services/adminApi";

const loading = ref(false);
const saving = ref(false);
const errorMsg = ref("");
const successMsg = ref("");
const keyword = ref("");
const statusFilter = ref("");
const goals = ref([]);
const editingGoalId = ref(null);

const metricTypeOptions = [
  { value: 1, label: "数值型" },
  { value: 2, label: "文本型" },
  { value: 3, label: "布尔型" }
];

const statusOptions = [
  { value: 0, label: "启用" },
  { value: 1, label: "停用" }
];

const form = ref(defaultForm());

function defaultForm() {
  return {
    goalCode: "",
    goalName: "",
    goalDescription: "",
    metricType: 1,
    unit: "",
    defaultTargetMin: "",
    defaultTargetMax: "",
    defaultTargetText: "",
    sortNo: 0,
    goalStatus: 0
  };
}

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length > 0 ? text : null;
}

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function statusText(status) {
  const target = statusOptions.find((item) => item.value === status);
  return target ? target.label : "未知";
}

function metricTypeText(metricType) {
  const target = metricTypeOptions.find((item) => item.value === metricType);
  return target ? target.label : "未知";
}

function formatTarget(goal) {
  if (goal.metricType === 1) {
    const hasMin = goal.defaultTargetMin != null && goal.defaultTargetMin !== "";
    const hasMax = goal.defaultTargetMax != null && goal.defaultTargetMax !== "";
    if (!hasMin && !hasMax) {
      return "--";
    }
    const left = hasMin ? goal.defaultTargetMin : "--";
    const right = hasMax ? goal.defaultTargetMax : "--";
    return `${left} - ${right}${goal.unit ? ` ${goal.unit}` : ""}`;
  }
  return goal.defaultTargetText || "--";
}

function fillForm(goal) {
  form.value = {
    goalCode: goal.goalCode ?? "",
    goalName: goal.goalName ?? "",
    goalDescription: goal.goalDescription ?? "",
    metricType: goal.metricType ?? 1,
    unit: goal.unit ?? "",
    defaultTargetMin: goal.defaultTargetMin ?? "",
    defaultTargetMax: goal.defaultTargetMax ?? "",
    defaultTargetText: goal.defaultTargetText ?? "",
    sortNo: goal.sortNo ?? 0,
    goalStatus: goal.goalStatus ?? 0
  };
}

function resetForm(clear = false) {
  editingGoalId.value = null;
  form.value = defaultForm();
  if (clear) {
    clearMessages();
  }
}

async function loadGoals(preserveMessages = false) {
  loading.value = true;
  if (!preserveMessages) {
    clearMessages();
  }
  try {
    // 管理端目标模板列表：keyword + 启用/停用筛选；空 status 表示不过滤状态
    goals.value = await adminApi.listHealthGoals({
      keyword: normalizeText(keyword.value),
      status: statusFilter.value === "" ? "" : Number(statusFilter.value)
    });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

function editGoal(goal) {
  editingGoalId.value = goal.goalId;
  fillForm(goal);
  clearMessages();
}

function toPayload() {
  const payload = {
    goalCode: normalizeText(form.value.goalCode)?.toUpperCase(),
    goalName: normalizeText(form.value.goalName),
    goalDescription: normalizeText(form.value.goalDescription),
    metricType: Number(form.value.metricType),
    unit: normalizeText(form.value.unit),
    defaultTargetMin:
      form.value.defaultTargetMin === "" || form.value.defaultTargetMin == null
        ? null
        : Number(form.value.defaultTargetMin),
    defaultTargetMax:
      form.value.defaultTargetMax === "" || form.value.defaultTargetMax == null
        ? null
        : Number(form.value.defaultTargetMax),
    defaultTargetText: normalizeText(form.value.defaultTargetText),
    sortNo: Number(form.value.sortNo || 0),
    goalStatus: Number(form.value.goalStatus)
  };

  if (!payload.goalCode) {
    throw new Error("请输入目标编码");
  }
  if (!payload.goalName) {
    throw new Error("请输入目标名称");
  }
  if (payload.metricType === 1) {
    payload.defaultTargetText = null;
    if (
      payload.defaultTargetMin != null &&
      payload.defaultTargetMax != null &&
      payload.defaultTargetMin > payload.defaultTargetMax
    ) {
      throw new Error("默认目标最小值不能大于最大值");
    }
  } else {
    payload.defaultTargetMin = null;
    payload.defaultTargetMax = null;
    if (!payload.defaultTargetText) {
      throw new Error("文本型或布尔型目标必须填写默认目标文本");
    }
  }

  return payload;
}

async function submitGoal() {
  saving.value = true;
  clearMessages();
  try {
    const payload = toPayload();
    // editingGoalId 有值走更新，否则新建；成功后保留提示信息并刷新列表
    if (editingGoalId.value) {
      await adminApi.updateHealthGoal(editingGoalId.value, payload);
      await loadGoals(true);
      resetForm();
      successMsg.value = "健康目标已更新";
    } else {
      await adminApi.createHealthGoal(payload);
      await loadGoals(true);
      resetForm();
      successMsg.value = "健康目标已创建";
    }
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    saving.value = false;
  }
}

onMounted(loadGoals);
</script>

<template>
  <section class="ink-card">
    <div class="row-head">
      <h3>健康目标管理</h3>
      <div class="inline-actions query-wrap">
        <input v-model.trim="keyword" placeholder="按编码/名称搜索" @keyup.enter="loadGoals" />
        <select v-model="statusFilter">
          <option value="">全部状态</option>
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <button @click="loadGoals" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
        <button class="ghost-btn" @click="resetForm(true)">新建目标</button>
      </div>
    </div>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>编码</th>
            <th>名称</th>
            <th>类型</th>
            <th>默认目标</th>
            <th>状态</th>
            <th>排序</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="goal in goals" :key="goal.goalId">
            <td>{{ goal.goalId }}</td>
            <td>{{ goal.goalCode }}</td>
            <td>{{ goal.goalName }}</td>
            <td>{{ metricTypeText(goal.metricType) }}</td>
            <td>{{ formatTarget(goal) }}</td>
            <td>{{ statusText(goal.goalStatus) }}</td>
            <td>{{ goal.sortNo }}</td>
            <td>{{ goal.lastChangeTime?.replace("T", " ") }}</td>
            <td>
              <button type="button" @click="editGoal(goal)">编辑</button>
            </td>
          </tr>
          <tr v-if="!loading && goals.length === 0">
            <td colspan="9" class="muted">暂无健康目标</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <section class="ink-card">
    <div class="row-head">
      <h3>{{ editingGoalId ? `编辑目标 #${editingGoalId}` : "新增健康目标" }}</h3>
      <div class="inline-actions">
        <button v-if="editingGoalId" type="button" class="ghost-btn" @click="resetForm(true)">取消编辑</button>
        <button type="button" @click="submitGoal" :disabled="saving">
          {{ saving ? "保存中..." : editingGoalId ? "更新目标" : "创建目标" }}
        </button>
      </div>
    </div>

    <form class="form-grid" @submit.prevent="submitGoal">
      <label>
        目标编码
        <input v-model.trim="form.goalCode" maxlength="64" placeholder="如 BMI" />
      </label>
      <label>
        目标名称
        <input v-model.trim="form.goalName" maxlength="128" placeholder="如 健康BMI" />
      </label>
      <label>
        指标类型
        <select v-model.number="form.metricType">
          <option v-for="item in metricTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label>
        单位
        <input v-model.trim="form.unit" maxlength="32" placeholder="如 kg/m² / bpm" />
      </label>
      <label v-if="form.metricType === 1">
        默认目标最小值
        <input type="number" step="0.01" v-model="form.defaultTargetMin" placeholder="可为空" />
      </label>
      <label v-if="form.metricType === 1">
        默认目标最大值
        <input type="number" step="0.01" v-model="form.defaultTargetMax" placeholder="可为空" />
      </label>
      <label v-else class="full-width">
        默认目标文本
        <input
          v-model.trim="form.defaultTargetText"
          maxlength="255"
          placeholder="如 达标 / true / 每日完成"
        />
      </label>
      <label>
        排序号
        <input type="number" v-model.number="form.sortNo" />
      </label>
      <label>
        状态
        <select v-model.number="form.goalStatus">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label class="full-width">
        目标说明
        <textarea v-model.trim="form.goalDescription" rows="4" maxlength="500"></textarea>
      </label>
    </form>
  </section>
</template>

<style scoped>
.query-wrap input {
  min-width: 220px;
}

.ghost-btn {
  background: #fff;
  color: #202224;
  border: 1px solid #c8c8c8;
}

.ghost-btn:hover {
  background: #f3f3f3;
}

.full-width {
  grid-column: 1 / -1;
}

textarea {
  width: 100%;
  border: 1px solid #c8c8c8;
  border-radius: 8px;
  padding: 8px;
  resize: vertical;
  font-family: inherit;
}

@media (max-width: 768px) {
  .query-wrap {
    width: 100%;
    flex-wrap: wrap;
  }

  .query-wrap input {
    min-width: 0;
    width: 100%;
  }
}
</style>
