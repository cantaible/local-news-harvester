package com.example.springboot3newsreader.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.repositories.FeedItemRepository;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import com.example.springboot3newsreader.repositories.ThumbnailTaskRepository;
import com.example.springboot3newsreader.services.FeedItemService;
import com.example.springboot3newsreader.services.IngestPipelineService;
import com.example.springboot3newsreader.ApiResponse;

@RestController
public class FormController {

  @Autowired
  private FeedItemService feedItemService;
  @Autowired
  private FeedItemRepository feedItemRepository;
  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @Autowired
  private ThumbnailTaskRepository thumbnailTaskRepository;
  @Autowired
  private IngestPipelineService ingestPipelineService;


  @PostMapping("/feeds/new")
  public ResponseEntity<?> createFeedItem(FeedItem feedItem) {
    System.out.println("[feeds/new] request received");
    System.out.println("[feeds/new] name=" + feedItem.getName()
      + ", url=" + feedItem.getUrl()
      + ", sourceType=" + feedItem.getSourceType()
      + ", enabled=" + feedItem.getEnabled());
    if (feedItem.getName() == null || feedItem.getName().trim().isEmpty()) {
      System.out.println("[feeds/new] validation failed: name is blank");
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "name should not be null!", null));
    }
    if (feedItem.getUrl() == null || feedItem.getUrl().trim().isEmpty()) {
      System.out.println("[feeds/new] validation failed: url is blank");
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "url should not be null!", null));
    }
    if (!feedItem.getUrl().startsWith("http://") && !feedItem.getUrl().startsWith("https://")) {
      System.out.println("[feeds/new] validation failed: url must start with http/https");
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "url should begin with http or https!", null));
    }
    if (feedItem.getSourceType() == null || feedItem.getSourceType().trim().isEmpty()) {
      System.out.println("[feeds/new] validation failed: sourceType is blank");
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "sourceType should not be null!", null));
    }
    if (!feedItem.getSourceType().equals("RSS") && !feedItem.getSourceType().equals("WEB")) {
      System.out.println("[feeds/new] validation failed: sourceType must be RSS or WEB");
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "sourceType must be RSS or WEB!", null));
    }
    if (feedItem.getEnabled() == null) {
      System.out.println("[feeds/new] validation failed: enabled is null");
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "enabled should not be null!", null));
    }

    String name = feedItem.getName().trim();
    String url = feedItem.getUrl().trim();
    System.out.println("[feeds/new] normalized name=" + name + ", url=" + url);
    if (feedItemRepository.existsByNameAndUrl(name, url)) {
      System.out.println("[feeds/new] duplicate feed found, abort");
      return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(new ApiResponse<>(409, "feed already exists", null));
    }

    feedItem.setName(name);
    feedItem.setUrl(url);

    System.out.println("[feeds/new] saving feed item");
    FeedItem saved = feedItemService.save(feedItem);
    System.out.println("[feeds/new] saved id=" + saved.getId());

    System.out.println("[feeds/new] trigger ingest pipeline async");
    ingestPipelineService.ingestFeedAsync(feedItem);
    System.out.println("[feeds/new] response 201");
    return ResponseEntity.status(HttpStatus.CREATED)
    .body(new ApiResponse<>(200, "ok", saved));
  }

  @PostMapping("/feeds/preview")
  public ResponseEntity<?> previewFeed(FeedItem feedItem) {
    if (feedItem.getUrl() == null || feedItem.getUrl().trim().isEmpty()) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "url should not be null!", null));
    }
    if (!feedItem.getUrl().startsWith("http://") && !feedItem.getUrl().startsWith("https://")) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "url should begin with http or https!", null));
    }
    if (feedItem.getSourceType() == null || feedItem.getSourceType().trim().isEmpty()) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "sourceType should not be null!", null));
    }
    if (!feedItem.getSourceType().equals("WEB")) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "preview only supports WEB sourceType!", null));
    }

    String name = feedItem.getName() == null ? "PREVIEW" : feedItem.getName().trim();
    String url = feedItem.getUrl().trim();
    try {
      List<NewsArticle> articles = webIngestService.previewOnly(url, name);
      return ResponseEntity.ok(new ApiResponse<>(200, "ok", articles));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(new ApiResponse<>(500, "preview failed", null));
    }
  }

  @PostMapping("/admin/clear")
  public ResponseEntity<?> clearBusinessTables() {
    // 按依赖顺序清空，避免外键约束问题
    thumbnailTaskRepository.deleteAll();
    newsArticleRepository.deleteAll();
    feedItemRepository.deleteAll();
    return ResponseEntity.ok(new ApiResponse<>(200, "cleared", null));
  }
  
}
