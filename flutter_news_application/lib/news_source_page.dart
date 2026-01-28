import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_news_application/config/app_config.dart';
import 'package:flutter_news_application/models/news_category.dart';
import 'package:flutter_news_application/models/news_source.dart';
import 'package:flutter_news_application/widgets/news_source_list_item.dart';

class NewsSourcePage extends StatefulWidget {
  const NewsSourcePage({super.key});

  @override
  State<NewsSourcePage> createState() => _NewsSourcePageState();
}

class _NewsSourcePageState extends State<NewsSourcePage> {
  String _sourceType = 'rss';
  String _categoryKey = 'UNCATEGORIZED';
  bool _enabled = true;
  bool _isSubmitting = false;
  List<NewsSource> _sources = <NewsSource>[];
  List<NewsCategory> _categories = <NewsCategory>[];
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _urlController = TextEditingController();
  final List<String> _presetUrls = const <String>[
    'https://www.qbitai.com/',
    'https://www.jiqizhixin.com/',
    'https://aiera.com.cn/',
    'https://news.mit.edu/topic/artificial-intelligence2',
    'https://venturebeat.com/feed',
    'https://techcrunch.com/feed/',
    'https://musically.com/feed/',
    'https://www.pocketgamer.com/news/index.rss',
    'https://www.gamesindustry.biz/feed',
    'http://feeds.seroundtable.com/SearchEngineRoundtable1',
    'https://www.musicbusinessworldwide.com/feed/',
  ];

  @override
  void initState() {
    super.initState();
    // Fetch existing sources when the page is created.
    _fetchFeedItems();
    _fetchCategories();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _fetchFeedItems() async {
    // Low-level HTTP client; no extra package needed.
    final HttpClient client = HttpClient();
    try {
      final Uri uri = Uri.parse('${AppConfig.baseUrl}/api/feeditems');
      final HttpClientRequest request = await client.getUrl(uri);
      final HttpClientResponse response = await request.close();
      // Read the full response body as a string.
      final String body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        throw HttpException(
          'Failed to fetch feed items: ${response.statusCode}',
          uri: uri,
        );
      }
      // Parse JSON and convert "data" to a list of NewsSource.
      final Map<String, dynamic> json =
          jsonDecode(body) as Map<String, dynamic>;
      final List<dynamic> data = json['data'] as List<dynamic>;
      final List<NewsSource> sources = data
          .map((item) => NewsSource.fromJson(item as Map<String, dynamic>))
          .toList();

      if (!mounted) {
        return;
      }

      setState(() {
        _sources = sources;
      });
    } finally {
      // Always close the client to free resources.
      client.close();
    }
  }

  Future<void> _fetchCategories() async {
    final HttpClient client = HttpClient();
    try {
      final Uri uri = Uri.parse('${AppConfig.baseUrl}/api/categories');
      final HttpClientRequest request = await client.getUrl(uri);
      final HttpClientResponse response = await request.close();
      final String body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        throw HttpException(
          'Failed to fetch categories: ${response.statusCode}',
          uri: uri,
        );
      }
      final Map<String, dynamic> json =
          jsonDecode(body) as Map<String, dynamic>;
      final List<dynamic> data = json['data'] as List<dynamic>;
      final List<NewsCategory> categories = data
          .map((item) => NewsCategory.fromJson(item as Map<String, dynamic>))
          .where((c) => c.enabled)
          .toList()
        ..sort((a, b) => a.order.compareTo(b.order));
      if (!mounted) {
        return;
      }
      setState(() {
        _categories = categories;
        if (categories.isNotEmpty &&
            categories.every((c) => c.key != _categoryKey)) {
          _categoryKey = categories.first.key;
        }
      });
    } finally {
      client.close();
    }
  }

  Future<void> _handleSubmit() async {
    setState(() {
      _isSubmitting = true;
    });

    final String name = _nameController.text.trim();
    final String url = _urlController.text.trim();
    if (name.isEmpty || url.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Name 和 URL 不能为空')),
      );
      setState(() {
        _isSubmitting = false;
      });
      return;
    }

    final HttpClient client = HttpClient();
    try {
      final Uri uri = Uri.parse('${AppConfig.baseUrl}/feeds/new');
      final HttpClientRequest request = await client.postUrl(uri);
      request.headers.contentType =
          ContentType('application', 'x-www-form-urlencoded');
      final String now = DateTime.now().toIso8601String();
      final Map<String, String> payload = <String, String>{
        'name': name,
        'url': url,
        'sourceType': _sourceType.toUpperCase(),
        'enabled': _enabled ? '1' : '0',
        'category': _categoryKey,
        'createdAt': now,
        'updatedAt': now,
      };
      final String requestBody = payload.entries
          .map((e) => '${Uri.encodeQueryComponent(e.key)}='
              '${Uri.encodeQueryComponent(e.value)}')
          .join('&');
      debugPrint('Create source payload: $requestBody');
      request.add(utf8.encode(requestBody));
      final HttpClientResponse response = await request.close();
      final String responseBody =
          await response.transform(utf8.decoder).join();
      if (response.statusCode != 200 && response.statusCode != 201) {
        throw HttpException(
          'Failed to create source: ${response.statusCode}, $responseBody',
          uri: uri,
        );
      }
      _nameController.clear();
      _urlController.clear();
      await _fetchFeedItems();
    } catch (error) {
      debugPrint('Create source failed: $error');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('提交失败：$error')),
        );
      }
    } finally {
      client.close();
    }

    if (!mounted) {
      return;
    }

    setState(() {
      _isSubmitting = false;
    });
  } // Future<void> _handleSubmit()

  @override
  Widget build(BuildContext context) {
    final BorderRadius borderRadius = BorderRadius.circular(16);
    final List<NewsCategory> categories =
        _categories.isEmpty ? _fallbackCategories() : _categories;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Card(
            child: InkWell(
              borderRadius: borderRadius,
              onTap: () {},
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          // Expanded在 Row/Column 里让子组件“占满剩余空间”，并按 flex 比例分配。
                          flex: 3,
                          child: TextFormField(
                            controller: _nameController,
                            decoration: const InputDecoration(
                              labelText: 'Name',
                            ),
                            textInputAction: TextInputAction.next,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          flex: 2,
                          child: DropdownButtonFormField<String>(
                            initialValue: _sourceType,
                            decoration: const InputDecoration(
                              labelText: 'Source Type',
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'rss',
                                child: Text('RSS'),
                              ),
                              DropdownMenuItem(
                                value: 'web',
                                child: Text('Web'),
                              ),
                            ],
                            onChanged: (value) {
                              if (value == null) {
                                return;
                              }
                              setState(() {
                                _sourceType = value;
                              });
                            },
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Autocomplete<String>(
                      optionsBuilder: (TextEditingValue textEditingValue) {
                        if (textEditingValue.text.isEmpty) {
                          return _presetUrls;
                        }
                        return _presetUrls.where(
                          (url) => url
                              .toLowerCase()
                              .contains(textEditingValue.text.toLowerCase()),
                        );
                      },
                      onSelected: (selection) {
                        _urlController.text = selection;
                      },
                      fieldViewBuilder: (
                        context,
                        textEditingController,
                        focusNode,
                        onFieldSubmitted,
                      ) {
                        _urlController.value = textEditingController.value;
                        return TextFormField(
                          controller: textEditingController,
                          focusNode: focusNode,
                          decoration: const InputDecoration(labelText: 'URL'),
                          keyboardType: TextInputType.url,
                          textInputAction: TextInputAction.done,
                          onFieldSubmitted: (_) => onFieldSubmitted(),
                        );
                      },
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      value: _categoryKey,
                      decoration: const InputDecoration(
                        labelText: 'Category',
                      ),
                      items: categories
                          .map(
                            (c) => DropdownMenuItem<String>(
                              value: c.key,
                              child: Text(c.label),
                            ),
                          )
                          .toList(),
                      onChanged: (value) {
                        if (value == null) {
                          return;
                        }
                        setState(() {
                          _categoryKey = value;
                        });
                      },
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: Row(
                            children: [
                              Switch(
                                value: _enabled,
                                onChanged: (value) {
                                  setState(() {
                                    _enabled = value;
                                  });
                                },
                              ),
                              const Text('Enabled'),
                            ],
                          ),
                        ),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _isSubmitting ? null : _handleSubmit,
                            child: _isSubmitting
                                ? const SizedBox(
                                    width: 18,
                                    height: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Text('Add Source'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: ListView.builder(
              itemCount: _sources.length,
              itemBuilder: (context, index) {
                return Column(
                  children: [
                    const SizedBox(height: 12),
                    NewsSourceListItem(source: _sources[index]),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  List<NewsCategory> _fallbackCategories() {
    return <NewsCategory>[
      NewsCategory(
        key: 'UNCATEGORIZED',
        label: 'Uncategorized',
        order: 99,
        enabled: true,
      ),
    ];
  }
}
