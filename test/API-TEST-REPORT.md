# NovaFlow AI — API TEST REPORT

> 阶段：第 2 次 · API + 后端测试  
> 日期：2026-09-02  
> 环境：本地 dev · `http://localhost:8080`  
> 基础设施：MySQL · Redis · MinIO · Qdrant（Docker）

---

## 1. 测试执行摘要

| 项 | 结果 |
|----|------|
| 单元测试（排除 local/testcontainers） | **PASS** |
| 本地集成测试 `novaflow-server`（25 项） | **PASS** |
| 实时 API 探测（curl） | **PASS**（见下方明细） |
| Open API 安全集成测试 | **PASS**（7 场景） |
| 全模块冒烟 `FullFeatureLocalIntegrationTest` | **PASS**（含在 25 项内） |

**测试命令：**

```bash
# 单元测试
mvn test -Dtest.excludedGroups=local,testcontainers

# 本地集成测试（需 MySQL + Redis）
mvn -pl novaflow-server test -Dtest.excludedGroups=testcontainers
```

---

## 2. 认证（Authentication）

| ID | 用例 | 预期 | 实际 | 结果 |
|----|------|------|------|------|
| A-04 | admin 登录 | 200 + token | code=0, token 36 字符 | ✅ PASS |
| A-05 | 错误密码 | 拒绝 | HTTP 400, code=40000 | ✅ PASS |
| A-09 | 无 Token `/auth/me` | 401 | HTTP 401, code=40101 | ✅ PASS |
| A-11 | 伪造 Token | 401 | HTTP 401, code=40101 | ✅ PASS |
| — | 登出 + `/auth/me` | 集成测试覆盖 | `AuthLocalIntegrationTest` | ✅ PASS |
| — | 注册新租户 | 集成测试覆盖 | `FullFeatureLocalIntegrationTest` | ✅ PASS |

---

## 3. 授权（Authorization）

| ID | 用例 | 预期 | 实际 | 结果 |
|----|------|------|------|------|
| Z-01 | `user` → `/platform/tenants` | 403 | HTTP 403, code=40301 | ✅ PASS |
| Z-03 | `user` → `/agents` | 403 | HTTP 403, code=40301 | ✅ PASS |
| Z-04 | `user` → 他人 Agent 详情 | 403 | HTTP 403, code=40301 | ✅ PASS |
| — | `user` → `/billing` | 403 | HTTP 403, code=40301 | ✅ PASS |
| — | `user` → `/audit-logs` | 403 | HTTP 403, code=40301 | ✅ PASS |
| — | `tenant_admin` → `/audit-logs` | 200 | HTTP 200, 分页正常 | ✅ PASS |
| — | 平台超管 tenants/stats | 集成测试覆盖 | `PlatformAdminLocalIntegrationTest` | ✅ PASS |

---

## 4. Open API 安全

| ID | 用例 | 预期 | 实际 | 结果 |
|----|------|------|------|------|
| O-02 | API Key chat 无 `X-Caller-Id` | 拒绝 | HTTP 400, code=40001 | ✅ PASS |
| O-06 | Embed Token list conversations | 403 | HTTP 403, code=40303 | ✅ PASS |
| O-07 | 无效 API Key | 401 | HTTP 401, code=40101 | ✅ PASS |
| O-07 | 无 Token welcome | 401 | HTTP 401 | ✅ PASS |
| O-01 | API Key welcome/chat/stream | 集成测试 | `OpenApiSecurityLocalIntegrationTest` | ✅ PASS |
| O-03 | callerId 会话隔离 | 集成测试 | 同上 | ✅ PASS |
| O-04 | 跨 callerId 读消息 | 集成测试 | 同上 | ✅ PASS |
| O-05 | Embed welcome/chat | 集成测试 | 同上 | ✅ PASS |

---

## 5. 业务模块 API 冒烟

| 模块 | 端点 | 结果 |
|------|------|------|
| Health | `GET /api/v1/health` | ✅ UP（MySQL/Redis/MinIO/Qdrant） |
| Actuator | `GET /actuator/health` | ✅ `{"status":"UP"}` |
| Portal | `GET /api/v1/portal/apps`（user） | ✅ 返回已发布应用列表 |
| Dashboard | `GET /api/v1/dashboard/overview` | ✅ 200 |
| Monitor | `GET /api/v1/monitor/overview` | ✅ 200 |
| Billing | `GET /api/v1/billing/overview` | ✅ 200 |
| Search | `GET /api/v1/search?keyword=agent` | ✅ 200 |
| Agent | CRUD + publish 闭环 | ✅ 集成测试 |
| Workflow | CRUD + run | ✅ 集成测试 |
| Knowledge | CRUD + upload | ✅ 集成测试 |
| Model | providers/configs | ✅ 集成测试 |
| Tool / MCP / Prompt | CRUD | ✅ 集成测试 |
| Org | workspaces/members | ✅ 集成测试 |

---

## 6. 异常 / 边界（抽样）

| 用例 | 结果 |
|------|------|
| 空邮箱密码登录 | HTTP 400 参数校验失败 | ✅ PASS |
| 超大 pageSize | NOT VERIFIED（未专项测试） |
| SQL 注入 keyword | NOT VERIFIED（第 5 次安全专项） |

---

## 7. 环境问题记录（非代码 Bug）

### ENV-001：陈旧后端进程导致误报 500

| 项 | 说明 |
|----|------|
| 现象 | 旧 JAR 进程（PID 13784）运行时，`/actuator/health`、`/portal/apps`、`/audit-logs` 均返回 HTTP 500 + `code=50000` |
| 根因 | 长时间运行的旧构建进程状态异常；非当前代码逻辑缺陷 |
| 验证 | 停止旧进程、重新 `mvn package` 并启动新 JAR 后，上述接口全部正常 |
| 建议 | 部署/测试前确保重启服务；生产使用 `restart: unless-stopped` + 健康检查 |

---

## 8. 统计

| 类别 | 已测 | 通过 | 失败 | 未验证 |
|------|------|------|------|--------|
| 认证 | 6 | 6 | 0 | 0 |
| 授权 | 8 | 8 | 0 | 0 |
| Open API | 8 | 8 | 0 | 0 |
| 模块冒烟 | 12+ | 12+ | 0 | 0 |
| 边界异常 | 1 | 1 | 0 | 2+ |

---

## 9. 结论

**API + 后端测试：PASS**（在当前本地环境、最新构建下）

遗留到后续阶段：

- 第 3 次：前端 E2E（Playwright）
- 第 5 次：SQL 注入 / XSS / SSRF 专项
- 第 6 次：并发与性能
- Testcontainers 集成测试（需 Docker 完整支持）

**上线结论：** 本阶段 **NOT READY**（仅完成 API 层，尚未完成全量 QA 门禁）
