package com.example.springboot3newsreader.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot3newsreader.ApiResponse;
import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.services.FeedItemService;

// FeedItem 的 REST API 控制器
@RestController
// 统一的接口前缀
@RequestMapping("/api/feeditems")
public class FeedItemController {

  @Autowired
  FeedItemService feedItemService;

  // 获取所有 feed 条目
  @GetMapping
  public ResponseEntity<?> getAllFeedItems() {
    List<FeedItem> feedList = feedItemService.getAll();
    return ResponseEntity.ok(new ApiResponse<>(200, "ok", feedList));
  }
  

  // 根据 id 获取单条 feed 条目
  @GetMapping("/{id}")
  public ResponseEntity<?> getFeedItem(@PathVariable Long id) {
    Optional<FeedItem> theItem = feedItemService.getById(id);
    if (theItem.isPresent()){
      return ResponseEntity.ok(new ApiResponse<>(200, "ok", theItem.get()));
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(new ApiResponse<>(404, "not found", null));
  }

  @PostMapping("/seed")
  public ResponseEntity<?> seedFeedItems() {
    List<FeedItem> feeds = new ArrayList<>();

    FeedItem bbc = new FeedItem();
    bbc.setName("SEED_FEED_AI_JAZEERA");
    bbc.setUrl("https://www.aljazeera.com/xml/rss/all.xml");
    bbc.setSourceType("RSS");
    bbc.setEnabled(true);
    feeds.add(bbc);

    FeedItem reuters = new FeedItem();
    reuters.setName("SEED_FEED_REUTERS_TOP");
    reuters.setUrl("https://feeds.reuters.com/reuters/topNews");
    reuters.setSourceType("RSS");
    reuters.setEnabled(true);
    feeds.add(reuters);

    FeedItem web = new FeedItem();
    web.setName("SEED_FEED_REUTERS_WEB");
    web.setUrl("https://www.reuters.com");
    web.setSourceType("WEB");
    web.setEnabled(true);
    feeds.add(web);

    // https://www.jiqizhixin.com/rss
    // https://www.qbitai.com/feed
    // https://techcrunch.com/feed/
    // https://www.theverge.com/rss/index.xml
    // https://www.infoq.cn/feed
    // https://www.artificialintelligence-news.com/feed/
    // https://venturebeat.com/feed

    List<FeedItem> saved = feedItemService.saveAll(feeds);
    return ResponseEntity.ok(new ApiResponse<>(200, "seeded", saved));
  }

  @DeleteMapping("/seed")
  public ResponseEntity<?> deleteSeedFeedItems() {
    feedItemService.deleteByNamePrefix("SEED_FEED_");
    return ResponseEntity.ok(new ApiResponse<>(200, "deleted", null));
  }
  
}
