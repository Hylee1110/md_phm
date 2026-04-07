<!--
  应用根布局：水墨风顶栏、按角色生成的导航、页面标题区、子路由出口，以及账号信息弹层入口。
-->
<script setup>
import { computed, ref } from "vue";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import ProfileInfoDialog from "./components/ProfileInfoDialog.vue";
import { authApi } from "./services/authApi";
import { clearSessionUser, sessionState } from "./stores/session";

const route = useRoute();
const router = useRouter();

// 各路由在 meta 中声明 title/subtitle，供主内容区页头展示（部分页用 hidePageHead 自行排版）
const pageTitle = computed(() => route.meta.title || "PHMS");
const pageSubtitle = computed(() => route.meta.subtitle || "");
const isGuestPage = computed(() => Boolean(route.meta.guestOnly));
const showPageHead = computed(() => !route.meta.hidePageHead);

// accountLevel：1=管理员（管理端导航），0=普通用户（业务功能导航）；未登录不渲染顶栏导航
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
    // 通知后端销毁 session；失败也继续清前端状态，避免卡在半登出
    await authApi.logout();
  } finally {
    profileDialogOpen.value = false;
    clearSessionUser();
    await router.push("/login");
  }
}

function openAccountEntry() {
  // 打开 ProfileInfoDialog：在弹层内可查看/编辑档案，不等同于独立「个人档案」整页
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
