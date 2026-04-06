# md_phms

集成多源数据的个人健康管理系统。

## 项目结构

- `phms_cpmmon`：公共响应模型
- `phms_pojo`：实体与视图对象
- `phms_server`：Spring Boot + MyBatis + MySQL 后端
- `phms_front`：Vue3 前端

## 数据库配置

后端默认使用以下配置（见 `phms_server/src/main/resources/application.yml`）：

- host: `localhost`
- port: `3306`
- database: `md_hms`
- username: `root`
- password: `123456`

启动前请先在 `md_hms` 中执行用户表脚本：

- `phms_server/sql/t_user.sql`

管理员账号会在服务启动时自动初始化：

- account: `admin`
- password: `123456`

## 启动后端

```bash
cd d:\Code\md_phms
.\mvnw.cmd clean package -DskipTests
java -jar .\phms_server\target\phms_server-1.0.0-SNAPSHOT.jar
```

后端地址：`http://localhost:8080`

## 启动前端

```bash
cd d:\Code\md_phms\phms_front
npm install
npm run dev
```

前端地址：`http://localhost:5173`

## 已实现功能

- 普通用户：注册、登录、登出
- 普通用户登录后：健康概览、个人档案、健康趋势、每日打卡页面
- 管理员登录后：普通用户列表、模糊检索、账号状态维护（正常/异常/禁用）
- 账号状态控制：
  - `正常`：可读可写
  - `异常`：只读，写操作会提示
  - `禁用`：无法登录
