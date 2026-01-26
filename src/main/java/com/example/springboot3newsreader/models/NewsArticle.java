package com.example.springboot3newsreader.models;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsArticle {
  
  @Id
  @GeneratedValue
  // 主键 ID（当前为自增 Long）
  Long id;

  // 文章标题
  String title;
  // 原文链接（唯一去重键）
  @Column(length = 1024)
  String sourceURL;
  // 来源名称（展示用）
  String sourceName;
  // 发布日期（ISO 8601 字符串）
  String publishedAt;
  // 抓取时间（ISO 8601 字符串）
  String scrapedAt;
  // 摘要（一句话，可为空）
  String summary;
  // 标签 JSON 字符串，可为空
  String tags;
  // 缩略图链接，可为空
  String tumbnailURL;
  // 原文内容/摘要，可为空
  @Lob
  @Column(columnDefinition = "LONGTEXT")
  String rawContent;

  
}
