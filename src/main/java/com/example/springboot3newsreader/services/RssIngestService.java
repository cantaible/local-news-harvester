package com.example.springboot3newsreader.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class RssIngestService {

  private static final Pattern OG_IMAGE_PATTERN =
    Pattern.compile("<meta\\s+property=[\"']og:image[\"']\\s+content=[\"']([^\"']+)[\"']",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern TWITTER_IMAGE_PATTERN =
    Pattern.compile("<meta\\s+name=[\"']twitter:image[\"']\\s+content=[\"']([^\"']+)[\"']",
      Pattern.CASE_INSENSITIVE);

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  public List<NewsArticle> parseOnly(String rssUrl, String sourceName) throws Exception {
    // 这里复制 ingest 里的解析逻辑
    // 唯一区别：最后 return articles，不要 saveAll
    URL url = new URL(rssUrl);
    // 拉取 RSS XML
    InputStream input = url.openStream();

    // 用 Rome 解析 RSS
    SyndFeedInput inputFeed = new SyndFeedInput();
    SyndFeed feed = inputFeed.build(new XmlReader(input));

    // 取 RSS 标题作为来源名
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
      if (thumbnailUrl == null && entry.getLink() != null) {
        String ogImage = fetchOgImage(entry.getLink());
        if (ogImage != null) {
          thumbnailUrl = ogImage;
        }
      }
      if (thumbnailUrl == null && feed.getImage() != null) {
        thumbnailUrl = feed.getImage().getUrl();
      }
      
      if (thumbnailUrl != null) {
        a.setTumbnailURL(thumbnailUrl);
      }

      // tags / thumbnail / rawContent 可留空
      articles.add(a);
    }
    return articles;
  }


  // 解析 RSS 并批量保存为 NewsArticle
  public List<NewsArticle> ingest(String rssUrl, String sourceName) throws Exception {
    List<NewsArticle> articles = parseOnly(rssUrl, sourceName);
    // 批量写入数据库
    return newsArticleRepository.saveAll(articles);
  }

  // 异步版本：不阻塞调用方
  @Async
  public void ingestAsync(String rssUrl, String sourceName) {
    try {
      ingest(rssUrl, sourceName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String fetchOgImage(String link) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(link);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("User-Agent", "Mozilla/5.0");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream()))) {
        StringBuilder html = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          html.append(line);
          if (html.length() > 200_000) {
            break;
          }
        }
        Matcher ogMatcher = OG_IMAGE_PATTERN.matcher(html);
        if (ogMatcher.find()) {
          return ogMatcher.group(1);
        }
        Matcher twitterMatcher = TWITTER_IMAGE_PATTERN.matcher(html);
        if (twitterMatcher.find()) {
          return twitterMatcher.group(1);
        }
      }
    } catch (Exception e) {
      // ignore parsing errors
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    return null;
  }

  
}
