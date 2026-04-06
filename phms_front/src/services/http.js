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
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  if (!API_BASE_URL) {
    return path;
  }
  const normalizedBase = API_BASE_URL.endsWith("/") ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
  return `${normalizedBase}${path}`;
}

function buildHeaders(options = {}) {
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

  const code = payload?.code ?? response.status;
  const message = payload?.message || "请求失败";
  if (code === 4231 || code === 4032) {
    window.alert(message);
  }
  throw new ApiError(message, code, response.status);
}
