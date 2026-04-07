/**
 * Vite 配置：Vue 插件、开发服务器端口，以及将 /api、/static 代理到后端（默认 localhost:8080）。
 */
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/static": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
