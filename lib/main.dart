import 'package:flutter/material.dart';

import 'core/app_state.dart';
import 'pages/root_shell.dart';
import 'theme/app_theme.dart';
import 'tools/registry.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // ---- 内存上限（启动即设定，避免图片解码把堆顶起来） ----
  // 图片只在「图片工具」里短暂出现且都带 cacheWidth，
  // 因此把全局图片缓存压到 16 张 / 6MB 足够，换来更低的常驻内存。
  final cache = PaintingBinding.instance.imageCache;
  cache.maximumSize = 16;
  cache.maximumSizeBytes = 6 << 20;

  // 先绑定工具 id 校验器，再加载本地数据：旧版本残留的失效 id 会被清掉。
  AppState.instance.bindToolValidator(toolExists);
  AppState.instance.init();

  runApp(const TreasureVaultApp());
}

/// 应用根组件。
///
/// `AppScope` 必须位于 `MaterialApp` **之上**，否则 Navigator 推出的工具页
/// 会处在 Provider 作用域之外，拿不到全局状态。
/// 主题切换通过独立的 ValueNotifier 只重建 MaterialApp，不牵动业务页面。
class TreasureVaultApp extends StatelessWidget {
  const TreasureVaultApp({super.key});

  @override
  Widget build(BuildContext context) {
    return AppScope(
      state: AppState.instance,
      child: ValueListenableBuilder<ThemeMode>(
        valueListenable: AppState.instance.themeNotifier,
        builder: (_, mode, __) {
          return MaterialApp(
            title: '宝藏盒',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.light(),
            darkTheme: AppTheme.dark(),
            themeMode: mode,
            home: const RootShell(),
          );
        },
      ),
    );
  }
}
