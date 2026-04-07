<!--
  管理端 - 运动课程：列表、详情编辑、字典项维护、封面上传与发布状态管理。
-->
<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { adminApi } from "../services/adminApi";
import { buildAssetUrl } from "../services/http";

const loading = ref(false);
const saving = ref(false);
const uploadingCover = ref(false);
const deletingId = ref(null);
const creatingOptionType = ref("");
const errorMsg = ref("");
const successMsg = ref("");
const keyword = ref("");
const statusFilter = ref("");
const courses = ref([]);
const editingCourseId = ref(null);
const loadingDetailId = ref(null);
const coverInputRef = ref(null);
const formModalOpen = ref(false);
const modalPanelRef = ref(null);

const options = ref({
  audiences: [],
  equipments: [],
  benefits: []
});

const optionDraft = ref({
  audience: "",
  equipment: "",
  benefit: ""
});

const statusOptions = [
  { value: "draft", label: "草稿" },
  { value: "published", label: "已发布" },
  { value: "archived", label: "已归档" }
];

const levelOptions = [
  { value: "all", label: "全部" },
  { value: "beginner", label: "入门" },
  { value: "intermediate", label: "进阶" },
  { value: "advanced", label: "高阶" }
];

const form = ref(defaultForm());

function defaultForm() {
  return {
    name: "",
    coverUrl: "",
    summary: "",
    description: "",
    recommendDurationMin: 60,
    caloriesPerHour: 300,
    recommendFrequencyPerWeek: 3,
    level: "all",
    status: "published",
    sortWeight: 0,
    audienceIds: [],
    equipmentIds: [],
    benefitIds: []
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

function resetCoverInput() {
  if (coverInputRef.value) {
    coverInputRef.value.value = "";
  }
}

function resetFormState() {
  // 新建/编辑公用同一个表单：关闭或重置时要清空“当前编辑课程”与封面输入框
  editingCourseId.value = null;
  form.value = defaultForm();
  resetCoverInput();
}

function closeFormModal() {
  formModalOpen.value = false;
  resetFormState();
}

async function openCreateModal() {
  // 新建课程：打开弹窗并聚焦面板，便于键盘操作
  clearMessages();
  resetFormState();
  formModalOpen.value = true;
  await focusModalPanel();
}

async function focusModalPanel() {
  await nextTick();
  modalPanelRef.value?.focus?.();
}

function clearCover() {
  form.value.coverUrl = "";
  resetCoverInput();
}

function statusText(status) {
  const found = statusOptions.find((item) => item.value === status);
  return found ? found.label : status;
}

function levelText(level) {
  const found = levelOptions.find((item) => item.value === level);
  return found ? found.label : level;
}

function toggleMulti(field, id, checked) {
  // 多选字典项（受众/器械/收益）维护成数组：用 Set 做去重与删除
  const current = new Set(form.value[field]);
  if (checked) {
    current.add(id);
  } else {
    current.delete(id);
  }
  form.value[field] = Array.from(current);
}

function isChecked(field, id) {
  return form.value[field].includes(id);
}

function appendUniqueOption(type, option) {
  const map = {
    audience: "audiences",
    equipment: "equipments",
    benefit: "benefits"
  };
  const listKey = map[type];
  const currentList = options.value[listKey] || [];
  const exists = currentList.some((item) => item.id === option.id);
  if (!exists) {
    options.value[listKey] = [...currentList, option].sort((a, b) => Number(a.id) - Number(b.id));
  }
}

async function createOption(type) {
  // 在线新增字典项：创建成功后自动加入 options 列表，并默认勾选到当前课程表单
  const name = normalizeText(optionDraft.value[type]);
  if (!name) {
    errorMsg.value = "请输入选项名称";
    return;
  }

  creatingOptionType.value = type;
  clearMessages();
  try {
    let option = null;
    if (type === "audience") {
      option = await adminApi.createAudienceOption(name);
      appendUniqueOption(type, option);
      toggleMulti("audienceIds", option.id, true);
    } else if (type === "equipment") {
      option = await adminApi.createEquipmentOption(name);
      appendUniqueOption(type, option);
      toggleMulti("equipmentIds", option.id, true);
    } else {
      option = await adminApi.createBenefitOption(name);
      appendUniqueOption(type, option);
      toggleMulti("benefitIds", option.id, true);
    }
    optionDraft.value[type] = "";
    successMsg.value = "选项已添加并自动勾选";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    creatingOptionType.value = "";
  }
}

async function uploadCover(event) {
  // 上传封面：成功后仅写入 coverUrl，真正保存课程在提交表单时发生
  const [file] = event.target.files ?? [];
  if (!file) {
    return;
  }

  uploadingCover.value = true;
  clearMessages();
  try {
    const uploaded = await adminApi.uploadSportCourseCover(file);
    form.value.coverUrl = uploaded.coverUrl ?? "";
    successMsg.value = "封面上传成功";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    uploadingCover.value = false;
    resetCoverInput();
  }
}

function toPayload() {
  // 提交前统一做空串转 null、数字字段 Number 化；关联 ID 数组原样提交
  const name = normalizeText(form.value.name);
  if (!name) {
    throw new Error("课程名称不能为空");
  }

  return {
    name,
    coverUrl: normalizeText(form.value.coverUrl),
    summary: normalizeText(form.value.summary),
    description: normalizeText(form.value.description),
    recommendDurationMin: Number(form.value.recommendDurationMin),
    caloriesPerHour: Number(form.value.caloriesPerHour),
    recommendFrequencyPerWeek: Number(form.value.recommendFrequencyPerWeek),
    level: form.value.level,
    status: form.value.status,
    sortWeight: Number(form.value.sortWeight || 0),
    audienceIds: form.value.audienceIds,
    equipmentIds: form.value.equipmentIds,
    benefitIds: form.value.benefitIds
  };
}

function fillForm(course) {
  // 列表行或详情接口返回均可填充；缺省字段用 defaultForm 中的业务默认值兜底
  form.value = {
    name: course.name ?? "",
    coverUrl: course.coverUrl ?? "",
    summary: course.summary ?? "",
    description: course.description ?? "",
    recommendDurationMin: course.recommendDurationMin ?? 60,
    caloriesPerHour: course.caloriesPerHour ?? 300,
    recommendFrequencyPerWeek: course.recommendFrequencyPerWeek ?? 3,
    level: course.level ?? "all",
    status: course.status ?? "published",
    sortWeight: course.sortWeight ?? 0,
    audienceIds: course.audienceIds ?? [],
    equipmentIds: course.equipmentIds ?? [],
    benefitIds: course.benefitIds ?? []
  };
  resetCoverInput();
}

async function loadOptions() {
  // 受众/器械/功效三类字典，供表单多选与在线新增选项使用
  options.value = await adminApi.getSportCourseOptions();
}

async function loadCourses(preserveMessages = false) {
  loading.value = true;
  if (!preserveMessages) {
    clearMessages();
  }
  try {
    // status 为空表示不按状态过滤；keyword 支持按名称检索
    courses.value = await adminApi.listSportCourses({
      keyword: normalizeText(keyword.value),
      status: normalizeText(statusFilter.value)
    });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function editCourse(course) {
  const courseId = course?.id;
  if (courseId == null || courseId === "") {
    errorMsg.value = "课程数据异常，无法编辑";
    return;
  }

  clearMessages();
  formModalOpen.value = true;
  editingCourseId.value = courseId;
  // 先用列表行数据即时填充，避免弹窗空白；再以详情接口覆盖（含完整关联 ID）
  fillForm(course);
  await focusModalPanel();

  loadingDetailId.value = courseId;
  try {
    const detail = await adminApi.getSportCourse(courseId);
    fillForm(detail);
  } catch (error) {
    errorMsg.value =
      error.message ||
      "加载课程详情失败（已用列表数据填充表单，适宜人群/器材/功效可能不完整，请稍后重试）";
  } finally {
    loadingDetailId.value = null;
  }
}

async function submitCourse() {
  if (uploadingCover.value) {
    errorMsg.value = "请等待封面上传完成";
    return;
  }

  saving.value = true;
  clearMessages();
  try {
    const payload = toPayload();
    // editingCourseId 有值表示更新，否则为新建；成功后刷新列表并关弹窗
    if (editingCourseId.value) {
      await adminApi.updateSportCourse(editingCourseId.value, payload);
      successMsg.value = "课程已更新";
    } else {
      await adminApi.createSportCourse(payload);
      successMsg.value = "课程已创建";
    }
    await loadCourses(true);
    closeFormModal();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    saving.value = false;
  }
}

async function removeCourse(courseId) {
  // 若正在编辑同一门课，删成功后需关弹窗，避免编辑已不存在的 ID
  if (!window.confirm(`确认删除课程 #${courseId} 吗？`)) {
    return;
  }

  deletingId.value = courseId;
  clearMessages();
  try {
    await adminApi.deleteSportCourse(courseId);
    successMsg.value = "课程已删除";
    if (Number(editingCourseId.value) === Number(courseId)) {
      closeFormModal();
    }
    await loadCourses(true);
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    deletingId.value = null;
  }
}

function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return String(value).replace("T", " ");
}

// 全屏弹窗打开时禁止背景滚动，关闭时恢复（组件卸载时兜底清理）
watch(formModalOpen, (open) => {
  document.body.style.overflow = open ? "hidden" : "";
});

onBeforeUnmount(() => {
  document.body.style.overflow = "";
});

onMounted(async () => {
  clearMessages();
  try {
    // 先字典后列表：列表渲染不依赖字典，但表单依赖 options
    await loadOptions();
    await loadCourses();
  } catch (error) {
    errorMsg.value = error.message;
  }
});
</script>

<template>
  <section class="ink-card">
    <div class="row-head">
      <h3>课程管理</h3>
      <div class="inline-actions query-wrap">
        <input v-model.trim="keyword" placeholder="按课程名搜索" @keyup.enter="loadCourses" />
        <select v-model="statusFilter">
          <option value="">全部状态</option>
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <button @click="loadCourses" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
        <button type="button" class="ghost-btn" @click="openCreateModal">新建课程</button>
      </div>
    </div>

    <p v-if="!formModalOpen && errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="!formModalOpen && successMsg" class="success">{{ successMsg }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>状态</th>
            <th>难度</th>
            <th>评分</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.id">
            <td>{{ course.id }}</td>
            <td>{{ course.name }}</td>
            <td>{{ statusText(course.status) }}</td>
            <td>{{ levelText(course.level) }}</td>
            <td>{{ Number(course.ratingAvg || 0).toFixed(1) }} / {{ course.ratingCount || 0 }}</td>
            <td>{{ formatDateTime(course.updatedAt) }}</td>
            <td>
              <div class="inline-actions">
                <button
                  type="button"
                  @click="editCourse(course)"
                  :disabled="loadingDetailId === course.id"
                >
                  {{ loadingDetailId === course.id ? "加载中..." : "编辑" }}
                </button>
                <button type="button" @click="removeCourse(course.id)" :disabled="deletingId === course.id">
                  {{ deletingId === course.id ? "删除中..." : "删除" }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && courses.length === 0">
            <td colspan="7" class="muted">暂无课程</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <Teleport to="body">
    <div
      v-if="formModalOpen"
      class="course-modal-backdrop"
      @click.self="closeFormModal"
    >
      <div
        ref="modalPanelRef"
        class="course-modal-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="course-modal-title"
        tabindex="-1"
        @keydown.escape.prevent="closeFormModal"
      >
        <div class="row-head course-modal-head">
          <h3 id="course-modal-title">
            {{ editingCourseId ? `编辑课程 #${editingCourseId}` : "新增课程" }}
          </h3>
          <div class="inline-actions">
            <button type="button" class="ghost-btn" @click="closeFormModal">关闭</button>
            <button type="button" @click="submitCourse" :disabled="saving || uploadingCover">
              {{ saving ? "保存中..." : editingCourseId ? "更新课程" : "创建课程" }}
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="error course-modal-msg">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success course-modal-msg">{{ successMsg }}</p>

        <form class="form-grid course-modal-form" @submit.prevent="submitCourse">
      <label>
        课程名称
        <input v-model.trim="form.name" maxlength="64" required />
      </label>
      <label class="full-width">
        课程封面
        <div class="cover-upload">
          <input
            ref="coverInputRef"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            :disabled="uploadingCover"
            @change="uploadCover"
          />
          <div class="cover-upload-actions">
            <button
              type="button"
              class="ghost-btn"
              :disabled="uploadingCover || !form.coverUrl"
              @click="clearCover"
            >
              清空封面
            </button>
            <span class="muted">
              {{ uploadingCover ? "上传中..." : "支持 PNG/JPG/JPEG/WebP/GIF，本地上传，最大 5MB" }}
            </span>
          </div>
          <img
            v-if="form.coverUrl"
            class="cover-preview"
            :src="buildAssetUrl(form.coverUrl)"
            alt="课程封面预览"
          />
          <p v-if="form.coverUrl" class="muted cover-path">{{ form.coverUrl }}</p>
        </div>
      </label>
      <label class="full-width">
        卡片简介
        <input v-model.trim="form.summary" maxlength="255" />
      </label>
      <label class="full-width">
        详细介绍
        <textarea v-model.trim="form.description" rows="3"></textarea>
      </label>
      <label>
        推荐时长(分钟)
        <input type="number" min="1" max="600" v-model.number="form.recommendDurationMin" required />
      </label>
      <label>
        消耗(千卡/小时)
        <input type="number" min="0" max="5000" v-model.number="form.caloriesPerHour" required />
      </label>
      <label>
        推荐频率(次/周)
        <input type="number" min="1" max="14" v-model.number="form.recommendFrequencyPerWeek" required />
      </label>
      <label>
        难度
        <select v-model="form.level">
          <option v-for="item in levelOptions" :value="item.value" :key="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label>
        状态
        <select v-model="form.status">
          <option v-for="item in statusOptions" :value="item.value" :key="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label>
        排序权重
        <input type="number" v-model.number="form.sortWeight" />
      </label>
    </form>

    <div class="multi-box-wrap">
      <div class="multi-box">
        <h4>适宜人群</h4>
        <div class="inline-actions option-create">
          <input
            v-model.trim="optionDraft.audience"
            placeholder="新增适宜人群"
            @keyup.enter="createOption('audience')"
          />
          <button
            type="button"
            @click="createOption('audience')"
            :disabled="creatingOptionType === 'audience'"
          >
            {{ creatingOptionType === "audience" ? "添加中..." : "添加" }}
          </button>
        </div>
        <label v-for="item in options.audiences" :key="`aud-${item.id}`">
          <input
            type="checkbox"
            :checked="isChecked('audienceIds', item.id)"
            @change="toggleMulti('audienceIds', item.id, $event.target.checked)"
          />
          <span>{{ item.name }}</span>
        </label>
      </div>

      <div class="multi-box">
        <h4>器材</h4>
        <div class="inline-actions option-create">
          <input
            v-model.trim="optionDraft.equipment"
            placeholder="新增器材"
            @keyup.enter="createOption('equipment')"
          />
          <button
            type="button"
            @click="createOption('equipment')"
            :disabled="creatingOptionType === 'equipment'"
          >
            {{ creatingOptionType === "equipment" ? "添加中..." : "添加" }}
          </button>
        </div>
        <label v-for="item in options.equipments" :key="`equip-${item.id}`">
          <input
            type="checkbox"
            :checked="isChecked('equipmentIds', item.id)"
            @change="toggleMulti('equipmentIds', item.id, $event.target.checked)"
          />
          <span>{{ item.name }}</span>
        </label>
      </div>

      <div class="multi-box">
        <h4>功效</h4>
        <div class="inline-actions option-create">
          <input
            v-model.trim="optionDraft.benefit"
            placeholder="新增功效"
            @keyup.enter="createOption('benefit')"
          />
          <button
            type="button"
            @click="createOption('benefit')"
            :disabled="creatingOptionType === 'benefit'"
          >
            {{ creatingOptionType === "benefit" ? "添加中..." : "添加" }}
          </button>
        </div>
        <label v-for="item in options.benefits" :key="`benefit-${item.id}`">
          <input
            type="checkbox"
            :checked="isChecked('benefitIds', item.id)"
            @change="toggleMulti('benefitIds', item.id, $event.target.checked)"
          />
          <span>{{ item.name }}</span>
        </label>
      </div>
    </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.course-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(10, 12, 14, 0.48);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px 16px;
  overflow-y: auto;
}

.course-modal-panel {
  width: min(760px, 100%);
  max-height: calc(100vh - 48px);
  overflow: auto;
  margin-bottom: 24px;
  border-radius: 20px;
  border: 1px solid var(--ink-line, #d9dcdf);
  background: var(--paper-strong, #fff);
  padding: 20px 22px;
  box-shadow: 0 24px 50px rgba(10, 12, 14, 0.2);
}

.course-modal-head {
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #eceef1;
}

.course-modal-head h3 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0.06em;
}

.course-modal-msg {
  margin: 0 0 12px;
}

.course-modal-form {
  margin-bottom: 8px;
}

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

.cover-upload {
  display: grid;
  gap: 10px;
}

.cover-upload input[type="file"] {
  padding: 8px;
}

.cover-upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview {
  width: min(100%, 360px);
  max-height: 220px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #dcdcdc;
  background: #fafafa;
}

.cover-path {
  margin: 0;
  word-break: break-all;
}

.multi-box-wrap {
  margin-top: 12px;
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.multi-box {
  border: 1px solid #dcdcdc;
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.multi-box h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.multi-box label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}

.multi-box input[type="checkbox"] {
  width: auto;
}

.option-create {
  margin-bottom: 8px;
}

.option-create input {
  min-width: 0;
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
