package com.example.springboot3newsreader.services.webadapters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.springboot3newsreader.models.NewsArticle;

@Component
@Order(2)
public class JiqizhixinWebAdapter extends BaseWebAdapter {

  private static final Pattern ONCLICK_OPEN_PATTERN =
    Pattern.compile("window\\.open\\(['\\\"]([^'\\\"]+)['\\\"]");

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("jiqizhixin.com");
  }

  @Override
  public List<NewsArticle> previewOnly(String siteUrl, String sourceName) throws Exception {
    // 机器之心首页“文章库”列表里包含标题/标签/时间/首图
    Document home = fetchDocument(siteUrl);
    Elements items = home.select(".home__center-left__list .home__center-card");
    List<NewsArticle> results = new ArrayList<>();
    for (Element item : items) {
      Element titleEl = item.selectFirst(".home__article-item__title");
      if (titleEl == null) {
        continue;
      }
      String title = titleEl.text();
      if (title == null || title.isBlank()) {
        continue;
      }

      String url = extractArticleUrl(item, siteUrl);

      // 标签
      List<String> tagList = new ArrayList<>();
      for (Element tag : item.select(".home__article-item__tag-item")) {
        String t = tag.text();
        if (t != null && !t.isBlank()) {
          tagList.add(t.trim());
        }
      }
      String tags = tagList.isEmpty() ? null : String.join(",", tagList);

      // 时间（首页为“今天/01月25日”等）
      String publishedAt = null;
      Element timeEl = item.selectFirst(".home__article-item__time");
      if (timeEl != null) {
        String t = timeEl.text();
        if (t != null && !t.isBlank()) {
          publishedAt = t.trim();
        }
      }

      // 首图
      Element img = item.selectFirst(".home__article-item__right img");
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
      a.setTags(tags);
      a.setPublishedAt(publishedAt);
      a.setTumbnailURL(thumb);
      a.setScrapedAt(Instant.now().toString());
      results.add(a);
      if (results.size() >= MAX_ARTICLES) {
        break;
      }
    }
    System.out.println("[preview] jiqizhixin items: " + results.size());
    return results;
  }

  @Override
  public List<NewsArticle> parseOnly(String siteUrl, String sourceName) throws Exception {
    // 先快返，正文信息后续再异步补
    return previewOnly(siteUrl, sourceName);
  }

  private String extractArticleUrl(Element item, String siteUrl) {
    // 1) 优先找显式的链接
    Element link = item.selectFirst("a[href]");
    if (link != null) {
      String abs = link.absUrl("href");
      if (abs != null && !abs.isBlank()) {
        return abs;
      }
      String raw = link.attr("href");
      if (raw != null && !raw.isBlank()) {
        return raw;
      }
    }

    // 2) 再尝试从 onclick 中解析 window.open('URL')
    for (Element el : item.select("[onclick]")) {
      String onclick = el.attr("onclick");
      if (onclick == null || onclick.isBlank()) {
        continue;
      }
      Matcher m = ONCLICK_OPEN_PATTERN.matcher(onclick);
      if (m.find()) {
        String url = m.group(1);
        if (url != null && !url.isBlank()) {
          return url;
        }
      }
    }

    // 3) 兜底为空（该站点可能使用 JS 动态注入链接）
    return null;
  }
}
