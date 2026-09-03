# NovaFlow AI — 上线前门禁脚本

> 对应 QA 遗漏项：部署验收、跨租户 IDOR、CC-02 回归、生产 Compose 冒烟、依赖扫描  
> 前提：本机后端 `http://localhost:8080`（可用 `NOVAFLOW_BASE_URL` 覆盖）

## 一键执行

```powershell
# 默认：部署门禁 + IDOR + publish 并发
pwsh test/run-pre-release-gates.ps1

# 跳过并发（省时间）
pwsh test/run-pre-release-gates.ps1 -SkipConcurrency

# 含生产 Compose 冒烟（需先 docker compose up）
pwsh test/run-pre-release-gates.ps1 -IncludeProdCompose
```

## 单脚本

| 脚本 | 覆盖项 | 命令 |
|------|--------|------|
| `pre-deploy-gate.ps1` | 健康检查、登录、`pageSize`≤100 | `pwsh test/pre-deploy-gate.ps1` |
| `cross-tenant-idor.ps1` | Z-04~Z-06 跨租户拒绝 | `pwsh test/cross-tenant-idor.ps1` |
| `publish-concurrency-gate.ps1` | CC-02 version +50 | `pwsh test/publish-concurrency-gate.ps1` |
| `prod-compose-smoke.ps1` | PR-01 全栈冒烟 | `pwsh test/prod-compose-smoke.ps1` |
| `dependency-audit.ps1` | DEP-01/02 | `pwsh test/dependency-audit.ps1` |
| `chat-rag-smoke.ps1` | 调试对话 + 知识库 retrieve | `pwsh test/chat-rag-smoke.ps1` |
| `fault-injection.ps1` | Redis/MySQL 停服恢复 | `pwsh test/fault-injection.ps1` |
| `coverage-gap-smoke.ps1` | 鉴权/RBAC/模块读接口/CORS/收藏并发 | `pwsh test/coverage-gap-smoke.ps1` |

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
```

需本机 MySQL + Redis（与现有 `@Tag("local")` 套件相同）。  
若 live API 的 `/auth/register` 返回 500（旧 JAR），请优先跑本集成测试。

## Playwright E2E（Portal / Embed）

```powershell
cd novaflow-web
npm run test:e2e -- --project=chromium-portal
npm run test:e2e -- --project=chromium-embed
```

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
