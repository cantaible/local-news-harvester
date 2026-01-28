package com.example.springboot3newsreader.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.models.NewsCategory;
import com.example.springboot3newsreader.repositories.FeedItemRepository;

@Service
public class FeedItemService {

  @Autowired
  FeedItemRepository feedItemRepository;

  public List<FeedItem> getAll(){ 
    return feedItemRepository.findAll();
  }

  public Optional<FeedItem> getById(Long id) {
    return feedItemRepository.findById(id);
  }

  public FeedItem save(FeedItem feedItem) {
    if (feedItem.getId() == null) {
      feedItem.setCreatedAt(LocalDateTime.now());
    }

    if (feedItem.getCategory() == null) {
      feedItem.setCategory(NewsCategory.UNCATEGORIZED);
    }

    feedItem.setUpdatedAt(LocalDateTime.now());
    return feedItemRepository.save(feedItem);
  }

  public List<FeedItem> saveAll(List<FeedItem> feedItems) {
    for (FeedItem feedItem : feedItems) {
      if (feedItem.getId() == null) {
        feedItem.setCreatedAt(LocalDateTime.now());
      }
      if (feedItem.getCategory() == null) {
        feedItem.setCategory(NewsCategory.UNCATEGORIZED);
      }
      feedItem.setUpdatedAt(LocalDateTime.now());
    }
    return feedItemRepository.saveAll(feedItems);
  }

  @Transactional
  public void deleteByNamePrefix(String prefix) {
    feedItemRepository.deleteByNameStartingWith(prefix);
  }
}
