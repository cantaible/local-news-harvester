package com.example.springboot3newsreader.services.webadapters;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.springboot3newsreader.models.NewsArticle;

@Component
@Order(0)
public class QbitaiWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("qbitai.com");
  }

  @Override
  public List<NewsArticle> previewOnly(String siteUrl, String sourceName) throws Exception {
    // 量子位首页包含标题/摘要/首图，直接从首页列表提取
    Document home = fetchDocument(siteUrl);
    Elements items = home.select(".article_list .picture_text");
    List<NewsArticle> results = new ArrayList<>();
    for (Element item : items) {
      Element titleLink = item.selectFirst(".text_box h4 a");
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

      // 摘要：取 text_box 下第一个非空 <p>
      String summary = null;
      Elements ps = item.select(".text_box p");
      for (Element p : ps) {
        String text = p.text();
        if (text != null && !text.isBlank()) {
          summary = text;
          break;
        }
      }

      // 首图：取列表图片
      Element img = item.selectFirst(".picture img");
      String thumb = null;
      if (img != null) {
        thumb = img.absUrl("src");
        if (thumb == null || thumb.isBlank()) {
          thumb = img.attr("src");
        }
      }

      NewsArticle a = new NewsArticle();
      a.setSourceURL(url);
      a.setSourceName(sourceName);
      a.setTitle(title.trim());
      a.setSummary(summary);
      a.setTumbnailURL(thumb);
      a.setScrapedAt(Instant.now().toString());
      results.add(a);
      if (results.size() >= MAX_ARTICLES) {
        break;
      }
    }
    System.out.println("[preview] qbitai items: " + results.size());
    return results;
  }

  @Override
  public List<NewsArticle> parseOnly(String siteUrl, String sourceName) throws Exception {
    // 先走首页快速预览，再针对缺失字段补抓详情页
    List<NewsArticle> previews = previewOnly(siteUrl, sourceName);
    if (previews.isEmpty()) {
      return previews;
    }
    ExecutorService pool = Executors.newFixedThreadPool(PARSE_THREADS);
    try {
      List<Future<NewsArticle>> tasks = new ArrayList<>();
      for (NewsArticle preview : previews) {
        if (!needsDetailFetch(preview)) {
          tasks.add(CompletableFuture.completedFuture(preview));
          continue;
        }
        tasks.add(pool.submit(new Callable<NewsArticle>() {
          @Override
          public NewsArticle call() {
            try {
              return enrichFromDetail(preview);
            } catch (Exception e) {
              System.out.println("[preview] qbitai detail error: " + preview.getSourceURL());
              e.printStackTrace();
              return preview;
            }
          }
        }));
      }
      List<NewsArticle> enriched = new ArrayList<>();
      for (Future<NewsArticle> task : tasks) {
        try {
          enriched.add(task.get());
        } catch (Exception e) {
          // ignore individual failures
        }
      }
      return enriched;
    } finally {
      pool.shutdown();
    }
  }

  private boolean needsDetailFetch(NewsArticle article) {
    return isBlank(article.getPublishedAt())
      || isBlank(article.getRawContent())
      || isBlank(article.getTags());
  }

  private boolean isBlank(String val) {
    return val == null || val.isBlank();
  }

  private NewsArticle enrichFromDetail(NewsArticle article) throws Exception {
    String url = article.getSourceURL();
    if (url == null || url.isBlank()) {
      return article;
    }
    Document doc = fetchDocument(url);

    if (isBlank(article.getPublishedAt())) {
      String date = textOrNull(doc.selectFirst(".article_info .date"));
      String time = textOrNull(doc.selectFirst(".article_info .time"));
      String publishedAt = parseDateTime(date, time);
      if (!isBlank(publishedAt)) {
        article.setPublishedAt(publishedAt);
      }
    }

    if (isBlank(article.getSummary())) {
      String summary = textOrNull(doc.selectFirst(".zhaiyao"));
      if (!isBlank(summary)) {
        article.setSummary(trimTo(summary, 200));
      }
    }

    if (isBlank(article.getTags())) {
      Elements tags = doc.select(".tags a");
      if (!tags.isEmpty()) {
        List<String> values = new ArrayList<>();
        for (Element tag : tags) {
          String t = tag.text();
          if (t != null && !t.isBlank()) {
            values.add(t.trim());
          }
        }
        if (!values.isEmpty()) {
          article.setTags(String.join(",", values));
        }
      }
    }

    if (isBlank(article.getRawContent())) {
      Element body = doc.selectFirst(".article");
      if (body != null) {
        body.select(".article_info, .zhaiyao, .tags, .line_font, script, style").remove();
        Elements nodes = body.select("h1,h2,h3,h4,h5,h6,p,li,blockquote");
        StringBuilder sb = new StringBuilder();
        for (Element node : nodes) {
          String text = node.text();
          if (text != null && !text.isBlank()) {
            if (sb.length() > 0) {
              sb.append('\n');
            }
            sb.append(text.trim());
          }
        }
        if (sb.length() > 0) {
          article.setRawContent(trimTo(sb.toString(), 4000));
        }
      }
    }

    return article;
  }

  private String parseDateTime(String date, String time) {
    if ((date == null || date.isBlank()) && (time == null || time.isBlank())) {
      return null;
    }
    String datePart = date != null ? date.trim() : "";
    String timePart = time != null ? time.trim() : "";
    try {
      if (!datePart.isBlank() && !timePart.isBlank()) {
        LocalDateTime dt = LocalDateTime.parse(datePart + " " + timePart,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
      if (!datePart.isBlank()) {
        LocalDateTime dt = LocalDateTime.parse(datePart + " 00:00:00",
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
    } catch (Exception e) {
      // ignore invalid date
    }
    return null;
  }

  @Override
  public String fetchThumbnailUrl(String articleUrl) throws Exception {
    Document doc = fetchDocument(articleUrl);
    Element img = doc.selectFirst(".pgc-img img");
    if (img == null) {
      img = doc.selectFirst(".article img");
    }
    if (img != null) {
      String best = bestImageSrc(img);
      if (best != null && !best.isBlank()) {
        return best;
      }
    }
    return super.fetchThumbnailUrl(articleUrl);
  }
}
