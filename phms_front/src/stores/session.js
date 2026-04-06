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
    return loadingPromise;
  }

  loadingPromise = (async () => {
    try {
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
