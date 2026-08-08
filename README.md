# NovaFlow AI

企业级 AI Agent 开发与运行平台。

## 技术栈

- **后端**：Java 21、Spring Boot 3、Sa-Token、MyBatis-Flex
- **前端**：Vue 3、TypeScript、Vite、Ant Design Vue
- **基础设施**：MySQL、Redis、MinIO、Qdrant

## 项目结构

```
NovaFlow-AI/
├── docs/                  # 设计文档（PRD、架构、数据库）
├── novaflow-common/       # 公共模块
├── novaflow-security/     # 安全认证
├── novaflow-dashboard/    # 工作台 API
├── novaflow-server/       # 启动模块
├── novaflow-web/          # Vue 3 前端
└── docker-compose.yml     # 基础设施
```

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

本地服务连接信息：

| 服务 | 控制台 / 地址 | 账号 | 密码 / Key |
|------|---------------|------|------------|
| **MySQL** | `localhost:3306` | `root` | `root` |
| **Redis** | `localhost:6379` | — | `redis123` |
| **MinIO** | API `localhost:9000`，控制台 [http://localhost:9001](http://localhost:9001) | `minioadmin` | `minioadmin123` |
| **Qdrant** | 控制台 [http://localhost:6333/dashboard](http://localhost:6333/dashboard) | — | —（本地默认无鉴权） |

> **Qdrant 说明**：后端通过 **gRPC `localhost:6334`** 连接（见 `application.yml` 的 `novaflow.qdrant`），6333 仅为 REST / Dashboard。本地 Docker 未启用 API Key，两列填「—」即可；生产环境可在 Qdrant 侧设置 `QDRANT__SERVICE__API_KEY` 后，用环境变量 `QDRANT_API_KEY` 传入后端（可选）。

知识库 RAG 功能需同时启动 **MinIO + Qdrant**，并在模型中心配置 Embedding 模型。

> **图片型 PDF**（如 PPT 导出为每页一张图）当前暂不支持 OCR，请上传 **PPTX 源文件**或**可搜索文字的 PDF**。

### 2. 启动后端

```bash
mvn clean package -DskipTests
java -jar novaflow-server/target/novaflow-server-0.1.0-SNAPSHOT.jar
```

后端地址：http://localhost:8080

### 3. 启动前端

```bash
cd novaflow-web
npm install
npm run dev
```

前端地址：http://localhost:3000

## API

- `GET /api/v1/health` — 健康检查
- `GET /api/v1/dashboard/overview` — Dashboard 数据

## 文档

- [PRD](docs/PRD.md)
- [系统架构设计](docs/系统架构设计.md)
- [数据库设计](docs/数据库设计.md)
