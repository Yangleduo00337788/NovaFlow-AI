# NovaFlow AI — AUTO-FIX REPORT

> 阶段：第 7 次 · 自动修复  
> 日期：2026-09-02

---

## 修复摘要

| ID | 级别 | 修复内容 | 验证 |
|----|------|----------|------|
| DB-P2-001 | P2 | 新增 `PageQueryUtils`，14 个列表 Service 统一 `pageSize ≤ 100` | ✅ 编译 + `PageQueryUtilsTest` |
| CONC-P2-001 | P2 | `AgentPublishService.publish` 使用 `SELECT … FOR UPDATE` 锁定 Agent 行 | ✅ 编译 + 集成测试 25/25 |
| SEC-P3-003 | P3 | `ToolDefinitionService.validateHttpTool` 保存时调用 `UrlSafetyValidator` | ✅ 编译 |
| FE-WARN-001 | P3 | 6 处 `maxlength="128"` → `:maxlength="128"` | ✅ 代码审查 |
| FE-WARN-002 | P3 | `/platform`、`/audit` 加载失败增加 `catch` + Toast | ✅ 代码审查 |

---

## 代码变更

### 后端

- `novaflow-common/.../PageQueryUtils.java`（新建）
- 分页 clamp：`AgentService`、`WorkflowService`、`ApplicationService`、`KnowledgeBaseService`、`DocumentService`、`ToolDefinitionService`、`McpServerService`、`PromptTemplateService`、`OrganizationService`、`PlatformAdminService`、`AuditLogQueryService`、`ConversationService`
- `AgentPublishService.lockAgentForUpdate()` — 并发 publish 串行化版本递增
- `ToolDefinitionService` — SSRF URL 保存时校验

### 前端

- `views/{agent,application,prompt,knowledge,workflow}/index.vue`、`knowledge/detail.vue`
- `views/platform/index.vue`、`views/audit/index.vue`

---

## 测试

```bash
mvn -pl novaflow-common test -Dtest=PageQueryUtilsTest,UrlSafetyValidatorTest
mvn -pl novaflow-server test -Dtest.excludedGroups=testcontainers  # 25/25 PASS
```

**说明：** 本地 `:8080` 若仍为旧 JAR，需重启后 live 验证 `pageSize=99999` 截断为 100。

---

## 遗留（未在本阶段修复）

| ID | 级别 | 说明 |
|----|------|------|
| ENV-001 | P3 | 运维：部署前重启陈旧进程 |
| SEC-P3-004 | P3 | 生产 CORS 域名需部署时配置 |
| DEP-P3-001 | P3 | npm audit 镜像限制 |
| CONC-P3-001 | P3 | 100 并发 favorite 6 次瞬时 500 |

---

*关联：`QA-REPORT.md`*
