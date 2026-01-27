package com.example.springboot3newsreader.services.webadapters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.springboot3newsreader.models.NewsArticle;

@Component
@Order(1)
public class AieraWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("aiera.com.cn");
  }

  @Override
  public List<NewsArticle> previewOnly(String siteUrl, String sourceName) throws Exception {
    // 新智元首页列表包含标题/首图/发布时间，直接解析首页列表
    Document home = fetchDocument(siteUrl);
    Elements items = home.select(".entries article.entry-card");
    List<NewsArticle> results = new ArrayList<>();
    for (Element item : items) {
      Element titleLink = item.selectFirst("h2.entry-title a");
      if (titleLink == null) {
        continue;
      }
      String url = titleLink.absUrl("href");
      if (url == null || url.isBlank()) {
        url = titleLink.attr("href");
      }
      String title = titleLink.text();
      if (title == null || title.isBlank()) {
        continue;
      }

      // 首图
      Element img = item.selectFirst(".ct-media-container img");
      String thumb = null;
      if (img != null) {
        thumb = img.absUrl("src");
        if (thumb == null || thumb.isBlank()) {
          thumb = img.attr("src");
        }
      }

      // 发布时间（ISO 8601）
      Element time = item.selectFirst("time.ct-meta-element-date[datetime]");
      String publishedAt = time != null ? time.attr("datetime") : null;

      NewsArticle a = new NewsArticle();
      a.setSourceURL(url);
      a.setSourceName(sourceName);
      a.setTitle(title.trim());
      a.setPublishedAt(publishedAt);
      a.setTumbnailURL(thumb);
      a.setScrapedAt(Instant.now().toString());
      results.add(a);
      if (results.size() >= MAX_ARTICLES) {
        break;
      }
    }
    System.out.println("[preview] aiera items: " + results.size());
    return results;
  }

  @Override
  public List<NewsArticle> parseOnly(String siteUrl, String sourceName) throws Exception {
    // 先快返，正文信息后续再异步补
    return previewOnly(siteUrl, sourceName);
  }

  @Override
  public String fetchThumbnailUrl(String articleUrl) throws Exception {
    Document doc = fetchDocument(articleUrl);
    Element img = doc.selectFirst(".post-thumbnail img, .featured-image img, .entry-content img");
    if (img != null) {
      String best = bestImageSrc(img);
      if (best != null && !best.isBlank()) {
        return best;
      }
    }
    return super.fetchThumbnailUrl(articleUrl);
  }
}
