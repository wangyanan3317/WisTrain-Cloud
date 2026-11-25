# 智培云 WisTrain-Cloud

基于权限联动的企业培训与第三方智能体集成系统（Vue 3 + Spring Boot 3 实现）。

> 设计定调：工业实用风——等宽数字、发丝边框、信号橙 `#FF6B2C` 全站唯一强调色，只给「解锁」状态。

## 目录结构

```
wis-train-cloud/
├── design/                  # 设计稿（自包含 HTML，Vue 端视觉基准）
│   └── dashboard-preview.html
├── backend/                 # Spring Boot 3 + MyBatis 后端
│   ├── pom.xml
│   └── src/main/java/com/wistrain/cloud/
│       ├── common/          # 统一响应 / JWT / 拦截器 / 异常 / AES
│       ├── config/          # Web 与拦截器注册
│       ├── entity/          # 9 张表实体
│       ├── mapper/          # MyBatis 注解 SQL
│       ├── dto/             # 请求 / 视图对象
│       ├── service/         # 业务逻辑（含核心 PermissionService 权限联动）
│       └── controller/      # REST 接口
├── frontend/                # Vue 3 + Element Plus 前端
│   └── src/
│       ├── styles/tokens.css    # 设计令牌（与设计稿一致）
│       ├── views/               # 员工端 + 管理端页面
│       └── ...
└── sql/ -> ../sql/          # 建表脚本见仓库根目录 sql/wistrain_cloud.sql
```

## 启动步骤

1. **数据库**：执行 `sql/wistrain_cloud.sql`（MySQL 8，含 9 张表与示例数据）
2. **后端**：
   ```bash
   cd backend
   mvn spring-boot:run   # 默认 8080，配置见 application.yml，可用环境变量覆盖
   ```
3. **前端**：
   ```bash
   cd frontend
   npm install
   npm run dev           # 默认 5173，/api 代理到 8080
   ```
4. **默认账号**：`admin / 123456`（部门管理员，示例数据中密码为占位符，首次启动后用接口重置或自行更新 BCrypt 密文）

## 核心机制：权限联动

```
员工标记资料已学 → LearningService 重算 PROGRESS
              → PermissionService.refreshUnlock（按 tb_project_agent 解锁规则判定）
              → 满足规则：点亮 UNLOCK_TIME（信号橙）
访问智能体接口 → PermissionService.checkAccess 服务端实时判定（不信任前端）
```

判定以实时计算为准，`UNLOCK_TIME` 仅作展示冗余——管理员修改解锁规则后即时生效。

## 接口约定

RESTful；员工接口 `/api/**`，管理接口 `/api/admin/**`；统一响应 `{code, message, data}`；
智能体对话接口返回 `text/event-stream`（SSE 流式）。完整清单见《设计说明书》第 4 章。
