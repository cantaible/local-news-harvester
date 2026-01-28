import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';
import 'package:flutter_news_application/config/app_config.dart';
import 'package:flutter_news_application/article_web_view_page.dart';
import 'package:flutter_news_application/models/news_article.dart';
import 'package:flutter_news_application/models/news_filter_state.dart';
import 'package:flutter_news_application/widgets/news_card.dart';
import 'package:flutter_news_application/widgets/news_filter_bar.dart';

class NewsArticlesPage extends StatefulWidget {
  const NewsArticlesPage({super.key, required this.categoryKey});

  final String categoryKey;

  @override
  State<NewsArticlesPage> createState() => _NewsArticlesPageState();
}

class _NewsArticlesPageState extends State<NewsArticlesPage> {
  // 当前筛选状态（后续由筛选条修改）
  NewsFilterState _filterState = const NewsFilterState();
  // 文章数据列表（后续从 API 获取）
  List<NewsArticle> _articles = <NewsArticle>[];
  // 是否正在加载
  bool _isLoading = false;
  // 请求错误信息（用于页面内展示）
  String? _errorMessage;
  // 是否正在刷新（按钮触发）
  bool _isRefreshing = false;
  // 列表滚动控制器
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    // 进入页面时拉取数据
    _fetchArticles();
  }

  @override
  void didUpdateWidget(covariant NewsArticlesPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.categoryKey != widget.categoryKey) {
      _fetchArticles();
    }
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _fetchArticles() async {
    // 开始请求：打开 loading
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    // await Future<void>.delayed(const Duration(seconds: 2));
    final HttpClient client = HttpClient();
    try {
      final Uri uri = Uri.parse(
        '${AppConfig.baseUrl}/api/categories/${widget.categoryKey}/newsarticles',
      );
      debugPrint('Fetching articles: $uri');
      final HttpClientRequest request = await client.getUrl(uri);
      final HttpClientResponse response =
          await request.close().timeout(const Duration(seconds: 10));
      final String body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        debugPrint('Fetch failed: ${response.statusCode} body=$body');
        throw HttpException('Status ${response.statusCode}', uri: uri);
      }
      final Map<String, dynamic> json =
          jsonDecode(body) as Map<String, dynamic>;
      final List<dynamic> data = json['data'] as List<dynamic>;
      final List<NewsArticle> articles = <NewsArticle>[];
      for (final dynamic item in data) {
        try {
          articles.add(NewsArticle.fromJson(item as Map<String, dynamic>));
        } catch (error) {
          debugPrint('Failed to parse article: $item, error=$error');
          rethrow;
        }
      }
      if (!mounted) {
        return;
      }
      setState(() {
        _articles = articles;
      });
    } catch (error) {
      debugPrint('Fetch exception: $error');
      if (mounted) {
        // 请求失败：记录错误消息，用于页面展示
        setState(() {
          _errorMessage = error.toString();
        });
      }
    } finally {
      // 不管成功失败，都关闭 loading
      client.close();
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final List<NewsArticle> visibleArticles = _applyFilters(_articles);
    // 页面骨架：上方筛选区、下方列表区
    return Stack(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
          // 顶部筛选区：先接入组件，逻辑后续再加
          NewsFilterBar(
            // 当前筛选状态（由父组件传入）
            state: _filterState,
            // 可用来源列表（用于渲染来源筛选选项）
            availableSources: _collectSources(_articles),
            // 可用标签列表（用于渲染标签筛选选项）
            availableTags: const <String>[],
            // 筛选变更回调（后续在这里更新状态）
            onChanged: (next) {
              setState(() {
                _filterState = next;
              });
            },
          ),
          const SizedBox(height: 16),
          // 下方列表区：加载/错误/正常三种情况
          Expanded(
            child: () {
              if (_isLoading) {
                return const Center(child: CircularProgressIndicator());
              }
              if (_errorMessage != null) {
                // 有错误时显示错误文案
                return Center(
                  child: Text(
                    _errorMessage!,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Theme.of(context).colorScheme.error,
                        ),
                    textAlign: TextAlign.center,
                  ),
                );
              }
              if (visibleArticles.isEmpty) {
                return Center(
                  child: Text(
                    '没有匹配的新闻',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                  ),
                );
              }
              // 正常状态：渲染文章列表（瀑布流双列）
              return MasonryGridView.count(
                controller: _scrollController,
                crossAxisCount: 2,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                itemCount: visibleArticles.length,
                itemBuilder: (context, index) {
                  return NewsCard(
                    article: visibleArticles[index],
                    onTap: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => ArticleWebViewPage(
                            url: visibleArticles[index].sourceUrl,
                          ),
                        ),
                      );
                    },
                  );
                },
              );
            }(),
          ),
            ],
          ),
        ),
        Positioned(
          right: 16,
          bottom: 16,
          child: FloatingActionButton.small(
            onPressed: _isRefreshing ? null : _refreshArticles,
            child: _isRefreshing
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.refresh),
          ),
        ),
      ],
    );
  }

  List<NewsArticle> _applyFilters(List<NewsArticle> input) {
    final List<NewsArticle> result = <NewsArticle>[];
    // 关键词筛选（标题 + 摘要）
    final String keyword = _filterState.keyword.trim().toLowerCase();
    // 来源筛选
    final Set<String> sources = _filterState.selectedSources;
    // 标签筛选
    final Set<String> tags = _filterState.selectedTags;
    // 日期范围筛选
    final DateTimeRange? range = _filterState.selectedDateRange;
    for (final NewsArticle article in input) {
      if (keyword.isNotEmpty) {
        final String text =
            '${article.title} ${article.summary}'.toLowerCase();
        if (!text.contains(keyword)) {
          continue;
        }
      }
      if (sources.isNotEmpty && !sources.contains(article.sourceName)) {
        continue;
      }
      if (tags.isNotEmpty) {
        final List<String> articleTags = article.tags ?? const <String>[];
        bool matched = false;
        for (final String tag in articleTags) {
          if (tags.contains(tag)) {
            matched = true;
            break;
          }
        }
        if (!matched) {
          continue;
        }
      }
      if (range != null) {
        final DateTime date = article.publishedAt.toLocal();
        final DateTime start = DateTime(
          range.start.year,
          range.start.month,
          range.start.day,
        );
        final DateTime end = DateTime(
          range.end.year,
          range.end.month,
          range.end.day,
          23,
          59,
          59,
        );
        if (date.isBefore(start) || date.isAfter(end)) {
          continue;
        }
      }
      result.add(article);
    }
    // 排序：最新优先 / 最早优先
    result.sort((a, b) {
      final int compare = a.publishedAt.compareTo(b.publishedAt);
      if (_filterState.sortOrder == SortOrder.latest) {
        return -compare;
      }
      return compare;
    });
    return result;
  }

  List<String> _collectSources(List<NewsArticle> input) {
    final Set<String> sources = <String>{};
    for (final NewsArticle article in input) {
      sources.add(article.sourceName);
    }
    return sources.toList()..sort();
  }

  Future<List<NewsArticle>> _fetchRefreshArticles() async {
    final HttpClient client = HttpClient();
    try {
      final Uri uri =
          Uri.parse('${AppConfig.baseUrl}/api/newsarticles/refresh');
      debugPrint('Refreshing articles: $uri');
      final HttpClientRequest request = await client.getUrl(uri);
      final HttpClientResponse response =
          await request.close().timeout(const Duration(seconds: 10));
      final String body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        debugPrint('Refresh failed: ${response.statusCode} body=$body');
        throw HttpException('Status ${response.statusCode}', uri: uri);
      }
      final Map<String, dynamic> json =
          jsonDecode(body) as Map<String, dynamic>;
      final List<dynamic> data = json['data'] as List<dynamic>;
      return data
          .map((item) => NewsArticle.fromJson(item as Map<String, dynamic>))
          .toList();
    } finally {
      client.close();
    }
  }

  List<NewsArticle> _mergeArticles(
    List<NewsArticle> current,
    List<NewsArticle> updates,
  ) {
    final Map<int, NewsArticle> map = <int, NewsArticle>{};
    for (final NewsArticle article in current) {
      map[article.id] = article;
    }
    for (final NewsArticle article in updates) {
      map[article.id] = article;
    }
    return map.values.toList();
  }

  void _scrollToTop() {
    _scrollController.animateTo(
      0,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOut,
    );
  }

  Future<void> _refreshArticles() async {
    setState(() {
      _isRefreshing = true;
    });
    try {
      final List<NewsArticle> updates = await _fetchRefreshArticles();
      if (!mounted) {
        return;
      }
      setState(() {
        _articles = _mergeArticles(_articles, updates);
      });
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('刷新失败：$error')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isRefreshing = false;
        });
      }
    }
  }

}
