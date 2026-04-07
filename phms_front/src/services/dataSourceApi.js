/**
 * 数据源与导入任务 API（封装在独立模块便于 DataSourcesPage 引用）。
 */
import { requestJson } from "./http";

export const dataSourceApi = {
  listSources() {
    return requestJson("/api/health/data-sources");
  },
  createSource(formData) {
    return requestJson("/api/health/data-sources", {
      method: "POST",
      body: JSON.stringify(formData)
    });
  },
  updateSource(sourceId, formData) {
    return requestJson(`/api/health/data-sources/${sourceId}`, {
      method: "PUT",
      body: JSON.stringify(formData)
    });
  },
  deleteSource(sourceId) {
    return requestJson(`/api/health/data-sources/${sourceId}`, {
      method: "DELETE"
    });
  },
  updateSourceStatus(sourceId, sourceStatus) {
    return requestJson(`/api/health/data-sources/${sourceId}/status`, {
      method: "PUT",
      body: JSON.stringify({ sourceStatus })
    });
  },
  importHealthMetrics(sourceId, file) {
    const formData = new FormData();
    formData.append("file", file);
    return requestJson(`/api/health/data-sources/${sourceId}/imports/health-metrics`, {
      method: "POST",
      body: formData
    });
  },
  importExerciseRecords(sourceId, file) {
    const formData = new FormData();
    formData.append("file", file);
    return requestJson(`/api/health/data-sources/${sourceId}/imports/exercise-records`, {
      method: "POST",
      body: formData
    });
  },
  listSyncTasks({ sourceId, limit = 10 } = {}) {
    const params = new URLSearchParams();
    if (sourceId != null && sourceId !== "") {
      params.set("sourceId", String(sourceId));
    }
    params.set("limit", String(limit));
    const query = params.toString();
    return requestJson(`/api/health/data-sources/sync-tasks${query ? `?${query}` : ""}`);
  },
  getOverview() {
    return requestJson("/api/health/data-sources/overview");
  }
};
