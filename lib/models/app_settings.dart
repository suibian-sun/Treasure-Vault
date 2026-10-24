import 'package:flutter/material.dart';

/// 主题模式（浅色 / 深色 / 跟随系统）。
enum ThemeSetting {
  light('light', '浅色'),
  dark('dark', '深色'),
  system('system', '跟随系统');

  final String value;
  final String label;
  const ThemeSetting(this.value, this.label);

  ThemeMode get mode {
    switch (this) {
      case ThemeSetting.light:
        return ThemeMode.light;
      case ThemeSetting.dark:
        return ThemeMode.dark;
      case ThemeSetting.system:
        return ThemeMode.system;
    }
  }

  static ThemeSetting fromValue(String? v) =>
      ThemeSetting.values.firstWhere((e) => e.value == v, orElse: () => ThemeSetting.system);
}

/// 应用级设置。业务数据极简：仅收藏、最近使用与若干开关。
class AppSettings {
  ThemeSetting theme = ThemeSetting.system;

  /// 启动时自动检查更新。
  bool autoCheckUpdate = true;

  /// GitHub 加速源前缀（空串 = 直连）。
  String githubProxy = kDefaultProxy;

  /// 低内存模式：关闭页面过渡动画与涟漪，进一步压低绘制开销。
  bool lowMemoryMode = true;

  /// 首页是否展示「最近使用」。
  bool showRecents = true;

  /// 上次检查更新时间（毫秒时间戳，0 表示从未检查）。
  int lastCheckAt = 0;

  /// 已提示过的版本，避免每次启动重复弹窗。
  String lastPromptVersion = '';

  static const String kDefaultProxy = 'https://gh-proxy.com/';

  /// 内置加速源候选（空串代表直连）。
  static const List<MapEntry<String, String>> proxyPresets = [
    MapEntry('https://gh-proxy.com/', 'gh-proxy.com（默认）'),
    MapEntry('https://gh-proxy.org/', 'gh-proxy.org'),
    MapEntry('https://hk.gh-proxy.com/', 'hk.gh-proxy.com（香港）'),
    MapEntry('https://ghfast.top/', 'ghfast.top'),
    MapEntry('https://gh.llkk.cc/', 'gh.llkk.cc'),
    MapEntry('', '不使用（直连 GitHub）'),
  ];

  Map<String, dynamic> toJson() => {
        'theme': theme.value,
        'autoCheck': autoCheckUpdate,
        'proxy': githubProxy,
        'lowMemory': lowMemoryMode,
        'showRecents': showRecents,
        'lastCheckAt': lastCheckAt,
        'lastPromptVersion': lastPromptVersion,
      };

  void applyJson(Map<String, dynamic> j) {
    theme = ThemeSetting.fromValue(j['theme'] as String?);
    autoCheckUpdate = j['autoCheck'] as bool? ?? true;
    githubProxy = j['proxy'] as String? ?? kDefaultProxy;
    lowMemoryMode = j['lowMemory'] as bool? ?? true;
    showRecents = j['showRecents'] as bool? ?? true;
    lastCheckAt = (j['lastCheckAt'] as num?)?.toInt() ?? 0;
    lastPromptVersion = j['lastPromptVersion'] as String? ?? '';
  }
}
