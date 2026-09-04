# NovaFlow AI — SECURITY AUDIT REPORT

> 阶段：第 5 次 · 安全专项 + 生产配置审计  
> 日期：2026-09-02  
> 环境：本地 dev · `http://localhost:8080` · MySQL / Redis / MinIO / Qdrant

---

## 1. 执行摘要

| 类别 | 结果 |
|------|------|
| SQL 注入（S-01） | ✅ PASS |
| XSS（S-02） | ✅ PASS（代码审查 + DOMPurify） |
| CSRF / CORS（S-03） | ✅ PASS（Bearer JWT + 白名单 CORS） |
| SSRF（S-04） | ✅ PASS（实时探测 + 单元测试） |
| IDOR（S-05） | ✅ PASS（同租户跨角色）；跨租户 ⚠️ NOT VERIFIED |
| 路径遍历（S-06） | ✅ PASS（MinIO 对象路径消毒） |
| 敏感信息泄露（S-07） | ✅ PASS（错误统一包装，无堆栈外泄） |
| Actuator 暴露（S-08） | ✅ PASS（prod 仅 health）；dev `/actuator/env` 返回 500 包装 |
| Swagger prod 关闭（S-09） | ✅ PASS（`application-prod.yml` 禁用）；dev 可访问 ⚠️ 预期行为 |
| 硬编码密钥扫描（S-10） | ✅ PASS（源码无硬编码凭证；部署示例为占位符） |
| 生产部署审计（PR-01–07） | ⚠️ 部分（配置审查 PASS；全栈 Compose 未实测） |
| 依赖 CVE（DEP-01–03） | ⚠️ 部分（安全单元测试 PASS；OWASP/npm audit 未完整执行） |

**结论：** 未发现 P0/P1 安全漏洞；遗留 1 项 P2（`DB-P2-001` pageSize 上限，属可用性/DoS 面）及若干 P3 运维/配置提醒。

---

## 2. SQL 注入（S-01）

| 端点 | Payload | HTTP | 结果 |
|------|---------|------|------|
| `GET /api/v1/search?keyword=' OR '1'='1` | 经典 OR 注入 | 200 | ✅ 空结果，无异常 |
| `GET /api/v1/audit-logs?keyword=' OR '1'='1` | 同上 | 200 | ✅ 空结果 |
| `GET /api/v1/agents?keyword=' OR '1'='1` | 同上 | 200 | ✅ 空结果 |

**机制：** MyBatis-Flex 参数化查询；无字符串拼接 SQL。

---

## 3. XSS（S-02）

| 位置 | 防护 | 结果 |
|------|------|------|
| Agent 调试 / 聊天 Markdown | `novaflow-web/src/utils/markdown.ts` → `marked` + `DOMPurify.sanitize()` | ✅ PASS |
| Portal / Embed 渲染 | 复用 `renderMarkdown()` | ✅ PASS（代码审查） |
| 实时 payload 注入 | 未在浏览器逐页验证 `<script>alert(1)</script>` | ⚠️ NOT VERIFIED（依赖 DOMPurify 单测惯例） |

---

## 4. CSRF / CORS（S-03）

| 项 | 说明 | 结果 |
|----|------|------|
| 认证模型 | 无 Session Cookie；`Authorization: Bearer <JWT>` | CSRF 风险低 ✅ |
| CORS | `application.yml` / `application-prod.yml` 白名单 `CORS_ALLOWED_ORIGIN` | ✅ PASS |
| 生产默认 | `deploy/docker-compose.prod.yml` 默认 `http://localhost` | ⚠️ 部署时必须改为生产域名（见 SEC-P3-004） |

---

## 5. SSRF（S-04）

### 5.1 实时 HTTP 工具探测

创建临时工具 `qa_ssrf_test`，URL `http://127.0.0.1:8080/...`，调用 `POST /api/v1/tools/{id}/test`：

```json
{"success":false,"error":"工具 URL 不允许访问内网或保留地址"}
```

工具已删除；临时文件 `test/tmp_ssrf_tool.json` 已清理。

### 5.2 单元测试

| 测试类 | 结果 |
|--------|------|
| `UrlSafetyValidatorTest` | ✅ PASS（localhost / RFC1918 / 169.254.169.254 / link-local IPv6） |
| `McpCommandValidatorTest` | ✅ PASS |

**机制：** `UrlSafetyValidator` 用于 `HttpToolExecutor`、`McpClient`；阻断内网、元数据 IP、`file://` 等。

**备注（SEC-P3-003）：** 工具定义可**保存**内网 URL，仅在**执行/测试**时拦截；建议保存时同步校验（防御纵深，P3）。

---

## 6. IDOR（S-05）

使用 `user@novaflow.ai` Token 访问管理员资源：

| 请求 | HTTP | code | 结果 |
|------|------|------|------|
| `GET /api/v1/agents/1` | 403 | 40301 | ✅ |
| `GET /api/v1/workflows/1` | 403 | 40301 | ✅ |
| `GET /api/v1/knowledge-bases/1` | 403 | 40301 | ✅ |
| `GET /api/v1/applications/1` | 403 | 40301 | ✅ |
| `GET /api/v1/platform/tenants` | 403 | 40301 | ✅ |
| `GET /api/v1/billing/records` | 403 | 40301 | ✅ |
| 无 Token `GET /api/v1/agents` | 401 | 40101 | ✅ |
| admin `GET /api/v1/agents/99999` | 400 | Agent 不存在 | ✅ |

**Open API：** `OpenApiSecurityLocalIntegrationTest`（callerId 隔离、Embed 边界）— 第 2 阶段已 PASS。

**未验证：** 第二租户用户横向访问租户 A 资源（环境仅单租户演示数据）。

---

## 7. 路径遍历 / 文件上传（S-06）

`MinioDocumentStorageService.buildObjectPath`：

- 文件名非法字符 `[\\/:*?"<>|]` → `_`
- 路径格式：`knowledge/{tenantId}/{kbId}/{uuid}_{safeName}`
- 无 `../` 穿越至桶外路径

**未验证：** 恶意 MIME / 超大文件 / 双扩展名上传的端到端拒绝（依赖业务层校验，未专项探测）。

---

## 8. 敏感信息泄露（S-07）

| 场景 | 观察 | 结果 |
|------|------|------|
| 未授权 API | `{"code":40101,"message":"登录已过期..."}` | ✅ 无堆栈 |
| 权限拒绝 | `code=40301` | ✅ |
| 系统错误 | `code=50000, 系统繁忙` | ✅ 无内部细节 |
| `GET /actuator/env`（dev） | HTTP 500 + ApiResult 包装 | ✅ 未泄露 env 键值 |

---

## 9. Actuator & Swagger（S-08 / S-09）

| 环境 | 端点 | 结果 |
|------|------|------|
| dev | `GET /actuator/health` | ✅ UP |
| dev | `GET /actuator/env` | 500 包装（非标准 actuator JSON） |
| dev | `GET /swagger-ui/index.html` | 200 可访问（开发预期） |
| prod 配置 | `application-prod.yml` | `springdoc` 关闭；`management.endpoints.web.exposure.include: health` ✅ |

---

## 10. 生产部署审计（PR-01–07）

| ID | 检查项 | 结果 |
|----|--------|------|
| PR-01 | `docker-compose.prod.yml` 全栈启动 | ⬜ NOT VERIFIED |
| PR-02 | `.env.prod.example` 弱密码占位 | ✅ 均为 `change-me-*` / `replace-with-*` |
| PR-03 | `NOVAFLOW_CRYPTO_KEY` 强度 | ✅ 示例要求 32+ 字符随机值 |
| PR-04 | Nginx 安全头 / 反代 | ✅ `X-Frame-Options`, `CSP`, `nosniff` 等；TLS 需在 LB 或 443 块配置 |
| PR-05 | `NOVAFLOW_REGISTRATION_ENABLED=false` | ✅ prod profile 默认 false |
| PR-06 | CORS 生产域名 | ⚠️ 依赖部署时设置 `CORS_ALLOWED_ORIGIN` |
| PR-07 | healthcheck + restart | ✅ compose 中已配置（第 4 阶段 INFRA 报告） |

---

## 11. 依赖安全（DEP-01–03）

| ID | 命令 | 结果 |
|----|------|------|
| DEP-01 | Maven OWASP dependency-check | ⬜ NOT RUN |
| DEP-02 | `npm audit`（novaflow-web） | ⬜ BLOCKED — npmmirror 不支持 audit API |
| DEP-03 | Docker 基础镜像版本 | ⬜ 未专项扫描 |
| — | 安全相关单元测试 | ✅ PASS（见下） |

```bash
mvn -pl novaflow-common,novaflow-agent test \
  -Dtest=UrlSafetyValidatorTest,McpCommandValidatorTest,OpenApiAuthServiceTest,OpenApiCallerIdValidatorTest,AgentOpenServiceSecurityTest
```

---

## 12. 新发现 / 更新问题

| ID | 级别 | 摘要 |
|----|------|------|
| DB-P2-001 | P2 | 列表 API `pageSize` 无统一上限（已有） |
| SEC-P3-003 | P3 | HTTP 工具保存阶段未校验 URL，仅执行时拦截 |
| SEC-P3-004 | P3 | 生产部署须显式设置 `CORS_ALLOWED_ORIGIN` 为真实前端域名 |
| DEP-P3-001 | P3 | `npm audit` 需切换 registry.npmjs.org 或 CI 镜像支持 audit |

---

## 13. 建议后续动作

1. **第 6 次：** 并发压测（Open API chat、publish、favorite）
2. **第 7 次：** 修复 `DB-P2-001`、`FE-WARN-*`、`SEC-P3-003`（可选）
3. **CI：** 增加 `npm audit --registry=https://registry.npmjs.org` 与 OWASP dependency-check
4. **可选：** 第二租户 IDOR 回归；Portal/Embed XSS 浏览器实测

---

*关联文档：`QA-REPORT.md` · `API-TEST-REPORT.md` · `INFRA-AUDIT-REPORT.md`*
