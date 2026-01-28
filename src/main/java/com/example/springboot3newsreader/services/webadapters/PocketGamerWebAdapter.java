package com.example.springboot3newsreader.services.webadapters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.springboot3newsreader.models.NewsArticle;

@Component
@Order(3)
public class PocketGamerWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("pocketgamer.com");
  }

  @Override
  protected NewsArticle parseArticle(String url, String sourceName) throws Exception {
    Document doc = fetchDocument(url);
    NewsArticle a = new NewsArticle();
    a.setSourceURL(url);
    a.setSourceName(sourceName);
    a.setScrapedAt(Instant.now().toString());

    String title = firstNonBlank(
      textOrNull(doc.selectFirst("article h1")),
      metaContent(doc, "property", "og:title"),
      metaContent(doc, "name", "twitter:title"),
      doc.title()
    );
    String description = firstNonBlank(
      metaContent(doc, "name", "description"),
      metaContent(doc, "property", "og:description")
    );
    String publishedAt = firstNonBlank(
      metaContent(doc, "property", "article:published_time"),
      attrOrNull(doc.selectFirst("article time[datetime]"), "datetime")
    );

    if (isBlank(title)) {
      return null;
    }

    a.setTitle(title.trim());
    a.setPublishedAt(normalizeDate(publishedAt, Instant.now().toString()));

    if (!isBlank(description)) {
      a.setSummary(trimTo(description, 160));
    } else {
      Element firstP = doc.selectFirst(".body-copy p");
      if (firstP != null && !isBlank(firstP.text())) {
        a.setSummary(trimTo(firstP.text(), 160));
      }
    }

    Element content = doc.selectFirst(".body-copy");
    if (content != null) {
      content.select(
        ".highlights, .fullWidthVideo, .bt-inline, .inline-button, script, style").remove();
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

    Set<String> tagSet = new LinkedHashSet<>();
    for (Element tag : doc.select(".formats a")) {
      String t = tag.text();
      if (!isBlank(t)) {
        tagSet.add(t.trim());
      }
    }
    Element subject = doc.selectFirst(".main-subject");
    if (subject != null && !isBlank(subject.text())) {
      tagSet.add(subject.text().trim());
    }
    if (!tagSet.isEmpty()) {
      a.setTags(String.join(",", new ArrayList<>(tagSet)));
    }

    Element img = doc.selectFirst("figure.lead img");
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
    Element img = doc.selectFirst("figure.lead img");
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
