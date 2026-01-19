package com.example.springboot3newsreader.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.springboot3newsreader.models.FeedItem;
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

    feedItem.setUpdatedAt(LocalDateTime.now());
    return feedItemRepository.save(feedItem);
  }
}
