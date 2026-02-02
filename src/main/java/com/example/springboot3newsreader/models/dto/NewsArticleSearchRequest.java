package com.example.springboot3newsreader.models.dto;

import java.util.List;
import lombok.Data;

@Data
public class NewsArticleSearchRequest {
    private String category;
    private String keyword;
    private List<String> sources;
    private List<String> tags;
    private String startDate; // yyyy-MM-dd
    private String endDate; // yyyy-MM-dd
    private String sortOrder; // latest, oldest
}
