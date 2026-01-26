package com.example.springboot3newsreader.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.ThumbnailTask;
import com.example.springboot3newsreader.repositories.FeedItemRepository;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import com.example.springboot3newsreader.repositories.ThumbnailTaskRepository;

@Service
public class NewsArticleService {

  @Autowired
  NewsArticleRepository newsArticleRepository;
  @Autowired
  FeedItemRepository feedItemRepository;
  @Autowired
  RssIngestService rssIngestService;
  @Autowired
  WebIngestService webIngestService;
  @Autowired
  ThumbnailTaskRepository thumbnailTaskRepository;
  @Autowired
  NewsArticleDedupeService newsArticleDedupeService;

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
    // 1) 取所有 RSS/WEB 源
    List<FeedItem> allFeeds = feedItemRepository.findAll();
    List<FeedItem> feeds = new ArrayList<>();
    for (FeedItem feed : allFeeds) {
      if ("RSS".equals(feed.getSourceType()) || "WEB".equals(feed.getSourceType())) {
        feeds.add(feed);
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
              if ("RSS".equals(feed.getSourceType())) {
                return rssIngestService.parseOnly(feed.getUrl(), feed.getName());
              }
              if ("WEB".equals(feed.getSourceType())) {
                return webIngestService.parseOnly(feed.getUrl(), feed.getName());
              }
              return new ArrayList<>();
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

      // 5) 去重（URL + 标题相似度）
      List<NewsArticle> newOnes = newsArticleDedupeService.filterNewArticles(all);

      // 6) 批量保存并返回
      for (NewsArticle a : newOnes) {
        a.setTumbnailURL(null);
      }
      List<NewsArticle> saved = newsArticleRepository.saveAll(newOnes);

      // 7) 为新文章创建补图任务（可恢复）
      List<ThumbnailTask> thumbnailTasks = new ArrayList<>();
      for (NewsArticle a : saved) {
        ThumbnailTask task = new ThumbnailTask();
        task.setArticleId(a.getId());
        task.setArticleUrl(a.getSourceURL());
        task.setStatus("WAITING");
        task.setAttempts(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        thumbnailTasks.add(task);
      }
      if (!thumbnailTasks.isEmpty()) {
        thumbnailTaskRepository.saveAll(thumbnailTasks);
      }

      return saved;

    } finally {
      pool.shutdown();
    }
  }



}
