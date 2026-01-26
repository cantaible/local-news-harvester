package com.example.springboot3newsreader.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThumbnailTask {

  @Id
  @GeneratedValue
  // 任务主键 ID（自增）
  Long id;

  // 关联的文章 ID（NewsArticle.id），用于补图时定位文章
  // 关联的文章 ID（NewsArticle.id）
  Long articleId;

  // 文章 URL（便于补图时直接使用，避免二次查文章）
  // 文章 URL（便于补图时直接使用）
  @Column(length = 1024)
  String articleUrl;

  // 任务状态：WAITING 待处理 / RUNNING 处理中 / SUCCESS 成功 / FAILED 失败可重试
  // 任务状态：WAITING / RUNNING / SUCCESS / FAILED
  String status;

  // 已尝试次数，用于限制重试上限
  // 重试次数
  Integer attempts;

  // 下一次允许重试时间（为空表示可以立刻执行）
  // 下一次重试时间（为空表示可立即处理）
  LocalDateTime nextRetryAt;

  // 最近一次失败原因（用于排查失败原因与观测）
  // 最近一次失败原因（简要）
  String lastError;

  // 任务创建时间
  LocalDateTime createdAt;
  // 任务更新时间
  LocalDateTime updatedAt;
}
