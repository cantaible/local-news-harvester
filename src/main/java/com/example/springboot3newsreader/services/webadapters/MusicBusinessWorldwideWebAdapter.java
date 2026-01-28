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
@Order(7)
public class MusicBusinessWorldwideWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("musicbusinessworldwide.com");
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
      textOrNull(doc.selectFirst(".mb-article h1")),
      doc.title()
    );
    String description = firstNonBlank(
      metaContent(doc, "name", "description"),
      metaContent(doc, "property", "og:description")
    );
    String publishedAt = firstNonBlank(
      metaContent(doc, "property", "article:published_time")
    );

    if (isBlank(title)) {
      return null;
    }

    a.setTitle(title.trim());
    a.setPublishedAt(normalizeDate(publishedAt, Instant.now().toString()));

    if (!isBlank(description)) {
      a.setSummary(trimTo(description, 160));
    } else {
      Element firstP = doc.selectFirst(".mb-article__body p");
      if (firstP != null && !isBlank(firstP.text())) {
        a.setSummary(trimTo(firstP.text(), 160));
      }
    }

    Element content = doc.selectFirst(".mb-article__body");
    if (content != null) {
      content.select(
        ".mb-advert__incontent, .mb-advert, .mb-newsletter, .mb-article__stamp, "
          + "script, style, iframe").remove();
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

    Elements tagLinks = doc.select(".mb-article__tags a[rel=tag]");
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

    String og = metaContent(doc, "property", "og:image");
    if (!isBlank(og)) {
      a.setTumbnailURL(og.trim());
    } else {
      Element img = doc.selectFirst(".mbw-leadpic img");
      if (img != null) {
        String best = bestImageSrc(img);
        if (!isBlank(best)) {
          a.setTumbnailURL(best);
        }
      }
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
    Element img = doc.selectFirst(".mbw-leadpic img");
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
