package com.example.springboot3newsreader.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot3newsreader.models.NewsArticle;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
  void deleteBySourceNameStartingWith(String prefix);
}
