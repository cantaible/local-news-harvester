package com.example.springboot3newsreader.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.NewsCategory;

@Service
public class IngestPipelineService {

  @Autowired
  private RssIngestService rssIngestService;
  @Autowired
  private WebIngestService webIngestService;

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
      return rssIngestService.ingest(feed.getUrl(), feed.getName(), category);
    }
    if ("WEB".equals(type)) {
      return webIngestService.ingest(feed.getUrl(), feed.getName(), category);
    }
    return new ArrayList<>();
  }

  public List<NewsArticle> ingestAll(List<FeedItem> feeds) {
    List<NewsArticle> all = new ArrayList<>();
    if (feeds == null) {
      return all;
    }
    for (FeedItem feed : feeds) {
      try {
        all.addAll(ingestFeed(feed));
      } catch (Exception e) {
        // ignore single feed failure
      }
    }
    return all;
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
