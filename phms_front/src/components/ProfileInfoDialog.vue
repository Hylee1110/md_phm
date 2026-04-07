<!--
  账号信息弹层：展示当前登录用户摘要（由 App 顶栏触发）。
-->
<script setup>
import { onBeforeUnmount, ref, watch } from "vue";
import { healthApi } from "../services/healthApi";

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["close"]);

const loading = ref(false);
const saving = ref(false);
const recognizing = ref(false);
const errorMsg = ref("");
const successMsg = ref("");
const profile = ref(null);
const lastRecognizedIdCard = ref("");

const form = ref({
  nickname: "",
  realname: "",
  idcard: "",
  gender: 0,
  age: ""
});

let recognizeTimer = null;

function mapProfileToForm(data) {
  return {
    nickname: data?.nickname ?? "",
    realname: data?.realname ?? "",
    idcard: data?.idcard ?? "",
    gender: data?.gender ?? 0,
    age: data?.age ?? ""
  };
}

function normalizeText(value) {
  const text = String(value ?? "").trim();
  return text.length ? text : null;
}

function normalizedIdCard() {
  const value = normalizeText(form.value.idcard);
  return value ? value.toUpperCase() : null;
}

function isCompleteIdCard(value) {
  return /^(\d{15}|\d{17}[\dXx])$/.test(value);
}

function buildPayload() {
  const ageValue = form.value.age === "" || form.value.age === null ? null : Number(form.value.age);
  return {
    nickname: normalizeText(form.value.nickname),
    realname: normalizeText(form.value.realname),
    idcard: normalizedIdCard(),
    gender: Number(form.value.gender ?? 0),
    age: Number.isNaN(ageValue) ? null : ageValue
  };
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

function clearMessages() {
  errorMsg.value = "";
  successMsg.value = "";
}

function clearRecognizeTimer() {
  if (recognizeTimer) {
    clearTimeout(recognizeTimer);
    recognizeTimer = null;
  }
}

async function loadProfile({ keepMessages = false } = {}) {
  loading.value = true;
  if (!keepMessages) {
    clearMessages();
  }
  try {
    // 弹层每次打开都会拉最新档案；lastRecognizedIdCard 用于防抖重复识别
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
  // 与 ProfilePage 逻辑一致：完整身份证号才请求后端解析
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
  clearRecognizeTimer();
  recognizeTimer = setTimeout(() => {
    recognizeIdCard(true);
  }, 350);
}

async function saveProfile() {
  saving.value = true;
  errorMsg.value = "";
  successMsg.value = "";
  try {
    // 保存前静默识别，尽量补全性别年龄再提交 updateProfile
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

function closeDialog() {
  emit("close");
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      document.body.style.overflow = "hidden";
      await loadProfile();
      return;
    }
    document.body.style.overflow = "";
    clearRecognizeTimer();
    clearMessages();
    // 关闭时不强制清空 profile，下次打开会重新 loadProfile 覆盖
  }
);

onBeforeUnmount(() => {
  document.body.style.overflow = "";
  clearRecognizeTimer();
});
</script>

<template>
  <teleport to="body">
    <div v-if="open" class="profile-dialog-mask" @click.self="closeDialog">
      <section class="profile-dialog" role="dialog" aria-modal="true" aria-labelledby="profile-dialog-title">
        <div class="profile-dialog-head">
          <div>
            <p class="profile-dialog-tag">账号信息</p>
            <h3 id="profile-dialog-title">个人信息</h3>
            <p>点击右上角账号即可快速查看和维护个人资料。</p>
          </div>
          <div class="profile-dialog-actions">
            <button type="button" class="ghost-btn" @click="loadProfile" :disabled="loading || saving">
              {{ loading ? "刷新中..." : "刷新" }}
            </button>
            <button type="button" @click="saveProfile" :disabled="saving || loading">
              {{ saving ? "保存中..." : "保存" }}
            </button>
            <button type="button" class="ghost-btn" @click="closeDialog">关闭</button>
          </div>
        </div>

        <p v-if="loading" class="muted">档案加载中...</p>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success">{{ successMsg }}</p>

        <div v-if="profile" class="form-grid profile-form">
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
    </div>
  </teleport>
</template>

<style scoped>
.profile-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(10, 12, 14, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.profile-dialog {
  width: min(960px, 96vw);
  max-height: calc(100vh - 32px);
  overflow: auto;
  border-radius: 24px;
  border: 1px solid #d9dcdf;
  background: #fff;
  padding: 24px;
  box-shadow: 0 24px 50px rgba(10, 12, 14, 0.18);
}

.profile-dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 18px;
  border-bottom: 1px solid #eceef1;
}

.profile-dialog-tag {
  margin: 0;
  color: #8a9094;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.profile-dialog-head h3 {
  margin: 10px 0 0;
  font-size: 34px;
}

.profile-dialog-head p {
  margin: 10px 0 0;
  color: #6d747a;
}

.profile-dialog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.profile-form {
  margin-top: 18px;
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
  .profile-dialog {
    padding: 18px;
  }

  .profile-dialog-head {
    flex-direction: column;
  }

  .profile-dialog-actions {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .profile-dialog-actions button {
    width: 100%;
  }
}
</style>
