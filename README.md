# Spring Boot 3 News Reader

## 本地重建与启动流程

### 1. 构建 Vue 前端（在 frontend/ 下）
```bash
cd /Users/bytedance/Documents/Codes/local-news-harvester/frontend
npm install --legacy-peer-deps --ignore-scripts
npm run build
```

说明：
- 这个前端依赖较旧，`--legacy-peer-deps` 用来绕过依赖冲突。
- `--ignore-scripts` 用来跳过旧版依赖里的二进制构建脚本（如 `grpc`/`chromedriver`）。
- 构建产物输出在 `frontend/dist/`。

### 2. 启动 Spring Boot 后端（根目录）
```bash
cd /Users/bytedance/Documents/Codes/local-news-harvester
SPRING_DOCKER_COMPOSE_ENABLED=false ./mvnw -DskipTests spring-boot:run
```

说明：
- 当前项目启用了 Spring Boot Docker Compose 集成，如果本机没有可用 Docker，建议临时关闭。
- 后端默认端口：`http://localhost:8080`

### 3. 启动 Vue 前端（另开终端）
```bash
cd /Users/bytedance/Documents/Codes/local-news-harvester/frontend
npm run dev
```

说明：
- 已将前端开发端口固定为 `8082`（见 `frontend/package.json`）。
- 前端地址：`http://localhost:8082`

### 清空数据库
```bash
cd /Users/bytedance/Documents/Codes/local-news-harvester
docker compose down -v
docker compose up -d
```