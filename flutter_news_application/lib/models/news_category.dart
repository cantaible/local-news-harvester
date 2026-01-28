class NewsCategory {
  NewsCategory({
    required this.key,
    required this.label,
    required this.order,
    required this.enabled,
  });

  final String key;
  final String label;
  final int order;
  final bool enabled;

  factory NewsCategory.fromJson(Map<String, dynamic> json) {
    return NewsCategory(
      key: (json['key'] ?? '').toString(),
      label: (json['label'] ?? '').toString(),
      order: (json['order'] as num?)?.toInt() ?? 0,
      enabled: (json['enabled'] as bool?) ?? true,
    );
  }
}
