import { requestJson } from "./http";

export const healthApi = {
  getDashboard() {
    return requestJson("/api/health/dashboard");
  },
  getProfile() {
    return requestJson("/api/health/profile");
  },
  updateProfile(formData) {
    return requestJson("/api/health/profile", {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  recognizeIdCard(idcard) {
    const value = String(idcard ?? "").trim();
    return requestJson(`/api/health/profile/idcard-recognition?idcard=${encodeURIComponent(value)}`);
  },
  getMetrics(days = 7) {
    return requestJson(`/api/health/metrics?days=${days}`);
  },
  listHealthGoals({ keyword } = {}) {
    const params = new URLSearchParams();
    if (keyword) {
      params.set("keyword", String(keyword).trim());
    }
    const query = params.toString();
    return requestJson(`/api/health/goals${query ? `?${query}` : ""}`);
  },
  listUserGoals({ status } = {}) {
    const params = new URLSearchParams();
    if (status !== "" && status != null) {
      params.set("status", String(status));
    }
    const query = params.toString();
    return requestJson(`/api/health/user-goals${query ? `?${query}` : ""}`);
  },
  selectHealthGoal(goalId, formData = {}) {
    return requestJson(`/api/health/goals/${goalId}/select`, {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  cancelHealthGoal(userGoalId) {
    return requestJson(`/api/health/user-goals/${userGoalId}`, {
      method: "DELETE"
    });
  },
  listHealthRecords(userGoalId, { rangeDays, limit = 400 } = {}) {
    const params = new URLSearchParams();
    if (rangeDays != null && rangeDays !== "") {
      params.set("rangeDays", String(rangeDays));
    }
    if (limit != null) {
      params.set("limit", String(limit));
    }
    const query = params.toString();
    return requestJson(`/api/health/user-goals/${userGoalId}/records${query ? `?${query}` : ""}`);
  },
  createHealthRecord(userGoalId, formData) {
    return requestJson(`/api/health/user-goals/${userGoalId}/records`, {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  listExerciseRecords({ startTime, endTime, limit = 50 } = {}) {
    const params = new URLSearchParams();
    if (startTime) {
      params.set("startTime", startTime);
    }
    if (endTime) {
      params.set("endTime", endTime);
    }
    if (limit != null) {
      params.set("limit", String(limit));
    }
    const query = params.toString();
    return requestJson(`/api/health/exercise-records${query ? `?${query}` : ""}`);
  },
  listSportCourses({ keyword, limit = 30 } = {}) {
    const params = new URLSearchParams();
    if (keyword) {
      params.set("keyword", String(keyword).trim());
    }
    if (limit != null) {
      params.set("limit", String(limit));
    }
    const query = params.toString();
    return requestJson(`/api/health/sport-courses${query ? `?${query}` : ""}`);
  },
  rateSportCourse(courseId, formData) {
    return requestJson(`/api/health/sport-courses/${courseId}/ratings`, {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  getExerciseRecord(recordId) {
    return requestJson(`/api/health/exercise-records/${recordId}`);
  },
  createExerciseRecord(formData) {
    return requestJson("/api/health/exercise-records", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  updateExerciseRecord(recordId, formData) {
    return requestJson(`/api/health/exercise-records/${recordId}`, {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  deleteExerciseRecord(recordId) {
    return requestJson(`/api/health/exercise-records/${recordId}`, {
      method: "DELETE"
    });
  },
  listMealRecords({ startTime, endTime, limit = 50 } = {}) {
    const params = new URLSearchParams();
    if (startTime) {
      params.set("startTime", startTime);
    }
    if (endTime) {
      params.set("endTime", endTime);
    }
    if (limit != null) {
      params.set("limit", String(limit));
    }
    const query = params.toString();
    return requestJson(`/api/health/meal-records${query ? `?${query}` : ""}`);
  },
  getMealRecord(recordId) {
    return requestJson(`/api/health/meal-records/${recordId}`);
  },
  listMealRecommendations({ keyword, mealType, limit = 60 } = {}) {
    const params = new URLSearchParams();
    if (keyword) {
      params.set("keyword", String(keyword).trim());
    }
    if (mealType != null && mealType !== "") {
      params.set("mealType", String(mealType));
    }
    if (limit != null) {
      params.set("limit", String(limit));
    }
    const query = params.toString();
    return requestJson(`/api/health/meal-recommendations${query ? `?${query}` : ""}`);
  },
  createMealRecommendation(formData) {
    return requestJson("/api/health/meal-recommendations", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  uploadMealRecommendationImage(file) {
    const formData = new FormData();
    formData.append("file", file);
    return requestJson("/api/health/meal-recommendations/image", {
      method: "POST",
      body: formData
    });
  },
  updateMealRecommendation(recipeId, formData) {
    return requestJson(`/api/health/meal-recommendations/${recipeId}`, {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  rateMealRecommendation(recipeId, formData) {
    return requestJson(`/api/health/meal-recommendations/${recipeId}/ratings`, {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  deleteMealRecommendation(recipeId) {
    return requestJson(`/api/health/meal-recommendations/${recipeId}`, {
      method: "DELETE"
    });
  },
  createMealRecord(formData) {
    return requestJson("/api/health/meal-records", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  updateMealRecord(recordId, formData) {
    return requestJson(`/api/health/meal-records/${recordId}`, {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  deleteMealRecord(recordId) {
    return requestJson(`/api/health/meal-records/${recordId}`, {
      method: "DELETE"
    });
  }
};
