import 'package:flutter/material.dart';
import 'package:flutter_news_application/models/news_filter_state.dart';

class NewsFilterBar extends StatefulWidget {
  const NewsFilterBar({
    super.key,
    required this.state,
    required this.availableSources,
    required this.availableTags,
    required this.onChanged,
  });

  // 当前筛选状态，由外部页面维护并传入
  final NewsFilterState state;
  // 可选来源列表（用于生成筛选 chips）
  final List<String> availableSources;
  // 可选标签列表（用于生成筛选 chips）
  final List<String> availableTags;
  // 当筛选条件变化时的回调
  final ValueChanged<NewsFilterState> onChanged;

  @override
  State<NewsFilterBar> createState() => _NewsFilterBarState();
}

class _NewsFilterBarState extends State<NewsFilterBar> {
  // 关键词输入框控制器
  late final TextEditingController _keywordController;
  // 是否展开完整筛选区
  bool _isExpanded = false;

  @override
  void initState() {
    super.initState();
    // 初始化输入框内容，保持与外部状态一致
    _keywordController = TextEditingController(text: widget.state.keyword);
  }

  @override
  void didUpdateWidget(NewsFilterBar oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 外部状态变化时，保持输入框文字同步
    if (oldWidget.state.keyword != widget.state.keyword &&
        _keywordController.text != widget.state.keyword) {
      _keywordController.text = widget.state.keyword;
    }
  }

  @override
  void dispose() {
    // 释放控制器，避免内存泄漏
    _keywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final NewsFilterState state = widget.state;

    // 整体采用 Card 样式，和项目内其他组件统一风格
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: AnimatedSize(
          // 展开/收起时平滑过渡
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeInOut,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header 永远显示（标题、摘要、展开按钮）
              _buildHeader(context, state),
              Offstage(
                // 把组件放在树里，但不显示、不占布局空间
                offstage: !_isExpanded,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 12),
                    // 关键词搜索
                    _buildKeywordField(context),
                    const SizedBox(height: 12),
                    // 日期选择 + 排序方式
                    _buildDateAndSort(context, state),
                    const SizedBox(height: 12),
                    // 来源筛选
                    _buildSectionTitle(context, 'Sources'),
                    const SizedBox(height: 8),
                    _buildSourceChips(context, state),
                    const SizedBox(height: 12),
                    // 标签筛选
                    _buildSectionTitle(context, 'Tags'),
                    const SizedBox(height: 8),
                    _buildTagChips(context, state),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, NewsFilterState state) {
    // 汇总当前筛选条件，展示为一行摘要
    final String summary = _buildSummary(state);

    return Row(
      children: [
        Expanded(
          child: InkWell(
            borderRadius: BorderRadius.circular(12),
            // 点击标题区域也能展开/收起
            onTap: _toggleExpanded,
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Filters',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                  if (summary.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      summary,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Theme.of(context)
                                .colorScheme
                                .onSurfaceVariant,
                          ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
        IconButton(
          // 右侧箭头旋转表示展开/收起状态
          onPressed: _toggleExpanded,
          icon: AnimatedRotation(
            turns: _isExpanded ? 0.5 : 0,
            duration: const Duration(milliseconds: 200),
            child: const Icon(Icons.expand_more),
          ),
          tooltip: _isExpanded ? 'Collapse filters' : 'Expand filters',
        ),
        TextButton(
          // 一键清空筛选
          onPressed: _handleClear,
          child: const Text('Clear'),
        ),
      ],
    );
  }

  Widget _buildKeywordField(BuildContext context) {
    return TextField(
      controller: _keywordController,
      textInputAction: TextInputAction.search,
      decoration: const InputDecoration(
        labelText: 'Keyword',
        hintText: 'Search title or summary',
        prefixIcon: Icon(Icons.search),
      ),
      // 输入变化时实时同步到外部状态
      onChanged: (value) {
        widget.onChanged(widget.state.copyWith(keyword: value));
      },
    );
  }

  Widget _buildDateAndSort(BuildContext context, NewsFilterState state) {
    final DateTimeRange? range = state.selectedDateRange;
    final String dateLabel =
        range == null ? 'Any date' : _formatDateRange(range);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            // 选择日期范围
            onPressed: () => _pickDateRange(context, state),
            icon: const Icon(Icons.event_outlined),
            label: Text(
              dateLabel,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: [
            ChoiceChip(
              label: const Text('Latest'),
              selected: state.sortOrder == SortOrder.latest,
              // 最新优先
              onSelected: (selected) {
                if (selected) {
                  widget.onChanged(
                    state.copyWith(sortOrder: SortOrder.latest),
                  );
                }
              },
            ),
            ChoiceChip(
              label: const Text('Oldest'),
              selected: state.sortOrder == SortOrder.oldest,
              // 最早优先
              onSelected: (selected) {
                if (selected) {
                  widget.onChanged(
                    state.copyWith(sortOrder: SortOrder.oldest),
                  );
                }
              },
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildSectionTitle(BuildContext context, String text) {
    return Text(
      text,
      style: Theme.of(context).textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w600,
          ),
    );
  }

  Widget _buildSourceChips(BuildContext context, NewsFilterState state) {
    if (widget.availableSources.isEmpty) {
      // 没有来源数据时的占位提示
      return Text(
        'No sources available',
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
      );
    }

    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: widget.availableSources.map((source) {
        final bool selected = state.selectedSources.contains(source);
        return FilterChip(
          label: Text(source),
          selected: selected,
          // 点击 chip 切换选中状态
          onSelected: (value) {
            final Set<String> updated = state.selectedSources.toSet();
            if (value) {
              updated.add(source);
            } else {
              updated.remove(source);
            }
            widget.onChanged(state.copyWith(selectedSources: updated));
          },
        );
      }).toList(),
    );
  }

  Widget _buildTagChips(BuildContext context, NewsFilterState state) {
    if (widget.availableTags.isEmpty) {
      // 没有标签数据时的占位提示
      return Text(
        'No tags available',
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
      );
    }

    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: widget.availableTags.map((tag) {
        final bool selected = state.selectedTags.contains(tag);
        return FilterChip(
          label: Text(tag),
          selected: selected,
          // 点击 chip 切换选中状态
          onSelected: (value) {
            final Set<String> updated = state.selectedTags.toSet();
            if (value) {
              updated.add(tag);
            } else {
              updated.remove(tag);
            }
            widget.onChanged(state.copyWith(selectedTags: updated));
          },
        );
      }).toList(),
    );
  }

  Future<void> _pickDateRange(
    BuildContext context,
    NewsFilterState state,
  ) async {
    // 日期范围选择器：限制可选范围
    final DateTime now = DateTime.now();
    final DateTimeRange initialRange = state.selectedDateRange ??
        DateTimeRange(
          start: now.subtract(const Duration(days: 7)),
          end: now,
        );
    final DateTimeRange? picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2010),
      lastDate: now.add(const Duration(days: 365)),
      initialDateRange: initialRange,
    );
    if (picked == null) {
      return;
    }
    widget.onChanged(state.copyWith(selectedDateRange: picked));
  }

  void _handleClear() {
    // 恢复默认筛选状态
    widget.onChanged(const NewsFilterState());
  }

  void _toggleExpanded() {
    // 展开/收起切换
    setState(() {
      _isExpanded = !_isExpanded;
    });
  }

  String _formatDate(DateTime dateTime) {
    // 简单的 yyyy-MM-dd 格式
    final DateTime local = dateTime.toLocal();
    final String year = local.year.toString().padLeft(4, '0');
    final String month = local.month.toString().padLeft(2, '0');
    final String day = local.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  String _formatDateRange(DateTimeRange range) {
    return '${_formatDate(range.start)} ~ ${_formatDate(range.end)}';
  }

  String _buildSummary(NewsFilterState state) {
    // 生成筛选摘要，用于 Header 展示
    final List<String> parts = <String>[];
    if (state.selectedSources.isNotEmpty) {
      parts.add('Sources ${state.selectedSources.length}');
    }
    if (state.selectedTags.isNotEmpty) {
      parts.add('Tags ${state.selectedTags.length}');
    }
    if (state.keyword.trim().isNotEmpty) {
      parts.add('Keyword ${state.keyword.trim()}');
    }
    if (state.selectedDateRange != null) {
      parts.add(_formatDateRange(state.selectedDateRange!));
    }
    parts.add(state.sortOrder == SortOrder.latest ? 'Latest' : 'Oldest');
    return parts.join(' • ');
  }
}
