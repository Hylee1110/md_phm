<!--
  运动课程（用户）：浏览已发布课程列表并对课程评分。
-->
<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { healthApi } from "../services/healthApi";
import { buildAssetUrl } from "../services/http";

const coursesLoading = ref(false);
const checkinCourseId = ref(null);
const ratingCourseId = ref(null);
const errorMsg = ref("");
const successMsg = ref("");
const keyword = ref("");
const levelFilter = ref("");
/** 下拉选择依据；三角形选择 asc/desc。均为空则保持接口顺序 */
const sortField = ref("");
const sortDir = ref(null);
const courses = ref([]);
const detailCourse = ref(null);

const checkinDraftMap = reactive({});

const levelOptions = [
  { value: "", label: "全部难度" },
  { value: "beginner", label: "入门" },
  { value: "intermediate", label: "进阶" },
  { value: "advanced", label: "高阶" }
];

const ratingOptions = [1, 2, 3, 4, 5];

const beginnerCount = computed(() => courses.value.filter((c) => c.level === "beginner").length);
const intermediateCount = computed(() => courses.value.filter((c) => c.level === "intermediate").length);
const advancedCount = computed(() => courses.value.filter((c) => c.level === "advanced").length);

const displayCourses = computed(() => {
  let list = levelFilter.value
    ? courses.value.filter((c) => c.level === levelFilter.value)
    : [...courses.value];
  const key = sortField.value;
  const dir = sortDir.value;
  if (!key || !dir) {
    return list;
  }
  const d = dir === "asc" ? 1 : -1;
  return list.sort((a, b) => {
    let cmp = 0;
    if (key === "rating") {
      const na = Number(a.ratingAvg);
      const nb = Number(b.ratingAvg);
      const ra = Number.isFinite(na) ? na : 0;
      const rb = Number.isFinite(nb) ? nb : 0;
      cmp = ra - rb;
    } else if (key === "created") {
      cmp = parseServerTimeMs(a.createdAt) - parseServerTimeMs(b.createdAt);
    } else if (key === "updated") {
      cmp =
        parseServerTimeMs(a.updatedAt || a.createdAt) -
        parseServerTimeMs(b.updatedAt || b.createdAt);
    }
    if (cmp !== 0) {
      return cmp * d;
    }
    return Number(a.id) - Number(b.id);
  });
});

const currentListTitle = computed(() => {
  if (normalizeText(keyword.value)) {
    return "搜索结果";
  }
  if (levelFilter.value) {
    return `${levelText(levelFilter.value)}课程`;
  }
  return "所有课程";
});

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length > 0 ? text : null;
}

function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function toNonNegativeInt(value, fallback = 0) {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return fallback;
  }
  return Math.max(0, Math.floor(num));
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

function levelText(level) {
  const map = {
    all: "全部",
    beginner: "入门",
    intermediate: "进阶",
    advanced: "高阶"
  };
  return map[level] || "全部";
}

function formatRating(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num.toFixed(1) : "0.0";
}

function ratingSummary(course) {
  return `${formatRating(course.ratingAvg)} / 5 (${course.ratingCount || 0}人评分)`;
}

function parseServerTimeMs(raw) {
  if (raw == null) {
    return 0;
  }
  if (Array.isArray(raw)) {
    const [y, mo = 1, d = 1, h = 0, mi = 0, s = 0, ns = 0] = raw;
    return new Date(y, mo - 1, d, h, mi, s, Math.floor(ns / 1e6)).getTime();
  }
  const text = String(raw).trim();
  if (!text) {
    return 0;
  }
  const normalized = text.includes("T") ? text : text.replace(" ", "T");
  const t = Date.parse(normalized);
  return Number.isFinite(t) ? t : 0;
}

function onSortFieldChange() {
  sortDir.value = null;
}

function toggleSortDir(order) {
  if (!sortField.value) {
    return;
  }
  sortDir.value = sortDir.value === order ? null : order;
}

function isSortDirActive(order) {
  return Boolean(sortField.value) && sortDir.value === order;
}

function courseSummary(course) {
  if (normalizeText(course.summary)) {
    return course.summary;
  }
  if (normalizeText(course.description)) {
    return course.description;
  }
  return "查看课程介绍并创建运动记录。";
}

function courseImageUrl(course) {
  return buildAssetUrl(normalizeText(course?.coverUrl) || "");
}

function todayDate() {
  const now = new Date();
  const localNow = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return localNow.toISOString().slice(0, 10);
}

function joinWithSeparator(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return "--";
  }
  return items.join("、");
}

function defaultDurationParts(course) {
  const recommended = toNonNegativeInt(course.recommendDurationMin, 60);
  const fallback = recommended > 0 ? recommended : 60;
  return splitDurationMinutes(fallback);
}

function ensureCheckinDraft(course) {
  const key = String(course.id);
  if (!checkinDraftMap[key]) {
    const initial = defaultDurationParts(course);
    checkinDraftMap[key] = {
      durationHour: initial.hours,
      durationMinute: initial.minutes,
      date: todayDate(),
      note: ""
    };
  }
  return checkinDraftMap[key];
}

function hydrateCheckinDrafts() {
  for (const course of courses.value) {
    const draft = ensureCheckinDraft(course);
    if (!draft.date) {
      draft.date = todayDate();
    }
    const normalized = normalizeHourMinute(draft.durationHour, draft.durationMinute);
    draft.durationHour = normalized.hours;
    draft.durationMinute = normalized.minutes;
  }
}

function checkinDraft(course) {
  return ensureCheckinDraft(course);
}

function setCheckinHour(course, value) {
  const draft = ensureCheckinDraft(course);
  const normalized = normalizeHourMinute(value, draft.durationMinute);
  draft.durationHour = normalized.hours;
  draft.durationMinute = normalized.minutes;
}

function setCheckinMinute(course, value) {
  const draft = ensureCheckinDraft(course);
  const normalized = normalizeHourMinute(draft.durationHour, value);
  draft.durationHour = normalized.hours;
  draft.durationMinute = normalized.minutes;
}

function setCheckinDate(course, value) {
  const draft = ensureCheckinDraft(course);
  draft.date = value;
}

function setCheckinNote(course, value) {
  const draft = ensureCheckinDraft(course);
  draft.note = String(value ?? "").slice(0, 200);
}

function noteLength(course) {
  return ensureCheckinDraft(course).note.length;
}

function draftDurationLabel(course) {
  const draft = ensureCheckinDraft(course);
  return formatHourMinute(draft.durationHour, draft.durationMinute);
}

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function patchCourse(courseId, patch) {
  courses.value = courses.value.map((item) => {
    if (item.id !== courseId) {
      return item;
    }
    return { ...item, ...patch };
  });
  if (detailCourse.value?.id === courseId) {
    detailCourse.value = { ...detailCourse.value, ...patch };
  }
}

function syncDetailCourse() {
  if (!detailCourse.value) {
    return;
  }
  const refreshed = courses.value.find((item) => item.id === detailCourse.value.id);
  detailCourse.value = refreshed || null;
  if (refreshed) {
    ensureCheckinDraft(refreshed);
  }
}

async function loadCourses() {
  coursesLoading.value = true;
  clearMessages();
  try {
    // 用户端课程广场：关键词 + limit；难度/排序由前端 displayCourses 二次处理
    courses.value = await healthApi.listSportCourses({
      keyword: normalizeText(keyword.value),
      limit: 60
    });
    hydrateCheckinDrafts();
    syncDetailCourse();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    coursesLoading.value = false;
  }
}

function resetSearch() {
  keyword.value = "";
  levelFilter.value = "";
  sortField.value = "";
  sortDir.value = null;
  loadCourses();
}

function openDetail(course) {
  detailCourse.value = course;
  ensureCheckinDraft(course);
}

function closeDetail() {
  detailCourse.value = null;
}

function starClass(course, score) {
  return Number(course.userScore || 0) >= score ? "active" : "";
}

async function submitCourseRating(course, score) {
  ratingCourseId.value = course.id;
  clearMessages();
  try {
    // 评分后接口返回新的平均分与当前用户分数，用于就地更新卡片与详情
    const rating = await healthApi.rateSportCourse(course.id, { score });
    patchCourse(course.id, {
      ratingAvg: rating.ratingAvg,
      ratingCount: rating.ratingCount,
      userScore: rating.userScore
    });
    successMsg.value = `已为 ${course.name} 打 ${score} 分`;
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    ratingCourseId.value = null;
  }
}

async function completeCheckin(course) {
  // 从课程详情「打卡」：写入一条运动记录（时长换算热量、固定时刻占位等由产品约定）
  const draft = ensureCheckinDraft(course);
  const date = normalizeText(draft.date);
  const normalized = normalizeHourMinute(draft.durationHour, draft.durationMinute);
  const durationMin = normalized.hours * 60 + normalized.minutes;

  draft.durationHour = normalized.hours;
  draft.durationMinute = normalized.minutes;

  if (!date) {
    errorMsg.value = "请选择运动日期";
    return;
  }
  if (durationMin <= 0) {
    errorMsg.value = "运动时长必须大于 0 分钟";
    return;
  }

  const caloriesPerHour = toNumber(course.caloriesPerHour, 0);
  const durationHourDecimal = durationMin / 60;
  const caloriesKcal = caloriesPerHour > 0 ? (durationHourDecimal * caloriesPerHour).toFixed(2) : null;

  checkinCourseId.value = course.id;
  clearMessages();
  try {
    await healthApi.createExerciseRecord({
      sportId: course.id,
      sportName: null,
      // 快捷打卡未采集具体时刻，使用固定 08:00 占位（与后端存储格式一致）
      recordTime: `${date}T08:00`,
      durationMin,
      caloriesKcal,
      note: normalizeText(draft.note),
      dataSource: "manual",
      externalId: null
    });
    successMsg.value = `${course.name} 记录已创建`;
    draft.note = "";
    draft.date = todayDate();
    closeDetail();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    checkinCourseId.value = null;
  }
}

onMounted(loadCourses);
</script>

<template>
  <section class="sport-hero">
    <span class="hero-pill">科学运动</span>
    <h2>找到适合你的训练</h2>
    <p>量力而行，循序渐进，把每一次运动都记录清楚、更好坚持。</p>
  </section>

  <section class="catalog-card">
    <div class="catalog-head">
      <div class="catalog-copy">
        <p class="section-tag">课程列表</p>
        <h3>{{ currentListTitle }}</h3>
      </div>
      <div class="catalog-meta">
        <span><b>{{ courses.length }}</b> 门课程</span>
        <span><b>{{ beginnerCount }}</b> 入门</span>
        <span><b>{{ intermediateCount }}</b> 进阶</span>
        <span><b>{{ advancedCount }}</b> 高阶</span>
      </div>
    </div>

    <div class="catalog-toolbar">
      <div class="search-shell">
        <input
          v-model.trim="keyword"
          placeholder="搜索课程名称"
          @keyup.enter="loadCourses"
        />
        <button type="button" @click="loadCourses" :disabled="coursesLoading">
          {{ coursesLoading ? "搜索中..." : "搜索" }}
        </button>
        <button class="ghost-btn" type="button" @click="resetSearch" :disabled="coursesLoading">
          重置
        </button>
      </div>
    </div>

    <div class="filter-row">
      <button
        v-for="item in levelOptions"
        :key="item.value || 'all'"
        type="button"
        class="filter-pill"
        :class="{ active: levelFilter === item.value }"
        @click="levelFilter = item.value"
      >
        {{ item.label }}
      </button>
      <div class="sort-toolbar">
        <label class="sort-select-label">
          <select
            v-model="sortField"
            class="sort-select"
            aria-label="选择排序依据"
            @change="onSortFieldChange"
          >
            <option value="">默认顺序</option>
            <option value="rating">评分</option>
            <option value="created">注册时间</option>
            <option value="updated">修改时间</option>
          </select>
        </label>
        <div class="sort-field sort-dir-field">
          <div class="sort-arrows" role="group" :aria-label="sortField ? '升序或降序' : '请先选择排序依据'">
            <button
              type="button"
              class="sort-arrow"
              :class="{ active: isSortDirActive('asc') }"
              :disabled="!sortField"
              title="正序（从低到早），再点取消；需先选择排序依据"
              aria-label="正序"
              @click="toggleSortDir('asc')"
            >
              <span class="sort-tri sort-tri-up" aria-hidden="true"></span>
            </button>
            <button
              type="button"
              class="sort-arrow"
              :class="{ active: isSortDirActive('desc') }"
              :disabled="!sortField"
              title="逆序（从高到晚），再点取消；需先选择排序依据"
              aria-label="逆序"
              @click="toggleSortDir('desc')"
            >
              <span class="sort-tri sort-tri-down" aria-hidden="true"></span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <p class="catalog-tip">在列表中查看概要，点「添加记录」可评分并写入运动记录。</p>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>
    <p v-if="coursesLoading" class="muted">课程加载中...</p>

    <div v-else class="course-list">
      <article
        v-for="course in displayCourses"
        :key="course.id"
        class="course-row"
        :class="{ 'course-row-highlight': detailCourse?.id === course.id }"
      >
        <div class="course-thumb">
          <img
            v-if="course.coverUrl"
            :src="courseImageUrl(course)"
            :alt="course.name"
          />
          <span v-else>{{ course.name?.slice(0, 1) || "动" }}</span>
        </div>

        <div class="course-content">
          <div class="course-labels">
            <span class="level-chip">{{ levelText(course.level) }}</span>
            <span class="rating-chip">★ {{ ratingSummary(course) }}</span>
          </div>

          <h4>{{ course.name }}</h4>
          <p class="course-desc">{{ courseSummary(course) }}</p>

          <div class="course-meta">
            <span>建议时长 {{ formatDuration(course.recommendDurationMin) }}</span>
            <span>建议频次 {{ toNumber(course.recommendFrequencyPerWeek, 0) }} 次/周</span>
            <span>我的评分 {{ course.userScore || "--" }}</span>
          </div>
        </div>

        <div class="course-side">
          <div class="energy-box">
            <div class="energy-main">
              <strong>{{ toNumber(course.caloriesPerHour, 0) }}</strong>
              <span>kcal / 小时</span>
            </div>
            <em>按实际运动时长换算消耗</em>
          </div>

          <div class="row-actions">
            <button class="add-record-btn" type="button" @click="openDetail(course)">
              ＋ 添加记录
            </button>
          </div>
        </div>
      </article>

      <div v-if="displayCourses.length === 0" class="empty-state">
        <h4>没有找到匹配的课程</h4>
        <p>可以调整关键词、切换难度筛选，或稍后再试。</p>
      </div>
    </div>
  </section>

  <div v-if="detailCourse" class="detail-mask" @click.self="closeDetail">
    <section class="detail-panel">
      <div class="detail-top">
        <div>
          <span class="detail-badge">课程详情</span>
          <h4>{{ detailCourse.name }}</h4>
          <p class="detail-subtitle">{{ courseSummary(detailCourse) }}</p>
        </div>
        <div class="inline-actions detail-actions">
          <button class="ghost-btn close-btn" type="button" @click="closeDetail">关闭</button>
        </div>
      </div>

      <div class="detail-layout">
        <div class="detail-left">
          <div class="detail-image">
            <img
              v-if="detailCourse.coverUrl"
              :src="courseImageUrl(detailCourse)"
              :alt="detailCourse.name"
            />
            <span v-else>暂无课程封面</span>
          </div>

          <div class="detail-summary-grid">
            <div class="summary-cell">
              <span>难度</span>
              <strong>{{ levelText(detailCourse.level) }}</strong>
            </div>
            <div class="summary-cell">
              <span>建议时长</span>
              <strong>{{ formatDuration(detailCourse.recommendDurationMin) }}</strong>
            </div>
            <div class="summary-cell">
              <span>每小时消耗</span>
              <strong>{{ toNumber(detailCourse.caloriesPerHour, 0) }} kcal</strong>
            </div>
          </div>

          <div class="detail-copy">
            <h5>课程介绍</h5>
            <p>{{ detailCourse.description || detailCourse.summary || "暂无详细介绍" }}</p>
          </div>

          <div class="detail-copy">
            <h5>训练要点</h5>
            <div class="detail-tags">
              <p><b>适合人群：</b>{{ joinWithSeparator(detailCourse.audiences) }}</p>
              <p><b>器材：</b>{{ joinWithSeparator(detailCourse.equipments) }}</p>
              <p><b>收益：</b>{{ joinWithSeparator(detailCourse.benefits) }}</p>
            </div>
          </div>
        </div>

        <aside class="detail-right">
          <div class="side-stack">
            <section class="side-card">
              <h5>为课程评分</h5>
              <p class="muted rating-note">点击星级即可提交评分。</p>
              <div class="rating-stars">
                <button
                  v-for="score in ratingOptions"
                  :key="score"
                  type="button"
                  class="star-btn"
                  :class="starClass(detailCourse, score)"
                  :disabled="ratingCourseId === detailCourse.id"
                  @click="submitCourseRating(detailCourse, score)"
                >
                  ★
                </button>
              </div>
              <p class="rating-meta">当前综合评分 {{ ratingSummary(detailCourse) }}</p>
            </section>

            <section class="side-card">
              <h5>创建运动记录</h5>

              <label class="form-field">
                <span>运动课程</span>
                <input :value="detailCourse.name" readonly />
              </label>

              <div class="inline-field-row">
                <label class="form-field">
                  <span><i>*</i> 小时</span>
                  <input
                    type="number"
                    min="0"
                    :value="checkinDraft(detailCourse).durationHour"
                    @input="setCheckinHour(detailCourse, $event.target.value)"
                  />
                </label>

                <label class="form-field">
                  <span><i>*</i> 分钟</span>
                  <input
                    type="number"
                    min="0"
                    :value="checkinDraft(detailCourse).durationMinute"
                    @input="setCheckinMinute(detailCourse, $event.target.value)"
                  />
                </label>
              </div>

              <div class="preview-box">
                <p>本次时长：{{ draftDurationLabel(detailCourse) }}</p>
              </div>

              <label class="form-field">
                <span><i>*</i> 运动日期</span>
                <input
                  type="date"
                  :value="checkinDraft(detailCourse).date"
                  @input="setCheckinDate(detailCourse, $event.target.value)"
                />
              </label>

              <label class="form-field note-field">
                <span>运动笔记</span>
                <textarea
                  :value="checkinDraft(detailCourse).note"
                  maxlength="200"
                  placeholder="输入本次运动记录"
                  @input="setCheckinNote(detailCourse, $event.target.value)"
                ></textarea>
                <em class="count">{{ noteLength(detailCourse) }}</em>
              </label>

              <div class="action-row">
                <button class="ghost-btn" type="button" @click="closeDetail">取消</button>
                <button
                  type="button"
                  @click="completeCheckin(detailCourse)"
                  :disabled="checkinCourseId === detailCourse.id"
                >
                  {{ checkinCourseId === detailCourse.id ? "提交中..." : "保存记录" }}
                </button>
              </div>
            </section>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.sport-hero {
  padding: 18px 0 42px;
  text-align: center;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 18px;
  border-radius: 999px;
  background: rgba(17, 19, 21, 0.96);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.sport-hero h2 {
  margin: 26px 0 0;
  font-size: clamp(42px, 8vw, 72px);
  font-weight: 400;
  letter-spacing: 0.06em;
  line-height: 1.08;
}

.sport-hero p {
  margin: 20px auto 0;
  max-width: 620px;
  color: #6f7478;
  font-size: 20px;
  line-height: 1.8;
}

.catalog-card {
  position: relative;
  margin-bottom: 20px;
  padding: 28px 22px 8px;
  border: 1px solid #dddddb;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 42px rgba(18, 18, 18, 0.06);
}

.catalog-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
}

.section-tag {
  margin: 0;
  color: #888c90;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.catalog-copy h3 {
  margin: 14px 0 0;
  font-size: clamp(34px, 5vw, 56px);
  font-weight: 400;
}

.catalog-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  color: #74797d;
}

.catalog-meta span {
  padding: 10px 14px;
  border-radius: 999px;
  background: #f4f4f2;
  font-size: 13px;
}

.catalog-meta b {
  color: #1f2326;
  font-size: 18px;
}

.catalog-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 28px;
}

.search-shell {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.search-shell input {
  flex: 1 1 280px;
  max-width: 360px;
  height: 46px;
  border-radius: 12px;
  border: 1px solid #bdbdb8;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.96);
  font-size: 15px;
}

.search-shell button {
  min-height: 46px;
  padding: 0 18px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1;
}

.ghost-btn {
  background: #fff;
  color: #2a2c2e;
  border: 1px solid #c7c7c7;
}

.ghost-btn:hover {
  background: #f2f3f5;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.filter-pill {
  padding: 10px 16px;
  border-radius: 999px;
  background: #f5f5f3;
  color: #586068;
  border: 1px solid transparent;
}

.filter-pill.active {
  background: rgba(17, 19, 21, 0.94);
  color: #fff;
}

.sort-toolbar {
  margin-left: auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.sort-select-label {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  min-width: 6.5em;
  max-width: min(200px, 42vw);
  margin: 0;
  padding: 4px 22px 4px 0;
  border: none;
  border-radius: 0;
  box-shadow: none;
  background-color: transparent;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6' viewBox='0 0 10 6'%3E%3Cpath fill='%2373787c' d='M0 0l5 6 5-6z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 2px center;
  font-size: 14px;
  font-family: inherit;
  color: #2a2c2e;
  cursor: pointer;
}

.sort-select:focus {
  outline: none;
}

.sort-select:focus-visible {
  outline: 2px solid rgba(17, 19, 21, 0.22);
  outline-offset: 2px;
  border-radius: 4px;
}

.sort-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-arrows {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
}

.sort-arrow {
  width: 24px;
  height: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  cursor: pointer;
}

.sort-tri {
  display: block;
  width: 0;
  height: 0;
  border-style: solid;
  border-color: transparent;
}

.sort-tri-up {
  border-width: 0 4px 5px 4px;
  border-bottom-color: #b4b9be;
}

.sort-tri-down {
  border-width: 5px 4px 0 4px;
  border-top-color: #b4b9be;
}

.sort-arrow:hover:not(:disabled) {
  background: transparent;
}

.sort-arrow:hover:not(:disabled) .sort-tri-up {
  border-bottom-color: #111315;
}

.sort-arrow:hover:not(:disabled) .sort-tri-down {
  border-top-color: #111315;
}

.sort-arrow.active {
  background: transparent;
}

.sort-arrow.active .sort-tri-up {
  border-bottom-color: #111315;
}

.sort-arrow.active .sort-tri-down {
  border-top-color: #111315;
}

.sort-arrow.active:hover {
  background: transparent;
}

.sort-arrow:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.sort-arrow:disabled:hover {
  background: transparent;
}

.catalog-tip {
  margin: 18px 0 6px;
  color: #73787c;
  font-size: 14px;
}

.course-list {
  display: grid;
  gap: 18px;
  margin-top: 22px;
}

.course-row {
  position: relative;
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid #e7e6e3;
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbf9 100%);
  box-shadow: 0 10px 22px rgba(20, 20, 20, 0.04);
}

.course-row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 20px;
  bottom: 20px;
  width: 4px;
  border-radius: 999px;
  background: transparent;
}

.course-row:hover::before,
.course-row-highlight::before {
  background: #15181b;
}

.course-thumb {
  width: 110px;
  height: 110px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1f24, #38434b);
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-thumb span {
  color: #fff;
  font-size: 44px;
}

.course-content {
  min-width: 0;
}

.course-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.level-chip,
.rating-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.level-chip {
  background: #f0f3f5;
  color: #586068;
}

.rating-chip {
  background: #f7f2eb;
  color: #8d5d21;
}

.course-content h4 {
  margin: 0;
  font-size: 34px;
  font-weight: 600;
}

.course-desc {
  margin: 14px 0 0;
  color: #5d656d;
  line-height: 1.8;
  font-size: 15px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 14px;
  color: #8a9095;
  font-size: 13px;
}

.course-side {
  display: grid;
  justify-items: end;
  gap: 14px;
  min-width: 184px;
}

.energy-box {
  padding: 12px 16px;
  border-radius: 14px;
  background: #f4f4f2;
  min-width: 132px;
}

.energy-main {
  display: flex;
  align-items: baseline;
  gap: 5px;
  color: #212426;
}

.energy-main strong {
  font-size: 20px;
}

.energy-box em {
  display: block;
  margin-top: 4px;
  color: #92979b;
  font-style: normal;
  font-size: 12px;
}

.row-actions {
  display: grid;
  gap: 10px;
}

.add-record-btn {
  min-width: 148px;
  border-radius: 14px;
  padding-inline: 18px;
}

.empty-state {
  padding: 46px 20px 52px;
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

.detail-panel {
  width: min(1180px, 96vw);
  max-height: 92vh;
  border-radius: 24px;
  border: 1px solid #d9dcdf;
  background: #fff;
  overflow: auto;
  box-shadow: 0 24px 50px rgba(10, 12, 14, 0.18);
}

.detail-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  border-bottom: 1px solid #eceef1;
  padding: 22px 24px;
}

.detail-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 6px 12px;
  background: rgba(17, 19, 21, 0.94);
  color: #fff;
  font-size: 12px;
}

.detail-top h4 {
  margin: 16px 0 0;
  font-size: 36px;
}

.detail-subtitle {
  margin: 12px 0 0;
  color: #666f79;
  line-height: 1.8;
  max-width: 640px;
}

.detail-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.close-btn {
  min-width: 84px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  min-height: 620px;
}

.detail-left {
  border-right: 1px solid #eceef1;
  padding: 20px 24px 24px;
}

.detail-image {
  width: 100%;
  min-height: 320px;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid #eceef1;
  background: linear-gradient(135deg, #f3e7d8 0%, #f9f7f3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-image img {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
}

.detail-image span {
  color: #7d7469;
}

.detail-summary-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-cell {
  border: 1px solid #e6e8eb;
  border-radius: 14px;
  padding: 14px 16px;
  background: #fbfbfa;
}

.summary-cell span {
  display: block;
  color: #7b838d;
  font-size: 12px;
}

.summary-cell strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
}

.detail-copy {
  margin-top: 18px;
  padding: 18px;
  border-radius: 16px;
  background: #fafbfb;
  border: 1px solid #eceef1;
}

.detail-copy h5,
.side-card h5 {
  margin: 0 0 12px;
  font-size: 19px;
}

.detail-copy p {
  margin: 0;
  color: #414950;
  line-height: 1.9;
  white-space: pre-wrap;
}

.detail-tags p {
  margin: 8px 0;
  color: #4a535d;
}

.detail-right {
  padding: 18px;
  background: #f8f9fa;
}

.side-stack {
  display: grid;
  gap: 14px;
}

.side-card {
  border: 1px solid #e5e8eb;
  border-radius: 18px;
  background: #fff;
  padding: 16px;
}

.rating-note {
  margin: 0;
}

.rating-stars {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.star-btn {
  min-width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid #dadada;
  background: #fff;
  color: #b7b7b7;
  font-size: 22px;
  line-height: 1;
}

.star-btn.active {
  color: #d07a22;
  border-color: #efc08f;
  background: #fff7ef;
}

.rating-meta {
  margin: 12px 0 0;
  color: #5f6872;
  font-size: 13px;
}

.form-field {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.form-field:first-of-type {
  margin-top: 0;
}

.form-field span {
  color: #5d646d;
  font-size: 14px;
}

.form-field i {
  color: #b83f3f;
  font-style: normal;
}

.inline-field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 12px;
}

.preview-box {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f5f7f8;
  color: #44505b;
  font-size: 13px;
}

.preview-box p {
  margin: 0;
}

.note-field {
  position: relative;
  padding-bottom: 20px;
}

.note-field textarea {
  min-height: 150px;
  resize: none;
}

.count {
  position: absolute;
  right: 2px;
  bottom: 0;
  color: #956f5f;
  font-size: 12px;
  font-style: normal;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.action-row button {
  min-width: 110px;
}

@media (max-width: 1040px) {
  .course-row {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .course-side {
    grid-column: 1 / -1;
    grid-template-columns: 1fr auto;
    width: 100%;
    justify-items: stretch;
    align-items: center;
  }

  .row-actions {
    grid-auto-flow: column;
    justify-content: end;
  }

  .detail-layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .detail-left {
    border-right: none;
    border-bottom: 1px solid #eceef1;
  }
}

@media (max-width: 860px) {
  .catalog-head,
  .catalog-toolbar,
  .detail-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .catalog-meta {
    justify-content: flex-start;
  }

  .search-shell {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-shell input {
    max-width: none;
  }

  .sort-toolbar {
    margin-left: 0;
    width: 100%;
    justify-content: flex-start;
  }

  .sort-select-label {
    width: 100%;
  }

  .sort-select {
    flex: 1 1 auto;
    min-width: 0;
    max-width: none;
  }

  .sort-dir-field {
    width: 100%;
  }

  .course-content h4 {
    font-size: 28px;
  }

  .detail-top h4 {
    font-size: 30px;
  }

  .detail-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .sport-hero {
    padding-bottom: 24px;
  }

  .sport-hero p {
    font-size: 16px;
  }

  .catalog-card {
    padding-inline: 16px;
  }

  .course-row {
    grid-template-columns: 1fr;
  }

  .course-thumb {
    width: 100%;
    height: 180px;
  }

  .course-side {
    grid-template-columns: 1fr;
  }

  .row-actions {
    grid-auto-flow: row;
  }

  .action-row {
    flex-direction: column;
    width: 100%;
  }

  .row-actions button,
  .action-row button {
    width: 100%;
  }

  .inline-field-row {
    grid-template-columns: 1fr;
  }
}
</style>
