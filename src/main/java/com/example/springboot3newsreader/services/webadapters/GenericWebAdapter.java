package com.example.springboot3newsreader.services.webadapters;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class GenericWebAdapter extends BaseWebAdapter {
  @Override
  public boolean supports(String siteUrl) {
    return true;
  }
}
