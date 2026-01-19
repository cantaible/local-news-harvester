package com.example.springboot3newsreader.services;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.NewsArticle;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class RssIngestService {

  @Autowired
  private NewsArticleService newsArticleService;

  // 解析 RSS 并批量保存为 NewsArticle
  public List<NewsArticle> ingest(String rssUrl) throws Exception {
    URL url = new URL(rssUrl);
    // 拉取 RSS XML
    InputStream input = url.openStream();

    // 用 Rome 解析 RSS
    SyndFeedInput inputFeed = new SyndFeedInput();
    SyndFeed feed = inputFeed.build(new XmlReader(input));

    // 取 RSS 标题作为来源名
    String sourceName = feed.getTitle() != null ? feed.getTitle() : "Unknown";
    List<NewsArticle> articles = new ArrayList<>();

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
      if (entry.getDescription() != null) {
        String desc = entry.getDescription().getValue();
        a.setSummary(desc.length() > 100 ? desc.substring(0, 100) : desc);
      }

      // tags / thumbnail / rawContent 可留空
      articles.add(a);
    }

    // 批量写入数据库
    return newsArticleService.saveAll(articles);
  }
}
