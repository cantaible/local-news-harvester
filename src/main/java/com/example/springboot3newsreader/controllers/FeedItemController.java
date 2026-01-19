package com.example.springboot3newsreader.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  
}
