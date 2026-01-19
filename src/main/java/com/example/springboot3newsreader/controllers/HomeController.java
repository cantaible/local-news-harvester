package com.example.springboot3newsreader.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.springboot3newsreader.models.FeedItem;
import com.example.springboot3newsreader.services.FeedItemService;

@Controller
public class HomeController {

  @Autowired
  private FeedItemService feedItemService;

  @GetMapping("/")
  public String defaultPage(Model model) {
    List<FeedItem> feeds = feedItemService.getAll();
    model.addAttribute("feeds", feeds);
    return "index";
  }

  @GetMapping("/feeds")
  public String feedsPage(Model model) {
    List<FeedItem> feeds = feedItemService.getAll();
    model.addAttribute("feeds", feeds);

    FeedItem feedItem = new FeedItem();
    model.addAttribute("newFeed", feedItem);
    return "feeds";
  }

  
}
