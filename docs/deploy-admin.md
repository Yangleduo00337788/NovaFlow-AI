# 平台后台独立部署（admin 子域）

NovaFlow 支持将**平台运营后台**与**租户 Studio**拆分为两个前端部署：

| 部署 | 构建命令 | 访问域名示例 | 路由前缀 |
|------|----------|--------------|----------|
| Studio（租户） | `npm run build` 或 `npm run build:studio` | `app.novaflow.ai` | `/dashboard`、`/agent`… |
| Platform Admin | `npm run build:platform` | `admin.novaflow.ai` | `/dashboard`、`/tenants`…（无 `/platform` 前缀） |

## 自动识别

未设置 `VITE_DEPLOY_SCOPE` 时，前端会根据域名自动判断：

- `admin.*` 或主机名 `admin` → 平台后台模式
- 其他 → Studio 模式

也可在 `.env.platform` / `.env.studio` 中显式设置 `VITE_DEPLOY_SCOPE=platform|tenant`。

## Nginx 示例

```nginx
# Studio
server {
    listen 443 ssl http2;
    server_name app.novaflow.ai;
    root /var/www/novaflow-studio/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}

# Platform Admin（admin 子域）
server {
    listen 443 ssl http2;
    server_name admin.novaflow.ai;
    root /var/www/novaflow-platform/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 构建产物

```bash
cd novaflow-web
npm ci
npm run build:studio    # 输出 dist/ → 部署到 app 子域
npm run build:platform  # 输出 dist/ → 部署到 admin 子域
```

两个构建共用同一后端 API（`https://api.novaflow.ai` 或同域 `/api` 反代），平台账号登录：

- Studio 集成模式：`/platform/login`
- Admin 独立部署：`/login`

演示账号：`platform@novaflow.ai` / `Platform123!`

## 本地验证

```bash
# 平台独立模式（路由为 /dashboard 而非 /platform/dashboard）
npm run dev -- --mode platform
```

或使用 hosts 将 `admin.localhost` 指向 127.0.0.1，访问 `http://admin.localhost:3000`。
