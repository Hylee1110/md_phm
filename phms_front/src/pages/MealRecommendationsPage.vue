<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { healthApi } from "../services/healthApi";
import { buildAssetUrl } from "../services/http";
import { sessionState } from "../stores/session";

const loading = ref(false);
const saving = ref(false);
const uploadingImage = ref(false);
const deletingId = ref(null);
const submittingMealId = ref(null);
const ratingRecipeId = ref(null);
const editingRecipeId = ref(null);
const editorOpen = ref(false);
const errorMsg = ref("");
const successMsg = ref("");
const keyword = ref("");
const mealTypeFilter = ref("");
/** 下拉选择依据；三角形选择 asc/desc。均为空则保持接口顺序 */
const sortField = ref("");
const sortDir = ref(null);
const recipes = ref([]);
const detailRecipe = ref(null);
const imageInputRef = ref(null);

const recipeForm = reactive(defaultRecipeForm());
const recordDraftMap = reactive({});

const mealTypeOptions = [
  { value: "", label: "全部餐次" },
  { value: "0", label: "未指定" },
  { value: "1", label: "早餐" },
  { value: "2", label: "午餐" },
  { value: "3", label: "晚餐" },
  { value: "4", label: "加餐" }
];

const editableMealTypeOptions = [
  { value: 0, label: "未指定" },
  { value: 1, label: "早餐" },
  { value: 2, label: "午餐" },
  { value: 3, label: "晚餐" },
  { value: 4, label: "加餐" }
];

const ratingOptions = [1, 2, 3, 4, 5];

const defaultMealClocks = {
  0: "19:00",
  1: "08:00",
  2: "12:30",
  3: "18:30",
  4: "15:30"
};

const isAdmin = computed(() => sessionState.user?.accountLevel === 1);
const isEditing = computed(() => editingRecipeId.value != null);
const adminRecipeCount = computed(() => recipes.value.filter((item) => item.adminRecommend).length);
const ownRecipeCount = computed(() => recipes.value.filter((item) => item.createdByCurrentUser).length);

const displayRecipes = computed(() => {
  const list = [...recipes.value];
  const key = sortField.value;
  const dir = sortDir.value;
  if (!key || !dir) {
    return list;
  }
  const d = dir === "asc" ? 1 : -1;
  return list.sort((a, b) => {
    let cmp = 0;
    if (key === "rating") {
      const ra = toNumber(a.ratingAvg);
      const rb = toNumber(b.ratingAvg);
      const na = ra == null ? 0 : ra;
      const nb = rb == null ? 0 : rb;
      cmp = na - nb;
    } else if (key === "created") {
      cmp = parseServerTimeMs(a.createdTime) - parseServerTimeMs(b.createdTime);
    } else if (key === "updated") {
      cmp =
        parseServerTimeMs(a.lastChangeTime || a.createdTime) -
        parseServerTimeMs(b.lastChangeTime || b.createdTime);
    }
    if (cmp !== 0) {
      return cmp * d;
    }
    return Number(a.recipeId) - Number(b.recipeId);
  });
});

const currentListTitle = computed(() => {
  if (normalizeText(keyword.value)) {
    return "搜索结果";
  }
  if (mealTypeFilter.value !== "") {
    return `${mealTypeText(mealTypeFilter.value)}食谱`;
  }
  return "所有食物";
});
const formTitle = computed(() => {
  if (isEditing.value) {
    return "修改食谱";
  }
  return isAdmin.value ? "新增管理员推荐食谱" : "新增个人食谱";
});
const formSubtitle = computed(() => {
  if (isEditing.value) {
    return "普通用户只能修改自己创建的食谱，管理员可以修改所有食谱。";
  }
  return isAdmin.value
    ? "当前为管理员模式，新建食谱将作为管理员推荐内容展示给所有用户。"
    : "当前为个人模式，新建食谱仅作为你自己的食谱数据。";
});
const submitButtonText = computed(() => {
  if (saving.value) {
    return isEditing.value ? "保存中..." : "创建中...";
  }
  return isEditing.value ? "保存修改" : "创建食谱";
});

function defaultRecipeForm() {
  return {
    foodName: "",
    mealType: 0,
    portion: "",
    unit: "g",
    calories: "",
    imageUrl: "",
    description: ""
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

function normalizePositiveDecimal(value) {
  const num = toNumber(value);
  if (num == null || num <= 0) {
    return null;
  }
  return num.toFixed(2);
}

function normalizeCalories(value) {
  if (value === "" || value == null) {
    return null;
  }
  const num = Number(value);
  if (!Number.isInteger(num) || num < 0) {
    return null;
  }
  return num;
}

function formatDecimal(value) {
  const num = toNumber(value);
  if (num == null) {
    return "--";
  }
  return Number.isInteger(num) ? String(num) : num.toFixed(2).replace(/\.?0+$/, "");
}

function editableDecimal(value) {
  const text = formatDecimal(value);
  return text === "--" ? "" : text;
}

function formatKcal(value) {
  const num = toNumber(value);
  return num == null ? "--" : `${formatDecimal(num)} kcal`;
}

function formatKcalPer100g(value) {
  const num = toNumber(value);
  return num == null ? "--" : `${formatDecimal(num)} kcal / 100g`;
}

function formatAmount(value, unit) {
  const amountText = formatDecimal(value);
  const normalizedUnit = normalizeText(unit);
  if (amountText === "--") {
    return normalizedUnit ? `-- ${normalizedUnit}` : "--";
  }
  return normalizedUnit ? `${amountText} ${normalizedUnit}` : amountText;
}

function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return String(value).replace("T", " ");
}

function formatRating(value) {
  const num = toNumber(value);
  return num == null ? "0.0" : num.toFixed(1);
}

function mealTypeText(type) {
  const target = editableMealTypeOptions.find((item) => item.value === Number(type));
  return target ? target.label : "未指定";
}

function recipeScopeText(recipe) {
  if (recipe.adminRecommend) {
    return "管理员推荐";
  }
  if (recipe.createdByCurrentUser) {
    return "我的食谱";
  }
  return "个人食谱";
}

function recipeSummary(recipe) {
  if (normalizeText(recipe.description)) {
    return recipe.description;
  }
  const portionText = formatAmount(recipe.portion, recipe.unit);
  if (portionText === "--") {
    return `建议在${mealTypeText(recipe.mealType)}食用，热量按每100g计算。`;
  }
  return `建议在${mealTypeText(recipe.mealType)}食用，参考食用分量 ${portionText}，热量按每100g计算。`;
}

function recipeImageUrl(recipe) {
  return buildAssetUrl(normalizeText(recipe?.imageUrl) || "");
}

function ratingSummary(recipe) {
  return `${formatRating(recipe.ratingAvg)} / 5 (${recipe.ratingCount || 0}人评分)`;
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

function todayDate() {
  const now = new Date();
  const localNow = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return localNow.toISOString().slice(0, 10);
}

function buildDefaultMealTime(mealType) {
  const clock = defaultMealClocks[mealType] || defaultMealClocks[0];
  return `${todayDate()}T${clock}`;
}

function recipeDraftKey(recipe) {
  return String(recipe.recipeId);
}

function isGramUnit(unit) {
  const normalizedUnit = normalizeText(unit);
  if (!normalizedUnit) {
    return true;
  }
  const lowerUnit = normalizedUnit.toLowerCase();
  return lowerUnit === "g" || lowerUnit === "gram" || lowerUnit === "grams" || normalizedUnit === "克";
}

function defaultDraftAmount(recipe) {
  if (!isGramUnit(recipe.unit)) {
    return "";
  }
  return editableDecimal(recipe.portion);
}

function ensureRecordDraft(recipe) {
  const key = recipeDraftKey(recipe);
  if (!recordDraftMap[key]) {
    const mealType = Number.isInteger(Number(recipe.mealType)) ? Number(recipe.mealType) : 0;
    recordDraftMap[key] = {
      mealType,
      mealTime: buildDefaultMealTime(mealType),
      amount: defaultDraftAmount(recipe),
      note: ""
    };
  }
  return recordDraftMap[key];
}

function recipeDraft(recipe) {
  return ensureRecordDraft(recipe);
}

function setDraftMealType(recipe, value) {
  const draft = ensureRecordDraft(recipe);
  draft.mealType = Number(value);
  if (!normalizeText(draft.mealTime)) {
    draft.mealTime = buildDefaultMealTime(draft.mealType);
  }
}

function setDraftMealTime(recipe, value) {
  ensureRecordDraft(recipe).mealTime = value;
}

function setDraftAmount(recipe, value) {
  ensureRecordDraft(recipe).amount = String(value ?? "").trim();
}

function setDraftNote(recipe, value) {
  ensureRecordDraft(recipe).note = String(value ?? "").slice(0, 200);
}

function noteLength(recipe) {
  return ensureRecordDraft(recipe).note.length;
}

function estimateScaledKcal(recipe, amountText) {
  const amount = toNumber(amountText);
  const caloriesPer100g = toNumber(recipe.calories);
  if (amount == null || amount <= 0 || caloriesPer100g == null) {
    return null;
  }
  return ((amount / 100) * caloriesPer100g).toFixed(2);
}

function draftAmountPreview(recipe) {
  return formatAmount(recipeDraft(recipe).amount, "g");
}

function draftKcalPreview(recipe) {
  return formatKcal(estimateScaledKcal(recipe, recipeDraft(recipe).amount));
}

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function resetImageInput() {
  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }
}

function resetRecipeForm() {
  Object.assign(recipeForm, defaultRecipeForm());
  editingRecipeId.value = null;
  resetImageInput();
}

function closeRecipeEditor(options = {}) {
  editorOpen.value = false;
  resetRecipeForm();
  if (options.clearMessages) {
    clearMessages();
  }
}

function openCreateRecipe() {
  resetRecipeForm();
  clearMessages();
  closeDetail();
  editorOpen.value = true;
}

function patchRecipe(recipeId, patch) {
  recipes.value = recipes.value.map((item) => {
    if (item.recipeId !== recipeId) {
      return item;
    }
    return { ...item, ...patch };
  });
  if (detailRecipe.value?.recipeId === recipeId) {
    detailRecipe.value = { ...detailRecipe.value, ...patch };
  }
}

function syncDetailRecipe() {
  if (!detailRecipe.value) {
    return;
  }
  const refreshed = recipes.value.find((item) => item.recipeId === detailRecipe.value.recipeId);
  detailRecipe.value = refreshed || null;
  if (refreshed) {
    ensureRecordDraft(refreshed);
  }
}

async function loadRecipes() {
  loading.value = true;
  clearMessages();
  try {
    recipes.value = await healthApi.listMealRecommendations({
      keyword: normalizeText(keyword.value),
      mealType: mealTypeFilter.value === "" ? null : Number(mealTypeFilter.value),
      limit: 60
    });
    syncDetailRecipe();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

function resetSearch() {
  keyword.value = "";
  mealTypeFilter.value = "";
  sortField.value = "";
  sortDir.value = null;
  loadRecipes();
}

function openDetail(recipe) {
  detailRecipe.value = recipe;
  ensureRecordDraft(recipe);
}

function closeDetail() {
  detailRecipe.value = null;
}

function startEditRecipe(recipe) {
  clearMessages();
  editingRecipeId.value = recipe.recipeId;
  recipeForm.foodName = recipe.foodName ?? "";
  recipeForm.mealType = Number.isInteger(Number(recipe.mealType)) ? Number(recipe.mealType) : 0;
  recipeForm.portion = editableDecimal(recipe.portion);
  recipeForm.unit = normalizeText(recipe.unit) || "g";
  recipeForm.calories = recipe.calories == null ? "" : String(recipe.calories);
  recipeForm.imageUrl = recipe.imageUrl ?? "";
  recipeForm.description = recipe.description ?? "";
  if (detailRecipe.value?.recipeId === recipe.recipeId) {
    closeDetail();
  }
  editorOpen.value = true;
}

function cancelEdit() {
  closeRecipeEditor({ clearMessages: true });
}

async function uploadRecipeImage(file) {
  if (!file) {
    return;
  }
  uploadingImage.value = true;
  clearMessages();
  try {
    const uploaded = await healthApi.uploadMealRecommendationImage(file);
    recipeForm.imageUrl = uploaded.imageUrl ?? "";
    successMsg.value = "食谱图片上传成功";
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    uploadingImage.value = false;
    resetImageInput();
  }
}

function handleImageChange(event) {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }
  uploadRecipeImage(file);
}

function clearRecipeImage() {
  recipeForm.imageUrl = "";
  resetImageInput();
}

function buildRecipePayload() {
  const foodName = normalizeText(recipeForm.foodName);
  const mealType = Number(recipeForm.mealType);
  const portion = recipeForm.portion === "" ? null : normalizePositiveDecimal(recipeForm.portion);
  const unit = normalizeText(recipeForm.unit);
  const calories = normalizeCalories(recipeForm.calories);

  if (!foodName) {
    errorMsg.value = "食物名称不能为空";
    return null;
  }
  if (!Number.isInteger(mealType) || mealType < 0 || mealType > 4) {
    errorMsg.value = "请选择有效的推荐餐次";
    return null;
  }
  if (recipeForm.portion !== "" && portion == null) {
    errorMsg.value = "参考食用分量必须大于 0";
    return null;
  }
  if (recipeForm.calories !== "" && calories == null) {
    errorMsg.value = "每100g热量必须是大于等于 0 的整数";
    return null;
  }

  return {
    foodName,
    mealType,
    portion,
    unit,
    calories,
    imageUrl: normalizeText(recipeForm.imageUrl),
    description: normalizeText(recipeForm.description)
  };
}

async function submitRecipe() {
  const payload = buildRecipePayload();
  if (!payload) {
    return;
  }

  saving.value = true;
  clearMessages();
  try {
    let successText = "";
    if (isEditing.value) {
      await healthApi.updateMealRecommendation(editingRecipeId.value, payload);
      successText = "食谱已更新";
    } else {
      await healthApi.createMealRecommendation(payload);
      successText = isAdmin.value ? "管理员食谱已创建" : "个人食谱已创建";
    }
    editorOpen.value = false;
    resetRecipeForm();
    await loadRecipes();
    successMsg.value = successText;
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    saving.value = false;
  }
}

async function deleteRecipe(recipe) {
  if (!window.confirm(`确认删除食谱 #${recipe.recipeId} 吗？`)) {
    return;
  }
  deletingId.value = recipe.recipeId;
  clearMessages();
  try {
    await healthApi.deleteMealRecommendation(recipe.recipeId);
    successMsg.value = "食谱已删除";
    if (detailRecipe.value?.recipeId === recipe.recipeId) {
      closeDetail();
    }
    if (editingRecipeId.value === recipe.recipeId) {
      closeRecipeEditor();
    }
    await loadRecipes();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    deletingId.value = null;
  }
}

async function rateRecipe(recipe, score) {
  ratingRecipeId.value = recipe.recipeId;
  clearMessages();
  try {
    const rating = await healthApi.rateMealRecommendation(recipe.recipeId, { score });
    patchRecipe(recipe.recipeId, {
      ratingAvg: rating.ratingAvg,
      ratingCount: rating.ratingCount,
      userScore: rating.userScore
    });
    successMsg.value = `已为 ${recipe.foodName} 打 ${score} 分`;
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    ratingRecipeId.value = null;
  }
}

function starClass(recipe, score) {
  return Number(recipe.userScore || 0) >= score ? "active" : "";
}

async function createMealRecordFromRecipe(recipe) {
  const draft = ensureRecordDraft(recipe);
  const diningTime = normalizeText(draft.mealTime);
  const mealType = Number(draft.mealType);
  const amount = normalizePositiveDecimal(draft.amount);

  if (!diningTime) {
    errorMsg.value = "请选择用餐时间";
    return;
  }
  if (!Number.isInteger(mealType) || mealType < 0 || mealType > 4) {
    errorMsg.value = "请选择有效的餐次";
    return;
  }
  if (!amount) {
    errorMsg.value = "摄入份量必须大于 0";
    return;
  }
  if (!Number.isInteger(Number(recipe.recipeId)) || Number(recipe.recipeId) <= 0) {
    errorMsg.value = "食谱ID无效";
    return;
  }
  if (toNumber(recipe.calories) == null) {
    errorMsg.value = "当前食谱未设置每100g热量，无法加入饮食记录";
    return;
  }

  submittingMealId.value = recipe.recipeId;
  clearMessages();
  try {
    await healthApi.createMealRecord({
      recipeId: recipe.recipeId,
      diningTime,
      mealType,
      intakeAmount: amount,
      remark: normalizeText(draft.note)
    });
    successMsg.value = `${recipe.foodName} 已加入饮食记录`;
    draft.note = "";
    draft.mealTime = buildDefaultMealTime(mealType);
    closeDetail();
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    submittingMealId.value = null;
  }
}

onMounted(loadRecipes);
</script>

<template>
  <section class="recipe-hero">
    <span class="hero-pill">健康饮食</span>
    <h2>记录你的每一餐</h2>
    <p>科学饮食，健康生活，让每一餐都更清晰、更好管理。</p>
  </section>

  <section class="catalog-card">
    <div class="catalog-head">
      <div class="catalog-copy">
        <p class="section-tag">饮食列表</p>
        <h3>{{ currentListTitle }}</h3>
      </div>
      <div class="catalog-meta">
        <span><b>{{ recipes.length }}</b> 道食谱</span>
        <span><b>{{ adminRecipeCount }}</b> 管理员推荐</span>
        <span><b>{{ ownRecipeCount }}</b> 我的食谱</span>
      </div>
    </div>

    <div class="catalog-toolbar">
      <div class="search-shell">
        <input
          v-model.trim="keyword"
          placeholder="搜索食物名称"
          @keyup.enter="loadRecipes"
        />
        <button type="button" @click="loadRecipes" :disabled="loading">
          {{ loading ? "搜索中..." : "搜索" }}
        </button>
        <button class="ghost-btn" type="button" @click="resetSearch" :disabled="loading">
          重置
        </button>
      </div>
      <button type="button" class="manage-btn" @click="openCreateRecipe">
        {{ isAdmin ? "新增推荐食谱" : "新增我的食谱" }}
      </button>
    </div>

    <div class="filter-row">
      <button
        v-for="item in mealTypeOptions"
        :key="item.value"
        type="button"
        class="filter-pill"
        :class="{ active: mealTypeFilter === item.value }"
        @click="mealTypeFilter = item.value; loadRecipes()"
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

    <p class="catalog-tip">
      普通用户只能修改或删除自己创建的食谱，管理员可以管理全部食谱。
    </p>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>
    <p v-if="loading" class="muted">食谱加载中...</p>

    <div v-else class="recipe-list">
      <article
        v-for="recipe in displayRecipes"
        :key="recipe.recipeId"
        class="recipe-row"
        :class="{ 'recipe-row-highlight': detailRecipe?.recipeId === recipe.recipeId }"
      >
        <div class="recipe-thumb">
          <img
            v-if="recipe.imageUrl"
            :src="recipeImageUrl(recipe)"
            :alt="recipe.foodName"
          />
          <span v-else>{{ recipe.foodName?.slice(0, 1) || "食" }}</span>
        </div>

        <div class="recipe-content">
          <div class="recipe-labels">
            <span
              class="scope-chip"
              :class="recipe.adminRecommend ? 'scope-global' : 'scope-private'"
            >
              {{ recipeScopeText(recipe) }}
            </span>
            <span class="meal-chip">{{ mealTypeText(recipe.mealType) }}</span>
            <span class="rating-chip">★ {{ ratingSummary(recipe) }}</span>
          </div>

          <h4>{{ recipe.foodName }}</h4>
          <p class="recipe-desc">{{ recipeSummary(recipe) }}</p>

          <div class="recipe-meta">
            <span>创建者 {{ recipe.creatorName || recipe.creatorAccount || "--" }}</span>
            <span>我的评分 {{ recipe.userScore || "--" }}</span>
            <span>更新于 {{ formatDateTime(recipe.lastChangeTime || recipe.createdTime) }}</span>
          </div>
        </div>

        <div class="recipe-side">
          <div class="energy-box">
            <div class="energy-main">
              <strong>{{ formatDecimal(recipe.calories) }}</strong>
              <span>kcal / 100g</span>
            </div>
            <em>
              {{
                formatAmount(recipe.portion, recipe.unit) === "--"
                  ? "按实际食用克数换算"
                  : `参考食用分量 ${formatAmount(recipe.portion, recipe.unit)}`
              }}
            </em>
          </div>

          <div class="row-actions">
            <button class="add-record-btn" type="button" @click="openDetail(recipe)">
              ＋ 添加记录
            </button>
          </div>

          <div v-if="recipe.editable || recipe.deletable" class="manage-links">
            <button
              v-if="recipe.editable"
              type="button"
              class="link-btn"
              @click="startEditRecipe(recipe)"
            >
              编辑
            </button>
            <button
              v-if="recipe.deletable"
              type="button"
              class="link-btn link-danger"
              :disabled="deletingId === recipe.recipeId"
              @click="deleteRecipe(recipe)"
            >
              {{ deletingId === recipe.recipeId ? "删除中..." : "删除" }}
            </button>
          </div>
        </div>
      </article>

      <div v-if="displayRecipes.length === 0" class="empty-state">
        <h4>没有找到匹配的食谱</h4>
        <p>可以调整关键词、切换餐次筛选，或者直接新增一条食谱。</p>
      </div>
    </div>
  </section>

  <div v-if="editorOpen" class="detail-mask" @click.self="cancelEdit">
    <section class="detail-panel editor-panel">
      <div class="detail-top editor-top">
        <div>
          <p class="section-tag">食谱管理</p>
          <h4>{{ formTitle }}</h4>
          <p class="detail-subtitle">{{ formSubtitle }}</p>
        </div>
        <div class="inline-actions detail-actions manage-actions">
          <button type="button" @click="submitRecipe" :disabled="saving">
            {{ submitButtonText }}
          </button>
          <button
            type="button"
            class="ghost-btn"
            :disabled="saving"
            @click="resetRecipeForm"
          >
            清空表单
          </button>
          <button
            v-if="isEditing"
            class="ghost-btn"
            type="button"
            :disabled="saving"
            @click="cancelEdit"
          >
            取消编辑
          </button>
          <button class="ghost-btn close-btn" type="button" :disabled="saving" @click="cancelEdit">
            关闭
          </button>
        </div>
      </div>

      <div class="editor-body">
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success">{{ successMsg }}</p>

        <form class="form-grid recipe-form" @submit.prevent="submitRecipe">
          <label>
            食物名称
            <input v-model.trim="recipeForm.foodName" maxlength="100" required />
          </label>
          <label>
            推荐餐次
            <select v-model.number="recipeForm.mealType">
              <option v-for="item in editableMealTypeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label>
            参考食用分量
            <input type="number" min="0.01" step="0.01" v-model="recipeForm.portion" placeholder="可选" />
          </label>
          <label>
            参考单位
            <input v-model.trim="recipeForm.unit" maxlength="20" placeholder="默认 g" />
          </label>
          <label>
            每100g热量(kcal)
            <input type="number" min="0" step="1" v-model="recipeForm.calories" placeholder="可选" />
          </label>

          <div class="full-width image-upload">
            <div class="image-upload-head">
              <span>食谱图片</span>
              <div class="inline-actions image-upload-actions">
                <input
                  ref="imageInputRef"
                  type="file"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  @change="handleImageChange"
                />
                <button type="button" class="ghost-btn" :disabled="uploadingImage" @click="clearRecipeImage">
                  清空图片
                </button>
              </div>
            </div>
            <p class="muted">
              {{ uploadingImage ? "图片上传中..." : "支持 png/jpg/jpeg/webp/gif，最大 5MB" }}
            </p>
            <div class="image-preview">
              <img v-if="recipeForm.imageUrl" :src="buildAssetUrl(recipeForm.imageUrl)" alt="食谱预览" />
              <span v-else>暂无图片</span>
            </div>
          </div>

          <label class="full-width">
            食谱说明
            <textarea
              v-model.trim="recipeForm.description"
              rows="4"
              placeholder="可选，补充推荐原因、适合场景或食用建议"
            ></textarea>
          </label>
        </form>
      </div>
    </section>
  </div>

  <div v-if="detailRecipe" class="detail-mask" @click.self="closeDetail">
    <section class="detail-panel">
      <div class="detail-top">
        <div>
          <span class="detail-badge">添加记录</span>
          <h4>{{ detailRecipe.foodName }}</h4>
          <p class="detail-subtitle">{{ recipeSummary(detailRecipe) }}</p>
        </div>
        <div class="inline-actions detail-actions">
          <button
            v-if="detailRecipe.editable"
            class="ghost-btn"
            type="button"
            @click="startEditRecipe(detailRecipe)"
          >
            编辑食谱
          </button>
          <button
            v-if="detailRecipe.deletable"
            class="ghost-btn danger-btn"
            type="button"
            :disabled="deletingId === detailRecipe.recipeId"
            @click="deleteRecipe(detailRecipe)"
          >
            {{ deletingId === detailRecipe.recipeId ? "删除中..." : "删除食谱" }}
          </button>
          <button class="ghost-btn close-btn" type="button" @click="closeDetail">关闭</button>
        </div>
      </div>

      <div class="detail-layout">
        <div class="detail-left">
          <div class="detail-image">
            <img
              v-if="detailRecipe.imageUrl"
              :src="recipeImageUrl(detailRecipe)"
              :alt="detailRecipe.foodName"
            />
            <span v-else>暂无食谱图片</span>
          </div>

          <div class="detail-summary-grid">
            <div class="summary-cell">
              <span>可见范围</span>
              <strong>{{ recipeScopeText(detailRecipe) }}</strong>
            </div>
            <div class="summary-cell">
              <span>推荐餐次</span>
              <strong>{{ mealTypeText(detailRecipe.mealType) }}</strong>
            </div>
            <div class="summary-cell">
              <span>每100g热量</span>
              <strong>{{ formatKcalPer100g(detailRecipe.calories) }}</strong>
            </div>
          </div>

          <div class="detail-copy">
            <h5>推荐说明</h5>
            <p>{{ recipeSummary(detailRecipe) }}</p>
          </div>

          <div class="detail-copy">
            <h5>基础信息</h5>
            <div class="detail-tags">
              <p><b>创建者：</b>{{ detailRecipe.creatorName || detailRecipe.creatorAccount || "--" }}</p>
              <p><b>参考食用分量：</b>{{ formatAmount(detailRecipe.portion, detailRecipe.unit) }}</p>
              <p><b>综合评分：</b>{{ ratingSummary(detailRecipe) }}</p>
              <p><b>我的评分：</b>{{ detailRecipe.userScore || "--" }}</p>
              <p><b>创建时间：</b>{{ formatDateTime(detailRecipe.createdTime) }}</p>
              <p><b>更新时间：</b>{{ formatDateTime(detailRecipe.lastChangeTime || detailRecipe.createdTime) }}</p>
            </div>
          </div>
        </div>

        <aside class="detail-right">
          <div class="side-stack">
            <section class="side-card">
              <h5>为食谱评分</h5>
              <p class="muted rating-note">点击星级即可提交评分。</p>
              <div class="rating-stars">
                <button
                  v-for="score in ratingOptions"
                  :key="score"
                  type="button"
                  class="star-btn"
                  :class="starClass(detailRecipe, score)"
                  :disabled="ratingRecipeId === detailRecipe.recipeId"
                  @click="rateRecipe(detailRecipe, score)"
                >
                  ★
                </button>
              </div>
              <p class="rating-meta">当前综合评分 {{ ratingSummary(detailRecipe) }}</p>
            </section>

            <section class="side-card">
              <h5>添加到饮食记录</h5>

              <label class="form-field">
                <span>食谱食物</span>
                <input :value="detailRecipe.foodName" readonly />
              </label>

              <label class="form-field">
                <span><i>*</i> 餐次</span>
                <select
                  :value="recipeDraft(detailRecipe).mealType"
                  @change="setDraftMealType(detailRecipe, $event.target.value)"
                >
                  <option v-for="item in editableMealTypeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>

              <label class="form-field">
                <span><i>*</i> 用餐时间</span>
                <input
                  type="datetime-local"
                  :value="recipeDraft(detailRecipe).mealTime"
                  @input="setDraftMealTime(detailRecipe, $event.target.value)"
                />
              </label>

              <label class="form-field">
                <span><i>*</i> 摄入份量(g)</span>
                <div class="amount-row">
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    :value="recipeDraft(detailRecipe).amount"
                    @input="setDraftAmount(detailRecipe, $event.target.value)"
                  />
                  <em>g</em>
                </div>
              </label>

              <div class="preview-box">
                <p>当前份量：{{ draftAmountPreview(detailRecipe) }}</p>
                <p>预计热量：{{ draftKcalPreview(detailRecipe) }}</p>
              </div>

              <label class="form-field note-field">
                <span>用餐备注</span>
                <textarea
                  :value="recipeDraft(detailRecipe).note"
                  maxlength="200"
                  placeholder="输入本次饮食说明"
                  @input="setDraftNote(detailRecipe, $event.target.value)"
                ></textarea>
                <em class="count">{{ noteLength(detailRecipe) }}</em>
              </label>

              <div class="action-row">
                <button class="ghost-btn" type="button" @click="closeDetail">取消</button>
                <button
                  type="button"
                  @click="createMealRecordFromRecipe(detailRecipe)"
                  :disabled="submittingMealId === detailRecipe.recipeId"
                >
                  {{ submittingMealId === detailRecipe.recipeId ? "提交中..." : "添加记录" }}
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
.recipe-hero {
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

.recipe-hero h2 {
  margin: 26px 0 0;
  font-size: clamp(42px, 8vw, 72px);
  font-weight: 400;
  letter-spacing: 0.06em;
  line-height: 1.08;
}

.recipe-hero p {
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

.search-shell button,
.manage-btn {
  min-height: 46px;
  padding: 0 18px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1;
}

.manage-btn {
  white-space: nowrap;
  flex-shrink: 0;
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
  border-color: #e2b3b3;
  color: #a04747;
}

.danger-btn:hover {
  background: #fff1f1;
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

.recipe-list {
  display: grid;
  gap: 18px;
  margin-top: 22px;
}

.recipe-row {
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

.recipe-row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 20px;
  bottom: 20px;
  width: 4px;
  border-radius: 999px;
  background: transparent;
}

.recipe-row:hover::before,
.recipe-row-highlight::before {
  background: #15181b;
}

.recipe-thumb {
  width: 110px;
  height: 110px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1f24, #38434b);
  display: flex;
  align-items: center;
  justify-content: center;
}

.recipe-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recipe-thumb span {
  color: #fff;
  font-size: 44px;
}

.recipe-content {
  min-width: 0;
}

.recipe-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.scope-chip,
.meal-chip,
.rating-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.scope-global {
  background: #e8efe9;
  color: #366545;
}

.scope-private {
  background: #f5ebe0;
  color: #8a5d24;
}

.meal-chip {
  background: #f0f3f5;
  color: #586068;
}

.rating-chip {
  background: #f7f2eb;
  color: #8d5d21;
}

.recipe-content h4 {
  margin: 0;
  font-size: 34px;
  font-weight: 600;
}

.recipe-desc {
  margin: 14px 0 0;
  color: #5d656d;
  line-height: 1.8;
  font-size: 15px;
}

.recipe-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 14px;
  color: #8a9095;
  font-size: 13px;
}

.recipe-side {
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

.manage-links {
  display: flex;
  gap: 10px;
  align-items: center;
}

.link-btn {
  padding: 0;
  background: transparent;
  border: none;
  color: #646b70;
  font-size: 14px;
}

.link-btn:hover {
  background: transparent;
  color: #171a1d;
}

.link-danger {
  color: #a04747;
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

.manage-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.recipe-form {
  margin-top: 10px;
}

.full-width {
  grid-column: 1 / -1;
}

textarea {
  width: 100%;
  border: 1px solid #c8c8c8;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
  font-family: inherit;
  resize: vertical;
}

.image-upload {
  display: grid;
  gap: 8px;
}

.image-upload-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.image-upload-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.image-upload-head input[type="file"] {
  width: auto;
  max-width: 280px;
}

.image-preview {
  min-height: 220px;
  border: 1px dashed #d5cfc5;
  border-radius: 10px;
  background: #fbfaf7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-preview img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.image-preview span {
  color: #8a8f95;
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

.editor-panel {
  width: min(1080px, 96vw);
}

.editor-top h4 {
  margin: 14px 0 0;
  font-size: clamp(34px, 5vw, 56px);
  font-weight: 400;
}

.editor-body {
  padding: 22px 24px 28px;
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

.amount-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.amount-row em {
  color: #65707a;
  font-style: normal;
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

.preview-box p + p {
  margin-top: 6px;
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
  .recipe-row {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .recipe-side {
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

  .manage-btn {
    width: 100%;
  }

  .recipe-content h4 {
    font-size: 28px;
  }

  .detail-top h4 {
    font-size: 30px;
  }

  .detail-summary-grid {
    grid-template-columns: 1fr;
  }

  .image-upload-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .recipe-hero {
    padding-bottom: 24px;
  }

  .recipe-hero p {
    font-size: 16px;
  }

  .catalog-card {
    padding-inline: 16px;
  }

  .recipe-row {
    grid-template-columns: 1fr;
  }

  .recipe-thumb {
    width: 100%;
    height: 180px;
  }

  .recipe-side {
    grid-template-columns: 1fr;
  }

  .row-actions {
    grid-auto-flow: row;
  }

  .manage-links,
  .action-row {
    flex-direction: column;
    width: 100%;
  }

  .row-actions button,
  .action-row button {
    width: 100%;
  }

  .amount-row {
    grid-template-columns: 1fr;
  }
}
</style>
