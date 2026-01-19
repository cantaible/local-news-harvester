package com.example.springboot3newsreader.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.services.FeedItemService;

@Controller
public class FormController {

  @Autowired
  private FeedItemService feedItemService;

  @PostMapping("/feeds/new")
  public String createFeedItem(@ModelAttribute FeedItem feedItem) {
    feedItemService.save(feedItem);
    return "redirect:/feeds";
  }
  
}
