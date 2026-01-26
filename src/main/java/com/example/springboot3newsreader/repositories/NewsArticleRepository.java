package com.example.springboot3newsreader.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

import com.example.springboot3newsreader.models.NewsArticle;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
  void deleteBySourceNameStartingWith(String prefix);

  @Query("select a.sourceURL from NewsArticle a where a.sourceURL is not null")
  List<String> findAllSourceUrls();

  @Query("select a.title from NewsArticle a where a.sourceName = :sourceName and a.title is not null order by a.id desc")
  List<String> findRecentTitlesBySourceName(@Param("sourceName") String sourceName, Pageable pageable);
}
