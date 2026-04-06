<script setup>
import { computed, onMounted, ref } from "vue";
import { healthApi } from "../services/healthApi";

const loading = ref(false);
const selectingGoalId = ref(null);
const removingUserGoalId = ref(null);
const errorMsg = ref("");
const successMsg = ref("");
const draftKeyword = ref("");
const appliedKeyword = ref("");
const goals = ref([]);
const myGoals = ref([]);

const selectedGoalIds = computed(() => new Set(myGoals.value.map((item) => item.goalId)));

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length > 0 ? text : null;
}

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function targetSummary(goal) {
  if (goal.metricType === 1) {
    const min = goal.targetMin ?? goal.defaultTargetMin;
    const max = goal.targetMax ?? goal.defaultTargetMax;
    if (min == null && max == null) {
      return "暂未设置默认范围";
    }
    return `${min ?? "--"} - ${max ?? "--"}${goal.unit ? ` ${goal.unit}` : ""}`;
  }
  return goal.targetText || goal.defaultTargetText || "暂未设置默认文本目标";
}

function goalIcon(goal) {
  const code = String(goal.goalCode ?? "").toUpperCase();
  if (code.includes("HEART")) {
    return "heart";
  }
  return "metric";
}

async function loadGoals() {
  loading.value = true;
  clearMessages();
  try {
    goals.value = await healthApi.listHealthGoals({
      keyword: normalizeText(appliedKeyword.value)
    });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function loadMyGoals() {
  try {
    myGoals.value = await healthApi.listUserGoals({ status: 0 });
  } catch (error) {
    errorMsg.value = error.message;
  }
}

async function loadPage() {
  await Promise.all([loadGoals(), loadMyGoals()]);
}

async function searchGoals() {
  appliedKeyword.value = draftKeyword.value.trim();
  await loadGoals();
}

async function resetSearch() {
  draftKeyword.value = "";
  appliedKeyword.value = "";
  await loadGoals();
}

async function selectGoal(goal) {
  selectingGoalId.value = goal.goalId;
  clearMessages();
  try {
    await healthApi.selectHealthGoal(goal.goalId, {});
    successMsg.value = `${goal.goalName} 已加入我的目标`;
    await loadPage();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    selectingGoalId.value = null;
  }
}

async function removeGoal(goal) {
  if (!window.confirm(`确认移除目标“${goal.goalName}”吗？`)) {
    return;
  }
  removingUserGoalId.value = goal.userGoalId;
  clearMessages();
  try {
    await healthApi.cancelHealthGoal(goal.userGoalId);
    successMsg.value = `${goal.goalName} 已从我的目标中移除`;
    await loadPage();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    removingUserGoalId.value = null;
  }
}

onMounted(loadPage);
</script>

<template>
  <section class="goals-page">
    <header class="goals-hero">
      <p class="hero-label">我的目标</p>
      <h2>健康目标</h2>
      <p class="hero-copy">选择适合你的健康目标，开始记录和追踪你的健康数据</p>
    </header>

    <section class="goal-shell">
      <div class="goal-shell-head">
        <div>
          <p class="section-tag">已选择目标</p>
          <h3>我的健康目标</h3>
        </div>
        <span class="goal-count">{{ myGoals.length }} 个进行中</span>
      </div>

      <div v-if="myGoals.length" class="chip-row">
        <button
          v-for="goal in myGoals"
          :key="goal.userGoalId"
          type="button"
          class="goal-chip active"
        >
          <span>{{ goal.goalName }}</span>
          <em @click.stop="removeGoal(goal)">
            {{ removingUserGoalId === goal.userGoalId ? "..." : "×" }}
          </em>
        </button>
      </div>
      <div v-else class="empty-inline">
        <p>你还没有选择健康目标，先从下面的列表中添加。</p>
      </div>
    </section>

    <section class="goal-shell">
      <div class="goal-shell-head">
        <div>
          <p class="section-tag">健康目标</p>
          <h3>选择你的目标</h3>
        </div>
      </div>

      <div class="goals-toolbar">
        <label class="goals-search-field">
          <span class="sr-only">搜索健康目标</span>
          <input
            v-model.trim="draftKeyword"
            placeholder="搜索健康目标..."
            @keyup.enter="searchGoals"
          />
        </label>
        <button type="button" class="goals-search-btn" @click="searchGoals">搜索</button>
        <button type="button" class="goals-reset-btn" @click="resetSearch">重置</button>
      </div>

      <p v-if="appliedKeyword" class="goals-feedback">
        共找到 {{ goals.length }} 个与“{{ appliedKeyword }}”相关的目标
      </p>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="success">{{ successMsg }}</p>
      <p v-if="loading" class="muted">健康目标加载中...</p>

      <div v-else-if="goals.length" class="goals-list">
        <article
          v-for="goal in goals"
          :key="goal.goalId"
          class="goal-card"
          :class="{ 'goal-card-active': selectedGoalIds.has(goal.goalId) }"
        >
          <div class="goal-icon">
            <svg
              v-if="goalIcon(goal) === 'metric'"
              viewBox="0 0 96 96"
              aria-hidden="true"
              class="goal-svg"
            >
              <rect x="18" y="26" width="60" height="42" rx="8" fill="#111315" />
              <circle cx="48" cy="19" r="15" fill="#fff" stroke="#111315" stroke-width="3" />
              <path
                d="M48 10v9l6 4"
                fill="none"
                stroke="#111315"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="3"
              />
              <path
                d="M31 56c2.8-5 7.1-8 12-8 2.2 0 3.6 1.1 3.6 3.1 0 4.6-2.7 13.7-2.7 18.8 0 1.7-1.2 2.7-2.8 2.7-2.9 0-8.5-3.3-10.1-7.7-.6-1.6-.7-5.2 0-8.9Z"
                fill="#fff"
              />
              <path
                d="M65 56c-2.8-5-7.1-8-12-8-2.2 0-3.6 1.1-3.6 3.1 0 4.6 2.7 13.7 2.7 18.8 0 1.7 1.2 2.7 2.8 2.7 2.9 0 8.5-3.3 10.1-7.7.6-1.6.7-5.2 0-8.9Z"
                fill="#fff"
              />
            </svg>
            <svg v-else viewBox="0 0 96 96" aria-hidden="true" class="goal-svg">
              <path
                d="M48 80c-1 0-2-.3-2.8-1C24.4 61.6 14 50.7 14 35.8 14 24.9 22.5 17 32.2 17c6.9 0 11.2 3.3 15.8 9.1 4.6-5.8 8.9-9.1 15.8-9.1C73.5 17 82 24.9 82 35.8c0 14.9-10.4 25.8-31.2 43.2-1 .7-1.8 1-2.8 1Z"
                fill="#f65b3b"
              />
              <path
                d="M20 48h13l7-12 8 26 8-18 6 10h14"
                fill="none"
                stroke="#fff"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="6"
              />
            </svg>
          </div>

          <div class="goal-copy-block">
            <div class="goal-title-row">
              <h4>{{ goal.goalName }}</h4>
              <span v-if="selectedGoalIds.has(goal.goalId)" class="goal-selected">已选择</span>
            </div>
            <p>{{ goal.goalDescription || "暂无目标说明" }}</p>
          </div>

          <div class="goal-side">
            <div class="goal-range">
              <span>{{ targetSummary(goal) }}</span>
            </div>
            <button
              v-if="!selectedGoalIds.has(goal.goalId)"
              type="button"
              class="goal-action"
              :disabled="selectingGoalId === goal.goalId"
              @click="selectGoal(goal)"
            >
              {{ selectingGoalId === goal.goalId ? "添加中..." : "加入目标" }}
            </button>
            <button
              v-else
              type="button"
              class="goal-action ghost"
              :disabled="removingUserGoalId === goal.userGoalId"
              @click="removeGoal(goal)"
            >
              {{ removingUserGoalId === goal.userGoalId ? "移除中..." : "移除目标" }}
            </button>
          </div>
        </article>
      </div>

      <div v-else class="goals-empty">
        <h4>没有找到匹配的健康目标</h4>
        <p>试试搜索 BMI、心率、体重 或 bpm。</p>
      </div>
    </section>
  </section>
</template>

<style scoped>
.goals-page {
  padding-bottom: 32px;
}

.goals-hero {
  padding: 16px 0 48px;
  text-align: center;
}

.hero-label {
  margin: 0;
  color: #8f9498;
  font-size: 16px;
}

.goals-hero h2 {
  margin: 18px 0 0;
  font-size: clamp(52px, 8vw, 84px);
  font-weight: 400;
  letter-spacing: 0.08em;
  line-height: 1.05;
}

.hero-copy {
  margin: 18px auto 0;
  max-width: 720px;
  color: #787d81;
  font-size: 18px;
  line-height: 1.8;
}

.goal-shell {
  margin-bottom: 22px;
  padding: 24px;
  border: 1px solid #dbdcdd;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 36px rgba(16, 18, 20, 0.04);
}

.goal-shell-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
}

.section-tag {
  margin: 0;
  color: #94999d;
  font-size: 14px;
  letter-spacing: 0.06em;
}

.goal-shell-head h3 {
  margin: 12px 0 0;
  font-size: clamp(30px, 4vw, 48px);
  font-weight: 400;
}

.goal-count {
  padding: 10px 14px;
  border-radius: 999px;
  background: #f4f4f2;
  color: #586068;
  font-size: 14px;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
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

.empty-inline {
  margin-top: 20px;
  color: #70767b;
}

.goals-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 340px) auto auto;
  gap: 12px;
  align-items: center;
  width: fit-content;
  margin-top: 30px;
}

.goals-search-field input {
  height: 58px;
  min-width: 0;
  border-radius: 14px;
  border: 1px solid #b5b7bb;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.96);
  font-size: 17px;
}

.goals-search-btn,
.goals-reset-btn,
.goal-action {
  min-height: 52px;
  border-radius: 14px;
  padding: 0 22px;
  font-size: 16px;
}

.goals-reset-btn,
.goal-action.ghost {
  border: 1px solid #c8c8c8;
  background: #fff;
  color: #2a2e31;
}

.goals-reset-btn:hover,
.goal-action.ghost:hover {
  background: #f3f4f5;
}

.goals-feedback {
  margin: 18px 0 0;
  color: #72777b;
  font-size: 14px;
}

.goals-list {
  display: grid;
  gap: 18px;
  margin-top: 22px;
}

.goal-card {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid #ececeb;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(16, 18, 20, 0.04);
}

.goal-card-active {
  border-color: #171a1d;
}

.goal-icon {
  width: 96px;
  height: 96px;
  border-radius: 18px;
  background: #f7f7f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.goal-svg {
  width: 76px;
  height: 76px;
  display: block;
}

.goal-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goal-title-row h4 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.goal-selected {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 10px;
  background: #121518;
  color: #fff;
  font-size: 12px;
}

.goal-copy-block p {
  margin: 12px 0 0;
  color: #74797d;
  line-height: 1.85;
  font-size: 15px;
}

.goal-side {
  display: grid;
  justify-items: end;
  gap: 12px;
}

.goal-range span {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 14px;
  background: #f4f4f2;
  color: #2e3337;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.goals-empty {
  padding: 48px 20px;
  border: 1px dashed #d8d8d4;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  text-align: center;
  margin-top: 22px;
}

.goals-empty h4 {
  margin: 0;
  font-size: 26px;
  font-weight: 500;
}

.goals-empty p {
  margin: 14px 0 0;
  color: #70767b;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 900px) {
  .goal-shell-head,
  .goal-card {
    grid-template-columns: 1fr;
  }

  .goals-toolbar {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .goal-side {
    justify-items: flex-start;
  }
}

@media (max-width: 640px) {
  .goal-shell {
    padding: 18px 16px;
  }

  .goal-title-row {
    flex-wrap: wrap;
  }

  .goal-range span,
  .goal-action {
    width: 100%;
    justify-content: center;
  }
}
</style>
