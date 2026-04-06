<script setup>
import { computed, ref } from "vue";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import ProfileInfoDialog from "./components/ProfileInfoDialog.vue";
import { authApi } from "./services/authApi";
import { clearSessionUser, sessionState } from "./stores/session";

const route = useRoute();
const router = useRouter();

const pageTitle = computed(() => route.meta.title || "PHMS");
const pageSubtitle = computed(() => route.meta.subtitle || "");
const isGuestPage = computed(() => Boolean(route.meta.guestOnly));
const showPageHead = computed(() => !route.meta.hidePageHead);

const navItems = computed(() => {
  if (!sessionState.user) {
    return [];
  }

  if (sessionState.user.accountLevel === 1) {
    return [
      { path: "/admin/health-goals", label: "健康目标管理" },
      { path: "/meal-recommendations", label: "食谱" },
      { path: "/admin/sport-courses", label: "课程管理" },
      { path: "/admin/users", label: "用户管理" }
    ];
  }

  return [
    { path: "/dashboard", label: "健康总览" },
    { path: "/trends", label: "健康趋势" },
    { path: "/data-sources", label: "数据源管理" },
    { path: "/sports", label: "运动课程" },
    { path: "/exercise-records", label: "运动记录" },
    { path: "/meal-recommendations", label: "食谱" },
    { path: "/meal-records", label: "饮食记录" },
    { path: "/health-goals", label: "健康目标" },
    { path: "/profile", label: "个人档案" }
  ];
});

const userLevelText = computed(() => {
  if (!sessionState.user) {
    return "";
  }
  return sessionState.user.accountLevel === 1 ? "管理员" : "普通用户";
});

const profileDialogOpen = ref(false);

async function logout() {
  try {
    await authApi.logout();
  } finally {
    profileDialogOpen.value = false;
    clearSessionUser();
    await router.push("/login");
  }
}

function openAccountEntry() {
  profileDialogOpen.value = true;
}
</script>

<template>
  <div class="ink-canvas">
    <div class="ink-cloud cloud-top"></div>
    <div class="ink-cloud cloud-bottom"></div>

    <header v-if="!isGuestPage" class="ink-header shell">
      <div class="header-top">
        <div>
          <p class="brand-mark">PHMS</p>
          <h1>个人健康管理系统</h1>
        </div>
        <div v-if="sessionState.user" class="session-actions">
          <button type="button" class="user-pill" @click="openAccountEntry">
            <span>{{ sessionState.user.account }}</span>
            <em>{{ userLevelText }}</em>
          </button>
          <button @click="logout">退出登录</button>
        </div>
      </div>
      <nav v-if="navItems.length" class="ink-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="ink-link"
          active-class="ink-link-active"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </header>

    <main class="shell">
      <section v-if="showPageHead" class="page-head">
        <h2>{{ pageTitle }}</h2>
        <p>{{ pageSubtitle }}</p>
      </section>
      <RouterView />
    </main>

    <ProfileInfoDialog :open="profileDialogOpen" @close="profileDialogOpen = false" />
  </div>
</template>
