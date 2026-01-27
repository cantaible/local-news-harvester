package com.example.springboot3newsreader.services.webadapters;

import org.jsoup.nodes.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
@Order(5)
public class TechCrunchWebAdapter extends BaseWebAdapter {

  @Override
  public boolean supports(String siteUrl) {
    String host = getHost(siteUrl);
    return host != null && normalizeHost(host).endsWith("techcrunch.com");
  }

  @Override
  public String fetchThumbnailUrl(String articleUrl) throws Exception {
    Document doc = fetchDocument(articleUrl);
    JsonNode jsonLd = findNewsArticleJsonLd(doc);
    String image = firstNonBlank(
      textFromJsonLd(jsonLd, "thumbnailUrl"),
      textFromJsonLdImage(jsonLd),
      metaContent(doc, "property", "og:image"),
      metaContent(doc, "name", "twitter:image")
    );
    if (image != null && !image.isBlank()) {
      return image.trim();
    }
    return super.fetchThumbnailUrl(articleUrl);
  }
}
