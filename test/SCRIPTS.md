# NovaFlow AI — 上线前门禁脚本

> 对应 QA 遗漏项：部署验收、跨租户 IDOR、CC-02 回归、生产 Compose 冒烟、依赖扫描  
> **缺口补全路线图：** 见 [`TEST-GAP-PLAN.md`](./TEST-GAP-PLAN.md)（缺口 → 脚本/单测/E2E 映射 + 分阶段计划）  
> 前提：本机后端 `http://localhost:8080`（可用 `NOVAFLOW_BASE_URL` 覆盖）

## 一键执行

```powershell
# 默认：部署门禁 + RBAC + 覆盖缺口 + 各模块 smoke + 依赖审计 + 并发 + 故障注入
pwsh test/run-pre-release-gates.ps1

# 跳过并发（省时间）
pwsh test/run-pre-release-gates.ps1 -SkipConcurrency

# 跳过故障注入（不停 MySQL/Redis/MinIO/Qdrant 容器，适合无 Docker 或共享环境）
pwsh test/run-pre-release-gates.ps1 -SkipFaultInjection

# 含生产 Compose 冒烟（需先 docker compose up）
pwsh test/run-pre-release-gates.ps1 -IncludeProdCompose
```

## 单脚本

| 脚本 | 覆盖项 | 命令 |
|------|--------|------|
| `pre-deploy-gate.ps1` | 健康检查（MySQL/Redis）、登录、`pageSize`≤100 | `pwsh test/pre-deploy-gate.ps1` |
| `cross-tenant-idor.ps1` | Z-04~Z-06 跨租户拒绝 | `pwsh test/cross-tenant-idor.ps1` |
| `rbac-api-acceptance.ps1` | 六角色 RBAC + 资源 ACL + 跨租户 IDOR + Owner/Portal/Z-07 | `pwsh test/rbac-api-acceptance.ps1` |
| `open-api-acceptance.ps1` | Open API / Embed 安全（O-01~O-08, AG-10） | `pwsh test/open-api-acceptance.ps1` |
| `agent-debug-smoke.ps1` | Agent 调试对话（AG-04~AG-08） | `pwsh test/agent-debug-smoke.ps1` |
| `conversation-key-isolation.ps1` | conversationKey 隔离（C-04） | `pwsh test/conversation-key-isolation.ps1` |
| `http-tool-ssrf.ps1` | HTTP 工具 SSRF 防护（T-02） | `pwsh test/http-tool-ssrf.ps1` |
| `mcp-command-whitelist.ps1` | MCP stdio 命令白名单（T-04） | `pwsh test/mcp-command-whitelist.ps1` |
| `model-api-key-encryption.ps1` | Model API Key 加密存储（M-02） | `pwsh test/model-api-key-encryption.ps1` |
| `registration-disabled-gate.ps1` | prod 注册关闭（A-03） | `pwsh test/registration-disabled-gate.ps1` |
| `scan-hardcoded-secrets.ps1` | 硬编码凭证扫描（ST-02） | `pwsh test/scan-hardcoded-secrets.ps1` |
| `agent-lifecycle-smoke.ps1` | Agent CRUD / 发布 / rotate（AG-01~03） | `pwsh test/agent-lifecycle-smoke.ps1` |
| `member-management-smoke.ps1` | 成员邀请 / 改角色 / 配额（U-03~04） | `pwsh test/member-management-smoke.ps1` |
| `mcp-server-smoke.ps1` | MCP CRUD / connect（T-03, T-05） | `pwsh test/mcp-server-smoke.ps1` |
| `agent-bindings-smoke.ps1` | Agent 绑定 KB/工具/Skill（AG-09） | `pwsh test/agent-bindings-smoke.ps1` |
| `application-lifecycle-smoke.ps1` | Application CRUD/发布（AP-01~02） | `pwsh test/application-lifecycle-smoke.ps1` |
| `audit-access-smoke.ps1` | 审计日志权限（U-06） | `pwsh test/audit-access-smoke.ps1` |
| `knowledge-base-smoke.ps1` | 知识库 CRUD/上传（K-01~02） | `pwsh test/knowledge-base-smoke.ps1` |
| `workflow-lifecycle-smoke.ps1` | 工作流 CRUD/发布/run（W-01~03） | `pwsh test/workflow-lifecycle-smoke.ps1` |
| `http-tool-crud-smoke.ps1` | HTTP 工具 CRUD + test（T-01） | `pwsh test/http-tool-crud-smoke.ps1` |
| `skill-upload-smoke.ps1` | Skill 上传（T-06） | `pwsh test/skill-upload-smoke.ps1` |
| `portal-studio-isolation-smoke.ps1` | Portal 与 Studio 权限隔离（AP-04） | `pwsh test/portal-studio-isolation-smoke.ps1` |
| `billing-overview-smoke.ps1` | Billing overview/quota（B-01） | `pwsh test/billing-overview-smoke.ps1` |
| `scan-api-permissions.ps1` | Z-07 Controller 权限注解扫描 | `pwsh test/scan-api-permissions.ps1` |
| `publish-concurrency-gate.ps1` | CC-02 version +50 | `pwsh test/publish-concurrency-gate.ps1` |
| `prod-compose-smoke.ps1` | PR-01 全栈冒烟 | `pwsh test/prod-compose-smoke.ps1` |
| `dependency-audit.ps1` | DEP-01/02 | `pwsh test/dependency-audit.ps1` |
| `chat-rag-smoke.ps1` | 调试对话 + 知识库 retrieve | `pwsh test/chat-rag-smoke.ps1` |
| `fault-injection.ps1` | Redis/MySQL/MinIO/Qdrant 停服恢复（F-01~F-04） | `pwsh test/fault-injection.ps1` |
| `coverage-gap-smoke.ps1` | 鉴权/RBAC/模块读接口/CORS/收藏并发 | `pwsh test/coverage-gap-smoke.ps1` |
| `auth-lifecycle-smoke.ps1` | 注册/登录边界（A-01, A-05） | `pwsh test/auth-lifecycle-smoke.ps1` |
| `auth-lock-smoke.ps1` | 登录失败锁定（A-06） | `pwsh test/auth-lock-smoke.ps1` |
| `api-boundary-smoke.ps1` | API 写接口边界（API-01~06） | `pwsh test/api-boundary-smoke.ps1` |
| `prompt-lifecycle-smoke.ps1` | Prompt CRUD/版本/测试（P-01~03） | `pwsh test/prompt-lifecycle-smoke.ps1` |
| `model-lifecycle-smoke.ps1` | 模型 Provider/Config（M-01/03/05/06） | `pwsh test/model-lifecycle-smoke.ps1` |
| `knowledge-boundary-smoke.ps1` | 知识库大文件/非法类型/路径遍历（K-03/04, S-06） | `pwsh test/knowledge-boundary-smoke.ps1` |
| `knowledge-document-lifecycle-smoke.ps1` | 文档 reprocess/删除（K-05） | `pwsh test/knowledge-document-lifecycle-smoke.ps1` |
| `workflow-agent-node-smoke.ps1` | 工作流 Agent 节点（W-04） | `pwsh test/workflow-agent-node-smoke.ps1` |
| `workflow-idempotency-smoke.ps1` | 工作流重复 run（W-05） | `pwsh test/workflow-idempotency-smoke.ps1` |
| `agent-tool-execution-smoke.ps1` | Agent 工具执行（T-07） | `pwsh test/agent-tool-execution-smoke.ps1` |
| `chat-history-smoke.ps1` | 对话历史分页（C-01） | `pwsh test/chat-history-smoke.ps1` |
| `billing-manage-smoke.ps1` | 配额修改（B-02） | `pwsh test/billing-manage-smoke.ps1` |
| `billing-token-accuracy-smoke.ps1` | Token 用量记录（B-04） | `pwsh test/billing-token-accuracy-smoke.ps1` |
| `billing-alert-smoke.ps1` | 账单预警配置（B-03） | `pwsh test/billing-alert-smoke.ps1` |
| `observability-smoke.ps1` | Trace 分页/详情（OB-01~02） | `pwsh test/observability-smoke.ps1` |
| `global-search-smoke.ps1` | 全局搜索（D-04） | `pwsh test/global-search-smoke.ps1` |
| `dashboard-extended-smoke.ps1` | recent/favorites（D-02/D-03） | `pwsh test/dashboard-extended-smoke.ps1` |
| `org-extended-smoke.ps1` | 租户/通知（U-01/U-05） | `pwsh test/org-extended-smoke.ps1` |
| `llm-fault-smoke.ps1` | LLM 故障降级（F-05） | `pwsh test/llm-fault-smoke.ps1` |
| `agent-concurrent-update-smoke.ps1` | 并发更新 Agent（DB-03） | `pwsh test/agent-concurrent-update-smoke.ps1` |
| `scan-code-smells.ps1` | 代码异味扫描（ST-01） | `pwsh test/scan-code-smells.ps1` |
| `auth-rate-limit-smoke.ps1` | 登录限流（A-07） | `pwsh test/auth-rate-limit-smoke.ps1` |
| `open-api-rate-limit-smoke.ps1` | Open API 限流（O-09） | `pwsh test/open-api-rate-limit-smoke.ps1` |
| `knowledge-quota-smoke.ps1` | 知识库套餐配额（K-09） | `pwsh test/knowledge-quota-smoke.ps1` |
| `billing-export-smoke.ps1` | 账单/日志导出（B-05） | `pwsh test/billing-export-smoke.ps1` |
| `workflow-invalid-graph-smoke.ps1` | 非法工作流图（W-06） | `pwsh test/workflow-invalid-graph-smoke.ps1` |
| `workflow-dashboard-runtime-smoke.ps1` | Dashboard 工作流运行时（W-07） | `pwsh test/workflow-dashboard-runtime-smoke.ps1` |
| `chat-redis-memory-smoke.ps1` | Redis 窗口记忆 vs DB（C-02） | `pwsh test/chat-redis-memory-smoke.ps1` |
| `prompt-agent-bind-smoke.ps1` | Agent 引用 Prompt（P-04） | `pwsh test/prompt-agent-bind-smoke.ps1` |
| `model-sync-smoke.ps1` | Provider sync（M-04） | `pwsh test/model-sync-smoke.ps1` |
| `observability-otlp-smoke.ps1` | OTLP 配置（OB-03） | `pwsh test/observability-otlp-smoke.ps1` |
| `docker-image-audit.ps1` | Docker 镜像版本（DEP-03） | `pwsh test/docker-image-audit.ps1` |
| `observability-langfuse-smoke.ps1` | Langfuse 配置（OB-04） | `pwsh test/observability-langfuse-smoke.ps1` |
| `cors-prod-audit.ps1` | 生产 CORS 配置（PR-06） | `pwsh test/cors-prod-audit.ps1` |

结果 JSON / 日志写在 `test/` 同目录。

## 环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `NOVAFLOW_BASE_URL` | `http://localhost:8080` | API 基址 |
| `NOVAFLOW_WEB_URL` | `http://localhost:3000` | 前端 / Nginx |
| `SKIP_DOCKER_CHECK` | — | 设为 `1` 跳过容器名检查 |

## Java 集成测试（跨租户）

```powershell
# 默认 pom 排除 @Tag("local")；用 none 占位以启用 local 用例
mvn -pl novaflow-server test "-Dtest=CrossTenantIdorLocalIntegrationTest" "-Dtest.excludedGroups=none"
mvn -pl novaflow-server test "-Dtest=PortalAccessLocalIntegrationTest" "-Dtest.excludedGroups=none"
mvn -pl novaflow-server test "-Dtest=UnpublishedAgentOpenApiLocalIntegrationTest" "-Dtest.excludedGroups=none"
```

需本机 MySQL + Redis（与现有 `@Tag("local")` 套件相同）。  
若 live API 的 `/auth/register` 返回 500（旧 JAR），请优先跑本集成测试。

## Playwright E2E（Portal / Embed）

```powershell
cd novaflow-web
npm run test:e2e -- --project=chromium-auth
npm run test:e2e -- --project=chromium-portal
npm run test:e2e -- --project=chromium-embed
npm run test:e2e -- --project=chromium-route-guard
```

- Auth（含 Token 过期 / 防重复提交）：`chromium-auth` 项目

- Portal：`user@novaflow.ai` / `User123!`
- Embed：脚本内用 admin 发布 Agent 后打开 `/embed/agents/:id`

## 生产 Compose 步骤

本机 Docker 若无法在构建阶段访问 Maven Central，用 **prebuilt** 覆盖（先 `mvn package` + `npm run build`）：

```powershell
cd novaflow-web
npm run build
cd ..
docker compose -p novaflow-prod-qa --env-file test/.env.prod.smoke `
  -f deploy/docker-compose.prod.yml -f deploy/docker-compose.prod.prebuilt.yml up -d --build

$env:NOVAFLOW_BASE_URL = 'http://127.0.0.1:18080'
$env:NOVAFLOW_WEB_URL  = 'http://localhost:13000'
pwsh test/prod-compose-smoke.ps1
```

从源码完整构建（需 Docker 能访问 Maven / npm）：

```powershell
copy deploy\.env.prod.example .env   # 改密钥与 CORS
docker compose -f deploy/docker-compose.prod.yml up -d --build
pwsh test/prod-compose-smoke.ps1
```
