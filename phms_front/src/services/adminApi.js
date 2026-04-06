import { requestJson } from "./http";

export const adminApi = {
  listUsers(keyword = "") {
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
    return requestJson(`/api/admin/users${query}`);
  },
  updateUserStatus(userId, accountStatus) {
    return requestJson(`/api/admin/users/${userId}/status`, {
      method: "PUT",
      body: JSON.stringify({ accountStatus })
    });
  },
  listSportCourses({ keyword = "", status = "" } = {}) {
    const params = new URLSearchParams();
    if (keyword) {
      params.set("keyword", String(keyword).trim());
    }
    if (status) {
      params.set("status", String(status).trim());
    }
    const query = params.toString();
    return requestJson(`/api/admin/sport-courses${query ? `?${query}` : ""}`);
  },
  listHealthGoals({ keyword = "", status = "" } = {}) {
    const params = new URLSearchParams();
    if (keyword) {
      params.set("keyword", String(keyword).trim());
    }
    if (status !== "" && status != null) {
      params.set("status", String(status));
    }
    const query = params.toString();
    return requestJson(`/api/admin/health-goals${query ? `?${query}` : ""}`);
  },
  createHealthGoal(formData) {
    return requestJson("/api/admin/health-goals", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  updateHealthGoal(goalId, formData) {
    return requestJson(`/api/admin/health-goals/${goalId}`, {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  getSportCourse(courseId) {
    return requestJson(`/api/admin/sport-courses/${courseId}`);
  },
  getSportCourseOptions() {
    return requestJson("/api/admin/sport-courses/options");
  },
  createAudienceOption(name) {
    return requestJson("/api/admin/sport-courses/options/audiences", {
      method: "POST",
      body: JSON.stringify({ name })
    });
  },
  createEquipmentOption(name) {
    return requestJson("/api/admin/sport-courses/options/equipments", {
      method: "POST",
      body: JSON.stringify({ name })
    });
  },
  createBenefitOption(name) {
    return requestJson("/api/admin/sport-courses/options/benefits", {
      method: "POST",
      body: JSON.stringify({ name })
    });
  },
  uploadSportCourseCover(file) {
    const formData = new FormData();
    formData.append("file", file);
    return requestJson("/api/admin/sport-courses/cover", {
      method: "POST",
      body: formData
    });
  },
  createSportCourse(formData) {
    return requestJson("/api/admin/sport-courses", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  updateSportCourse(courseId, formData) {
    return requestJson(`/api/admin/sport-courses/${courseId}`, {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  deleteSportCourse(courseId) {
    return requestJson(`/api/admin/sport-courses/${courseId}`, {
      method: "DELETE"
    });
  }
};
