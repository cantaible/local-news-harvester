import 'package:flutter/material.dart';
import 'package:flutter_news_application/news_articles_page.dart';
import 'package:flutter_news_application/news_source_page.dart';
import 'dart:convert';
import 'dart:io';
import 'package:flutter_news_application/config/app_config.dart';
import 'package:flutter_news_application/models/news_category.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFF1F4B99),
      brightness: Brightness.light,
    );

    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        useMaterial3: true,
        fontFamily: 'Noto Sans',
        colorScheme: scheme,
        scaffoldBackgroundColor: scheme.surface,
        appBarTheme: AppBarTheme(
          backgroundColor: scheme.surface,
          foregroundColor: scheme.onSurface,
          centerTitle: false,
          elevation: 0,
          titleTextStyle: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
          ),
        ),
        cardTheme: CardThemeData(
          color: scheme.surfaceContainerLow,
          elevation: 1,
          shadowColor: scheme.shadow.withValues(alpha: 0.08),
          margin: EdgeInsets.zero,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(16)),
          ),
        ),
        dividerTheme: DividerThemeData(
          color: scheme.outlineVariant,
          thickness: 1,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: scheme.surfaceContainerLow,
          labelStyle: TextStyle(color: scheme.onSurfaceVariant),
          hintStyle: TextStyle(color: scheme.onSurfaceVariant),
          enabledBorder: OutlineInputBorder(
            borderRadius: const BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: scheme.outlineVariant, width: 1),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: const BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: scheme.primary, width: 2),
          ),
          errorBorder: OutlineInputBorder(
            borderRadius: const BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: scheme.error, width: 1),
          ),
          focusedErrorBorder: OutlineInputBorder(
            borderRadius: const BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: scheme.error, width: 2),
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: scheme.primary,
            foregroundColor: scheme.onPrimary,
            shape: const RoundedRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(12)),
            ),
          ),
        ),
        chipTheme: ChipThemeData(
          backgroundColor: scheme.surfaceContainerLow,
          labelStyle: TextStyle(color: scheme.primary),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        ),
        bottomNavigationBarTheme: BottomNavigationBarThemeData(
          backgroundColor: scheme.surface,
          selectedItemColor: scheme.primary,
          unselectedItemColor: scheme.onSurfaceVariant,
          showUnselectedLabels: true,
        ),
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
} // class MyApp extends StatelessWidget

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _currentIndex = 0;
  List<NewsCategory> _categories = <NewsCategory>[];
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchCategories();
  }

  Future<void> _fetchCategories() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    final HttpClient client = HttpClient();
    try {
      final Uri uri = Uri.parse('${AppConfig.baseUrl}/api/categories');
      final HttpClientRequest request = await client.getUrl(uri);
      final HttpClientResponse response =
          await request.close().timeout(const Duration(seconds: 10));
      final String body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        throw HttpException('Status ${response.statusCode}', uri: uri);
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
        _currentIndex = 0;
      });
    } catch (error) {
      if (mounted) {
        setState(() {
          _errorMessage = error.toString();
        });
      }
    } finally {
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
    if (_isLoading) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (_errorMessage != null) {
      return Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_errorMessage!),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _fetchCategories,
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    if (_categories.isEmpty) {
      return Scaffold(
        appBar: AppBar(
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
          title: const Text('Sources'),
          actions: [
            IconButton(
              onPressed: _fetchCategories,
              icon: const Icon(Icons.refresh),
              tooltip: 'Reload categories',
            ),
          ],
        ),
        body: const NewsSourcePage(),
      );
    }

    final List<_TabItem> tabs = [
      for (final NewsCategory c in _categories)
        _TabItem(
          title: c.label,
          icon: Icons.article_outlined,
          page: NewsArticlesPage(categoryKey: c.key),
        ),
      const _TabItem(
        title: 'Sources',
        icon: Icons.source_outlined,
        page: NewsSourcePage(),
      ),
    ];

    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(tabs[_currentIndex].title),
      ),
      body: tabs[_currentIndex].page,
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        items: tabs
            .map(
              (tab) => BottomNavigationBarItem(
                icon: Icon(tab.icon),
                label: tab.title,
              ),
            )
            .toList(),
        onTap: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
      ),
    );
  }
}

class _TabItem {
  const _TabItem({
    required this.title,
    required this.icon,
    required this.page,
  });

  final String title;
  final IconData icon;
  final Widget page;
}
