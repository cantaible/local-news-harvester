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
@Order(6)
public class SearchEngineRoundtableWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("seroundtable.com");
  }

  @Override
  protected NewsArticle parseArticle(String url, String sourceName) throws Exception {
    Document doc = fetchDocument(url);
    NewsArticle a = new NewsArticle();
    a.setSourceURL(url);
    a.setSourceName(sourceName);
    a.setScrapedAt(Instant.now().toString());

    String title = firstNonBlank(
      textOrNull(doc.selectFirst("h1")),
      metaContent(doc, "property", "og:title"),
      metaContent(doc, "name", "twitter:title"),
      doc.title()
    );
    String description = firstNonBlank(
      metaContent(doc, "name", "description"),
      metaContent(doc, "property", "og:description")
    );
    String publishedAt = firstNonBlank(
      metaContent(doc, "name", "article:published_time"),
      metaContent(doc, "property", "article:published_time")
    );

    if (isBlank(title)) {
      return null;
    }

    a.setTitle(title.trim());
    a.setPublishedAt(normalizeDate(publishedAt, Instant.now().toString()));

    if (!isBlank(description)) {
      a.setSummary(trimTo(description, 160));
    }

    Element content = doc.selectFirst(".post-body");
    if (content != null) {
      content.select("script, style, iframe, .disqus_thread").remove();
      Elements nodes = content.select("h1,h2,h3,h4,h5,h6,p,li,blockquote");
      StringBuilder sb = new StringBuilder();
      for (Element node : nodes) {
        String text = node.text();
        if (!isBlank(text)) {
          if (sb.length() > 0) {
            sb.append('\n');
          }
          sb.append(text.trim());
        }
      }
      if (sb.length() > 0) {
        a.setRawContent(trimTo(sb.toString(), 4000));
      }
    }

    String keywords = metaContent(doc, "name", "news_keywords");
    if (!isBlank(keywords)) {
      List<String> tags = new ArrayList<>();
      for (String part : keywords.split(",")) {
        String t = part.trim();
        if (!t.isEmpty()) {
          tags.add(t);
        }
      }
      if (!tags.isEmpty()) {
        a.setTags(String.join(",", tags));
      }
    }

    String img = firstNonBlank(
      metaContent(doc, "property", "og:image"),
      metaContent(doc, "name", "twitter:image")
    );
    if (!isBlank(img)) {
      a.setTumbnailURL(img.trim());
    }

    return a;
  }

  @Override
  public String fetchThumbnailUrl(String articleUrl) throws Exception {
    Document doc = fetchDocument(articleUrl);
    String og = metaContent(doc, "property", "og:image");
    if (!isBlank(og)) {
      return og.trim();
    }
    return super.fetchThumbnailUrl(articleUrl);
  }

  private boolean isBlank(String val) {
    return val == null || val.isBlank();
  }
}
