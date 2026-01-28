package com.example.springboot3newsreader.models;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedItem {
  
  @Id
  @GeneratedValue
  Long id;

  String name;

  String sourceType;
  Boolean enabled;

  String url;

  @Enumerated(EnumType.STRING)
  NewsCategory category;

  LocalDateTime createdAt;

  LocalDateTime updatedAt;
  
}
