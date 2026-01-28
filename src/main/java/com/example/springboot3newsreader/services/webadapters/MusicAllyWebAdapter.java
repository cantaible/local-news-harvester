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
@Order(4)
public class MusicAllyWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("musically.com");
  }

  @Override
  protected NewsArticle parseArticle(String url, String sourceName) throws Exception {
    Document doc = fetchDocument(url);
    NewsArticle a = new NewsArticle();
    a.setSourceURL(url);
    a.setSourceName(sourceName);
    a.setScrapedAt(Instant.now().toString());

    String title = firstNonBlank(
      metaContent(doc, "property", "og:title"),
      metaContent(doc, "name", "twitter:title"),
      doc.title(),
      textOrNull(doc.selectFirst("h1.entry-title")),
      textOrNull(doc.selectFirst("h1"))
    );
    String description = firstNonBlank(
      metaContent(doc, "name", "description"),
      metaContent(doc, "property", "og:description")
    );
    String publishedAt = firstNonBlank(
      metaContent(doc, "property", "article:published_time"),
      attrOrNull(doc.selectFirst("time.entry-date.published[datetime]"), "datetime")
    );

    if (isBlank(title)) {
      return null;
    }

    a.setTitle(title.trim());
    a.setPublishedAt(normalizeDate(publishedAt, Instant.now().toString()));

    if (!isBlank(description)) {
      a.setSummary(trimTo(description, 160));
    } else {
      Element firstP = doc.selectFirst(".entry-content p");
      String summary = firstP != null ? firstP.text() : null;
      if (!isBlank(summary)) {
        a.setSummary(trimTo(summary, 160));
      }
    }

    Elements tagLinks = doc.select(".entry-footer .tags-links a[rel=tag]");
    if (!tagLinks.isEmpty()) {
      List<String> tags = new ArrayList<>();
      for (Element tag : tagLinks) {
        String t = tag.text();
        if (!isBlank(t)) {
          tags.add(t.trim());
        }
      }
      if (!tags.isEmpty()) {
        a.setTags(String.join(",", tags));
      }
    }

    Element content = doc.selectFirst(".entry-content");
    if (content != null) {
      content.select(
        "section, aside, nav, form, .jp-relatedposts-i2, .newspack-popup-container, "
          + ".newspack-popup, .widget, script, style").remove();
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

    Element img = doc.selectFirst(".post-thumbnail img, .entry-content img");
    if (img != null) {
      String best = bestImageSrc(img);
      if (!isBlank(best)) {
        a.setTumbnailURL(best);
      }
    }

    return a;
  }

  @Override
  public String fetchThumbnailUrl(String articleUrl) throws Exception {
    Document doc = fetchDocument(articleUrl);
    Element img = doc.selectFirst(".post-thumbnail img");
    if (img != null) {
      String best = bestImageSrc(img);
      if (!isBlank(best)) {
        return best;
      }
    }
    return super.fetchThumbnailUrl(articleUrl);
  }

  private boolean isBlank(String val) {
    return val == null || val.isBlank();
  }
}
