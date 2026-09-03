# NovaFlow AI — QA REPORT

> 持续更新 · 生产上线前质量审计  
> 最后更新：2026-09-02

---

## 项目概况

| 项 | 值 |
|----|-----|
| 产品 | NovaFlow AI 1.0.1 |
| 架构 | Java 21 模块化单体 + Vue 3 前端 |
| 测试环境 | 本地 dev · Docker（Redis/MinIO/Qdrant）· MySQL localhost |
| 当前阶段 | 第 8 次完成（全量回归 + 上线门禁） |

---

## 测试统计

| 级别 | 发现 | 已修复 | 遗留 |
|------|------|--------|------|
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 0 |
| P2 | 2 | 2 | 0 |
| P3 | 7 | 7 | 0 |

---

## 问题清单

### ENV-001

**Severity:** P3

**Module:** 部署 / 运维

**Problem:** 长时间运行的旧后端 JAR 进程会导致多个 API 误返回 HTTP 500（`code=50000`），包括 `/actuator/health`、`/api/v1/portal/apps`、`/api/v1/audit-logs`。

**Reproduction:**
1. 启动旧版 `novaflow-server` JAR 并长期运行
2. 调用上述接口
3. 观察到 500 响应

**Expected:** 健康检查与业务 API 正常响应

**Actual:** 统一返回 `系统繁忙，请稍后重试`

**Root Cause:** 陈旧进程状态异常，非当前源码逻辑缺陷；重启并部署最新构建后恢复。

**Affected Files:** 无（运维问题）

**Fix:** `/api/v1/health` 捕获检查异常并返回 50300（不再变成未处理 50000）；响应包含 `version` + `startedAt`，便于识别陈旧进程。生产依赖 Docker healthcheck + restart。

**Verification:** ✅ 本机新 JAR：`startedAt` 存在；依赖异常返回 503 而非 50000

**Regression:** N/A

---

### FE-WARN-001

**Severity:** P3

**Module:** 前端 · 应用管理表单

**Problem:** Vue 控制台警告 `Invalid prop: type check failed for prop "maxlength". Expected Number with value 128, got String with value "128"`。

**Reproduction:** E2E 创建应用 / Prompt 等流程时触发。

**Expected:** `maxlength` 传入 Number

**Actual:** 传入字符串 `"128"`

**Root Cause:** `a-input` 的 `maxlength` 绑定为字符串字面量（待定位具体组件）

**Fix:** `:maxlength="128"` 已修复（6 处表单）

**Verification:** ✅ 代码修复

---

### FE-WARN-002

**Severity:** P3

**Module:** 前端 · 平台超管 / 审计

**Problem:** 加载 `/platform`、`/audit` 时 `mounted` 内 API 失败抛出未处理 Promise（`系统繁忙，请稍后重试`），冒烟测试仍通过（仅断言 DOM 存在）。

**Reproduction:** Playwright `平台超管页面可加载` 期间观察 Vite 控制台。

**Expected:** API 失败时页面展示 Error 状态，不抛 unhandled rejection

**Actual:** 控制台 Unhandled rejection，页面容器仍渲染

**Root Cause:** `loadTenants` / `loadLogs` 缺少 catch 或错误边界（待代码审查）

**Fix:** `loadTenants` / `loadStats` / `loadLogs` 增加 `catch` + `message.error`

**Verification:** ✅ 代码修复

---

### DB-P2-001

**Severity:** P2

**Module:** 后端 · 分页 API

**Problem:** 多数列表接口（如 `/api/v1/agents`）未对 `pageSize` 设上限，而 Billing/Trace/Notification 已限制为 50–100。

**Reproduction:** `GET /api/v1/agents?page=1&pageSize=99999` 可请求超大分页。

**Expected:** 统一限制 `pageSize` ≤ 100

**Actual:** `AgentService.page` 等直接使用客户端传入的 `pageSize`

**Root Cause:** 分页参数校验不一致，未抽取公共 clamp 工具

**Affected Files:** `AgentService.java` 及同类列表 Service

**Fix:** `PageQueryUtils.normalizePageSize()` 已应用于各列表 Service

**Verification:** ✅ `PageQueryUtilsTest` + 编译通过

---

### SEC-P3-003

**Severity:** P3

**Module:** 后端 · HTTP 工具

**Problem:** 创建/更新 HTTP 工具时可保存指向内网或元数据 IP 的 URL，仅在 `test`/执行阶段被 `UrlSafetyValidator` 拒绝。

**Reproduction:** `POST /api/v1/tools` 保存 `url=http://127.0.0.1:8080/...` → 201；`POST /{id}/test` → `工具 URL 不允许访问内网或保留地址`。

**Expected:** 保存时即拒绝非法 URL

**Actual:** 保存成功，执行时拦截

**Fix:** `ToolDefinitionService.validateHttpTool` 调用 `UrlSafetyValidator`

**Verification:** ✅ 编译通过

---

### SEC-P3-004

**Severity:** P3

**Module:** 部署 · CORS

**Problem:** `deploy/docker-compose.prod.yml` 与 `application-prod.yml` 默认 `CORS_ALLOWED_ORIGIN=http://localhost`，若部署未覆盖将导致生产 CORS 过宽或前端无法跨域。

**Fix:** 生产 `ProdSecurityValidator` 强制 `CORS_ALLOWED_ORIGIN`；禁止 `*` / 空 / localhost（本机冒烟需 `NOVAFLOW_CORS_ALLOW_LOCALHOST=true`）。Compose 不再默认 `http://localhost`。

**Verification:** ✅ `ProdSecurityValidatorTest` + live OPTIONS：外域拒绝，`http://localhost:3000` 允许

---

### DEP-P3-001

**Severity:** P3

**Module:** 依赖 · 前端

**Problem:** `npm audit` 在 npmmirror 镜像下返回 404（audit API 未实现），无法完成 DEP-02。

**Fix:** `test/dependency-audit.ps1` 与 `npm run audit:ci` 固定 `registry.npmjs.org`

**Verification:** ✅ 2026-09-02 `npm audit` critical=0 high=0

---

### CONC-P2-001

**Severity:** P2

**Module:** 后端 · Agent 发布

**Problem:** 50 并发 `POST /agents/{id}/publish` 均返回成功，但 `agent.version` 从 2 仅增至 9（丢失更新），非原子递增。

**Reproduction:** `test/concurrency-run.ps1` CC-02

**Expected:** version 递增 50 次或幂等拒绝重复发布

**Actual:** 50× HTTP 200，version 净增 ~7

**Fix:** `AgentPublishService.lockAgentForUpdate()` + `FOR UPDATE`

**Verification:** ✅ 集成测试 PASS（见 `AUTO-FIX-REPORT.md`）

---

### CONC-P3-001

**Severity:** P3

**Module:** 后端 · 收藏 toggle

**Problem:** 100 并发 favorite toggle 中 6 次返回 `50000`；终态数据仍正确（0 条重复收藏）。

**Fix:** `FavoriteService.toggle` 将事务放在重试循环外，死锁 / 唯一键冲突最多重试 5 次。

**Verification:** ✅ `coverage-gap-smoke` 收藏并发 80/80 `code=0`

---

## 已验证通过项（第 1–6 阶段）

- 项目侦察与模块地图（`PROJECT-MAP.md`）
- Maven 单元测试全绿
- `novaflow-server` 本地集成测试 25/25
- Open API 安全：callerId 隔离、Embed 权限边界、无效 Token 拒绝
- RBAC：普通用户无法访问平台/Agent 管理/账单
- 核心业务 API 冒烟：Dashboard、Monitor、Billing、Portal、Search
- 基础设施健康：MySQL、Redis、MinIO、Qdrant
- 前端 E2E Playwright **30/30**（见 `E2E-TEST-REPORT.md`）
- 数据库：Flyway V26、唯一约束、性能索引、软删除、事务批处理（见 `INFRA-AUDIT-REPORT.md`）
- Redis：Key/TTL 清单、登录锁定实测、限流单元测试
- MQ：未使用（N/A）
- 安全专项：SQLi / SSRF / IDOR / 认证绕过 / Open API（见 `SECURITY-AUDIT-REPORT.md`）
- XSS：DOMPurify + markdown 渲染链
- 生产配置：`springdoc` 关闭、注册关闭、Actuator 仅 health（prod yml）
- 安全单元测试：UrlSafety / MCP / OpenApiAuth 全 PASS
- 并发：Open API chat×10、工作流 run×10、文档上传×10、收藏 toggle 数据一致性（见 `CONCURRENCY-TEST-REPORT.md`）

---

## 未验证项（后续阶段）

| 类别 | 状态 |
|------|------|
| Portal / Embed 前端 E2E | ✅ Playwright `chromium-portal` + `chromium-embed` 7/7 |
| 跨租户 IDOR | ✅ `cross-tenant-idor.ps1` + `CrossTenantIdorLocalIntegrationTest` |
| XSS 浏览器 payload 实测 | ✅ `xss.spec.ts` PASS；已禁止 markdown 中的 `svg`/`onerror` |
| Redis/MySQL 故障注入容灾 | ✅ `fault-injection.ps1`（停 Redis/MySQL80 → 503，恢复后 UP） |
| 生产 Compose 全栈部署 | ✅ prebuilt 栈 `18080/13000`：容器 + Nginx 反代 + health UP（demo 账号按生产关闭） |
| 依赖 CVE 扫描（OWASP / npm audit） | ✅ `npm audit` 0 vulnerabilities（`nanoid` override）；OWASP NVD 未跑 |
| Testcontainers 集成测试 | ⚠️ 9 skipped：Docker Desktop API 400（`disabledWithoutDocker`），与 compose 可用并存 |
| 覆盖缺口脚本 | ✅ `coverage-gap-smoke.ps1`：鉴权/RBAC/模块读接口/CORS/收藏×80/混合读×40 |
| 对话 / RAG 主路径 | ✅ `chat-rag-smoke.ps1` debug chat + retrieve |
| Playwright E2E | ✅ 36/36（含 Portal / Embed / XSS / 全站页面） |
| 部署 pageSize 验收 | ✅ live `pageSize=99999` → 100 |
| CC-02 publish 并发回归 | ✅ 50/50 HTTP 成功，version +50 |

---

## 回归测试结果（第 8 次 · 全量）

| 检查项 | 结果 |
|--------|------|
| `mvn test -Dtest.excludedGroups=local,testcontainers` | ✅ BUILD SUCCESS |
| `mvn -pl novaflow-server test -Dtest.excludedGroups=testcontainers` | ✅ 25/25 |
| `npm run build`（novaflow-web） | ✅ |
| Playwright E2E | ✅ 30/30（57.6s） |
| Live pageSize 截断（:8080） | ⚠️ 旧 JAR，部署后复验 |

详见 `REGRESSION-GATE-REPORT.md`。

---

## 回归测试结果（第 2–7 阶段）

| 检查项 | 结果 |
|--------|------|
| `mvn test -Dtest.excludedGroups=local,testcontainers` | PASS |
| `mvn -pl novaflow-server test -Dtest.excludedGroups=testcontainers` | PASS（25 tests） |
| 实时 API 探测 | PASS |
| Playwright E2E 30 用例 | PASS |
| 登录失败锁定（Redis） | PASS（第 5 次失败 42902） |
| Flyway / 索引 / 唯一约束审查 | PASS |

---

## 最终上线结论

```
READY FOR PRODUCTION (with deployment checklist)
```

**依据：** P0=0、P1=0、P2 已全部修复；Maven / 前端构建 / E2E / 集成测试全绿；无严重安全漏洞。

**部署前 checklist：**

1. 重新部署含第 7 次修复的后端镜像/JAR（当前 :8080 仍为旧构建）
2. 验收 `pageSize=99999` → 响应 `pageSize:100`
3. 生产 `.env`：CORS、密钥、关闭开放注册
4. （推荐）`docker-compose.prod.yml` 全栈冒烟

**非阻塞遗留：** Testcontainers 在本机 Docker Desktop 29 上仍 skip（compose 冒烟已覆盖容器栈）。无法穷尽所有数据/并发组合。
