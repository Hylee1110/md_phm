<script setup>
import { onMounted, ref } from "vue";
import { adminApi } from "../services/adminApi";

const loading = ref(false);
const savingUserId = ref(null);
const errorMsg = ref("");
const keyword = ref("");
const users = ref([]);

const statusOptions = [
  { value: 0, label: "正常" },
  { value: 1, label: "异常(只读)" },
  { value: 2, label: "禁用(无法登录)" }
];

function statusLabel(status) {
  const target = statusOptions.find((item) => item.value === status);
  return target ? target.label : "未知";
}

async function loadUsers() {
  loading.value = true;
  errorMsg.value = "";
  try {
    users.value = await adminApi.listUsers(keyword.value.trim());
  } catch (error) {
    errorMsg.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function updateStatus(user) {
  savingUserId.value = user.userId;
  errorMsg.value = "";
  try {
    await adminApi.updateUserStatus(user.userId, user.accountStatus);
  } catch (error) {
    errorMsg.value = error.message;
    await loadUsers();
  } finally {
    savingUserId.value = null;
  }
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

onMounted(loadUsers);
</script>

<template>
  <section class="ink-card">
    <div class="row-head">
      <h3>普通用户管理</h3>
      <div class="inline-actions">
        <input v-model.trim="keyword" placeholder="按账号/昵称/姓名模糊检索" />
        <button @click="loadUsers">检索</button>
      </div>
    </div>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="loading" class="muted">加载用户列表中...</p>

    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>用户ID</th>
            <th>账号</th>
            <th>昵称</th>
            <th>姓名</th>
            <th>性别</th>
            <th>年龄</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>最近更新</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.userId">
            <td>{{ user.userId }}</td>
            <td>{{ user.account }}</td>
            <td>{{ user.nickname || "--" }}</td>
            <td>{{ user.realname || "--" }}</td>
            <td>{{ genderText(user.gender) }}</td>
            <td>{{ user.age ?? "--" }}</td>
            <td>{{ statusLabel(user.accountStatus) }}</td>
            <td>{{ user.createdTime?.replace("T", " ") }}</td>
            <td>{{ user.lastChangeTime?.replace("T", " ") }}</td>
            <td>
              <div class="inline-actions">
                <select v-model.number="user.accountStatus">
                  <option v-for="item in statusOptions" :value="item.value" :key="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <button @click="updateStatus(user)" :disabled="savingUserId === user.userId">
                  {{ savingUserId === user.userId ? "保存中..." : "保存" }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="10" class="muted">暂无普通用户</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
