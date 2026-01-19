package com.example.springboot3newsreader.repositories;

import org.springframework.data.repository.ListCrudRepository;

import com.example.springboot3newsreader.models.FeedItem;

public interface FeedItemRepository extends ListCrudRepository<FeedItem, Long> {
  
}
