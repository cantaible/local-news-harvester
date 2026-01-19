package com.example.springboot3newsreader.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.services.FeedItemService;
import com.example.springboot3newsreader.ApiResponse;

@RestController
public class FormController {

  @Autowired
  private FeedItemService feedItemService;

  @PostMapping("/feeds/new")
  public ResponseEntity<?> createFeedItem(FeedItem feedItem) {
    if (feedItem.getName() == null || feedItem.getName().trim().isEmpty()) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "name should not be null!", null));
    }
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
    if (!feedItem.getSourceType().equals("RSS") && !feedItem.getSourceType().equals("WEB")) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "sourceType must be RSS or WEB!", null));
    }
    if (feedItem.getEnabled() == null) {
      return ResponseEntity.badRequest()
      .body(new ApiResponse<>(400, "enabled should not be null!", null));
    }

    FeedItem saved = feedItemService.save(feedItem);
    return ResponseEntity.status(HttpStatus.CREATED)
    .body(new ApiResponse<>(200, "ok", saved));
  }
  
}
