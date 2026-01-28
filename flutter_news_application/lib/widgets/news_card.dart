import 'package:flutter/material.dart';
import 'package:flutter_news_application/config/app_config.dart';
import 'package:flutter_news_application/models/news_article.dart';

class NewsCard extends StatelessWidget {
  const NewsCard({
    super.key,
    required this.article,
    this.onTap,
  });

  final NewsArticle article;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final BorderRadius borderRadius = BorderRadius.circular(16);
    final String publishedAtLabel = _formatPublishedAt(article.publishedAt);
    final String summaryText = _stripHtml(article.summary);
    final List<String> tags = article.tags ?? const <String>[];
    final String? imageUrl = article.thumbnailUrl;
    final String? proxiedImageUrl = _buildProxyImageUrl(imageUrl);

    return Card(
      child: InkWell(
        borderRadius: borderRadius,
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (proxiedImageUrl != null &&
                  proxiedImageUrl.trim().isNotEmpty) ...[
                ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: AspectRatio(
                    aspectRatio: 16 / 9,
                    child: Image.network(
                      proxiedImageUrl,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) {
                        debugPrint(
                          'Image load failed: $proxiedImageUrl, '
                          'error: $error',
                        );
                        return Container(
                          color: Theme.of(context)
                              .colorScheme
                              .surfaceContainerHighest,
                          alignment: Alignment.center,
                          child: Icon(
                            Icons.image_not_supported_outlined,
                            color: Theme.of(context)
                                .colorScheme
                                .onSurfaceVariant,
                          ),
                        );
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 12),
              ],
              Text(
                article.title,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
              ),
              const SizedBox(height: 6),
              Text(
                '${article.sourceName} | $publishedAtLabel',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              Text(
                summaryText,
                style: Theme.of(context).textTheme.labelSmall,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              if (tags.isNotEmpty) ...[
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: tags
                      .map((tag) => Chip(label: Text(tag)))
                      .toList(),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  String _formatPublishedAt(DateTime dateTime) {
    final DateTime local = dateTime.toLocal();
    final String year = local.year.toString().padLeft(4, '0');
    final String month = local.month.toString().padLeft(2, '0');
    final String day = local.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  String _stripHtml(String input) {
    final String withoutTags = input.replaceAll(RegExp(r'<[^>]*>'), '');
    return withoutTags.replaceAll(RegExp(r'\\s+'), ' ').trim();
  }

  String? _buildProxyImageUrl(String? imageUrl) {
    if (imageUrl == null || imageUrl.trim().isEmpty) {
      return null;
    }
    final String encoded = Uri.encodeComponent(imageUrl);
    return '${AppConfig.baseUrl}/api/image?url=$encoded';
  }
}
