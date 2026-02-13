package com.example.springboot3newsreader.services;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.repositories.FeedItemRepository;
import com.example.springboot3newsreader.models.NewsCategory;
import com.example.springboot3newsreader.models.dto.NewsArticleSearchRequest;

import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class NewsArticleService {

  @Autowired
  NewsArticleRepository newsArticleRepository;
  @Autowired
  FeedItemRepository feedItemRepository;
  @Autowired
  IngestPipelineService ingestPipelineService;

  public List<NewsArticle> getAll() {
    return newsArticleRepository.findAll();
  }

  public Optional<NewsArticle> getById(Long id) {
    return newsArticleRepository.findById(id);
  }

  public NewsArticle save(NewsArticle newsArticle) {
    return newsArticleRepository.save(newsArticle);
  }

  public List<NewsArticle> saveAll(List<NewsArticle> newsArticles) {
    return newsArticleRepository.saveAll(newsArticles);
  }

  @Transactional
  public void deleteBySourceNamePrefix(String prefix) {
    newsArticleRepository.deleteBySourceNameStartingWith(prefix);
  }

  public List<NewsArticle> refreshFromRssFeeds() {
    // 1) 取所有 RSS/WEB 源
    List<FeedItem> allFeeds = feedItemRepository.findAll();
    List<FeedItem> feeds = new ArrayList<>();
    for (FeedItem feed : allFeeds) {
      if ("RSS".equals(feed.getSourceType()) || "WEB".equals(feed.getSourceType())) {
        feeds.add(feed);
      }
    }
    // 2) 走统一 ingest pipeline（与 feeds/new 一致）
    // 2) 走统一 ingest pipeline（与 feeds/new 一致）
    List<NewsArticle> results = ingestPipelineService.ingestAll(feeds);
    // 默认不返回大内容，节省流量
    results.forEach(a -> a.setRawContent(null));
    return results;
  }

  public List<NewsArticle> search(NewsArticleSearchRequest request) {
    Instant startDateTime = parseUtcDateTimeOrNull(request.getStartDateTime(), "startDateTime");
    Instant endDateTime = parseUtcDateTimeOrNull(request.getEndDateTime(), "endDateTime");

    Specification<NewsArticle> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 1. Category
      if (request.getCategory() != null && !request.getCategory().isBlank()) {
        try {
          NewsCategory cat = NewsCategory.valueOf(request.getCategory().toUpperCase());
          predicates.add(cb.equal(root.get("category"), cat));
        } catch (IllegalArgumentException e) {
          // ignore invalid category
        }
      }

      // 2. Keyword (Title or Summary)
      if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
        String k = "%" + request.getKeyword().trim().toLowerCase() + "%";
        Predicate titleMatch = cb.like(cb.lower(root.get("title")), k);
        Predicate summaryMatch = cb.like(cb.lower(root.get("summary")), k);
        predicates.add(cb.or(titleMatch, summaryMatch));
      }

      // 3. Sources
      if (request.getSources() != null && !request.getSources().isEmpty()) {
        predicates.add(root.get("sourceName").in(request.getSources()));
      }

      // 4. Tags (JSON String like check)
      if (request.getTags() != null && !request.getTags().isEmpty()) {
        List<Predicate> tagPredicates = new ArrayList<>();
        for (String tag : request.getTags()) {
          tagPredicates.add(cb.like(root.get("tags"), "%\"" + tag + "\"%"));
        }
        predicates.add(cb.or(tagPredicates.toArray(new Predicate[0])));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };

    Sort sort = Sort.by(Sort.Direction.DESC, "publishedAt");
    if ("oldest".equalsIgnoreCase(request.getSortOrder())) {
      sort = Sort.by(Sort.Direction.ASC, "publishedAt");
    }

    List<NewsArticle> results = newsArticleRepository.findAll(spec, sort).stream()
        .filter(a -> matchesDateTimeRange(a.getPublishedAt(), startDateTime, endDateTime))
        .collect(Collectors.toList());

    // Optimize payload: set rawContent to null if not requested
    if (!request.isIncludeContent()) {
      results.forEach(a -> a.setRawContent(null));
    }

    return results;
  }

  private Instant parseUtcDateTimeOrNull(String rawValue, String fieldName) {
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }
    if (!rawValue.endsWith("Z")) {
      throw new IllegalArgumentException(fieldName
          + " must be an ISO 8601 UTC datetime with 'Z', e.g. 2026-02-13T02:35:00Z");
    }
    try {
      return Instant.parse(rawValue);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(fieldName
          + " must be a valid ISO 8601 UTC datetime with 'Z', e.g. 2026-02-13T02:35:00Z");
    }
  }

  private boolean matchesDateTimeRange(String publishedAt, Instant startDateTime, Instant endDateTime) {
    if (startDateTime == null && endDateTime == null) {
      return true;
    }
    if (publishedAt == null || publishedAt.isBlank()) {
      return false;
    }
    final Instant publishedInstant;
    try {
      publishedInstant = Instant.parse(publishedAt);
    } catch (DateTimeParseException ex) {
      return false;
    }

    if (startDateTime != null && publishedInstant.isBefore(startDateTime)) {
      return false;
    }
    if (endDateTime != null && !publishedInstant.isBefore(endDateTime)) {
      return false;
    }
    return true;
  }
}
