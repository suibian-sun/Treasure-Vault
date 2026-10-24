import 'package:flutter/material.dart';

import '../app_version.dart';
import '../core/app_state.dart';
import '../core/utils.dart';
import '../models/app_settings.dart';
import '../theme/app_theme.dart';
import '../tools/registry.dart';
import '../widgets/kit.dart';
import '../widgets/page_header.dart';
import '../widgets/update_dialog.dart';
import 'updates_page.dart';

/// 我的（设置）。
class MinePage extends StatelessWidget {
  const MinePage({super.key});

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final s = state.settings;
    final b = Theme.of(context).brightness;

    return Column(
      children: [
        const PageHeader(title: '我的', subtitle: '应用设置与更新'),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
            children: [
              _AppCard(state: state),
              SectionCard(
                title: '更新',
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Column(
                  children: [
                    SettingRow(
                      icon: Icons.system_update_alt_rounded,
                      title: '检查更新',
                      subtitle: '当前 v$appVersion',
                      showDivider: true,
                      onTap: () => checkUpdateManually(context),
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                    SettingRow(
                      icon: Icons.history_rounded,
                      title: '更新记录',
                      subtitle: '查看每个版本的改动',
                      showDivider: true,
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute<void>(builder: (_) => const UpdatesPage()),
                      ),
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                    SettingRow(
                      icon: Icons.autorenew_rounded,
                      title: '自动检查更新',
                      subtitle: '启动时静默检查，有新版本才提示',
                      showDivider: true,
                      trailing: Switch.adaptive(
                        value: s.autoCheckUpdate,
                        onChanged: state.setAutoCheck,
                      ),
                    ),
                    SettingRow(
                      icon: Icons.bolt_rounded,
                      title: '下载加速源',
                      subtitle: _proxyLabel(s.githubProxy),
                      onTap: () => _pickProxy(context, state),
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                  ],
                ),
              ),
              SectionCard(
                title: '外观',
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Column(
                  children: [
                    SettingRow(
                      icon: Icons.dark_mode_outlined,
                      title: '主题',
                      subtitle: s.theme.label,
                      showDivider: true,
                      onTap: () => _pickTheme(context, state),
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                    SettingRow(
                      icon: Icons.memory_rounded,
                      title: '低内存模式',
                      subtitle: '关闭页面过渡动画，进一步降低内存与绘制开销',
                      trailing: Switch.adaptive(
                        value: s.lowMemoryMode,
                        onChanged: state.setLowMemory,
                      ),
                    ),
                  ],
                ),
              ),
              SectionCard(
                title: '首页',
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Column(
                  children: [
                    SettingRow(
                      icon: Icons.history_toggle_off_rounded,
                      title: '显示最近使用',
                      subtitle: '关闭后首页只保留收藏',
                      showDivider: true,
                      trailing: Switch.adaptive(
                        value: s.showRecents,
                        onChanged: state.setShowRecents,
                      ),
                    ),
                    SettingRow(
                      icon: Icons.delete_outline_rounded,
                      title: '清除使用记录',
                      subtitle: '已记录 ${state.recents.length} 个工具',
                      onTap: () async {
                        state.clearRecents();
                        toast(context, '已清除最近使用记录');
                      },
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                  ],
                ),
              ),
              SectionCard(
                title: '关于',
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Column(
                  children: [
                    SettingRow(
                      icon: Icons.info_outline_rounded,
                      title: '版本信息',
                      subtitle: 'v$appVersion ($appBuildNumber) · ${kTools.length} 个工具',
                      onTap: () => _showAbout(context),
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                    SettingRow(
                      icon: Icons.cloud_outlined,
                      title: '更新源',
                      subtitle: releaseRepo,
                      showDivider: true,
                      onTap: () => copyText(context, releaseRepo),
                      trailing: Icon(Icons.copy_rounded, size: 15, color: AppTheme.ink3),
                    ),
                    SettingRow(
                      icon: Icons.brush_outlined,
                      title: '界面设计',
                      subtitle: '沿用 SunBuilder-grpc 的 Minimal 设计语言',
                      onTap: () => copyText(context, '设计参考：SunBuilder-grpc'),
                      trailing: Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Column(
                  children: [
                    Text(
                      '$appName · $appNameEn',
                      style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '零第三方依赖 · 数据全部留在本机',
                      style: TextStyle(fontSize: 10.5, color: AppTheme.ink3),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  static String _proxyLabel(String url) {
    if (url.trim().isEmpty) return '直连 GitHub';
    for (final e in AppSettings.proxyPresets) {
      if (e.key == url) return e.value;
    }
    return url;
  }

  Future<void> _pickProxy(BuildContext context, AppState state) async {
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('下载加速源'),
        contentPadding: const EdgeInsets.only(top: 8, bottom: 8),
        content: SizedBox(
          width: 320,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                for (final e in AppSettings.proxyPresets)
                  _ChoiceTile(
                    label: e.value,
                    selected: e.key == state.settings.githubProxy,
                    onTap: () {
                      state.setProxy(e.key);
                      Navigator.of(ctx).pop();
                    },
                  ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('取消')),
        ],
      ),
    );
  }

  Future<void> _pickTheme(BuildContext context, AppState state) async {
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('主题'),
        contentPadding: const EdgeInsets.only(top: 8, bottom: 8),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final t in ThemeSetting.values)
              _ChoiceTile(
                label: t.label,
                selected: t == state.settings.theme,
                onTap: () {
                  state.setTheme(t);
                  Navigator.of(ctx).pop();
                },
              ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('取消')),
        ],
      ),
    );
  }

  void _showAbout(BuildContext context) {
    final b = Theme.of(context).brightness;
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: AppTheme.brandOf(b),
                borderRadius: BorderRadius.circular(AppTheme.radSmall),
              ),
              child: const Icon(Icons.inventory_2_rounded, color: Colors.white, size: 20),
            ),
            const SizedBox(width: 10),
            const Text('宝藏盒'),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '一个把常用小工具装进兜里的 App：${kTools.length} 个工具全部离线运行，'
              '不申请多余权限，不上传任何数据。',
              style: const TextStyle(fontSize: 13, height: 1.6),
            ),
            const SizedBox(height: 12),
            KVRow(k: '版本', v: 'v$appVersion ($appBuildNumber)'),
            KVRow(k: '工具数', v: '${kTools.length}'),
            KVRow(k: '包名', v: appPackageId),
            KVRow(k: '更新源', v: releaseRepo),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('知道了')),
        ],
      ),
    );
  }
}

/// 单选行（不依赖 RadioListTile，避免新版本 Flutter 的 RadigGroup 迁移问题）。
class _ChoiceTile extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _ChoiceTile({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        child: Row(
          children: [
            Icon(
              selected ? Icons.radio_button_checked : Icons.radio_button_off,
              size: 18,
              color: selected ? AppTheme.brandOf(b) : AppTheme.ink3,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                  color: selected ? AppTheme.brandOf(b) : AppTheme.inkOf(b),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AppCard extends StatelessWidget {
  final AppState state;

  const _AppCard({required this.state});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.surfaceOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radLarge),
        border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
      ),
      child: Row(
        children: [
          Container(
            width: 50,
            height: 50,
            decoration: BoxDecoration(
              color: AppTheme.brandOf(b),
              borderRadius: BorderRadius.circular(AppTheme.radMedium),
            ),
            alignment: Alignment.center,
            child: const Icon(Icons.inventory_2_rounded, color: Colors.white, size: 25),
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  appName,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.inkOf(b),
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  'v$appVersion · ${kTools.length} 个工具',
                  style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                ),
              ],
            ),
          ),
          OutlinedButton(
            onPressed: () => checkUpdateManually(context),
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
              visualDensity: VisualDensity.compact,
              textStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
            ),
            child: const Text('检查更新'),
          ),
        ],
      ),
    );
  }
}
