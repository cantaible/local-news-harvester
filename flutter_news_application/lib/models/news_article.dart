class NewsArticle {
  NewsArticle({
    required this.id,
    required this.title,
    required this.sourceUrl,
    required this.sourceName,
    required this.publishedAt,
    required this.scrapedAt,
    required this.summary,
    required this.tags,
    required this.thumbnailUrl,
    required this.rawContent,
    this.category,
  });

  final int id;
  final String title;
  final String sourceUrl;
  final String sourceName;
  final DateTime publishedAt;
  final DateTime scrapedAt;
  final String summary;
  final List<String>? tags;
  final String? thumbnailUrl;
  final String? rawContent;
  final String? category;

  factory NewsArticle.fromJson(Map<String, dynamic> json) {
    final dynamic tagsValue = json['tags'];
    List<String>? parsedTags;
    if (tagsValue is List) {
      parsedTags = tagsValue.map((tag) => tag.toString()).toList();
    }

    final String? publishedAtValue = json['publishedAt']?.toString();
    final String? scrapedAtValue = json['scrapedAt']?.toString();

    return NewsArticle(
      id: (json['id'] as num?)?.toInt() ?? 0,
      title: (json['title'] ?? '').toString(),
      sourceUrl: (json['sourceURL'] ?? '').toString(),
      sourceName: (json['sourceName'] ?? '').toString(),
      publishedAt: publishedAtValue == null
          ? DateTime.fromMillisecondsSinceEpoch(0, isUtc: true)
          : DateTime.parse(publishedAtValue),
      scrapedAt: scrapedAtValue == null
          ? DateTime.fromMillisecondsSinceEpoch(0, isUtc: true)
          : DateTime.parse(scrapedAtValue),
      summary: (json['summary'] ?? '').toString(),
      tags: parsedTags,
      thumbnailUrl: json['tumbnailURL']?.toString(),
      rawContent: json['rawContent']?.toString(),
      category: json['category']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'sourceURL': sourceUrl,
      'sourceName': sourceName,
      'publishedAt': publishedAt.toIso8601String(),
      'scrapedAt': scrapedAt.toIso8601String(),
      'summary': summary,
      'tags': tags,
      'tumbnailURL': thumbnailUrl,
      'rawContent': rawContent,
      'category': category,
    };
  }
}
