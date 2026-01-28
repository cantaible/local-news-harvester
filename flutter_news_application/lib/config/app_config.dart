import 'dart:io';

class AppConfig {
  static String get baseUrl {
    if (Platform.isAndroid) {
      return 'http://127.0.0.1:8080';
    }
    if (Platform.isIOS) {
      return 'http://127.0.0.1:8080';
    }
    return 'http://localhost:8080';
  }
}
