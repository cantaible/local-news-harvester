package com.example.springboot3newsreader.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot3newsreader.ApiResponse;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.NewsCategory;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @GetMapping
  public ResponseEntity<?> listCategories() {
    List<CategoryInfo> result = new ArrayList<>();
    for (NewsCategory c : NewsCategory.all()) {
      result.add(new CategoryInfo(c.getKey(), c.getLabel(), c.getOrder(), c.isEnabled()));
    }
    result.sort(Comparator.comparingInt(CategoryInfo::getOrder));
    return ResponseEntity.ok(new ApiResponse<>(200, "ok", result));
  }

  @GetMapping("/{category}/newsarticles")
  public ResponseEntity<?> getArticlesByCategory(@PathVariable String category) {
    NewsCategory c = NewsCategory.fromKey(category);
    if (c == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ApiResponse<>(400, "invalid category", null));
    }
    List<NewsArticle> articles = newsArticleRepository.findByCategoryOrderByIdDesc(c);
    return ResponseEntity.ok(new ApiResponse<>(200, "ok", articles));
  }

  public static class CategoryInfo {
    private final String key;
    private final String label;
    private final int order;
    private final boolean enabled;

    public CategoryInfo(String key, String label, int order, boolean enabled) {
      this.key = key;
      this.label = label;
      this.order = order;
      this.enabled = enabled;
    }

    public String getKey() {
      return key;
    }

    public String getLabel() {
      return label;
    }

    public int getOrder() {
      return order;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }
}
