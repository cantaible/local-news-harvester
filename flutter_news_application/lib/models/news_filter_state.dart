import 'package:flutter/material.dart';

enum SortOrder {
  latest,
  oldest,
}

class NewsFilterState {
  const NewsFilterState({
    this.selectedDateRange,
    this.sortOrder = SortOrder.latest,
    this.selectedSources = const <String>{},
    this.selectedTags = const <String>{},
    this.keyword = '',
  });

  final DateTimeRange? selectedDateRange;
  final SortOrder sortOrder;
  final Set<String> selectedSources;
  final Set<String> selectedTags;
  final String keyword;

  NewsFilterState copyWith({
    DateTimeRange? selectedDateRange,
    bool clearSelectedDateRange = false,
    SortOrder? sortOrder,
    Set<String>? selectedSources,
    Set<String>? selectedTags,
    String? keyword,
  }) {
    return NewsFilterState(
      selectedDateRange: clearSelectedDateRange
          ? null
          : (selectedDateRange ?? this.selectedDateRange),
      sortOrder: sortOrder ?? this.sortOrder,
      selectedSources: selectedSources ?? this.selectedSources,
      selectedTags: selectedTags ?? this.selectedTags,
      keyword: keyword ?? this.keyword,
    );
  }
}
