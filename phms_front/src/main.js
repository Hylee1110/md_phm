/**
 * 前端应用入口：挂载 Vue 根组件、注册路由并加载全局样式。
 */
import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "./styles.css";

createApp(App).use(router).mount("#app");
