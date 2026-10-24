import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:treasure_vault/app_version.dart';
import 'package:treasure_vault/core/app_state.dart';
import 'package:treasure_vault/core/tool.dart';
import 'package:treasure_vault/core/utils.dart';
import 'package:treasure_vault/main.dart';
import 'package:treasure_vault/tools/registry.dart';

void main() {
  test('工具注册表至少 50 个且 id 唯一', () {
    expect(kTools.length, greaterThanOrEqualTo(50));
    final ids = kTools.map((t) => t.id).toSet();
    expect(ids.length, kTools.length, reason: '存在重复的工具 id');
    // 每个分类都必须有工具，且都能被搜索到
    for (final c in Categories.all) {
      expect(toolsOf(c), isNotEmpty, reason: '分类 ${c.name} 没有工具');
    }
    expect(kTools.every((t) => t.name.isNotEmpty && t.desc.isNotEmpty), isTrue);
  });

  test('app_version.dart 与 pubspec.yaml 版本一致（CI 同样会校验）', () {
    final pub = File('pubspec.yaml').readAsStringSync();
    final m = RegExp(r'version:\s*([0-9]+\.[0-9]+\.[0-9]+)\+([0-9]+)').firstMatch(pub);
    expect(m, isNotNull, reason: 'pubspec.yaml 缺少 version 字段');
    expect(m!.group(1), appVersion);
    expect(int.parse(m.group(2)!), appBuildNumber);
  });

  test('格式化工具函数', () {
    expect(formatBytes(0), '0 B');
    expect(formatBytes(1024), '1.00 KB');
    expect(trimNumber(1.2300), '1.23');
    expect(trimNumber(2), '2');
    expect(parseNum('12%'), closeTo(0.12, 1e-9));
    expect(parseNum('1,234'), 1234);
  });

  testWidgets('应用可以启动并显示三个底部 Tab', (tester) async {
    // 测试环境禁用启动自动检查更新，避免真实网络请求留下未完成的定时器
    AppState.instance.settings.autoCheckUpdate = false;
    await tester.pumpWidget(const TreasureVaultApp());
    await tester.pump(const Duration(milliseconds: 300));

    expect(find.text('首页'), findsOneWidget);
    expect(find.text('全部'), findsOneWidget);
    expect(find.text('我的'), findsOneWidget);

    // 切到「全部」页，应能看到工具列表
    await tester.tap(find.text('全部'));
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('全部工具'), findsOneWidget);

    // 切到「我的」页
    await tester.tap(find.text('我的'));
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('应用设置与更新'), findsOneWidget);
  });
}
