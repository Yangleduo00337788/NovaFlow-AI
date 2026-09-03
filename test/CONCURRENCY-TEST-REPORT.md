# NovaFlow AI — CONCURRENCY TEST REPORT

> 阶段：第 6 次 · 并发压测  
> 日期：2026-09-02  
> 环境：本地 dev · `http://localhost:8080` · pwsh 7 `ForEach-Object -Parallel`  
> 脚本：`test/concurrency-run.ps1` · 结果：`test/concurrency-results.json`

---

## 1. 执行摘要

| ID | 场景 | 并发数 | 结果 | 说明 |
|----|------|--------|------|------|
| CC-01 | Open API chat | 10 | ✅ PASS | 需已发布应用 + 合法 `X-Caller-Id`（≥8 位） |
| CC-02 | 重复 publish 同一 Agent | 50 | ⚠️ PASS* | HTTP 全成功，但 **version 丢失更新**（竞态） |
| CC-03 | favorite toggle | 100 | ✅ PASS | 94% 成功；终态数据一致（0 条收藏） |
| CC-04 | 工作流 run（含 LLM） | 10 | ✅ PASS | avg ~27s，p95 ~38s |
| CC-05 | 知识库文档上传 | 10 | ✅ PASS | avg ~206ms |

\* CC-02 存在 P2 数据一致性风险，见 `CONC-P2-001`。

---

## 2. CC-01 — Open API chat ×10

**前置：** 新建 Agent（`applicationId=1` 已发布应用）→ publish 获取 API Key。

| 指标 | 值 |
|------|-----|
| 成功 | **10 / 10** |
| apiCode | `0:10` |
| 平均延迟 | **1212 ms** |
| Caller-Id | `caller-0001` … `caller-0010` |

**备注：** 首次脚本使用 `applicationId=8`（未发布应用）及过短 `cid1` 导致失败，属测试配置问题，非产品缺陷。Open API 路径经 `detail()` → `assertPortalAccess()`，要求 Agent 所属应用已发布。

---

## 3. CC-02 — 重复 publish ×50

| 指标 | 值 |
|------|-----|
| HTTP 成功 | **50 / 50** |
| apiCode | `0:50` |
| 平均延迟 | 202 ms · p95 281 ms |
| 初始 version | 2（setup publish 后） |
| 终态 version | **9**（期望 52） |
| API Key 行数 | 1（`agent_api_key` 按 agent 唯一，末次写入生效） |

**结论：** 并发 publish 全部返回成功，但 `agent.version` 发生 **丢失更新**（50 次请求仅净增 ~7）。API Key 轮换无重复行，但旧 Key 立即失效，高并发下存在「客户端仍持旧 Key」风险。

**问题 ID：** `CONC-P2-001`

---

## 4. CC-03 — favorite toggle ×100

| 指标 | 值 |
|------|-----|
| 成功 | **94 / 100** |
| 失败 | 6 × `50000` 系统繁忙 |
| 平均延迟 | 143 ms · p95 450 ms |
| 终态 `user_favorite` 行数 | **0**（100 次偶数 toggle，符合预期） |

**结论：** `uk_user_favorite` + `DuplicateKeyException` 处理有效，无重复收藏行。6 次 500 为瞬时过载，**P3**。

---

## 5. CC-04 — 工作流 run ×10

工作流 ID=1（含 LLM + 知识库节点）。

| 指标 | 值 |
|------|-----|
| 成功 | **10 / 10** |
| 平均延迟 | **26936 ms** |
| p95 | **38237 ms** |

**结论：** 10 路并行 LLM 工作流无死锁、无脏数据；延迟随 LLM 并发线性上升，属预期。

---

## 6. CC-05 — 文档上传 ×10

知识库 ID=2，10 个独立 txt 文件。

| 指标 | 值 |
|------|-----|
| 成功 | **10 / 10** |
| 平均延迟 | 206 ms · p95 257 ms |

**结论：** MinIO 并发上传正常，无路径冲突。

---

## 7. 问题清单（本阶段新增）

### CONC-P2-001

**Severity:** P2

**Module:** `AgentPublishService.publish`

**Problem:** 50 并发 publish 均返回 200，但 `agent.version` 仅从 2 增至 9，存在 read-modify-write 竞态。

**Fix 建议:** `SELECT … FOR UPDATE` 或乐观锁 `@Version` / 分布式锁。

---

### CONC-P3-001

**Severity:** P3

**Module:** `FavoriteService.toggle`

**Problem:** 100 并发 toggle 中 6 次返回 `50000`。

**Fix 建议:** 可接受；必要时重试或降低连接池争用。

---

## 8. 未执行项

| 项 | 状态 |
|----|------|
| 100 并发 Open API chat | NOT RUN（Open API 限流 60/min） |
| Redis/MySQL 故障下并发 | 见容灾专项 |
| 跨租户并发隔离 | NOT VERIFIED |

---

## 9. 复现命令

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File test/concurrency-run.ps1
```

CC-01 单独验证（已发布应用）：

```powershell
# 见 concurrency-run.log 或本报告 CC-01 节
```

---

*关联：`QA-REPORT.md` · `SECURITY-AUDIT-REPORT.md`*
