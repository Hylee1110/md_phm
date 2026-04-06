import { createRouter, createWebHistory } from "vue-router";
import AdminHealthGoalsPage from "../pages/AdminHealthGoalsPage.vue";
import AdminSportCoursesPage from "../pages/AdminSportCoursesPage.vue";
import AdminUsersPage from "../pages/AdminUsersPage.vue";
import DashboardPage from "../pages/DashboardPage.vue";
import DataSourcesPage from "../pages/DataSourcesPage.vue";
import ExerciseRecordsPage from "../pages/ExerciseRecordsPage.vue";
import HealthGoalsPage from "../pages/HealthGoalsPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import MealRecommendationsPage from "../pages/MealRecommendationsPage.vue";
import MealRecordsPage from "../pages/MealRecordsPage.vue";
import ProfilePage from "../pages/ProfilePage.vue";
import RegisterPage from "../pages/RegisterPage.vue";
import SportCoursesPage from "../pages/SportCoursesPage.vue";
import TrendsPage from "../pages/TrendsPage.vue";
import { ensureSessionLoaded, sessionState, userHomePath } from "../stores/session";

const routes = [
  {
    path: "/",
    redirect: "/dashboard"
  },
  {
    path: "/login",
    name: "login",
    component: LoginPage,
    meta: {
      guestOnly: true,
      title: "账号登录",
      subtitle: "普通用户和管理员统一登录入口"
    }
  },
  {
    path: "/register",
    name: "register",
    component: RegisterPage,
    meta: {
      guestOnly: true,
      title: "注册账号",
      subtitle: "创建普通用户账号"
    }
  },
  {
    path: "/dashboard",
    name: "dashboard",
    component: DashboardPage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "健康总览",
      subtitle: "今日指标与系统建议"
    }
  },
  {
    path: "/health-goals",
    name: "health-goals",
    component: HealthGoalsPage,
    meta: {
      requiresAuth: true,
      role: "user",
      hidePageHead: true,
      title: "健康目标",
      subtitle: "选择适合你的健康目标"
    }
  },
  {
    path: "/sports",
    name: "sports",
    component: SportCoursesPage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "运动课程",
      subtitle: "搜索课程并进行评分"
    }
  },
  {
    path: "/profile",
    name: "profile",
    component: ProfilePage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "个人档案",
      subtitle: "基础信息与健康标签"
    }
  },
  {
    path: "/trends",
    name: "trends",
    component: TrendsPage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "健康趋势",
      subtitle: "步数、心率、睡眠和血压变化"
    }
  },
  {
    path: "/data-sources",
    name: "data-sources",
    component: DataSourcesPage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "数据源管理",
      subtitle: "管理数据源状态、CSV 导入与同步日志"
    }
  },
  {
    path: "/exercise-records",
    name: "exercise-records",
    component: ExerciseRecordsPage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "运动记录",
      subtitle: "查看与删除历史运动记录"
    }
  },
  {
    path: "/meal-recommendations",
    name: "meal-recommendations",
    component: MealRecommendationsPage,
    meta: {
      requiresAuth: true,
      hidePageHead: true,
      title: "食谱",
      subtitle: "浏览、评分、上传图片并维护个人或管理员食谱"
    }
  },
  {
    path: "/meal-records",
    name: "meal-records",
    component: MealRecordsPage,
    meta: {
      requiresAuth: true,
      role: "user",
      title: "饮食记录",
      subtitle: "按食谱记录每次饮食摄入"
    }
  },
  {
    path: "/admin/users",
    name: "admin-users",
    component: AdminUsersPage,
    meta: {
      requiresAuth: true,
      role: "admin",
      title: "用户管理",
      subtitle: "查询并维护普通用户状态"
    }
  },
  {
    path: "/admin/health-goals",
    name: "admin-health-goals",
    component: AdminHealthGoalsPage,
    meta: {
      requiresAuth: true,
      role: "admin",
      title: "健康目标管理",
      subtitle: "维护系统内可选的健康目标"
    }
  },
  {
    path: "/admin/sport-courses",
    name: "admin-sport-courses",
    component: AdminSportCoursesPage,
    meta: {
      requiresAuth: true,
      role: "admin",
      title: "课程管理",
      subtitle: "维护运动课程与标签"
    }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  await ensureSessionLoaded();
  const currentUser = sessionState.user;

  if (to.meta.guestOnly && currentUser) {
    return userHomePath();
  }

  if (to.meta.requiresAuth && !currentUser) {
    return "/login";
  }

  if (to.meta.role === "admin" && currentUser?.accountLevel !== 1) {
    return userHomePath();
  }

  if (to.meta.role === "user" && currentUser?.accountLevel !== 0) {
    return userHomePath();
  }

  return true;
});

export default router;
