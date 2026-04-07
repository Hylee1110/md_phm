/**
 * 与后端通信的底层封装：JSON 请求、FormData、携带 Cookie（session）、统一解析 ApiResponse。
 *
 * 可通过环境变量 VITE_API_BASE_URL 指定 API 根地址；开发环境通常留空走 Vite 代理。
 */
export class ApiError extends Error {
  constructor(message, code, status) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

const API_BASE_URL = String(import.meta.env.VITE_API_BASE_URL ?? "").trim();

function buildApiUrl(path) {
  // 允许传入完整 URL（例如第三方资源），此时不做 base 拼接
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  // 开发环境通常不配置 base，通过 Vite 代理转发；生产环境可通过 VITE_API_BASE_URL 指定后端地址
  if (!API_BASE_URL) {
    return path;
  }
  const normalizedBase = API_BASE_URL.endsWith("/") ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
  return `${normalizedBase}${path}`;
}

function buildHeaders(options = {}) {
  // 自动推断是否需要 JSON Content-Type，避免 FormData 被错误设置导致后端无法解析 boundary
  const headers = new Headers(options.headers ?? {});
  const method = String(options.method ?? "GET").toUpperCase();
  const hasBody = options.body != null && options.body !== "";
  const isFormData = options.body instanceof FormData;
  const shouldSetJson =
    !isFormData && !headers.has("Content-Type") && (hasBody || !["GET", "HEAD"].includes(method));
  if (shouldSetJson) {
    headers.set("Content-Type", "application/json");
  }
  return headers;
}

export function buildAssetUrl(path) {
  const value = String(path ?? "").trim();
  if (!value) {
    return "";
  }
  // 已经是可访问的绝对地址/内联资源时直接返回
  if (/^(https?:)?\/\//i.test(value) || /^data:/i.test(value) || /^blob:/i.test(value)) {
    return value;
  }
  if (!API_BASE_URL) {
    return value;
  }
  const normalizedBase = API_BASE_URL.endsWith("/") ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
  return value.startsWith("/") ? `${normalizedBase}${value}` : `${normalizedBase}/${value}`;
}

export async function requestJson(url, options = {}) {
  // credentials: include 用于携带 Cookie（后端以 session 维护登录态）
  const response = await fetch(buildApiUrl(url), {
    credentials: "include",
    ...options,
    headers: buildHeaders(options)
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch (error) {
    throw new ApiError("服务响应不可解析", -1, response.status);
  }

  if (response.ok && payload?.code === 200) {
    return payload.data;
  }

  // 统一把后端 ApiResponse 的 code/message 映射为前端异常
  const code = payload?.code ?? response.status;
  const message = payload?.message || "请求失败";
  // 账号异常/禁用这类提示需要即时告知用户（后端也会返回对应业务码）
  if (code === 4231 || code === 4032) {
    window.alert(message);
  }
  throw new ApiError(message, code, response.status);
}
