# NovaFlow AI — 测试缺口补全计划

> 生成日期：2026-09-05  
> 关联：`TEST-CHECKLIST.md` · `SCRIPTS.md` · `run-pre-release-gates.ps1`  
> 目标：将「缺口 → 测试资产」一一映射，按阶段落地并可纳入门禁

---

## 1. 现状摘要

| 维度 | 已有 | 缺口（清单 ⬜） |
|------|------|----------------|
| Java 单测/集成 | ~40 类（10 个模块有 `src/test`） | 10 个业务模块零单测 |
| PowerShell 冒烟 | 34 脚本 | 约 15 项能力无专用脚本 |
| Playwright E2E | 12 spec | 4 项前端行为未测 |
| 默认门禁 `run-pre-release-gates.ps1` | 22 步 | 6 个已有脚本未纳入 |

**说明：** 部分清单项已在脚本中实现，但 `TEST-CHECKLIST.md` 状态未同步（见 §2「已覆盖待同步」）。

---

## 2. 缺口 → 测试资产映射（全表）

图例：**✅ 已有** · **🔧 扩展现有脚本** · **🆕 新建脚本** · **☕ Java 单测** · **🎭 E2E** · **📋 同步清单**

### 2.1 Authentication

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| A-01 | 注册：合法邮箱/用户名 | P1 | 🆕 | `auth-lifecycle-smoke.ps1` | Phase 1 |
| A-02 | 注册：重复邮箱/用户名 | P1 | 📋 | `coverage-gap-smoke.ps1`（已有） | Phase 0 |
| A-03 | prod 注册关闭 | P0 | ✅ | `registration-disabled-gate.ps1` | 已纳入 |
| A-04 | 登录：正确凭证 | P1 | ✅ | `pre-deploy-gate.ps1` | 已纳入 |
| A-05 | 登录：错误密码 | P1 | 🔧 | `auth-lifecycle-smoke.ps1` | Phase 1 |
| A-06 | 连续失败锁定 5/15min | P1 | 🆕 + ☕ | `auth-lock-smoke.ps1` + `LoginFailureLockServiceTest`（扩 E2E） | Phase 1 |
| A-07 | 登录限流 120/min | P2 | 🆕 | `auth-rate-limit-smoke.ps1` | Phase 3 |
| A-08 | 登出后 Token 失效 | P1 | 📋 | `coverage-gap-smoke.ps1`（已有） | Phase 0 |
| A-09 | `/auth/me` 未登录 401 | P1 | ✅ | `coverage-gap-smoke.ps1` | Phase 0 |
| A-10 | Token 过期 86400s | P2 | 🎭 | `novaflow-web/e2e/auth-expiry.spec.ts` | Phase 3 |
| A-11 | Token 篡改/伪造 | P0 | ✅ | `open-api-acceptance.ps1` / 集成测试 | 已纳入 |
| A-12 | Redis 重启后会话 | P1 | 🔧 | `fault-injection.ps1` 增 Redis restart 段 | Phase 2 |

### 2.2 Authorization（RBAC + 多租户）

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| Z-01~Z-06 | 角色/跨租户 | P0 | ✅ | `rbac-api-acceptance.ps1` + `cross-tenant-idor.ps1` + Java 集成 | Phase 0 纳入 rbac |
| Z-07 | 权限注解扫描 | P1 | ✅ | `scan-api-permissions.ps1` | Phase 0 |
| Z-08 | 前后端权限一致 | P1 | ✅ | `route-guard.spec.ts` | CI E2E |
| Z-09~Z-10 | 平台/Portal | P1 | ✅ | `rbac-api-acceptance.ps1` + `PortalAccessLocalIntegrationTest` | Phase 0 |

### 2.3 User / Org

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| U-01 | 查看/修改租户信息 | P2 | 🔧 | `coverage-gap-smoke.ps1` 增 PUT `/org/tenant` | Phase 2 |
| U-02 | 工作空间 CRUD | P2 | 📋 | `coverage-gap-smoke.ps1`（已有 create/delete） | Phase 0 |
| U-03~U-04 | 成员管理/配额 | P1 | ✅ | `member-management-smoke.ps1` | 已纳入 |
| U-05 | 通知列表/已读/未读 | P2 | 🔧 | `coverage-gap-smoke.ps1` 增 list + mark-read | Phase 2 |
| U-06 | 审计权限 | P1 | ✅ | `audit-access-smoke.ps1` | 已纳入 |

### 2.4 Agent

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| AG-01~AG-09 | CRUD/调试/绑定 | P1 | ✅ | `agent-*-smoke.ps1` 系列 | 已纳入 |
| AG-10 | 未发布 Open API 拒绝 | P0 | ✅ | `UnpublishedAgentOpenApiLocalIntegrationTest` | Java CI |

### 2.5 Open API / Embed

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| O-01~O-08 | 安全主路径 | P0 | ✅ | `open-api-acceptance.ps1` | 已纳入 |
| O-09 | Open API 限流 60/min | P2 | 🆕 | `open-api-rate-limit-smoke.ps1` | Phase 3 |
| O-10 | Embed 前端流程 | P1 | ✅ | `embed.spec.ts` | CI E2E |

### 2.6 Application & Portal

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| AP-01~AP-04 | 应用/门户 | P1 | ✅ | `application-lifecycle` + `portal-studio-isolation` + `portal.spec.ts` | 已纳入 |
| AP-05 | user 默认首页 `/portal` | P2 | 📋 | `coverage-gap-smoke.ps1`（portal list as user） | Phase 0 |

### 2.7 Workflow

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| W-01~W-03 | CRUD/发布/run | P1 | ✅ | `workflow-lifecycle-smoke.ps1` | 已纳入 |
| W-04 | Agent 节点调用已发布 Agent | P1 | 🆕 | `workflow-agent-node-smoke.ps1` | **Phase 1（高优）** |
| W-05 | 重复 run 幂等/副作用 | P1 | 🆕 | `workflow-idempotency-smoke.ps1` | Phase 1 |
| W-06 | 非法图（无起点、环） | P2 | ☕ | `WorkflowGraphValidatorTest`（`novaflow-workflow`） | Phase 2 |
| W-07 | Dashboard workflow runtime | P2 | 🔧 | `coverage-gap-smoke.ps1` 增 runtime 字段断言 | Phase 2 |

### 2.8 Knowledge & RAG

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| K-01~K-02 | CRUD/上传 | P1 | ✅ | `knowledge-base-smoke.ps1` | 已纳入 |
| K-03 | 超大文件 >50MB 拒绝 | P1 | 🆕 | `knowledge-boundary-smoke.ps1` | **Phase 1（高优）** |
| K-04 | 非法文件类型 | P1 | 🆕 | 同上 | Phase 1 |
| K-05 | reprocess / 删除文档 | P1 | 🆕 | `knowledge-document-lifecycle-smoke.ps1` | Phase 1 |
| K-06 | retrieve API | P1 | 📋 | `chat-rag-smoke.ps1`（已有） | Phase 0 |
| K-07 | MinIO 失败 DB 一致性 | P1 | 🔧 | `fault-injection.ps1` MinIO 段 + 断言 DB | Phase 2 |
| K-08 | Qdrant 不可用降级 | P1 | 🔧 | `fault-injection.ps1` Qdrant 段 + retrieve 错误码 | Phase 2 |
| K-09 | max_knowledge 配额 | P2 | 🆕 | `knowledge-quota-smoke.ps1` | Phase 3 |

### 2.9 Model

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| M-01 | Provider CRUD | P1 | 🆕 | `model-lifecycle-smoke.ps1` | **Phase 1（高优）** |
| M-02 | API Key 加密 | P0 | ✅ | `model-api-key-encryption.ps1` | 已纳入 |
| M-03 | 连通性 test | P1 | 🆕 | `model-lifecycle-smoke.ps1`（需真实 Key 或 mock profile） | Phase 1 |
| M-04 | sync 模型列表 | P2 | 🔧 | 同上 | Phase 2 |
| M-05 | Model config 默认项 | P1 | 🆕 | `model-lifecycle-smoke.ps1` | Phase 1 |
| M-06 | embedding-options | P1 | 🆕 | 同上 | Phase 1 |

### 2.10 Tool / MCP / Skill

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| T-01~T-06 | HTTP/MCP/Skill | P1 | ✅ | 对应 smoke 脚本 | 已纳入 |
| T-07 | Agent 绑定工具执行 | P1 | 🆕 | `agent-tool-execution-smoke.ps1` | **Phase 1（高优）** |

### 2.11 Prompt（全模块空白）

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| P-01 | 模板 CRUD | P2 | 🆕 | `prompt-lifecycle-smoke.ps1` | **Phase 1** |
| P-02 | 版本与 rollback | P2 | 🆕 | 同上 | Phase 1 |
| P-03 | 在线 test | P2 | 🆕 | 同上 | Phase 1 |
| P-04 | Agent 引用 Prompt | P2 | 🔧 | `agent-bindings-smoke.ps1` 扩展 | Phase 2 |

### 2.12 Chat / Conversation

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| C-01 | 消息持久化与分页 | P1 | 🆕 | `chat-history-smoke.ps1` | Phase 1 |
| C-02 | Redis 窗口记忆 vs DB | P1 | ☕ + 🆕 | `ConversationServiceTest` + `chat-history-smoke.ps1` | Phase 2 |
| C-03 | 90 天保留 cron | P2 | ☕ | `ConversationRetentionServiceTest`（`@Tag("slow")`） | Phase 3 |
| C-04 | conversationKey 隔离 | P0 | ✅ | `conversation-key-isolation.ps1` | 已纳入 |

### 2.13 Billing & Monitor

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| B-01 | overview / quota | P1 | ✅ | `billing-overview-smoke.ps1` | 已纳入 |
| B-02 | 配额修改 billing:manage | P1 | 🆕 | `billing-manage-smoke.ps1` | Phase 1 |
| B-03 | 预警配置与触发 | P2 | ☕ | `AlertDispatchServiceTest`（扩集成）+ smoke | Phase 2 |
| B-04 | token_usage 准确性 | P1 | 🆕 | `billing-token-accuracy-smoke.ps1`（chat 前后对比 logs） | Phase 1 |
| B-05 | export 账单/日志 | P2 | 🆕 | `billing-export-smoke.ps1` | Phase 3 |
| B-06 | monitor overview | P2 | 📋 | `coverage-gap-smoke.ps1` GET 已有 | Phase 0 |
| B-07 | 基础设施健康 | P1 | ✅ | `pre-deploy-gate.ps1` | 已纳入 |

### 2.14 Observability / Trace

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| OB-01~OB-02 | spans 分页/详情 | P2 | 🆕 | `observability-smoke.ps1` | Phase 2 |
| OB-03 | OTLP Span 上报 | P2 | 🆕 | `observability-otlp-smoke.ps1`（需 collector） | Phase 3 |
| OB-04 | Langfuse | P3 | 手动 | 部署文档 + 可选 smoke | — |

### 2.15 Dashboard & Search

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| D-01 | dashboard overview | P2 | 📋 | `coverage-gap-smoke.ps1` | Phase 0 |
| D-02 | recent-items / favorites | P2 | 🔧 | `coverage-gap-smoke.ps1` 增 recent + 二次 toggle | Phase 2 |
| D-03 | favorites 幂等 | P2 | ✅ | `coverage-gap-smoke.ps1` CC×80 | Phase 0 |
| D-04 | 全局搜索 | P2 | 🆕 | `global-search-smoke.ps1` | Phase 2 |

### 2.16 API 通用边界

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| API-01~04 | null/超长/非法ID/缺字段 | P1 | 🆕 | `api-boundary-smoke.ps1`（抽样 Agent/KB/Workflow 写 API） | Phase 1 |
| API-05~06 | Unicode/Content-Type | P2 | 🔧 | 同上扩展 | Phase 3 |

### 2.17 Redis / DB / 安全 / 并发 / 容灾

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| R-01~R-02 | Redis 故障/重启 | P1 | 🔧 | `fault-injection.ps1` | Phase 2 |
| DB-03 | 并发更新 Agent | P1 | 🆕 | `agent-concurrent-update-smoke.ps1` | Phase 2 |
| S-02 | XSS 浏览器实测 | P0 | 🎭 | `xss.spec.ts`（已有，补门户场景） | CI E2E |
| S-05/S-06 | IDOR/MIME | P0 | 🔧 | 扩 `cross-tenant-idor` + `knowledge-boundary` | Phase 1 |
| CC-01~05 | 并发 | P1 | ✅ | `concurrency-run.ps1` 等 | 可选纳入 |
| F-01~F-04 | 中间件故障 | P1 | ✅ | `fault-injection.ps1` | Phase 2 纳入 |
| F-05 | LLM 超时/失败 | P1 | 🆕 | `llm-fault-smoke.ps1`（无效 model endpoint） | Phase 2 |
| F-06 | Server 容器重启 | P2 | 🔧 | `prod-compose-smoke.ps1` restart 段 | Phase 3 |

### 2.18 前端 E2E

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| FE-01~FE-06 | 主流程 | P1 | ✅ | 现有 e2e | CI |
| FE-07 | Token 过期跳转 | P1 | 🎭 | `auth-expiry.spec.ts` | Phase 1 |
| FE-08 | API 失败 Toast | P2 | 🎭 | `error-states.spec.ts` | Phase 2 |
| FE-09 | 重复点击提交 | P1 | 🎭 | `double-submit.spec.ts` | Phase 1 |
| FE-10 | 响应式布局 | P3 | 🎭 | `responsive.spec.ts` | — |

### 2.19 生产 / 依赖 / 静态

| 清单 ID | 用例 | 优先级 | 动作 | 目标资产 | 纳入门禁 |
|---------|------|--------|------|----------|----------|
| PR-01 | prod compose 全栈 | P1 | ✅ | `prod-compose-smoke.ps1` | `-IncludeProdCompose` |
| PR-02~PR-07 | 配置审计 | P0~P2 | ✅/⚠️ | 脚本 + 人工 | 发布前 |
| DEP-01~02 | 依赖扫描 | P1 | ✅ | `dependency-audit.ps1` | Phase 0 |
| ST-01 | 关键字扫描 | P1 | 🆕 | `scan-code-smells.ps1` | Phase 2 |
| ST-02 | 硬编码密钥 | P0 | ✅ | `scan-hardcoded-secrets.ps1` | 已纳入 |
| ST-03 | prod debug 日志 | P1 | ☕ | `ProdSecurityValidatorTest`（扩） | Phase 2 |

---

## 3. Java 单测补全（按模块）

| 模块 | 现状 | 建议新增测试类 | 优先级 |
|------|------|----------------|--------|
| `novaflow-ai-engine` | ❌ 零单测 | `ChatAgentExecutorTest`（mock LLM）、`ToolCallLoopTest` | P0 |
| `novaflow-ai-rag` | ❌ 零单测 | `DocumentChunkerTest`、`QdrantVectorServiceTest`（Testcontainers） | P0 |
| `novaflow-chat` | ❌ 零单测 | `ConversationServiceTest`、`ConversationRetentionServiceTest` | P1 |
| `novaflow-knowledge` | ❌ 零单测 | `DocumentServiceTest`、`KnowledgeBaseQuotaTest` | P1 |
| `novaflow-prompt` | ❌ 零单测 | `PromptTemplateServiceTest`、`PromptVersionRollbackTest` | P2 |
| `novaflow-model` | 仅配额 | `ModelProviderServiceTest`、`CryptoServiceRoundTripTest` | P1 |
| `novaflow-application` | ❌ 零单测 | `PortalServiceTest`、`ApplicationPublishTest` | P2 |
| `novaflow-dashboard` | ❌ 零单测 | `DashboardServiceTest`、`FavoriteServiceTest` | P2 |
| `novaflow-monitor` | ❌ 零单测 | `MonitorServiceHealthAggregationTest` | P3 |
| `novaflow-observability` | ❌ 零单测 | `TraceServicePaginationTest` | P3 |
| `novaflow-tenant` | ❌ 零单测 | 实体/Mapper 层可合并到 `novaflow-user` 集成测 | P3 |
| `novaflow-workflow` | 2 类 | `WorkflowGraphValidatorTest`、`WorkflowExecutionServiceTest` | P1 |
| `novaflow-tool` | 1 类 | `HttpToolExecutorTest`、`UrlSafetyIntegrationTest` | P1 |
| `novaflow-server` | 集成丰富 | `WorkflowAgentNodeLocalIntegrationTest`、`KnowledgeRagLocalIntegrationTest` | P1 |

**集成测试放置约定：** 需要真实 MySQL/Redis 的放 `novaflow-server/.../integration/`，加 `@Tag("local")`，与现有 `AbstractLocalIntegrationTest` 对齐。

---

## 4. 分阶段执行计划

### Phase 0 — 零代码：纳入门禁 + 同步清单（0.5 天）✅ 已完成

在 `run-pre-release-gates.ps1` 追加：

```powershell
Invoke-GateStep 'RBAC API acceptance' (Join-Path $PSScriptRoot 'rbac-api-acceptance.ps1')
Invoke-GateStep 'Coverage gap smoke' (Join-Path $PSScriptRoot 'coverage-gap-smoke.ps1')
Invoke-GateStep 'Chat RAG smoke' (Join-Path $PSScriptRoot 'chat-rag-smoke.ps1')
Invoke-GateStep 'Dependency audit' (Join-Path $PSScriptRoot 'dependency-audit.ps1')
# 可选 -SkipFaultInjection
Invoke-GateStep 'Fault injection' (Join-Path $PSScriptRoot 'fault-injection.ps1')
```

同步 `TEST-CHECKLIST.md`：A-02、A-08、A-09、K-06、U-02、AP-05、D-01、D-03、B-06、DEP-01、DEP-02 → ✅。

### Phase 1 — 高优 P1 脚本（3~5 天）✅ 已完成

| 顺序 | 新建脚本 | 覆盖清单 |
|------|----------|----------|
| 1 | `model-lifecycle-smoke.ps1` | M-01, M-03, M-05, M-06 |
| 2 | `prompt-lifecycle-smoke.ps1` | P-01~P-03 |
| 3 | `knowledge-boundary-smoke.ps1` + `knowledge-document-lifecycle-smoke.ps1` | K-03~K-05 |
| 4 | `workflow-agent-node-smoke.ps1` | W-04 |
| 5 | `agent-tool-execution-smoke.ps1` | T-07 |
| 6 | `chat-history-smoke.ps1` | C-01 |
| 7 | `billing-manage-smoke.ps1` + `billing-token-accuracy-smoke.ps1` | B-02, B-04 |
| 8 | `api-boundary-smoke.ps1` | API-01~04 |
| 9 | `auth-lifecycle-smoke.ps1` + `auth-lock-smoke.ps1` | A-01, A-05, A-06 |
| 10 | E2E: `auth-expiry.spec.ts`, `double-submit.spec.ts` | FE-07, FE-09 |

以上脚本已纳入 `run-pre-release-gates.ps1`。

### Phase 2 — 深度与故障（3~4 天）✅ 已完成

- 扩展 `fault-injection.ps1`：K-07/K-08、R-01/R-02、A-12
- `observability-smoke.ps1`、`global-search-smoke.ps1`
- `dashboard-extended-smoke.ps1`、`org-extended-smoke.ps1`
- `billing-alert-smoke.ps1`、`llm-fault-smoke.ps1`、`agent-concurrent-update-smoke.ps1`
- `scan-code-smells.ps1`
- Java：`WorkflowAgentNodeLocalIntegrationTest`、`KnowledgeRagLocalIntegrationTest`、`ProdLoggingConfigTest`
- E2E：`error-states.spec.ts`

以上已纳入 `run-pre-release-gates.ps1`（故障注入为可选 `-SkipFaultInjection`）。

### Phase 3 — P2/P3 与可选项 ✅ 已完成

- 限流类：`auth-rate-limit-smoke.ps1`（A-07）、`open-api-rate-limit-smoke.ps1`（O-09）
- 配额类：`knowledge-quota-smoke.ps1`（K-09）
- 导出/同步：`billing-export-smoke.ps1`（B-05）、`model-sync-smoke.ps1`（M-04）
- 工作流/对话：`workflow-invalid-graph-smoke.ps1`（W-06）、`workflow-dashboard-runtime-smoke.ps1`（W-07）、`chat-redis-memory-smoke.ps1`（C-02）
- Prompt 绑定：`prompt-agent-bind-smoke.ps1`（P-04）
- API 边界扩展：`api-boundary-smoke.ps1`（API-05/06）
- 可观测/镜像：`observability-otlp-smoke.ps1`（OB-03）、`docker-image-audit.ps1`（DEP-03）
- 生产恢复：`prod-compose-smoke.ps1` F-06 容器重启段
- Java：`WorkflowPublishValidatorTest`、`ConversationRetentionServiceTest`
- E2E：`responsive.spec.ts`（FE-10）；A-10 已由 `auth-expiry.spec.ts` 覆盖

以上已纳入 `run-pre-release-gates.ps1`（`prod-compose-smoke` 仍为 `-IncludeProdCompose` 可选）。

### Phase 4 — 安全收尾与生产配置 ✅ 已完成

- **S-02**：`xss.spec.ts` 扩展门户 `assistant-content` 场景
- **S-05**：`cross-tenant-idor.ps1` 扩展 prompt / tool / token logs
- **S-06**：`knowledge-boundary-smoke.ps1` 路径遍历文件名探测
- **OB-04**：`observability-langfuse-smoke.ps1`
- **PR-06**：`cors-prod-audit.ps1`
- **CC-02**：`AgentPublishService.lockAgentForUpdate` 已行锁；清单同步为 ✅

---

## 5. 新建脚本模板（复制起点）

所有新脚本遵循现有约定：

```powershell
#requires -Version 7.0
# NovaFlow AI — <模块>验收（<清单 ID>）
# 用法: pwsh test/<name>-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot '<name>-smoke.log'
$outFile = Join-Path $PSScriptRoot '<name>-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== <name>-smoke ===' $logFile
$token = Get-NovaLoginToken

# ... 用例 ...

Write-NovaGateResult -ScriptName '<name>-smoke' -Passed $allPass -Details @{ checks = @($results) } -OutFile $outFile | Out-Null
if (-not $allPass) { exit 1 }
```

公共 helper 见 `NovaFlow-TestCommon.ps1`：`Get-NovaLoginToken`、`Invoke-NovaApi`、`New-NovaKnowledgeBase`、`New-NovaAgent` 等。

---

## 6. 验收标准

| 阶段 | 完成定义 |
|------|----------|
| Phase 0 | `run-pre-release-gates.ps1` 全绿；清单同步项 ≥ 10 |
| Phase 1 | P1 缺口 ≤ 15；Prompt/Model/Knowledge 边界有专用脚本 |
| Phase 2 | 故障注入覆盖 K/R；W-04/W-05 有自动化 |
| Phase 3 | P2 缺口 ≤ 20；`mvn test` + E2E + 全门禁可在 CI 一键跑 |

---

## 7. 建议下一步（立即可做）

1. **执行 Phase 0**：改 `run-pre-release-gates.ps1` + 更新 `TEST-CHECKLIST.md` 状态列  
2. **从 `prompt-lifecycle-smoke.ps1` 开始**：全模块零覆盖，API 简单、无外部依赖  
3. **并行写 `model-lifecycle-smoke.ps1`**：补齐模型中心最大盲区  

如需自动生成 Phase 1 第一个脚本，指定模块名即可（推荐 **Prompt** 或 **Model**）。
