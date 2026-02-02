package com.example.springboot3newsreader.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.NewsCategory;
import com.example.springboot3newsreader.models.ThumbnailTask;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import com.example.springboot3newsreader.repositories.ThumbnailTaskRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;

@Service
public class RssIngestService {

  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @Autowired
  private NewsArticleDedupeService newsArticleDedupeService;
  @Autowired
  private ThumbnailTaskRepository thumbnailTaskRepository;

  @Value("${app.feature.thumbnail-task.enabled:true}")
  private boolean thumbnailTaskEnabled;

  public List<NewsArticle> parseOnly(String rssUrl, String sourceName) throws Exception {
    return parseOnly(rssUrl, sourceName, null);
  }

  public List<NewsArticle> parseOnly(String rssUrl, String sourceName, NewsCategory category)
      throws Exception {
    System.out.println("[rss] parse start: " + rssUrl);
    // 这里复制 ingest 里的解析逻辑
    // 唯一区别：最后 return articles，不要 saveAll
    URL url = new URL(rssUrl);
    // 拉取 RSS XML
    InputStream input = url.openStream();

    // 用 Rome 解析 RSS（部分源带 DOCTYPE，会被默认安全策略拦截）
    String xml = readToString(input);
    xml = stripDoctype(xml);
    SyndFeedInput inputFeed = new SyndFeedInput();
    SyndFeed feed = inputFeed.build(new StringReader(xml));

    // 取 RSS 标题作为来源名
    List<NewsArticle> articles = new ArrayList<>();
    System.out.println("[rss] entries: " + feed.getEntries().size());

    for (SyndEntry entry : feed.getEntries()) {
      NewsArticle a = new NewsArticle();
      // 标题和原文链接
      a.setTitle(entry.getTitle());
      a.setSourceURL(entry.getLink());
      a.setSourceName(sourceName);

      // 发布时间（无则用当前时间）
      String publishedAt = entry.getPublishedDate() != null
          ? entry.getPublishedDate().toInstant().toString()
          : Instant.now().toString();
      a.setPublishedAt(publishedAt);

      // 抓取时间
      a.setScrapedAt(Instant.now().toString());

      // 摘要（最多 100 字符）
      // 摘要与图片提取
      String descriptionHtml = null;
      if (entry.getDescription() != null) {
        descriptionHtml = entry.getDescription().getValue();
        // 保存完整 HTML 到 rawContent 供详情页展示
        a.setRawContent(descriptionHtml);
      }

      String imgFromDesc = null;
      if (descriptionHtml != null && !descriptionHtml.isEmpty()) {
        try {
          Document doc = Jsoup.parse(descriptionHtml);
          // 1. 提取描述中的第一张图片
          Element img = doc.selectFirst("img");
          if (img != null) {
            String src = img.attr("src");
            if (src != null && !src.isEmpty()) {
              imgFromDesc = src;
            }
          }
          // 2. 清理 HTML 标签作为摘要
          String cleanText = doc.text();
          if (cleanText != null) {
            a.setSummary(cleanText.length() > 200 ? cleanText.substring(0, 200) + "..." : cleanText);
          }
        } catch (Exception e) {
          // 降级处理
          a.setSummary(descriptionHtml.length() > 100 ? descriptionHtml.substring(0, 100) : descriptionHtml);
        }
      }

      // 缩略图：优先使用条目图片，其次使用文章页面 OG/Twitter 图
      String thumbnailUrl = null;
      if (entry.getEnclosures() != null) {
        for (var enclosure : entry.getEnclosures()) {
          if (enclosure.getType() != null && enclosure.getType().startsWith("image/")) {
            thumbnailUrl = enclosure.getUrl();
            break;
          }
        }
      }
      if (thumbnailUrl == null && imgFromDesc != null) {
        thumbnailUrl = imgFromDesc;
      }
      if (thumbnailUrl == null && feed.getImage() != null) {
        thumbnailUrl = feed.getImage().getUrl();
      }

      if (thumbnailUrl != null && shouldIgnoreRssThumbnail(rssUrl, sourceName, thumbnailUrl)) {
        thumbnailUrl = null;
      }

      if (thumbnailUrl != null) {
        a.setTumbnailURL(thumbnailUrl);
      }
      if (category != null) {
        a.setCategory(category);
      }

      // tags / thumbnail / rawContent 可留空
      articles.add(a);
    }
    return articles;
  }

  // 解析 RSS 并批量保存为 NewsArticle
  public List<NewsArticle> ingest(String rssUrl, String sourceName) throws Exception {
    return ingest(rssUrl, sourceName, null);
  }

  public List<NewsArticle> ingest(String rssUrl, String sourceName, NewsCategory category)
      throws Exception {
    System.out.println("[rss] ingest start: " + rssUrl);
    List<NewsArticle> articles = parseOnly(rssUrl, sourceName, category);
    System.out.println("[rss] parsed articles: " + articles.size());
    // 批量写入数据库
    int before = articles.size();
    articles = newsArticleDedupeService.filterNewArticles(articles);
    System.out.println("[rss] after dedupe: " + articles.size()
        + " (removed " + (before - articles.size()) + ")");
    System.out.println("[rss] saving articles...");
    List<NewsArticle> saved = newsArticleRepository.saveAll(articles);

    List<ThumbnailTask> tasks = new ArrayList<>();
    if (thumbnailTaskEnabled) {
      for (NewsArticle a : saved) {
        if (a.getTumbnailURL() != null && !a.getTumbnailURL().isBlank()) {
          continue;
        }
        ThumbnailTask task = new ThumbnailTask();
        task.setArticleId(a.getId());
        task.setArticleUrl(a.getSourceURL());
        task.setStatus("WAITING");
        task.setAttempts(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        tasks.add(task);
      }
      if (!tasks.isEmpty()) {
        System.out.println("[rss] creating thumbnail tasks: " + tasks.size());
        thumbnailTaskRepository.saveAll(tasks);
      } else {
        System.out.println("[rss] no thumbnail tasks created");
      }
    } else {
      System.out.println("[rss] skipping thumbnail tasks (disabled by config)");
    }

    return saved;
  }

  // 异步版本：不阻塞调用方
  @Async
  public void ingestAsync(String rssUrl, String sourceName) {
    try {
      System.out.println("[rss] ingest async start: " + rssUrl);
      List<NewsArticle> saved = ingest(rssUrl, sourceName);
      System.out.println("[rss] ingest async done, saved: " + saved.size());
    } catch (Exception e) {
      System.out.println("[rss] ingest async failed: " + rssUrl);
      e.printStackTrace();
    }
  }

  private String readToString(InputStream input) throws Exception {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append('\n');
        if (sb.length() > 5_000_000) {
          break;
        }
      }
    }
    return sb.toString();
  }

  private String stripDoctype(String xml) {
    if (xml == null) {
      return null;
    }
    return xml.replaceFirst("(?is)<!DOCTYPE[^>]*>", "");
  }

  private boolean shouldIgnoreRssThumbnail(String rssUrl, String sourceName, String thumbnailUrl) {
    if (thumbnailUrl == null) {
      return true;
    }
    String lower = thumbnailUrl.toLowerCase();
    if (lower.contains("logo") || lower.contains("favicon") || lower.contains("icon")) {
      return true;
    }
    if (rssUrl != null && rssUrl.toLowerCase().contains("techcrunch.com")) {
      if (lower.contains("tc-logo") || (lower.contains("techcrunch") && lower.contains("logo"))) {
        return true;
      }
    }
    return false;
  }

  // fetchOgImage 已迁移到异步补图任务中，这里不再同步抓取

}
