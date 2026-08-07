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
