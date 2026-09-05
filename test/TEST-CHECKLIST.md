# NovaFlow AI — TEST CHECKLIST

> 关联：`PROJECT-MAP.md` · QA 任务文档第 3 阶段  
> 状态：**第 4 阶段完成**（DB + Redis + MQ 审计）  
> 日期：2026-09-02 · 更新：2026-09-02 17:40

---

## 测试环境要求

| 项 | 要求 |
|----|------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node | 20+ |
| Docker | MySQL 8、Redis 7、MinIO、Qdrant |
| 后端 | `http://localhost:8080`，profile `dev` 或独立测试 profile |
| 前端 | `http://localhost:3000` |
| 测试账号 | 需准备 super_admin、tenant_admin、developer、user 四类角色 |
| Open API | 已发布 Agent 的 `nf_live_` Key 与 `nf_embed_` Token |

---

## 1. Authentication

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| A-01 | 注册：合法邮箱/用户名 | P1 | ✅ `auth-lifecycle-smoke.ps1` |
| A-02 | 注册：重复邮箱/用户名 | P1 | ✅ `coverage-gap-smoke.ps1` |
| A-03 | 注册：prod 下 `registration-enabled=false` 拒绝 | P0 | ✅ `registration-disabled-gate.ps1` |
| A-04 | 登录：正确凭证 | P1 | ✅ |
| A-05 | 登录：错误密码 | P1 | ✅ `auth-lifecycle-smoke.ps1` |
| A-06 | 登录：连续失败触发锁定（阈值 5 / 15min） | P1 | ✅ `auth-lock-smoke.ps1` |
| A-07 | 登录限流（120/min） | P2 | ✅ `auth-rate-limit-smoke.ps1` |
| A-08 | 登出后 Token 失效 | P1 | ✅ `coverage-gap-smoke.ps1` |
| A-09 | `/auth/me` 未登录 401 | P1 | ✅ `coverage-gap-smoke.ps1` |
| A-10 | Token 过期（86400s）后拒绝 | P2 | ✅ `auth-expiry.spec.ts`（401 跳转） |
| A-11 | Token 篡改 / 伪造 | P0 | ✅ |
| A-12 | Redis 重启后登录态（Sa-Token 持久化） | P1 | ✅ `fault-injection.ps1` |

---

## 2. Authorization（RBAC + 多租户）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| Z-01 | `user` 无法访问 `/platform`、`/audit` | P0 | ✅ |
| Z-02 | `developer` 无法 `tenant:manage` / `member:manage` | P1 | ✅ `rbac-api-acceptance.ps1` |
| Z-03 | `user` 仅 `portal` + `agent:chat` | P0 | ✅ |
| Z-04 | 租户 A 用户无法读/改租户 B 的 Agent | P0 | ✅ `rbac-api-acceptance.ps1` / `cross-tenant-idor.ps1` |
| Z-05 | 租户 A 用户无法读/改租户 B 的知识库/工作流 | P0 | ✅ `rbac-api-acceptance.ps1` / `cross-tenant-idor.ps1` |
| Z-06 | 租户 A 用户无法读/改租户 B 的应用 | P0 | ✅ `rbac-api-acceptance.ps1` / `cross-tenant-idor.ps1` |
| Z-07 | 无 `@SaCheckPermission` 的 API 清点与加固 | P1 | ✅ `scan-api-permissions.ps1` |
| Z-08 | 前端路由守卫 vs 后端权限一致性 | P1 | ✅ `route-guard.spec.ts` |
| Z-09 | `super_admin` 平台租户 CRUD | P1 | ✅ `rbac-api-acceptance.ps1` |
| Z-10 | Portal `portal:access` 无权限用户被拒 | P1 | ✅ `rbac-api-acceptance.ps1` + `PortalAccessLocalIntegrationTest` |

---

## 3. User / Org

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| U-01 | 查看/修改租户信息 | P2 | ✅ `org-extended-smoke.ps1` |
| U-02 | 工作空间 CRUD | P2 | ✅ `coverage-gap-smoke.ps1` |
| U-03 | 成员邀请 / 改角色 / 删除 | P1 | ✅ `member-management-smoke.ps1` |
| U-04 | 成员配额 `max_members` 超限 | P1 | ✅ `member-management-smoke.ps1` |
| U-05 | 通知列表、已读、未读数 | P2 | ✅ `org-extended-smoke.ps1` |
| U-06 | 审计日志仅 `audit:view` 可见 | P1 | ✅ `audit-access-smoke.ps1` |

---

## 4. Agent（控制台）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| AG-01 | Agent CRUD 闭环 | P1 | ✅ `agent-lifecycle-smoke.ps1` |
| AG-02 | 发布 / 下架 | P1 | ✅ `agent-lifecycle-smoke.ps1` |
| AG-03 | rotate-api-key / rotate-embed-token | P1 | ✅ `agent-lifecycle-smoke.ps1` |
| AG-04 | 调试对话（同步） | P1 | ✅ `agent-debug-smoke.ps1` |
| AG-05 | 调试对话 SSE 流式 | P1 | ✅ `agent-debug-smoke.ps1` |
| AG-06 | 调试附件上传 | P1 | ✅ `agent-debug-smoke.ps1` |
| AG-07 | 会话列表 / 消息历史 | P1 | ✅ `agent-debug-smoke.ps1` |
| AG-08 | 删除调试会话 | P2 | ✅ `agent-debug-smoke.ps1` |
| AG-09 | 绑定知识库 / 工具 / Skill | P1 | ✅ `agent-bindings-smoke.ps1` |
| AG-10 | 未发布 Agent Open API 拒绝 | P0 | ✅ `open-api-acceptance.ps1` + `UnpublishedAgentOpenApiLocalIntegrationTest` |

---

## 5. Open API / Embed（安全重点）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| O-01 | API Key `welcome` / `chat` / `stream` | P0 | ✅ `open-api-acceptance.ps1` |
| O-02 | API Key 无 `X-Caller-Id` 拒绝 chat | P0 | ✅ `open-api-acceptance.ps1` |
| O-03 | API Key callerId 仅看自己的 conversations | P0 | ✅ `open-api-acceptance.ps1` |
| O-04 | API Key 无法读其他 callerId 消息 | P0 | ✅ `open-api-acceptance.ps1` |
| O-05 | Embed Token 可 welcome/chat | P0 | ✅ `open-api-acceptance.ps1` |
| O-06 | Embed Token **不可** list conversations/messages | P0 | ✅ `open-api-acceptance.ps1` |
| O-07 | 错误 / 过期 Token | P0 | ✅ `open-api-acceptance.ps1` |
| O-08 | 其他 Agent 的 Key 访问本 Agent | P0 | ✅ `open-api-acceptance.ps1` |
| O-09 | Open API 限流 60/min、IP 30/min | P2 | ✅ `open-api-rate-limit-smoke.ps1` |
| O-10 | `/embed/agents/:id` 前端 + Token 流程 | P1 | ✅ `embed.spec.ts` |

---

## 6. Application & Portal

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| AP-01 | Application CRUD | P1 | ✅ `application-lifecycle-smoke.ps1` |
| AP-02 | 发布 / 下架 | P1 | ✅ `application-lifecycle-smoke.ps1` |
| AP-03 | Portal 列表仅已发布应用 | P1 | ✅ |
| AP-04 | Portal 对话与 Studio 权限隔离 | P1 | ✅ `portal-studio-isolation-smoke.ps1` |
| AP-05 | `user` 默认首页 `/portal` | P2 | ✅ `coverage-gap-smoke.ps1` |

---

## 7. Workflow

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| W-01 | 工作流 CRUD | P1 | ✅ `workflow-lifecycle-smoke.ps1` |
| W-02 | 发布 | P1 | ✅ `workflow-lifecycle-smoke.ps1` |
| W-03 | `run` 同步执行 | P1 | ✅ `workflow-lifecycle-smoke.ps1` |
| W-04 | Agent 节点调用已发布 Agent | P1 | ✅ `workflow-agent-node-smoke.ps1` |
| W-05 | 重复 run 幂等 / 副作用 | P1 | ✅ `workflow-idempotency-smoke.ps1` |
| W-06 | 非法图结构（无起点、环） | P2 | ✅ `workflow-invalid-graph-smoke.ps1` + `WorkflowPublishValidatorTest` |
| W-07 | Dashboard workflow runtime | P2 | ✅ `workflow-dashboard-runtime-smoke.ps1` |

---

## 8. Knowledge & RAG

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| K-01 | 知识库 CRUD | P1 | ✅ `knowledge-base-smoke.ps1` |
| K-02 | 文档上传（合法 PDF/TXT 等） | P1 | ✅ `knowledge-base-smoke.ps1` |
| K-03 | 超大文件（>50MB）拒绝 | P1 | ✅ `knowledge-boundary-smoke.ps1` |
| K-04 | 非法文件类型 | P1 | ✅ `knowledge-boundary-smoke.ps1` |
| K-05 | reprocess / 删除文档 | P1 | ✅ `knowledge-document-lifecycle-smoke.ps1` |
| K-06 | `retrieve` API 检索 | P1 | ✅ `chat-rag-smoke.ps1` |
| K-07 | MinIO 失败时 DB 一致性 | P1 | ✅ `fault-injection.ps1` |
| K-08 | Qdrant 不可用时的降级/错误 | P1 | ✅ `fault-injection.ps1` |
| K-09 | `max_knowledge` 配额 | P2 | ✅ `knowledge-quota-smoke.ps1` |

---

## 9. Model

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| M-01 | Provider CRUD | P1 | ✅ `model-lifecycle-smoke.ps1` |
| M-02 | API Key 加密存储（crypto-key） | P0 | ✅ `model-api-key-encryption.ps1` |
| M-03 | 连通性 test | P1 | ✅ `model-lifecycle-smoke.ps1` |
| M-04 | sync 模型列表 | P2 | ✅ `model-sync-smoke.ps1` |
| M-05 | Model config 默认项 | P1 | ✅ `model-lifecycle-smoke.ps1` |
| M-06 | embedding-options | P1 | ✅ `model-lifecycle-smoke.ps1` |

---

## 10. Tool / MCP / Skill

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| T-01 | HTTP 工具 CRUD + test | P1 | ✅ `http-tool-crud-smoke.ps1` |
| T-02 | HTTP 工具 SSRF（内网 URL） | P0 | ✅ `http-tool-ssrf.ps1` |
| T-03 | MCP Server CRUD | P1 | ✅ `mcp-server-smoke.ps1` |
| T-04 | MCP 命令白名单外拒绝 | P0 | ✅ `mcp-command-whitelist.ps1` |
| T-05 | MCP connect / sync-tools | P1 | ✅ `mcp-server-smoke.ps1`（connect） |
| T-06 | Skill 上传 | P1 | ✅ `skill-upload-smoke.ps1` |
| T-07 | Agent 绑定工具执行 | P1 | ✅ `agent-tool-execution-smoke.ps1` |

---

## 11. Prompt

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| P-01 | 模板 CRUD | P2 | ✅ `prompt-lifecycle-smoke.ps1` |
| P-02 | 版本与 rollback | P2 | ✅ `prompt-lifecycle-smoke.ps1` |
| P-03 | 在线 test | P2 | ✅ `prompt-lifecycle-smoke.ps1` |
| P-04 | Agent 引用 Prompt 模板 | P2 | ✅ `prompt-agent-bind-smoke.ps1` |

---

## 12. Chat / Conversation

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| C-01 | 消息持久化与分页 | P1 | ✅ `chat-history-smoke.ps1` |
| C-02 | Redis 窗口记忆 vs DB 历史 | P1 | ✅ `chat-redis-memory-smoke.ps1` |
| C-03 | 保留策略 cron 删除（90 天） | P2 | ✅ `ConversationRetentionServiceTest` |
| C-04 | conversationKey 隔离 | P0 | ✅ `conversation-key-isolation.ps1` |

---

## 13. Billing & Monitor

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| B-01 | billing overview / quota | P1 | ✅ `billing-overview-smoke.ps1` |
| B-02 | 配额修改 billing:manage | P1 | ✅ `billing-manage-smoke.ps1` |
| B-03 | 预警配置与触发 | P2 | ✅ `billing-alert-smoke.ps1` |
| B-04 | token_usage 记录准确性 | P1 | ✅ `billing-token-accuracy-smoke.ps1` |
| B-05 | export 账单/日志 | P2 | ✅ `billing-export-smoke.ps1` |
| B-06 | monitor overview | P2 | ✅ `coverage-gap-smoke.ps1` |
| B-07 | 基础设施健康（MySQL/Redis/MinIO/Qdrant） | P1 | ✅ |

---

## 14. Observability / Trace

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| OB-01 | trace spans 分页 | P2 | ✅ `observability-smoke.ps1` |
| OB-02 | span 详情与 nodes | P2 | ✅ `observability-smoke.ps1` |
| OB-03 | OTLP 开启时 Span 上报 | P2 | ✅ `observability-otlp-smoke.ps1`（默认关闭时验配置） |
| OB-04 | Langfuse 集成（若配置） | P3 | ✅ `observability-langfuse-smoke.ps1`（live 需 Key） |

---

## 15. Dashboard & Search

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| D-01 | dashboard overview | P2 | ✅ `coverage-gap-smoke.ps1` |
| D-02 | recent-items / favorites | P2 | ✅ `dashboard-extended-smoke.ps1` |
| D-03 | favorites toggle 幂等 | P2 | ✅ `coverage-gap-smoke.ps1` |
| D-04 | 全局搜索 search:global | P2 | ✅ `global-search-smoke.ps1` |

---

## 16. API 通用边界（每个写 API 抽样）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| API-01 | null / 空字符串 body | P1 | ✅ `api-boundary-smoke.ps1` |
| API-02 | 超长字符串 | P1 | ✅ `api-boundary-smoke.ps1` |
| API-03 | 负数 / 0 / 超大 ID | P1 | ✅ `api-boundary-smoke.ps1` |
| API-04 | 缺少必填字段 | P1 | ✅ `api-boundary-smoke.ps1` |
| API-05 | 特殊字符 / Unicode / Emoji | P2 | ✅ `api-boundary-smoke.ps1` |
| API-06 | 错误 Content-Type | P2 | ✅ `api-boundary-smoke.ps1` |

---

## 17. Redis 专项

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| R-01 | Redis 不可用：登录/限流行为 | P1 | ✅ `fault-injection.ps1` |
| R-02 | Redis 重启：会话恢复 | P1 | ✅ `fault-injection.ps1` |
| R-03 | 对话记忆 TTL / 一致性 | P2 | ✅（TTL 7d 实测） |

---

## 18. 数据库专项

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| DB-01 | 软删除 `is_deleted` 不泄露 | P1 | ✅（抽样） |
| DB-02 | 唯一约束（email、tenant_user） | P1 | ✅ |
| DB-03 | 并发更新同一 Agent 配置 | P1 | ✅ `agent-concurrent-update-smoke.ps1` |
| DB-04 | 分页边界 page=0、超大 pageSize | P2 | ✅ |
| DB-05 | Flyway prod validate-on-migrate | P1 | ✅（配置审查） |

---

## 19. 安全专项

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| S-01 | SQL Injection（搜索、过滤参数） | P0 | ✅ |
| S-02 | XSS（Agent 输出、Markdown、门户） | P0 | ✅ `xss.spec.ts`（about + portal） |
| S-03 | CSRF（CORS 配置） | P1 | ✅ |
| S-04 | SSRF（HTTP 工具、MCP） | P0 | ✅ |
| S-05 | IDOR（全模块资源 ID） | P0 | ✅ `cross-tenant-idor.ps1`（agent/app/wf/kb/prompt/tool） |
| S-06 | 路径遍历（文件下载/上传） | P0 | ✅ `knowledge-boundary-smoke.ps1`（S-06 段） |
| S-07 | 敏感信息泄露（日志、错误响应） | P1 | ✅ |
| S-08 | Actuator 暴露面（prod 仅 health） | P1 | ✅ |
| S-09 | Swagger prod 关闭 | P1 | ✅ |
| S-10 | 硬编码密钥 / localhost 残留扫描 | P1 | ✅ |

---

## 20. 并发（第 6 次专项）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| CC-01 | 10 并发 Open API chat | P1 | ✅ |
| CC-02 | 50 并发重复 publish | P1 | ✅ `publish-concurrency-gate.ps1`（`forUpdate` 行锁） |
| CC-03 | 100 并发 favorite toggle | P2 | ✅ |
| CC-04 | 并发工作流 run | P1 | ✅ |
| CC-05 | 并发文档上传 | P2 | ✅ |

---

## 21. 容灾（第 4/6 次专项）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| F-01 | MySQL 不可用 → 健康检查失败、无脏写 | P1 | ✅ `fault-injection.ps1` |
| F-02 | Redis 不可用 | P1 | ✅ `fault-injection.ps1` |
| F-03 | MinIO 不可用 | P1 | ✅ `fault-injection.ps1`（需 Docker） |
| F-04 | Qdrant 不可用 | P1 | ✅ `fault-injection.ps1`（需 Docker） |
| F-05 | LLM API 超时/失败 | P1 | ✅ `llm-fault-smoke.ps1` |
| F-06 | Server 容器重启恢复 | P2 | ✅ `prod-compose-smoke.ps1`（F-06 段） |

---

## 22. 前端 E2E（第 3 次专项）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| FE-01 | 登录 / 注册 / 登出 | P1 | ✅（登录+注册 smoke） |
| FE-02 | 各 Studio 页面 smoke（已有 e2e） | P1 | ✅ |
| FE-03 | Agent 创建与调试 UI | P1 | ✅ |
| FE-04 | 工作流编辑器保存与运行 | P1 | ✅ |
| FE-05 | Portal 用户流程 | P1 | ✅ `portal.spec.ts` |
| FE-06 | Embed 页面 | P1 | ✅ `embed.spec.ts` |
| FE-07 | Token 过期跳转登录 | P1 | ✅ `auth-expiry.spec.ts` |
| FE-08 | API 失败 Toast / Empty / Loading | P2 | ✅ `error-states.spec.ts` |
| FE-09 | 重复点击提交 | P1 | ✅ `double-submit.spec.ts` |
| FE-10 | 响应式布局抽样 | P3 | ✅ `responsive.spec.ts` |

---

## 23. 生产部署审计（第 5 次专项）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| PR-01 | `deploy/docker-compose.prod.yml` 全栈启动 | P1 | ✅ `prod-compose-smoke.ps1`（`-IncludeProdCompose`） |
| PR-02 | `.env.prod.example` 弱密码检查 | P0 | ✅ |
| PR-03 | `NOVAFLOW_CRYPTO_KEY` 强度 | P0 | ✅ |
| PR-04 | Nginx HTTPS / 反代配置 | P1 | ✅（TLS 需 LB/443） |
| PR-05 | `NOVAFLOW_REGISTRATION_ENABLED=false` | P0 | ✅ |
| PR-06 | CORS 仅生产域名 | P1 | ✅ `cors-prod-audit.ps1` |
| PR-07 | 健康检查与 restart 策略 | P2 | ✅ |

---

## 24. 依赖安全（第 5 次专项）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| DEP-01 | Maven `dependency-check` 或 OWASP 扫描 | P1 | ✅ `dependency-audit.ps1` |
| DEP-02 | npm audit（novaflow-web） | P1 | ✅ `dependency-audit.ps1` |
| DEP-03 | Docker 基础镜像版本 | P2 | ✅ `docker-image-audit.ps1` |

---

## 25. 静态代码审计（第 4 次专项）

扫描项：`TODO`、`FIXME`、`localhost`、`password`、`secret`、`console.log`、`System.out.println`、mock 数据残留

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| ST-01 | 全项目关键字扫描 | P1 | ✅ `scan-code-smells.ps1` |
| ST-02 | 硬编码凭证清零 | P0 | ✅ `scan-hardcoded-secrets.ps1` |
| ST-03 | Debug 日志级别 prod | P1 | ✅ `ProdLoggingConfigTest` |

---

## 26. 回归与门禁（第 8 次）

| # | 用例 | 优先级 | 状态 |
|---|------|--------|------|
| RG-01 | `mvn test` 全绿 | P0 | ✅ |
| RG-02 | `npm run build` 成功 | P0 | ✅ |
| RG-03 | Playwright E2E 全绿 | P1 | ✅ |
| RG-04 | P0 = 0 | P0 | ✅ |
| RG-05 | P1 = 0 | P0 | ✅ |
| RG-06 | 核心业务流程 PASS | P0 | ✅ |

---

## 统计模板（测试过程中更新）

| 级别 | 发现 | 已修复 | 遗留 |
|------|------|--------|------|
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 0 |
| P2 | 2 | 2 | 0 |
| P3 | 7 | 3 | 4 |

**上线结论：** `READY FOR PRODUCTION (with deployment checklist)` — 见 `REGRESSION-GATE-REPORT.md`

---

*执行进度在 `QA-REPORT.md` 中记录具体 Bug。*
