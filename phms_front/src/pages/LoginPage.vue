<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "../services/authApi";
import { setSessionUser, userHomePath } from "../stores/session";

const route = useRoute();
const router = useRouter();

const form = ref({
  account: route.query.account || "",
  password: ""
});
const submitting = ref(false);
const errorMsg = ref("");

async function submitLogin() {
  submitting.value = true;
  errorMsg.value = "";
  try {
    const user = await authApi.login(form.value);
    setSessionUser(user);
    await router.push(userHomePath());
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="ink-card auth-card">
    <h3>登录账号</h3>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <form class="form-grid" @submit.prevent="submitLogin">
      <label>
        账号
        <input v-model.trim="form.account" placeholder="请输入账号" />
      </label>
      <label>
        密码
        <input type="password" v-model="form.password" placeholder="请输入密码" />
      </label>
    </form>
    <div class="auth-actions">
      <button @click="submitLogin" :disabled="submitting">
        {{ submitting ? "登录中..." : "登录" }}
      </button>
      <RouterLink to="/register" class="inline-link">没有账号？去注册</RouterLink>
    </div>
  </section>
</template>
