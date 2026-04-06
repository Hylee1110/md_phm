<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { authApi } from "../services/authApi";

const router = useRouter();

const form = ref({
  account: "",
  password: "",
  nickname: "",
  realname: "",
  gender: 0,
  age: null
});
const submitting = ref(false);
const errorMsg = ref("");

async function submitRegister() {
  submitting.value = true;
  errorMsg.value = "";
  try {
    await authApi.register(form.value);
    window.alert("注册成功，请登录");
    await router.push({ path: "/login", query: { account: form.value.account } });
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="ink-card auth-card">
    <h3>注册普通用户</h3>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <form class="form-grid" @submit.prevent="submitRegister">
      <label>
        账号
        <input v-model.trim="form.account" placeholder="4-32位字母/数字/下划线" />
      </label>
      <label>
        密码
        <input type="password" v-model="form.password" placeholder="6-32位" />
      </label>
      <label>
        昵称
        <input v-model.trim="form.nickname" placeholder="可选" />
      </label>
      <label>
        真实姓名
        <input v-model.trim="form.realname" placeholder="可选" />
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
        <input type="number" min="0" max="130" v-model.number="form.age" placeholder="可选" />
      </label>
    </form>
    <div class="auth-actions">
      <button @click="submitRegister" :disabled="submitting">
        {{ submitting ? "提交中..." : "注册" }}
      </button>
      <RouterLink to="/login" class="inline-link">已有账号？去登录</RouterLink>
    </div>
  </section>
</template>
