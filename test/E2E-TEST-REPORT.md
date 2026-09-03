# NovaFlow AI — E2E TEST REPORT

> 阶段：第 3 次 · 前端 E2E  
> 日期：2026-09-02  
> 工具：Playwright 1.51 · Chrome  
> 命令：`cd novaflow-web && npm run test:e2e`

---

## 1. 环境

| 项 | 值 |
|----|-----|
| 前端 | Vite dev `http://localhost:3000`（Playwright webServer 自动启动） |
| 后端 | `http://localhost:8080`（API 代理） |
| 账号 | `admin@novaflow.ai` / `platform@novaflow.ai`（见 `e2e/helpers/auth.ts`） |

---

## 2. 执行结果

```
30 passed (59.8s)
0 failed
0 skipped
```

| Project | 用例数 | 结果 |
|---------|--------|------|
| setup（登录态缓存） | 2 | ✅ |
| chromium-auth（登录流程） | 2 | ✅ |
| chromium（租户侧） | 25 | ✅ |
| chromium-platform（平台超管） | 1 | ✅ |

---

## 3. 用例明细

### 认证（auth.spec.ts）

| 用例 | 结果 |
|------|------|
| 未登录访问 `/dashboard` 跳转登录 | ✅ |
| 演示账号登录进入工作台 | ✅ |

### Agent Studio（agent.spec.ts）

| 用例 | 结果 |
|------|------|
| 列表搜索 + 打开调试面板 | ✅ |
| 编辑 Agent 显示右侧调试面板 | ✅ |

### 业务模块（modules.spec.ts）

| 用例 | 结果 |
|------|------|
| 工作台欢迎横幅与统计 | ✅ |
| 应用创建并删除 | ✅ |
| Prompt 创建并删除 | ✅ |
| 工作流创建、编辑、删除 | ✅ |
| 知识库创建、详情、删除 | ✅ |
| 工具市场 Skill/MCP Tab | ✅ |
| 模型中心三 Tab | ✅ |
| 监控 / 日志 / 链路 / 可观测性 | ✅ |
| 组织 / 权限 / 账单 | ✅ |

### 全站冒烟（smoke-pages.spec.ts）

| 用例 | 结果 |
|------|------|
| 租户侧 17 个页面可加载 | ✅ |
| 注册页可加载 | ✅ |
| 平台超管 `/platform`、`/audit` | ✅ |

### 关于页（about.spec.ts）

| 用例 | 结果 |
|------|------|
| `/about` 及 6 个子页 | ✅（7 项） |

---

## 4. 未覆盖（NOT VERIFIED）

| 项 | 说明 |
|----|------|
| Portal 用户流程 `/portal` | 无专用 E2E |
| Embed 页 `/embed/agents/:id` | 无专用 E2E |
| Token 过期跳转 | 无专用 E2E |
| 响应式布局 | 无专用 E2E |
| 重复点击 / 快速点击 | 无专用 E2E |

---

## 5. 测试期间观察（非失败）

| ID | 观察 | 级别 |
|----|------|------|
| FE-WARN-001 | Vue 警告：`maxlength` prop 期望 Number 收到 String `"128"`（应用创建等表单） | P3 |
| FE-WARN-002 | 平台超管加载 `/platform`、`/audit` 时控制台出现 `系统繁忙，请稍后重试`（mounted hook 未捕获），但页面 DOM 冒烟仍通过 | P3 |

---

## 6. 结论

**前端 E2E：PASS**（30/30）

建议后续补充 Portal / Embed 专项 E2E，并修复 `maxlength` 类型警告。
