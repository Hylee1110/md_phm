/**
 * 全局登录态（响应式）：当前用户、是否已初始化、以及拉取 /api/auth/me 的并发去重。
 *
 * 路由守卫与页面依赖 ensureSessionLoaded()，保证先拿到会话再渲染受保护页面。
 */
import { reactive } from "vue";
import { authApi } from "../services/authApi";

export const sessionState = reactive({
  initialized: false,
  user: null
});

let loadingPromise = null;

export async function ensureSessionLoaded(force = false) {
  if (sessionState.initialized && !force) {
    return sessionState.user;
  }
  if (loadingPromise && !force) {
    // 并发去重：多个路由/页面同时触发加载时复用同一个 Promise，避免重复请求 /me
    return loadingPromise;
  }

  loadingPromise = (async () => {
    try {
      // /api/auth/me：后端根据 session 返回当前用户信息；未登录会返回错误码并被捕获
      const me = await authApi.me();
      sessionState.user = me;
    } catch (error) {
      sessionState.user = null;
    } finally {
      sessionState.initialized = true;
      loadingPromise = null;
    }
    return sessionState.user;
  })();

  return loadingPromise;
}

export function setSessionUser(user) {
  sessionState.user = user;
  sessionState.initialized = true;
}

export function clearSessionUser() {
  sessionState.user = null;
  sessionState.initialized = true;
}

export function userHomePath() {
  if (!sessionState.user) {
    return "/login";
  }
  return sessionState.user.accountLevel === 1 ? "/admin/sport-courses" : "/dashboard";
}
