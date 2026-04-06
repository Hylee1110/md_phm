import { requestJson } from "./http";

export const authApi = {
  register(formData) {
    return requestJson("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  login(formData) {
    return requestJson("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  logout() {
    return requestJson("/api/auth/logout", {
      method: "POST"
    });
  },
  me() {
    return requestJson("/api/auth/me");
  }
};
