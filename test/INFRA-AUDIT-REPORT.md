# NovaFlow AI — 数据库 / Redis / MQ 审计报告

> 阶段：第 4 次 · 基础设施审计  
> 日期：2026-09-02  
> 环境：MySQL 8 localhost · Redis 7 Docker · 无 MQ

---

## 1. 执行摘要

| 域 | 结论 |
|----|------|
| 数据库（MySQL + Flyway） | **PASS**（有 1 项 P2 改进建议） |
| Redis | **PASS** |
| 消息队列 | **N/A**（项目未使用） |
| 容灾（Redis/MySQL 不可用） | **NOT VERIFIED**（未主动停服） |

---

## 2. 消息队列（MQ）

| 检查项 | 结果 |
|--------|------|
| Kafka / RabbitMQ / RocketMQ 依赖 | ❌ 未引入 |
| `@KafkaListener` / `@RabbitListener` | ❌ 无 |
| 异步任务替代 | `@Scheduled` 定时任务（会话保留）+ 同步 API |

**结论：** MQ 审计 **N/A**，无消息幂等/丢失风险面。

---

## 3. 数据库审计

### 3.1 Schema 概况

| 项 | 值 |
|----|-----|
| Flyway 版本 | V1 – V26 |
| 业务表数量 | 34 张（见 `PROJECT-MAP.md`） |
| 字符集 | utf8mb4_unicode_ci |
| 迁移策略 | dev `validate-on-migrate: false` · prod `true` |

### 3.2 唯一约束（抽样验证）

| 表 | 约束 | 业务意义 | 结果 |
|----|------|----------|------|
| `user` | `uk_email`, `uk_username` | 防重复注册 | ✅ 设计合理 |
| `tenant_member` | `uk_tenant_user` | 同租户不重复加入 | ✅ |
| `conversation` | `uk_agent_conversation` | 同 Agent 会话键唯一 | ✅ |
| `agent_api_key` | `uk_agent_id`, `uk_api_key_hash` | 一 Agent 一 Key | ✅ |
| `agent_embed_token` | `uk_agent_embed`, `uk_embed_token_hash` | 一 Agent 一 Embed Token | ✅ |
| `role_permission` | `uk_role_perm` | 角色权限不重复 | ✅ |
| `user_favorite` | `uk_user_favorite` | 收藏幂等 | ✅ |

集成测试与 E2E 的创建-删除闭环间接验证写入路径正常。

### 3.3 索引（性能）

**V21 专项索引：**

- `token_usage`: `idx_tenant_created`, `idx_tenant_trace`, `idx_tenant_success_created`
- `workflow_execution`: `idx_tenant_started`
- `workflow`: `idx_tenant_status_created`
- `conversation_message`: `idx_conv_role_time`

**V22 Open API 隔离：**

- `conversation.idx_agent_channel_caller` (`agent_id`, `channel`, `caller_id`)

**审计日志：**

- `audit_log.idx_tenant_created`, `idx_tenant_action`

**结论：** 高频查询路径（租户隔离 + 时间排序）有索引覆盖。✅

### 3.4 软删除

| 模式 | 表示例 | 查询是否带 `is_deleted=0` |
|------|--------|---------------------------|
| 软删除 | `agent`, `user`, `application`, `knowledge_base`… | ✅ 服务层普遍过滤 |
| 硬删除 | `conversation` / `conversation_message`（保留策略） | 定时任务物理删除 |

当前库中软删除 Agent 数：**18**（`is_deleted=1`），说明软删除在实际使用。

**风险点（P3）：** 需确保所有读路径都带 `is_deleted` 过滤；抽样代码审查显示主要 Service 已覆盖，**未做全量静态扫描**。

### 3.5 事务

关键写路径均使用 `@Transactional`：

- `AuthService.register` / 登录相关
- `AgentPublishService.publish` / `unpublish`（API Key + Embed Token + 状态原子更新）
- `ConversationRetentionService.purgeExpiredConversations`（先删消息再删会话）
- `WorkflowService` / `KnowledgeBaseService` / `OrganizationService` 等 CRUD

**会话保留任务：**

```java
@Transactional
// 每批最多 500 条 conversation，先 message 后 conversation
```

✅ 批处理 + 事务，失败可回滚。

### 3.6 分页

| 模块 | pageSize 上限 | 结果 |
|------|---------------|------|
| Billing / Token 日志 | `min(max(pageSize,1), 100)` | ✅ |
| Trace | 100 | ✅ |
| Notification | 50 | ✅ |
| Agent / Workflow / Knowledge / Application 等 | **无上限** | ⚠️ P2 |

**DB-P2-001：** 多数列表 API 未限制 `pageSize` 上限，传入 `pageSize=99999` 可能一次拉取大量数据（性能/内存风险）。Billing/Trace 已做示范，建议统一抽取 `PageUtils.clamp(pageSize, 100)`。

### 3.7 N+1 查询

| 位置 | 模式 | 结果 |
|------|------|------|
| `ConversationService.loadPreviewMap` | 批量 `listLatestUserPreviews` | ✅ 避免 N+1 |
| Agent 列表 `toSimpleVO` | 单表分页 | ✅ |
| 其他模块 | NOT VERIFIED（未全量 EXPLAIN） |

### 3.8 并发与幂等

| 场景 | 机制 | 结果 |
|------|------|------|
| 同会话重复创建 | `uk_agent_conversation` | ✅ DB 约束 |
| Agent 发布 | `@Transactional` 单事务 | ✅ |
| 收藏 toggle | `uk_user_favorite` | ✅ 集成测试覆盖 |
| 并发 publish 双 API Key | 无分布式锁 | ⚠️ 低风险（单租户管理员操作）NOT VERIFIED |

### 3.9 实时验证

| 测试 | 结果 |
|------|------|
| Flyway 当前版本 V26 | ✅ |
| `conversation` 表 154 条 | ✅ 有真实数据 |
| `audit_log` 索引存在 | ✅ |

---

## 4. Redis 审计

### 4.1 Key 清单

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `novaflow:chat:memory:{memoryId}` | Agent 对话窗口记忆 | **7 天**（实测 TTL ≈ 529955s） |
| `novaflow:auth:login:fail:{hash}` | 登录失败计数 | 15 分钟 |
| `novaflow:auth:login:lock:{hash}` | 登录锁定 | 15 分钟 |
| `novaflow:auth:login:rate:{hash}` | 登录限流 | 1 分钟 |
| `novaflow:auth:register:rate:{hash}` | 注册限流 | 1 分钟 |
| `novaflow:open-api:rate:{hash}` | Open API Key 限流 | 1 分钟 |
| `novaflow:open-api:ip-rate:{ip}` | Open API IP 限流 | 1 分钟 |
| Sa-Token 会话 Key | 登录态（框架管理） | 86400s（配置） |

### 4.2 缓存一致性

| 场景 | 策略 | 结果 |
|------|------|------|
| 对话记忆 vs DB 历史 | Redis 窗口 + MySQL 全量持久化 | ✅ 分层设计 |
| DB 成功 / Redis 失败 | 记忆丢失但对话仍在 DB | ⚠️ 可接受降级 |
| Redis 成功 / DB 失败 | NOT VERIFIED | |
| 缓存穿透 | 记忆 miss 返回空列表 | ✅ |
| 缓存击穿/雪崩 | 无限流统一策略 | ⚠️ 对话记忆无 singleflight，低风险 |
| 分布式锁 | **未使用** | N/A |

### 4.3 限流与锁定（实测）

**登录失败锁定（实时 curl）：**

| 次数 | 响应 |
|------|------|
| 1–4 | `40000` 邮箱或密码错误 |
| 5–6 | `42902` 登录失败次数过多，请 15 分钟后再试 |

✅ **PASS**（阈值 5 与配置一致）

**单元测试：**

- `LoginFailureLockServiceTest` ✅
- `AuthRateLimiterTest` ✅
- `OpenApiRateLimiterTest` ✅

### 4.4 容灾

| 场景 | 结果 |
|------|------|
| Redis 重启后会话恢复 | NOT VERIFIED（Sa-Token 持久化设计支持，未实测） |
| Redis 不可用时 API 行为 | NOT VERIFIED（未停 Redis） |
| MySQL 不可用 | `/api/v1/health` 可反映（集成测试覆盖） |

---

## 5. 问题与建议

| ID | 级别 | 问题 | 建议 |
|----|------|------|------|
| DB-P2-001 | P2 | 多数列表 API 未限制 `pageSize` 上限 | 统一 `clamp(pageSize, 1, 100)` |
| DB-P3-001 | P3 | 会话表硬删除，无软删除 | 符合保留策略设计，文档已说明 |
| REDIS-P3-001 | P3 | 无分布式锁，极高并发 publish 理论竞态 | 可接受；必要时 DB 唯一约束兜底 |
| INFRA-NV-001 | — | Redis/MySQL 故障注入未测 | 第 6 次容灾专项 |

---

## 6. 结论

**第 4 阶段：PASS**（无 P0/P1；1 项 P2 改进建议）

**下一步：** 第 5 次 — 安全专项（SQL 注入、XSS、SSRF、IDOR 全量）
