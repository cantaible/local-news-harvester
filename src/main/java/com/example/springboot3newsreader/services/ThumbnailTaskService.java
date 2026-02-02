package com.example.springboot3newsreader.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.models.ThumbnailTask;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;
import com.example.springboot3newsreader.repositories.ThumbnailTaskRepository;
import com.example.springboot3newsreader.services.webadapters.WebAdapter;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ThumbnailTaskService {

  // 单次扫描处理的任务上限（避免一次性处理过多任务）
  private static final int BATCH_SIZE = 10;
  // 最大重试次数（超过则不再处理）
  private static final int MAX_ATTEMPTS = 3;
  // 失败后下次重试的延迟时间（分钟）
  private static final int RETRY_DELAY_MINUTES = 30;

  // 任务状态常量
  private static final String STATUS_WAITING = "WAITING";
  private static final String STATUS_RUNNING = "RUNNING";
  private static final String STATUS_SUCCESS = "SUCCESS";
  private static final String STATUS_FAILED = "FAILED";

  @Autowired
  ThumbnailTaskRepository thumbnailTaskRepository;
  @Autowired
  NewsArticleRepository newsArticleRepository;
  @Autowired
  private List<WebAdapter> webAdapters;

  @Value("${app.feature.thumbnail-task.enabled:true}")
  private boolean thumbnailTaskEnabled;

  // 定时任务：周期性扫描可执行任务并处理
  @Scheduled(fixedDelay = 15000)
  public void processTasks() {
    if (!thumbnailTaskEnabled) {
      return;
    }
    // 1) 只取可执行状态 + 到期的任务
    List<ThumbnailTask> tasks = thumbnailTaskRepository.findReadyTasks(
        List.of(STATUS_WAITING, STATUS_FAILED),
        LocalDateTime.now(),
        PageRequest.of(0, BATCH_SIZE));
    // 2) 逐条处理（避免一个任务异常导致整个批次中断）
    for (ThumbnailTask task : tasks) {
      processSingleTask(task.getId());
    }
  }

  // 处理单条任务（事务内更新任务状态，保证幂等与一致性）
  @Transactional
  public void processSingleTask(Long taskId) {
    // 1) 取任务，找不到则直接退出
    ThumbnailTask task = thumbnailTaskRepository.findById(taskId).orElse(null);
    if (task == null) {
      return;
    }

    // 2) 已成功任务直接跳过（幂等）
    if (STATUS_SUCCESS.equals(task.getStatus())) {
      return;
    }

    // 3) 超过最大重试次数则标记 FAILED 并退出
    int attempts = task.getAttempts() == null ? 0 : task.getAttempts();
    if (attempts >= MAX_ATTEMPTS) {
      if (!STATUS_FAILED.equals(task.getStatus())) {
        task.setStatus(STATUS_FAILED);
        task.setUpdatedAt(LocalDateTime.now());
        thumbnailTaskRepository.save(task);
      }
      return;
    }

    // 4) 标记 RUNNING + 增加次数，避免并发重复处理
    task.setStatus(STATUS_RUNNING);
    task.setAttempts(attempts + 1);
    task.setUpdatedAt(LocalDateTime.now());
    if (task.getCreatedAt() == null) {
      task.setCreatedAt(LocalDateTime.now());
    }
    thumbnailTaskRepository.save(task);

    try {
      // 5) 取文章，找不到则失败（可能文章被删除）
      NewsArticle article = newsArticleRepository.findById(task.getArticleId()).orElse(null);
      if (article == null) {
        failTask(task, "article_not_found");
        return;
      }
      // 6) 如果文章已存在首图，直接成功（幂等）
      if (article.getTumbnailURL() != null && !article.getTumbnailURL().isBlank()) {
        succeedTask(task);
        return;
      }

      // 7) 获取文章 URL（优先任务内缓存的 URL）
      String url = task.getArticleUrl() != null && !task.getArticleUrl().isBlank()
          ? task.getArticleUrl()
          : article.getSourceURL();
      if (url == null || url.isBlank()) {
        failTask(task, "missing_article_url");
        return;
      }

      // 8) 使用站点适配器补图（每个站点可有独立规则）
      String image = fetchThumbnailByAdapter(url);
      if (image == null || image.isBlank()) {
        failTask(task, "thumbnail_not_found");
        return;
      }

      // 9) 写回首图到文章表，再标记任务成功
      article.setTumbnailURL(image);
      newsArticleRepository.save(article);
      succeedTask(task);
    } catch (Exception e) {
      // 10) 任何异常都当作失败，进入重试流程
      failTask(task, "exception:" + e.getClass().getSimpleName());
    }
  }

  // 成功结束任务：标记 SUCCESS，清理失败信息
  private void succeedTask(ThumbnailTask task) {
    task.setStatus(STATUS_SUCCESS);
    task.setLastError(null);
    task.setNextRetryAt(null);
    task.setUpdatedAt(LocalDateTime.now());
    thumbnailTaskRepository.save(task);
  }

  // 失败结束任务：标记 FAILED，并设置下次重试时间
  private void failTask(ThumbnailTask task, String error) {
    task.setStatus(STATUS_FAILED);
    task.setLastError(error);
    task.setNextRetryAt(LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES));
    task.setUpdatedAt(LocalDateTime.now());
    thumbnailTaskRepository.save(task);
  }

  private String fetchThumbnailByAdapter(String url) {
    for (WebAdapter adapter : webAdapters) {
      if (adapter.supports(url)) {
        try {
          return adapter.fetchThumbnailUrl(url);
        } catch (Exception e) {
          return null;
        }
      }
    }
    return null;
  }
}
