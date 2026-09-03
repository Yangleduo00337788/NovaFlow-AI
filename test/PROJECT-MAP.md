# NovaFlow AI — PROJECT MAP（第一阶段侦察）

> 生成阶段：生产上线前 QA · 第 1 次 · 项目侦察  
> 日期：2026-09-02  
> 状态：**侦察完成，未开始大规模测试**

---

## 1. 项目概况

| 项 | 说明 |
|----|------|
| 产品名 | NovaFlow AI — 企业级 AI Agent 平台 |
| 版本 | 1.0.1 |
| 定位 | 单 Web 应用 + RBAC 功能分区；模块化单体（Modular Monolith） |
| 仓库结构 | Maven 多模块后端 + `novaflow-web` 独立前端 |
| 默认端口 | 后端 `8080` · 前端 `3000` |

---

## 2. 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行时 |
| Spring Boot | 3.3.5 | Web / JDBC / Redis / Actuator |
| Sa-Token | 1.39.0 | 登录态、权限注解 |
| MyBatis-Flex | 1.9.9 | ORM |
| Flyway | — | 数据库迁移（`V1`–`V26`） |
| LangChain4j | 0.36.2 | Agent / LLM 抽象 |
| LiteFlow | 2.12.4 | 工作流规则引擎 |
| SpringDoc | 2.6.0 | OpenAPI（dev 开启，prod 关闭） |
| Testcontainers | 1.20.6 | 集成测试 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| TypeScript | 6.0 | 类型 |
| Vite | 8.2 | 构建 |
| Ant Design Vue | 4.2 | 组件库 |
| Vue Flow | 1.48 | 工作流可视化 |
| Pinia | 4.0 | 状态管理 |
| Playwright | 1.51 | E2E 测试 |
| DOMPurify | 3.4 | XSS 防护（Markdown 渲染） |

### 基础设施

| 组件 | 用途 | 默认端口 |
|------|------|----------|
| MySQL 8 | 业务数据 | 3306 |
| Redis 7 | Sa-Token 会话、对话记忆、限流、登录锁定 | 6379 |
| MinIO | 知识库文档、Skill 包对象存储 | 9000 / 9001 |
| Qdrant | 向量检索（gRPC） | 6334（REST 6333） |
| Nginx | 生产静态资源 + 反向代理 | deploy/nginx.conf |

### 未使用

| 组件 | 状态 |
|------|------|
| Kafka / RabbitMQ / RocketMQ | **未使用** |
| 原生 WebSocket | **未使用**（流式输出用 SSE） |

---

## 3. 项目模块

```
NovaFlow-AI/
├── novaflow-server/          # Spring Boot 启动入口、Flyway、全局搜索
├── novaflow-web/             # Vue 3 前端
├── novaflow-common/          # 公共工具、异常、WebMvc、URL 安全校验
├── novaflow-security/        # Sa-Token 配置、限流、登录锁定、加密
├── novaflow-user/            # 认证、用户、角色、组织、审计、通知、平台超管
├── novaflow-tenant/          # 租户 / 工作空间实体
├── novaflow-application/     # 应用 CRUD、发布、Portal API
├── novaflow-agent/           # Agent CRUD、调试、发布、Open API、Embed
├── novaflow-chat/            # 会话与消息持久化、保留策略定时任务
├── novaflow-workflow/        # 工作流编排与执行
├── novaflow-knowledge/       # 知识库管理
├── novaflow-model/           # 模型提供商、配置、Token 用量
├── novaflow-tool/            # HTTP 工具、MCP Server、Skill
├── novaflow-prompt/          # Prompt 模板
├── novaflow-dashboard/       # 工作台聚合
├── novaflow-monitor/         # 运行监控、基础设施健康检查
├── novaflow-observability/   # 链路追踪（基于 token_usage / workflow 聚合）
├── novaflow-billing/         # 账单、配额、预警
├── novaflow-ai-engine/       # LangChain4j Agent 运行时、Redis 对话记忆
├── novaflow-ai-rag/          # RAG 管道、检索 API
├── novaflow-ai-workflow-engine/  # LiteFlow 封装
├── docs/                     # PRD、架构、数据库设计
├── deploy/                   # 生产 Docker Compose + Nginx
├── docker-compose.yml        # 本地全量基础设施
└── test/                     # QA 文档与报告（本目录）
```

---

## 4. 产品形态与角色

| 功能区 | 路由 | 角色 | 说明 |
|--------|------|------|------|
| 总控 | `/platform`、`/audit` | `super_admin` | 租户管理、审计 |
| Studio | `/dashboard`、Agent/工作流/知识库等 | `tenant_admin`、`developer` | 开发与治理 |
| 应用门户 | `/portal`、`/portal/apps/:id` | `user` | 使用已发布应用 |
| 网页嵌入 | `/embed/agents/:id` | 公开 + `nf_embed_` Token | 需 `X-Caller-Id` |
| Open API | `/api/v1/open/**` | `nf_live_` API Key | 服务端集成 |

**系统角色（种子数据 V11/V24/V25）：** `super_admin` · `tenant_admin` · `developer` · `user`

**权限码示例：** `agent:*`、`workflow:*`、`knowledge:*`、`model:config`、`application:manage`、`billing:*`、`monitor:view`、`trace:view`、`tenant:manage`、`member:manage`、`platform:manage`、`audit:view`、`search:global`、`portal:access`

---

## 5. 核心业务流程

### 5.1 Agent 对话（Studio 调试 / Portal / Open API / Embed）

1. 鉴权：Sa-Token（控制台）或 API Key / Embed Token（Open API）
2. 加载 Agent 配置（模型、Prompt、工具、知识库、Skill）
3. 可选 RAG：Qdrant 向量检索 Top-K
4. LangChain4j 流式生成；工具调用循环（HTTP / MCP，最多多轮）
5. SSE 推送 token 事件（`/debug/chat/stream`、`/open/agents/{id}/chat/stream`）
6. 持久化 `conversation` / `conversation_message`；Redis 窗口记忆
7. 记录 `token_usage`；可选 OTLP / Langfuse Span

### 5.2 工作流执行

1. 可视化编排（`workflow` / `workflow_node` / `workflow_edge`）
2. LiteFlow 动态 EL 注册与执行
3. 支持 Agent 节点调用已发布 Agent
4. 记录 `workflow_execution` / `workflow_node_log`

### 5.3 知识库 RAG

1. 创建知识库 → 上传文档（MinIO）→ 解析分块 → Embedding → Qdrant 写入
2. Agent 绑定时按 `retrieval_config` 检索增强

### 5.4 多租户与 RBAC

1. 用户注册/登录 → `tenant_member` 绑定租户与角色
2. 业务数据带 `tenant_id` 隔离
3. `@SaCheckPermission` + 前端路由权限码双重控制

### 5.5 应用发布与门户

1. 创建 Application → 绑定 Agent → 发布（`publish_status`）
2. Portal 用户通过 `portal:access` 访问已发布应用
3. Open API / Embed 要求 Agent 已发布

---

## 6. API 清单（按模块）

**公共路径（无需 Sa-Token 登录）：**

- `/api/v1/auth/login`、`/api/v1/auth/register`
- `/api/v1/health`
- `/api/v1/open/**`
- `/actuator/**`、`/swagger-ui/**`、`/v3/api-docs/**`（prod 关闭文档）

### Auth — `/api/v1/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | 注册 |
| POST | `/login` | 登录 |
| GET | `/me` | 当前用户 |
| POST | `/logout` | 登出 |

### Health — `/api/v1`

| GET | `/health` | 健康检查 |

### Agent（控制台）— `/api/v1/agents`

| 方法 | 路径 | 权限要点 |
|------|------|----------|
| GET | `/` | agent:create/edit |
| GET | `/{id}` | agent:create/edit |
| POST | `/` | agent:create |
| PUT | `/{id}` | agent:edit |
| DELETE | `/{id}` | agent:delete |
| GET/POST | `/{id}/publish` 等 | agent:publish |
| POST | `/{id}/rotate-api-key`、`rotate-embed-token` | agent:publish |
| GET/POST | `/{id}/debug/*` | agent:chat / edit / portal:access |
| POST | `/{id}/debug/chat/stream` | SSE 流式调试 |

### Agent Open API — `/api/v1/open/agents`

| 方法 | 路径 | 鉴权 |
|------|------|------|
| GET | `/{id}/welcome` | API Key 或 Embed Token |
| POST | `/{id}/chat` | + `X-Caller-Id`（API Key 场景） |
| POST | `/{id}/chat/stream` | SSE |
| GET | `/{id}/conversations` | callerId 隔离 |
| GET | `/{id}/conversations/messages` | callerId 隔离 |

### Application — `/api/v1/applications`

CRUD、options、publish/unpublish

### Portal — `/api/v1/portal`

| GET | `/apps`、`/apps/{id}` | portal:access |

### Workflow — `/api/v1/workflows`

CRUD、options、publish、run、delete

### Knowledge — `/api/v1/knowledge-bases`

CRUD、documents 列表/上传/reprocess/delete；RAG 模块 `POST /{id}/retrieve`

### Model — `/api/v1/models`

providers CRUD、test、sync；configs CRUD、default；embedding-options；overview

### Tool — `/api/v1/tools`

CRUD、options、test

### MCP — `/api/v1/mcp-servers`

列表、connect、sync-tools、CRUD（需 agent:edit）

### Skill — `/api/v1/skills`

options、upload

### Prompt — `/api/v1/prompts`

CRUD、versions、rollback、test

### Chat 相关（合并在 Agent debug / open）

conversations、messages、attachments、conversation 删除

### Dashboard — `/api/v1/dashboard`

overview、recent-items、favorites、published-workflows、favorites/toggle、workflows runtime

### Monitor — `/api/v1/monitor`

overview、observability

### Trace — `/api/v1/trace`

spans 分页、详情、nodes（trace:view）

### Billing — `/api/v1/billing`

overview、quota、alerts、records、export（billing:view/manage）

### Token Usage — `/api/v1/token-usage`

logs、export

### Org — `/api/v1/org`

tenant、plan-summary、workspaces、members invite/update/delete

### RBAC — `/api/v1`

roles、permissions、grouped

### Platform — `/api/v1/platform`

tenants CRUD、stats（platform:manage）

### Audit — `/api/v1/audit-logs`

列表（audit:view）

### Notifications — `/api/v1/notifications`

列表、unread-count、read、read-all

### Search — `/api/v1/search`

全局搜索（search:global）

**API 总数（Controller 端点）：约 120+**（含 REST 变体与 debug/open 重复路径）

---

## 7. 数据库表（Flyway V1–V26）

| 域 | 表名 |
|----|------|
| 用户与权限 | `user`、`role`、`permission`、`role_permission` |
| 租户 | `tenant`、`tenant_member`、`workspace` |
| 应用 | `application` |
| Agent | `agent`、`agent_config`、`agent_knowledge`、`agent_tool`、`agent_skill`、`agent_api_key`、`agent_embed_token` |
| 对话 | `conversation`、`conversation_message` |
| 工作流 | `workflow`、`workflow_node`、`workflow_edge`、`workflow_execution`、`workflow_node_log` |
| 知识库 | `knowledge_base`、`document` |
| 模型 | `model_provider`、`model_config` |
| 工具 | `tool_definition`、`mcp_server` |
| Prompt | `prompt_template`、`prompt_template_version` |
| 计费 | `billing_quota`、`billing_alert`、`token_usage` |
| 工作台 | `user_recent_access`、`user_favorite` |
| 通知 | `user_notification` |
| 审计 | `audit_log` |

**说明：** `docs/数据库设计.md` 中的 `trace_span`、`agent_trace` 等表**未出现在 Flyway 迁移**；当前 Trace API 从 `token_usage` + `workflow_execution` 等聚合（`TraceMapper`）。

---

## 8. Redis 使用点

| 用途 | 模块 |
|------|------|
| Sa-Token 会话持久化 | security / spring-data-redis |
| Agent 对话窗口记忆 | `RedisChatMemoryStore`（ai-engine） |
| 登录限流 | `AuthRateLimiter` |
| Open API 限流 | `OpenApiRateLimiter` |
| 登录失败锁定 | `LoginFailureLockService` |
| 基础设施健康探测缓存 | `InfrastructureHealthChecker` |

---

## 9. 消息队列

**无。** 无异步 MQ 生产者/消费者；工作流与 Agent 执行为同步 + SSE 流式。

---

## 10. 流式与实时

| 机制 | 端点 | 说明 |
|------|------|------|
| SSE | Agent debug/open `chat/stream` | `SseEmitter`，非 WebSocket |
| 定时任务 | `ConversationRetentionService` | 默认每日 03:30 清理过期会话 |

---

## 11. 外部依赖

| 依赖 | 用途 | 失败影响 |
|------|------|----------|
| MySQL | 全业务 | 服务不可用 |
| Redis | 登录态、记忆、限流 | 登录/对话记忆/限流异常 |
| MinIO | 文档与 Skill 存储 | 上传/知识库失败 |
| Qdrant | 向量检索 | RAG 不可用 |
| 外部 LLM API | OpenAI 兼容等 | Agent/工作流推理失败 |
| MCP 子进程 | npx/node/uv 等 | MCP 工具不可用 |
| HTTP 工具目标 URL | 用户配置 | SSRF/超时风险 |
| OTLP / Langfuse | 可选遥测 | 仅观测缺失 |

---

## 12. 认证与授权架构

```
请求 → SaInterceptor(checkLogin)  [排除 PUBLIC_API_PATHS]
     → SaInterceptor(权限注解 @SaCheckPermission)
     → Open API: OpenApiAuthService（nf_live_ / nf_embed_）
     → 租户上下文（tenant_id 从登录态注入）
```

**Open API 凭证：**

- `nf_live_*` API Key：可 list conversations/messages，**必须** `X-Caller-Id`
- `nf_embed_*` Embed Token：**禁止** list conversations/messages，chat 需 callerId

**生产配置要点（application-prod.yml）：**

- `registration-enabled: false`（默认）
- SpringDoc 关闭
- Flyway `validate-on-migrate: true`
- 强随机 `NOVAFLOW_CRYPTO_KEY`（模型 API Key 加密）

---

## 13. 前端路由地图

| 类型 | 路径 |
|------|------|
| 公开 | `/login`、`/register`、`/embed/agents/:id` |
| Studio | `/dashboard`、`/agent`、`/workflow`、`/workflow/:id`、`/knowledge`、`/knowledge/:id`、`/model`、`/tool`、`/prompt`、`/application` |
| 运维 | `/monitor`、`/log`、`/trace`、`/observability`、`/billing` |
| 治理 | `/org`、`/permission`、`/settings`、`/platform`、`/audit` |
| 门户 | `/portal`、`/portal/apps/:id` |
| 关于 | `/about/*` |

E2E：`novaflow-web/e2e/`（auth、agent、modules、smoke-pages、about）

---

## 14. 测试资产（已有）

### 后端单元 / 集成测试

| 类型 | 代表 |
|------|------|
| Open API 安全 | `OpenApiSecurityLocalIntegrationTest`、`OpenApiSecurityTestcontainersIntegrationTest` |
| 认证 | `AuthLocalIntegrationTest`、`AuthTestcontainersIntegrationTest` |
| 全模块冒烟 | `FullFeatureLocalIntegrationTest` |
| 平台超管 | `PlatformAdminLocalIntegrationTest` |
| Agent 安全 | `AgentOpenServiceSecurityTest`、`OpenApiAuthServiceTest`、`OpenApiCallerIdValidatorTest` |
| 限流 | `AuthRateLimiterTest`、`OpenApiRateLimiterTest`、`LoginFailureLockServiceTest` |
| 其他 | `UrlSafetyValidatorTest`、`McpCommandValidatorTest`、`WorkflowElBuilderTest` |

### 前端 E2E

Playwright：`npm run test:e2e`（需 `test:e2e:install`）

### 建议测试命令（第二阶段）

```bash
# 后端
mvn test
mvn -pl novaflow-server test -Dgroups=local    # 需本地 MySQL/Redis 等
mvn -pl novaflow-server test -Dgroups=testcontainers

# 前端
cd novaflow-web && npm install && npm run build
cd novaflow-web && npm run test:e2e
```

---

## 15. 高风险模块（优先测试）

| 优先级 | 模块 | 风险类型 |
|--------|------|----------|
| P0 | Open API / Embed | 鉴权绕过、callerId 隔离、Embed 越权读会话 |
| P0 | 多租户数据隔离 | IDOR：跨 tenant 读写 Agent/应用/知识库 |
| P0 | RBAC | 普通用户访问管理 API、权限码遗漏 |
| P0 | 认证 | 暴力破解、Token 泄露、注册开关（prod） |
| P1 | MCP 命令执行 | 命令注入、非白名单可执行文件 |
| P1 | HTTP 工具 | SSRF、内网探测 |
| P1 | 文件上传 | 知识库文档、Skill 包：类型/大小/路径遍历 |
| P1 | Agent 对话 | 重复提交、会话一致性、SSE 断连 |
| P1 | 工作流执行 | 重复 run、Agent 节点嵌套、超时 |
| P1 | RAG 管道 | 大文件、Embedding 失败后的数据一致性 |
| P1 | 计费配额 | 超限拦截、token 统计准确性 |
| P2 | 会话保留任务 | 定时删除边界、误删 |
| P2 | CORS / 生产配置 | localhost 残留、crypto-key 弱配置 |
| P2 | 前端 XSS | Markdown 渲染（已有 DOMPurify，需验证） |

---

## 16. 部署架构（生产）

`deploy/docker-compose.prod.yml`：mysql、redis、minio、qdrant、server（Spring prod）、web（Nginx 静态 + 反代）

环境变量模板：`deploy/.env.prod.example`

---

## 17. 侦察阶段发现（待验证，非 Bug 结论）

| # | 观察 | 状态 |
|---|------|------|
| 1 | 设计文档 `trace_span` 表与 Flyway 不一致 | NOT VERIFIED（可能仅文档超前） |
| 2 | dev 默认 `registration-enabled: true` | 预期行为；prod 默认 false |
| 3 | dev 开启 SpringDoc | prod 已关闭 |
| 4 | Flyway dev `validate-on-migrate: false` | prod 为 true |
| 5 | git 中存在 `target/`、`.idea/` 等未跟踪构建产物 | 仓库卫生，非运行时问题 |

---

## 18. 下一阶段计划（第 2 次：API + 后端）

1. 确认本地/CI 环境可 `mvn test` 全绿
2. 启动完整栈（MySQL + Redis + MinIO + Qdrant + Server）
3. 按 `TEST-CHECKLIST.md` 执行 API 全量与越权测试
4. 产出 `API-TEST-REPORT.md` 与 `QA-REPORT.md` 初稿

---

*本文件为 QA 第 1 阶段交付物；不包含最终上线结论。*
