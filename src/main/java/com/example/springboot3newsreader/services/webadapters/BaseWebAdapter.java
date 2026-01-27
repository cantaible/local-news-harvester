package com.example.springboot3newsreader.services.webadapters;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.example.springboot3newsreader.models.NewsArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseWebAdapter implements WebAdapter {

  // 模拟浏览器 UA，降低被反爬拦截概率
  protected static final String USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
      + "Chrome/120.0.0.0 Safari/537.36";
  // 单次抓取超时时间（毫秒）
  protected static final int TIMEOUT_MS = 20000;
  // 抓取失败后的重试次数（总尝试次数 = 1 + FETCH_RETRIES）
  protected static final int FETCH_RETRIES = 2;
  // 单个站点最多解析的文章数量（避免抓取过多）
  protected static final int MAX_ARTICLES = 20;
  // 解析文章的并发线程数（控制站点压力）
  protected static final int PARSE_THREADS = 4;

  private final ObjectMapper objectMapper = new ObjectMapper();

  // 默认解析流程：抓首页 -> 提取文章链接 -> 逐篇解析
  @Override
  public List<NewsArticle> parseOnly(String siteUrl, String sourceName) throws Exception {
    System.out.println("[preview] fetch home: " + siteUrl);
    Document home = fetchDocument(siteUrl);
    List<String> links = extractArticleLinks(home, siteUrl);
    System.out.println("[preview] links found: " + links.size());
    if (!links.isEmpty()) {
      int limit = Math.min(5, links.size());
      System.out.println("[preview] sample links: " + links.subList(0, limit));
    }
    List<NewsArticle> articles = new ArrayList<>();
    ExecutorService pool = Executors.newFixedThreadPool(PARSE_THREADS);
    try {
      List<Future<NewsArticle>> tasks = new ArrayList<>();
      for (String link : links) {
        tasks.add(pool.submit(new Callable<NewsArticle>() {
          @Override
          public NewsArticle call() {
            try {
              System.out.println("[preview] parse article: " + link);
              NewsArticle article = parseArticle(link, sourceName);
              if (article == null) {
                System.out.println("[preview] skip article (no title): " + link);
              }
              return article;
            } catch (Exception e) {
              System.out.println("[preview] parse error: " + link);
              e.printStackTrace();
              return null;
            }
          }
        }));
      }
      for (Future<NewsArticle> task : tasks) {
        try {
          NewsArticle article = task.get();
          if (article != null) {
            articles.add(article);
          }
        } catch (Exception e) {
          // ignore parse errors from a single article
        }
      }
    } finally {
      pool.shutdown();
    }
    System.out.println("[preview] articles parsed: " + articles.size());
    return articles;
  }

  // 预览模式：只抓首页可见链接，不抓文章正文
  @Override
  public List<NewsArticle> previewOnly(String siteUrl, String sourceName) throws Exception {
    System.out.println("[preview] fetch home: " + siteUrl);
    Document home = fetchDocument(siteUrl);
    String host = getHost(siteUrl);
    Set<String> seen = new LinkedHashSet<>();
    List<NewsArticle> previews = new ArrayList<>();
    Elements candidates = home.select(
      "article a[href], h2 a[href], h3 a[href], h4 a[href], a[href*=/article/], a[href*=/posts/], "
        + "a[href*=/post/], a[href*=/news/], a[href*=/p/]");
    for (Element a : candidates) {
      String abs = a.absUrl("href");
      if (abs == null || abs.isBlank()) {
        continue;
      }
      int hash = abs.indexOf('#');
      if (hash >= 0) {
        abs = abs.substring(0, hash);
      }
      if (abs.equals(siteUrl) || abs.equals(siteUrl + "/")) {
        continue;
      }
      if (!isLikelyArticleUrl(abs, host)) {
        continue;
      }
      if (seen.contains(abs)) {
        continue;
      }
      seen.add(abs);

      NewsArticle aPreview = new NewsArticle();
      aPreview.setSourceURL(abs);
      aPreview.setSourceName(sourceName);
      aPreview.setTitle(textOrNull(a));
      aPreview.setScrapedAt(Instant.now().toString());
      previews.add(aPreview);
      if (previews.size() >= MAX_ARTICLES) {
        break;
      }
    }
    System.out.println("[preview] preview items: " + previews.size());
    return previews;
  }

  protected Document fetchDocument(String url) throws Exception {
    Exception lastError = null;
    for (int attempt = 0; attempt <= FETCH_RETRIES; attempt++) {
      long start = System.currentTimeMillis();
      try {
        Document doc = Jsoup.connect(url)
          .userAgent(USER_AGENT)
          .referrer("https://www.google.com/")
          .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
          .timeout(TIMEOUT_MS)
          .followRedirects(true)
          .get();
        long cost = System.currentTimeMillis() - start;
        System.out.println("[preview] fetch ok (" + cost + "ms): " + url);
        return doc;
      } catch (Exception e) {
        lastError = e;
        long cost = System.currentTimeMillis() - start;
        System.out.println("[preview] fetch failed (" + cost + "ms) attempt "
          + (attempt + 1) + "/" + (FETCH_RETRIES + 1) + ": " + url);
        if (attempt < FETCH_RETRIES) {
          try {
            Thread.sleep(500L * (attempt + 1));
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }
    throw lastError;
  }

  protected List<String> extractArticleLinks(Document doc, String siteUrl) {
    String host = getHost(siteUrl);
    Set<String> links = new LinkedHashSet<>();
    Elements candidates = doc.select(
      "article a[href], h2 a[href], h3 a[href], h4 a[href], a[href*=/article/], a[href*=/posts/], "
        + "a[href*=/post/], a[href*=/news/], a[href*=/p/]");
    for (Element a : candidates) {
      String abs = a.absUrl("href");
      if (abs == null || abs.isBlank()) {
        continue;
      }
      // 去掉锚点，避免重复
      int hash = abs.indexOf('#');
      if (hash >= 0) {
        abs = abs.substring(0, hash);
      }
      // 过滤主页链接
      if (abs.equals(siteUrl) || abs.equals(siteUrl + "/")) {
        continue;
      }
      if (!isLikelyArticleUrl(abs, host)) {
        continue;
      }
      links.add(abs);
      if (links.size() >= MAX_ARTICLES) {
        break;
      }
    }
    return new ArrayList<>(links);
  }

  protected boolean isLikelyArticleUrl(String url, String host) {
    String lower = url.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".gif")
      || lower.endsWith(".pdf")) {
      return false;
    }
    if (lower.contains("/tag/") || lower.contains("/category/") || lower.contains("/author/")
      || lower.contains("/login") || lower.contains("/signup")) {
      return false;
    }
    if (host != null && !host.isBlank()) {
      String urlHost = getHost(url);
      if (urlHost == null) {
        return false;
      }
      return normalizeHost(urlHost).equals(normalizeHost(host));
    }
    return true;
  }

  protected String getHost(String url) {
    try {
      return new URI(url).getHost();
    } catch (Exception e) {
      return null;
    }
  }

  protected String normalizeHost(String host) {
    if (host == null) {
      return "";
    }
    return host.startsWith("www.") ? host.substring(4) : host;
  }

  protected NewsArticle parseArticle(String url, String sourceName) throws Exception {
    Document doc = fetchDocument(url);
    NewsArticle a = new NewsArticle();
    a.setSourceURL(url);
    a.setSourceName(sourceName);
    a.setScrapedAt(Instant.now().toString());

    JsonNode jsonLd = findNewsArticleJsonLd(doc);
    String title = firstNonBlank(
      textFromJsonLd(jsonLd, "headline"),
      metaContent(doc, "property", "og:title"),
      metaContent(doc, "name", "twitter:title"),
      doc.title(),
      textOrNull(doc.selectFirst("h1"))
    );
    String description = firstNonBlank(
      textFromJsonLd(jsonLd, "description"),
      metaContent(doc, "name", "description"),
      metaContent(doc, "property", "og:description")
    );
    String publishedAt = firstNonBlank(
      textFromJsonLd(jsonLd, "datePublished"),
      metaContent(doc, "property", "article:published_time"),
      attrOrNull(doc.selectFirst("time[datetime]"), "datetime")
    );

    if (title == null || title.isBlank()) {
      return null;
    }

    a.setTitle(title.trim());
    a.setPublishedAt(normalizeDate(publishedAt, Instant.now().toString()));
    if (description != null && !description.isBlank()) {
      a.setSummary(trimTo(description, 160));
    } else {
      String fallback = textOrNull(doc.selectFirst("article"));
      if (fallback != null) {
        a.setSummary(trimTo(fallback, 160));
      }
    }
    // 首图改为异步补图，这里不再同步解析与设置

    String keywords = metaContent(doc, "name", "keywords");
    if (keywords != null && !keywords.isBlank()) {
      a.setTags(keywords.trim());
    }

    return a;
  }

  // 默认首图抽取：OG/Twitter → 正文首图
  @Override
  public String fetchThumbnailUrl(String articleUrl) throws Exception {
    Document doc = fetchDocument(articleUrl);
    String og = metaContent(doc, "property", "og:image");
    if (og != null && !og.isBlank()) {
      return og.trim();
    }
    String twitter = metaContent(doc, "name", "twitter:image");
    if (twitter != null && !twitter.isBlank()) {
      return twitter.trim();
    }
    Element img = doc.selectFirst("article img, .article img");
    if (img != null) {
      String src = bestImageSrc(img);
      if (src != null && !src.isBlank()) {
        return src;
      }
    }
    return null;
  }

  protected JsonNode findNewsArticleJsonLd(Document doc) {
    Elements scripts = doc.select("script[type=application/ld+json]");
    for (Element script : scripts) {
      String json = script.data();
      if (json == null || json.isBlank()) {
        continue;
      }
      try {
        JsonNode node = objectMapper.readTree(json);
        JsonNode found = findArticleNode(node);
        if (found != null) {
          return found;
        }
      } catch (Exception e) {
        // ignore broken json-ld
      }
    }
    return null;
  }

  protected JsonNode findArticleNode(JsonNode node) {
    if (node == null) {
      return null;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        JsonNode found = findArticleNode(child);
        if (found != null) {
          return found;
        }
      }
    } else if (node.isObject()) {
      JsonNode type = node.get("@type");
      if (type != null) {
        String typeText = type.isArray() ? type.get(0).asText("") : type.asText("");
        if (typeText.contains("NewsArticle") || typeText.contains("Article") || typeText.contains("Report")) {
          return node;
        }
      }
      JsonNode graph = node.get("@graph");
      if (graph != null) {
        JsonNode found = findArticleNode(graph);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  protected String textFromJsonLd(JsonNode node, String field) {
    if (node == null || node.get(field) == null) {
      return null;
    }
    JsonNode val = node.get(field);
    if (val.isArray() && val.size() > 0) {
      return val.get(0).asText(null);
    }
    return val.asText(null);
  }

  protected String textFromJsonLdImage(JsonNode node) {
    if (node == null || node.get("image") == null) {
      return null;
    }
    JsonNode val = node.get("image");
    if (val.isTextual()) {
      return val.asText();
    }
    if (val.isArray() && val.size() > 0) {
      return val.get(0).asText(null);
    }
    if (val.isObject()) {
      JsonNode url = val.get("url");
      if (url != null) {
        return url.asText(null);
      }
    }
    return null;
  }

  protected String metaContent(Document doc, String attr, String value) {
    Element el = doc.selectFirst("meta[" + attr + "=" + value + "]");
    return el != null ? el.attr("content") : null;
  }

  protected String textOrNull(Element el) {
    return el != null ? el.text() : null;
  }

  protected String attrOrNull(Element el, String attr) {
    return el != null ? el.attr(attr) : null;
  }

  protected String firstNonBlank(String... vals) {
    for (String v : vals) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }

  protected String trimTo(String text, int max) {
    if (text == null) {
      return null;
    }
    String trimmed = text.trim().replaceAll("\\s+", " ");
    return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
  }

  protected String normalizeDate(String input, String fallback) {
    if (input == null || input.isBlank()) {
      return fallback;
    }
    String val = input.trim();
    try {
      return Instant.parse(val).toString();
    } catch (Exception e) {
      // ignore
    }
    try {
      return OffsetDateTime.parse(val).toInstant().toString();
    } catch (Exception e) {
      // ignore
    }
    try {
      LocalDateTime dt = LocalDateTime.parse(val, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      return dt.toString();
    } catch (Exception e) {
      // ignore
    }
    return fallback;
  }

  protected String bestImageSrc(Element img) {
    if (img == null) {
      return null;
    }
    String srcset = img.attr("srcset");
    String best = pickBestFromSrcset(srcset);
    if (best != null && !best.isBlank()) {
      String abs = img.absUrl(best);
      return abs == null || abs.isBlank() ? best : abs;
    }
    String src = img.absUrl("src");
    if (src == null || src.isBlank()) {
      src = img.attr("src");
    }
    return src;
  }

  protected String pickBestFromSrcset(String srcset) {
    if (srcset == null || srcset.isBlank()) {
      return null;
    }
    String[] parts = srcset.split(",");
    int bestW = -1;
    String bestUrl = null;
    for (String part : parts) {
      String item = part.trim();
      if (item.isEmpty()) {
        continue;
      }
      String[] segs = item.split("\\s+");
      String url = segs[0];
      int width = 0;
      if (segs.length > 1 && segs[1].endsWith("w")) {
        try {
          width = Integer.parseInt(segs[1].replace("w", ""));
        } catch (Exception e) {
          width = 0;
        }
      }
      if (width >= bestW) {
        bestW = width;
        bestUrl = url;
      }
    }
    return bestUrl;
  }
}
