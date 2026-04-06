<script setup>
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { dataSourceApi } from "../services/dataSourceApi";

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  source: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(["close", "saved"]);

const saving = ref(false);
const errorMsg = ref("");
const form = ref(createEmptyForm());
const isEditMode = computed(() => Boolean(props.source?.sourceId));
const dialogTitle = computed(() => (isEditMode.value ? "编辑数据源" : "创建数据源"));
const dialogDescription = computed(() =>
  isEditMode.value
    ? "修改名称、类型和说明，保存后会立刻生效。"
    : "填写名称、类型和说明，保存后即可在列表中使用。"
);
const submitLabel = computed(() => {
  if (saving.value) {
    return isEditMode.value ? "保存中..." : "创建中...";
  }
  return isEditMode.value ? "保存" : "创建";
});

function createEmptyForm() {
  return {
    sourceName: "",
    sourceType: "file",
    description: ""
  };
}

function mapSourceToForm(source) {
  return {
    sourceName: source?.sourceName ?? "",
    sourceType: source?.sourceType ?? "file",
    description: source?.description ?? ""
  };
}

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length ? text : null;
}

function resetForm() {
  form.value = mapSourceToForm(props.source);
  errorMsg.value = "";
}

function closeDialog() {
  if (saving.value) {
    return;
  }
  emit("close");
}

async function saveSource() {
  const payload = {
    sourceName: normalizeText(form.value.sourceName),
    sourceType: form.value.sourceType,
    description: normalizeText(form.value.description)
  };
  if (!payload.sourceName) {
    errorMsg.value = "请输入数据源名称。";
    return;
  }

  saving.value = true;
  errorMsg.value = "";
  try {
    const saved = isEditMode.value
      ? await dataSourceApi.updateSource(props.source.sourceId, payload)
      : await dataSourceApi.createSource(payload);
    emit("saved", saved);
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    saving.value = false;
  }
}

watch(
  () => [props.open, props.source?.sourceId],
  ([open]) => {
    if (open) {
      document.body.style.overflow = "hidden";
      resetForm();
      return;
    }
    document.body.style.overflow = "";
    errorMsg.value = "";
    form.value = createEmptyForm();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  document.body.style.overflow = "";
});
</script>

<template>
  <teleport to="body">
    <div v-if="open" class="source-dialog-mask" @click.self="closeDialog">
      <section class="source-dialog" role="dialog" aria-modal="true" aria-labelledby="source-dialog-title">
        <div class="source-dialog-head">
          <div>
            <p class="source-dialog-tag">数据源管理</p>
            <h3 id="source-dialog-title">{{ dialogTitle }}</h3>
            <p>{{ dialogDescription }}</p>
          </div>
          <div class="source-dialog-actions">
            <button type="button" @click="saveSource" :disabled="saving">{{ submitLabel }}</button>
            <button type="button" class="ghost-btn" @click="closeDialog" :disabled="saving">关闭</button>
          </div>
        </div>

        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

        <form class="form-grid source-dialog-form" @submit.prevent="saveSource">
          <label>
            数据源名称
            <input
              v-model.trim="form.sourceName"
              maxlength="64"
              placeholder="例如：小米手环 CSV"
            />
          </label>
          <label>
            数据源类型
            <select v-model="form.sourceType">
              <option value="file">文件导入</option>
              <option value="manual">手动录入</option>
              <option value="device">设备同步</option>
              <option value="platform">第三方平台</option>
            </select>
          </label>
          <label class="full-width">
            数据源说明
            <input
              v-model.trim="form.description"
              maxlength="255"
              placeholder="可选，用于补充说明该数据源"
            />
          </label>
        </form>
      </section>
    </div>
  </teleport>
</template>

<style scoped>
.source-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(10, 12, 14, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.source-dialog {
  width: min(760px, 96vw);
  max-height: calc(100vh - 32px);
  overflow: auto;
  border-radius: 24px;
  border: 1px solid #d9dcdf;
  background: #fff;
  padding: 24px;
  box-shadow: 0 24px 50px rgba(10, 12, 14, 0.18);
}

.source-dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 18px;
  border-bottom: 1px solid #eceef1;
}

.source-dialog-tag {
  margin: 0;
  color: #8a9094;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.source-dialog-head h3 {
  margin: 10px 0 0;
  font-size: 32px;
}

.source-dialog-head p {
  margin: 10px 0 0;
  color: #6d747a;
}

.source-dialog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.source-dialog-form {
  margin-top: 18px;
}

.full-width {
  grid-column: 1 / -1;
}

.ghost-btn {
  background: #fff;
  color: #202224;
  border: 1px solid #c8c8c8;
}

.ghost-btn:hover {
  background: #f3f3f3;
}

@media (max-width: 768px) {
  .source-dialog {
    padding: 18px;
  }

  .source-dialog-head {
    flex-direction: column;
  }

  .source-dialog-actions {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .source-dialog-actions button {
    width: 100%;
  }
}
</style>
