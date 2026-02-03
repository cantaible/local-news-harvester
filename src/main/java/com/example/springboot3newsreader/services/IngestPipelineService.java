package com.example.springboot3newsreader.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.NewsCategory;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Service
public class IngestPipelineService {

  @Autowired
  private RssIngestService rssIngestService;
  @Autowired
  private WebIngestService webIngestService;

  @Value("${app.feature.web-ingest.enabled:true}")
  private boolean webIngestEnabled;

  public List<NewsArticle> ingestFeed(FeedItem feed) throws Exception {
    if (feed == null || feed.getSourceType() == null) {
      return new ArrayList<>();
    }
    String type = feed.getSourceType();
    NewsCategory category = feed.getCategory();
    if (category == null) {
      category = NewsCategory.UNCATEGORIZED;
    }
    if ("RSS".equals(type)) {
      // Use logic with Etag/Last-Modified support
      return rssIngestService.ingest(feed);
    }
    if ("WEB".equals(type)) {
      if (!webIngestEnabled) {
        System.out.println("Skipping WEB feed ingestion (disabled by config): " + feed.getName());
        return new ArrayList<>();
      }
      return webIngestService.ingest(feed.getUrl(), feed.getName(), category);
    }
    return new ArrayList<>();
  }

  public List<NewsArticle> ingestAll(List<FeedItem> feeds) {
    if (feeds == null || feeds.isEmpty()) {
      return new ArrayList<>();
    }
    // 使用并行流加速抓取 (多线程并发执行)
    // 注意：ArrayList 非线程安全，使用 Collections.synchronizedList 或 collect
    return feeds.parallelStream()
        .flatMap(feed -> {
          try {
            return ingestFeed(feed).stream();
          } catch (Exception e) {
            // ignore failure
            return java.util.stream.Stream.empty();
          }
        })
        .collect(Collectors.toList());
  }

  @Async
  public void ingestFeedAsync(FeedItem feed) {
    try {
      ingestFeed(feed);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
