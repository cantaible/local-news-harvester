# Local News Harvester

## 项目简介
本项目是一个本地新闻聚合与阅读器，后端负责抓取/整理新闻源，前端提供移动端 UI 展示与浏览。

## 功能概览
- 新闻源管理：添加 RSS / Web 类型的源并触发抓取
- 内容聚合：按分类查看、刷新新闻列表
- 图片代理：为来源站点图片提供统一代理接口
- 前端展示：Flutter 应用展示来源列表与文章详情

## 快速开始

### 1. 启动数据库（MariaDB）
```bash
docker compose up -d
```

### 2. 启动后端（Spring Boot）
```bash
SPRING_DOCKER_COMPOSE_ENABLED=false ./mvnw -DskipTests spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 3. 启动 Flutter 前端
```bash
cd flutter_news_application
flutter pub get
flutter run
```

Flutter 默认会请求 `http://localhost:8080` 的 API（见 `flutter_news_application/lib/config/app_config.dart`）。

## 配置说明
- 数据库配置：`src/main/resources/application.properties`
- Docker 环境变量：`.env`（默认账号已配置）
- 关闭 Spring Docker Compose 自动启动：`SPRING_DOCKER_COMPOSE_ENABLED=false`
- CORS 允许的前端地址：`src/main/java/com/example/springboot3newsreader/config/CorsConfig.java`
- Feature Flags (默认为 RSS-Only 模式):
  - `app.feature.web-ingest.enabled=false`: 禁用 Web 页面抓取
  - `app.feature.thumbnail-task.enabled=false`: 禁用后台补图任务

如果不使用 Docker，请自行准备 MariaDB，并确保以下连接信息一致：
- DB 名称：`news_reader`
- 用户名：`reader`
- 密码：`readerpass`

## 数据接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/categories` | 获取全部分类 |
| GET | `/api/categories/{category}/newsarticles` | 按分类获取文章 |
| GET | `/api/newsarticles` | 获取全部文章 |
| GET | `/api/newsarticles/{id}` | 获取单条文章 |
| GET | `/api/newsarticles/refresh` | 从 RSS 刷新文章 |
| POST | `/api/newsarticles/seed` | 插入示例文章 |
| DELETE | `/api/newsarticles/seed` | 删除示例文章 |
| GET | `/api/feeditems` | 获取全部新闻源 |
| GET | `/api/feeditems/{id}` | 获取单条新闻源 |
| POST | `/api/feeditems/seed` | 插入示例新闻源 |
| DELETE | `/api/feeditems/seed` | 删除示例新闻源 |
| POST | `/feeds/new` | 新建新闻源（表单提交） |
| POST | `/feeds/preview` | 预览 Web 类型新闻源 |
| POST | `/admin/clear` | 清空业务表 |
| POST | `/admin/seed-rss` | 批量导入内置 RSS 源 |
| POST | `/api/newsarticles/search` | 高级搜索（Keyword, Sources, Data, Tags） |
| GET | `/api/image?url=...` | 图片代理 |

## 云服务器部署 (Docker)

本项目支持使用 Docker Compose 一键部署后端服务。

### 1. 准备工作
将项目代码推送到 Git 仓库，并在云服务器上拉取代码。
```bash
git clone <your-repo-url>
cd local-news-harvester
```

### 2. 启动服务
在项目根目录下执行以下命令：
```bash
docker compose up -d --build
```
此命令会自动：
- 构建 Spring Boot 后端镜像（利用多阶段构建，无需服务器安装 Maven/JDK）
- 启动 MariaDB 数据库
- 启动 PhpMyAdmin 管理界面
- 启动后端 App 服务

### 3. 访问服务
服务启动后，后端 API 将在 **9090** 端口提供服务：
- API 地址: `http://<server-ip>:9090/api/newsarticles`
- PhpMyAdmin: `http://<server-ip>:8081`

> **注意**：前端代码 (`flutter_news_application/`) 在 Docker 构建过程中会被自动忽略，不会影响服务器构建速度。

