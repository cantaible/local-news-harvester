# Twitter 接入计划

## 目标
- 支持通过 RapidAPI 抓取指定 Twitter/X 用户的最新推文。
- 尽量复用现有 `FeedItem`、`NewsArticle`、搜索、去重和入库逻辑。
- 不影响当前 RSS / Web 功能的稳定运行。

## 设计原则
- 一位用户 = 一条 `FeedItem`
- 新增 `sourceType=TWITTER`
- `url` 继续使用 `https://x.com/{username}`
- `NewsArticle` 不重构，只做字段映射
- Twitter 默认独立开关、独立调度
- 首版尽量不改表结构
- `/api/newsarticles/refresh` 包含 Twitter

## 兼容性要求
- 不修改现有 RSS / WEB 行为
- 不改变现有 `/api/newsarticles/search` 返回结构
- 不复用 `etag` / `lastModified` 存 Twitter 状态
- `/feeds/preview` 继续仅支持 `WEB`
- 首版不要求新增 `FeedItem` 字段

## 数据方案
### FeedItem
- `name`: 例如 `X @realDonaldTrump`
- `sourceType`: `TWITTER`
- `url`: 例如 `https://x.com/realDonaldTrump`

首版只复用现有字段，不新增 `configJson` / `stateJson`。

### NewsArticle 映射
- `title`: 推文正文截断
- `summary`: 推文纯文本
- `sourceURL`: tweet 永久链接
- `sourceName`: feed 名称
- `publishedAt`: tweet 发布时间 UTC
- `scrapedAt`: 当前抓取时间
- `tags`: hashtags + `twitter`
- `tumbnailURL`: 首图或视频封面
- `rawContent`: tweet 原始 JSON

## 抓取流程
以 RapidAPI 第三方 API 为准，按 provider 适配，不直接把 provider 响应泄漏到业务层。

以 `twitter241` 类接口为例：
1. 从 `url` 提取 `username`
2. 调 `GET /user`，用 `username` 换取 `userId/rest_id`
3. 调 `GET /user-tweets`，按 `userId/rest_id` 拉取推文
4. 过滤 reply、retweet、无效项
5. 映射为 `NewsArticle`
6. 复用现有去重和入库逻辑

说明：
- 首版缓存 `userId/rest_id`，避免每次多一次用户解析请求
- 首版不保存分页 `cursor`
- 首版单用户每次抓取最新 40 条
- 如后续配额或性能不足，再增加分页增量状态

## 内容规则
- 不保留 reply
- 不保留 retweet / repost
- 不保留 quote tweet
- 不保留 pinned tweet
- 保留原创 tweet
- 保留无图纯文本 tweet

## 风险控制
- Twitter 抓取失败不得影响 RSS / WEB
- 单个账号失败不影响其他账号
- 对 RapidAPI 429 / 超时 / 结构变更做隔离处理
- Provider 实现可替换，不在业务层写死 `twitter241`
- 429 或超时采用有限重试，建议最多 2 次，指数退避
- `enabled=false` 的 Twitter feed 不参与 refresh

## 分阶段实施
### Phase 1
- 支持 `TWITTER` feed
- 基于 RapidAPI 抓指定用户最新一页
- 仅靠 `sourceURL` 去重
- 纳入现有 `/api/newsarticles/refresh`
- 不新增数据库字段
- 缓存 `userId/rest_id`
- 固定内容过滤规则

### Phase 2
- 视需要增加 `lastSeenTweetId` 或分页状态
- 支持首次多页回填
- 增强限流、重试和错误记录

## 暂不做
- 关键词搜索抓取
- 多用户合并为一条 feed
- replies / likes / followers 抓取
- 改造现有搜索接口
- 改造现有 WEB 预览接口
