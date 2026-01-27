package com.example.springboot3newsreader.services.webadapters;

import java.util.List;

import com.example.springboot3newsreader.models.NewsArticle;

public interface WebAdapter {
  boolean supports(String siteUrl);

  List<NewsArticle> previewOnly(String siteUrl, String sourceName) throws Exception;

  List<NewsArticle> parseOnly(String siteUrl, String sourceName) throws Exception;

  // 站点级首图抽取（用于异步补图）
  String fetchThumbnailUrl(String articleUrl) throws Exception;
}
