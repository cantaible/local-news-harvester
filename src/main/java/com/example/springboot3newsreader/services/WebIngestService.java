package com.example.springboot3newsreader.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.ApiResponse;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.NewsCategory;
import com.example.springboot3newsreader.models.ThumbnailTask;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import com.example.springboot3newsreader.repositories.ThumbnailTaskRepository;
import com.example.springboot3newsreader.services.webadapters.WebAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WebIngestService {

  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @Autowired
  private ThumbnailTaskRepository thumbnailTaskRepository;
  @Autowired
  private NewsArticleDedupeService newsArticleDedupeService;

  @Autowired
  private List<WebAdapter> webAdapters;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public List<NewsArticle> parseOnly(String siteUrl, String sourceName) throws Exception {
    return parseOnly(siteUrl, sourceName, null);
  }

  public List<NewsArticle> parseOnly(String siteUrl, String sourceName, NewsCategory category)
    throws Exception {
    System.out.println("[web] parse start: " + siteUrl);
    // 选择匹配的适配器（按 @Order 排序，优先更具体的源）
    for (WebAdapter adapter : webAdapters) {
      if (adapter.supports(siteUrl)) {
        System.out.println("[web] adapter selected: " + adapter.getClass().getSimpleName());
        List<NewsArticle> articles = adapter.parseOnly(siteUrl, sourceName);
        if (category != null && articles != null) {
          for (NewsArticle a : articles) {
            a.setCategory(category);
          }
        }
        return articles;
      }
    }
    throw new IllegalStateException("no web adapter found for " + siteUrl);
  }

  public List<NewsArticle> previewOnly(String siteUrl, String sourceName) throws Exception {
    System.out.println("[web] preview start: " + siteUrl);
    // 预览模式：只抓首页链接，不抓文章正文
    for (WebAdapter adapter : webAdapters) {
      if (adapter.supports(siteUrl)) {
        System.out.println("[web] adapter selected: " + adapter.getClass().getSimpleName());
        return adapter.previewOnly(siteUrl, sourceName);
      }
    }
    throw new IllegalStateException("no web adapter found for " + siteUrl);
  }

  public List<NewsArticle> ingest(String siteUrl, String sourceName) throws Exception {
    return ingest(siteUrl, sourceName, null);
  }

  public List<NewsArticle> ingest(String siteUrl, String sourceName, NewsCategory category)
    throws Exception {
    System.out.println("[web] ingest start: " + siteUrl);
    // 先解析，再批量入库
    List<NewsArticle> articles = parseOnly(siteUrl, sourceName, category);
    System.out.println("[web] parsed articles: " + articles.size());
    int before = articles.size();
    articles = newsArticleDedupeService.filterNewArticles(articles);
    System.out.println("[web] after dedupe: " + articles.size()
      + " (removed " + (before - articles.size()) + ")");
    for (NewsArticle a : articles) {
      // 已有首图则保留，否则置空交给异步补图
      if (a.getTumbnailURL() == null || a.getTumbnailURL().isBlank()) {
        a.setTumbnailURL(null);
      }
    }
    System.out.println("[web] saving articles...");
    List<NewsArticle> saved = newsArticleRepository.saveAll(articles);
    System.out.println("[web] saved: " + saved.size());

    List<ThumbnailTask> tasks = new java.util.ArrayList<>();
    for (NewsArticle a : saved) {
      if (a.getTumbnailURL() != null && !a.getTumbnailURL().isBlank()) {
        continue;
      }
      ThumbnailTask task = new ThumbnailTask();
      task.setArticleId(a.getId());
      task.setArticleUrl(a.getSourceURL());
      task.setStatus("WAITING");
      task.setAttempts(0);
      task.setCreatedAt(java.time.LocalDateTime.now());
      task.setUpdatedAt(java.time.LocalDateTime.now());
      tasks.add(task);
    }
    if (!tasks.isEmpty()) {
      System.out.println("[web] creating thumbnail tasks: " + tasks.size());
      thumbnailTaskRepository.saveAll(tasks);
    } else {
      System.out.println("[web] no thumbnail tasks created");
    }

    return saved;
  }

  @Async
  public void ingestAsync(String siteUrl, String sourceName) {
    try {
      // 异步抓取与保存，避免阻塞调用方
      System.out.println("[web] ingest async start: " + siteUrl);
      List<NewsArticle> saved = ingest(siteUrl, sourceName);
      ApiResponse<List<NewsArticle>> resp = new ApiResponse<>(200, "ok", saved);
      String json = objectMapper.writeValueAsString(resp);
      System.out.println("[ingest] preview-format response: " + json);
      System.out.println("[web] ingest async done: " + siteUrl);
    } catch (Exception e) {
      System.out.println("[web] ingest async failed: " + siteUrl);
      e.printStackTrace();
    }
  }
}
