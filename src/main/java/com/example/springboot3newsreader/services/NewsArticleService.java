package com.example.springboot3newsreader.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.repositories.FeedItemRepository;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;

@Service
public class NewsArticleService {

  @Autowired
  NewsArticleRepository newsArticleRepository;
  @Autowired
  FeedItemRepository feedItemRepository;
  @Autowired
  RssIngestService rssIngestService;

  public List<NewsArticle> getAll(){ 
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
    // 1) 取所有 RSS 源
    List<FeedItem> allFeeds = feedItemRepository.findAll();
    List<FeedItem> feeds = new ArrayList<>();
    for (FeedItem feed : allFeeds) {
      if ("RSS".equals(feed.getSourceType())) {
        feeds.add(feed);
      }
    }

    // 2) 取已有 URL 集合
    Set<String> existing = new HashSet<>();
    for (NewsArticle a : newsArticleRepository.findAll()) {
      if (a.getSourceURL() != null) {
        existing.add(a.getSourceURL());
      }
    }

    // 3) 并发解析 RSS
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Future<List<NewsArticle>>> tasks = new ArrayList<>();
      for (FeedItem feed : feeds) {
        tasks.add(pool.submit(new Callable<List<NewsArticle>>() {
          @Override
          public List<NewsArticle> call() {
            try {
              return rssIngestService.parseOnly(feed.getUrl(), feed.getName());
            } catch (Exception e) {
              return new ArrayList<>();
            }
          }
        }));
      }

      // 4) 合并结果
      List<NewsArticle> all = new ArrayList<>();
      for (Future<List<NewsArticle>> task : tasks) {
        try {
          List<NewsArticle> result = task.get();
          all.addAll(result);
        } catch (Exception e) {
          // ignore parse errors from a single feed
        }
      }

      // 5) 过滤新文章（按 sourceURL 去重）
      List<NewsArticle> newOnes = new ArrayList<>();
      for (NewsArticle a : all) {
        String url = a.getSourceURL();
        if (url != null && !existing.contains(url)) {
          existing.add(url);
          newOnes.add(a);
        }
      }

      // 6) 批量保存并返回
      return newsArticleRepository.saveAll(newOnes);

    } finally {
      pool.shutdown();
    }
  }



}
