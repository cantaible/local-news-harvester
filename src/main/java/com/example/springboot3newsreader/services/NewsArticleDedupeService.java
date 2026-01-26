package com.example.springboot3newsreader.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.NewsArticle;
import com.example.springboot3newsreader.repositories.NewsArticleRepository;

@Service
public class NewsArticleDedupeService {

  private static final int TITLE_LOOKBACK = 500;
  private static final double TITLE_SIM_THRESHOLD = 0.9;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  public List<NewsArticle> filterNewArticles(List<NewsArticle> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return new ArrayList<>();
    }

    Set<String> existingUrls = new HashSet<>();
    List<String> urls = newsArticleRepository.findAllSourceUrls();
    if (urls != null) {
      existingUrls.addAll(urls);
    }

    Map<String, List<Set<String>>> titleTokensBySource = new HashMap<>();
    Set<String> sourceNames = new HashSet<>();
    for (NewsArticle a : candidates) {
      if (a.getSourceName() != null) {
        sourceNames.add(a.getSourceName());
      }
    }
    for (String source : sourceNames) {
      List<String> titles = newsArticleRepository.findRecentTitlesBySourceName(
        source, PageRequest.of(0, TITLE_LOOKBACK));
      List<Set<String>> tokens = new ArrayList<>();
      if (titles != null) {
        for (String t : titles) {
          Set<String> tok = tokenizeTitle(t);
          if (!tok.isEmpty()) {
            tokens.add(tok);
          }
        }
      }
      titleTokensBySource.put(source, tokens);
    }

    List<NewsArticle> result = new ArrayList<>();
    for (NewsArticle a : candidates) {
      String url = a.getSourceURL();
      if (url != null && existingUrls.contains(url)) {
        continue;
      }
      String source = a.getSourceName();
      if (source == null) {
        source = "";
      }
      Set<String> tokens = tokenizeTitle(a.getTitle());
      if (!tokens.isEmpty()) {
        List<Set<String>> existingTokens = titleTokensBySource.get(source);
        if (existingTokens != null && isSimilarToAny(tokens, existingTokens)) {
          continue;
        }
      }

      result.add(a);
      if (url != null) {
        existingUrls.add(url);
      }
      if (!tokens.isEmpty()) {
        titleTokensBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(tokens);
      }
    }

    return result;
  }

  private boolean isSimilarToAny(Set<String> tokens, List<Set<String>> existingTokens) {
    for (Set<String> other : existingTokens) {
      if (jaccard(tokens, other) >= TITLE_SIM_THRESHOLD) {
        return true;
      }
    }
    return false;
  }

  private double jaccard(Set<String> a, Set<String> b) {
    if (a.isEmpty() || b.isEmpty()) {
      return 0.0;
    }
    int inter = 0;
    for (String s : a) {
      if (b.contains(s)) {
        inter++;
      }
    }
    int union = a.size() + b.size() - inter;
    return union == 0 ? 0.0 : (double) inter / (double) union;
  }

  private Set<String> tokenizeTitle(String title) {
    Set<String> tokens = new HashSet<>();
    if (title == null) {
      return tokens;
    }
    String normalized = title.toLowerCase(Locale.ROOT)
      .replaceAll("[\\p{Punct}]+", " ")
      .replaceAll("\\s+", " ")
      .trim();
    if (normalized.isEmpty()) {
      return tokens;
    }
    for (String part : normalized.split(" ")) {
      if (!part.isBlank()) {
        tokens.add(part);
      }
    }
    return tokens;
  }
}
