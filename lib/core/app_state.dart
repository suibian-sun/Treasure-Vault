import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';

import '../core/native_bridge.dart';
import '../models/app_settings.dart';

/// 全局应用状态（单例 + ChangeNotifier）。
///
/// 刻意不使用 Provider / Riverpod / Bloc 等状态管理库：
/// 本应用只有一份全局状态，直接用 `ChangeNotifier` + `InheritedNotifier`
/// 即可，省掉一层框架依赖与其常驻对象。
class AppState extends ChangeNotifier {
  AppState._();
  static final AppState instance = AppState._();

  AppSettings settings = AppSettings();

  /// 收藏的工具 id（有序：最近收藏在后）。
  final List<String> favorites = [];

  /// 最近使用（最新在前，最多 [maxRecents] 条）。
  final List<String> recents = [];
  static const int maxRecents = 10;

  /// 主题模式（独立 ValueNotifier，避免切换主题时重建业务页面）。
  final ValueNotifier<ThemeMode> themeNotifier = ValueNotifier(ThemeMode.system);

  bool ready = false;

  /// 初始化完成信号（页面用它等待本地设置加载，避免轮询定时器）。
  final Completer<void> _readyCompleter = Completer<void>();
  Future<void> get readyFuture => _readyCompleter.future;

  String _dataDir = '';
  String get dataDir => _dataDir;

  File get _file => File('$_dataDir/settings.json');

  Timer? _saveTimer;

  Future<void> init() async {
    try {
      final dirs = await NativeBridge.appDirs();
      _dataDir = dirs['files'] ?? Directory.systemTemp.path;
      final dir = Directory(_dataDir);
      if (!await dir.exists()) await dir.create(recursive: true);
      if (await _file.exists()) {
        final raw = await _file.readAsString();
        if (raw.trim().isNotEmpty) {
          final map = jsonDecode(raw) as Map<String, dynamic>;
          settings.applyJson((map['settings'] as Map?)?.cast<String, dynamic>() ?? {});
          final f = map['favorites'];
          if (f is List) favorites.addAll(f.map((e) => e.toString()));
          final r = map['recents'];
          if (r is List) recents.addAll(r.map((e) => e.toString()));
        }
      }
      _pruneUnknown();
    } catch (_) {
      // 存储异常不应阻断应用启动
    }
    themeNotifier.value = settings.theme.mode;
    ready = true;
    if (!_readyCompleter.isCompleted) _readyCompleter.complete();
    notifyListeners();
  }

  /// 清理已不存在的工具 id（版本升级后工具 id 可能变化）。
  void _pruneUnknown() {
    // 由 registry 注入校验函数，避免 core 层反向依赖 tools 层
    if (_validator == null) return;
    favorites.removeWhere((id) => !_validator!(id));
    recents.removeWhere((id) => !_validator!(id));
  }

  bool Function(String id)? _validator;
  void bindToolValidator(bool Function(String id) validator) {
    _validator = validator;
    _pruneUnknown();
  }

  // ---------------- 收藏 ----------------

  bool isFavorite(String id) => favorites.contains(id);

  void toggleFavorite(String id) {
    if (favorites.contains(id)) {
      favorites.remove(id);
    } else {
      favorites.insert(0, id);
    }
    _scheduleSave();
    notifyListeners();
  }

  // ---------------- 最近使用 ----------------

  void markUsed(String id) {
    recents.remove(id);
    recents.insert(0, id);
    if (recents.length > maxRecents) {
      recents.removeRange(maxRecents, recents.length);
    }
    _scheduleSave();
    notifyListeners();
  }

  void clearRecents() {
    recents.clear();
    _scheduleSave();
    notifyListeners();
  }

  // ---------------- 设置 ----------------

  void setTheme(ThemeSetting t) {
    settings.theme = t;
    themeNotifier.value = t.mode;
    _scheduleSave();
    notifyListeners();
  }

  void setAutoCheck(bool v) {
    settings.autoCheckUpdate = v;
    _scheduleSave();
    notifyListeners();
  }

  void setProxy(String v) {
    settings.githubProxy = v;
    _scheduleSave();
    notifyListeners();
  }

  void setLowMemory(bool v) {
    settings.lowMemoryMode = v;
    _scheduleSave();
    notifyListeners();
  }

  void setShowRecents(bool v) {
    settings.showRecents = v;
    _scheduleSave();
    notifyListeners();
  }

  void markChecked(String version) {
    settings.lastCheckAt = DateTime.now().millisecondsSinceEpoch;
    settings.lastPromptVersion = version;
    _scheduleSave();
  }

  // ---------------- 持久化（300ms 防抖，避免频繁写盘） ----------------

  void _scheduleSave() {
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 300), saveNow);
  }

  Future<void> saveNow() async {
    if (_dataDir.isEmpty) return;
    try {
      final payload = jsonEncode({
        'version': 1,
        'settings': settings.toJson(),
        'favorites': favorites,
        'recents': recents,
      });
      await _file.writeAsString(payload, flush: false);
    } catch (_) {
      // 忽略写入失败
    }
  }
}

/// 通过 InheritedNotifier 向下暴露状态，避免引入 Provider。
class AppScope extends InheritedNotifier<AppState> {
  const AppScope({super.key, required AppState state, required super.child})
      : super(notifier: state);

  static AppState of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AppScope>()!.notifier!;

  /// 只读取、不建立依赖（用于事件回调中）。
  static AppState read(BuildContext context) =>
      context.getInheritedWidgetOfExactType<AppScope>()!.notifier!;
}
