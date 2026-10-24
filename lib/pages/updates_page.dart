import 'package:flutter/material.dart';

import '../app_version.dart';
import '../core/app_state.dart';
import '../models/release_notes.dart';
import '../services/update_service.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';

/// 独立的「更新记录」页面。
///
/// 数据优先来自 GitHub Release（真实发布说明），
/// 网络不可用时回退到随包内置的 [kReleaseNotes]，保证离线也能看到历史。
class UpdatesPage extends StatefulWidget {
  const UpdatesPage({super.key});

  @override
  State<UpdatesPage> createState() => _UpdatesPageState();
}

class _UpdatesPageState extends State<UpdatesPage> {
  List<ReleaseInfo>? _releases;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final list = await UpdateService.history(proxy: AppState.instance.settings.githubProxy);
    if (!mounted) return;
    setState(() {
      _releases = list;
      _loading = false;
      if (list.isEmpty) _error = '暂时无法获取在线更新记录，已展示内置日志';
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Scaffold(
      appBar: AppBar(
        shape: Border(bottom: BorderSide(color: AppTheme.lineOf(b), width: AppTheme.stroke)),
        titleSpacing: 0,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('更新记录'),
            Text(
              '当前版本 v$appVersion',
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w400,
                color: AppTheme.ink2Of(b),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: '刷新',
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh_rounded, size: 20),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
              children: [
                if (_error != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: InfoNote(text: _error!, icon: Icons.cloud_off_rounded),
                  ),
                if (_releases != null && _releases!.isNotEmpty)
                  for (final r in _releases!) _ReleaseCard(release: r)
                else
                  for (final n in kReleaseNotes) _LocalCard(note: n),
                const SizedBox(height: 6),
                Text(
                  '更新源：$releaseRepo',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 10.5, color: AppTheme.ink3),
                ),
              ],
            ),
    );
  }
}

class _ReleaseCard extends StatelessWidget {
  final ReleaseInfo release;

  const _ReleaseCard({required this.release});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final current = release.version == appVersion;
    final items = _parseBody(release.body);

    return SectionCard(
      title: 'v${release.version}',
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (current)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: AppTheme.selectedFillOf(b),
                borderRadius: BorderRadius.circular(AppTheme.radPill),
              ),
              child: Text(
                '当前版本',
                style: TextStyle(
                  fontSize: 10.5,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.brandOf(b),
                ),
              ),
            ),
          if (release.publishedAt != null)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Text(
                _date(release.publishedAt!),
                style: TextStyle(fontSize: 11, color: AppTheme.ink2Of(b)),
              ),
            ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (items.isEmpty)
            Text(
              '本次更新包含体验优化与问题修复。',
              style: TextStyle(fontSize: 12.5, height: 1.6, color: AppTheme.ink2Of(b)),
            )
          else
            for (final line in items)
              Padding(
                padding: const EdgeInsets.only(bottom: 7),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(top: 6, right: 8),
                      child: Container(
                        width: 4,
                        height: 4,
                        decoration: BoxDecoration(
                          color: AppTheme.brandOf(b),
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                    Expanded(
                      child: Text(
                        line,
                        style: TextStyle(fontSize: 12.5, height: 1.6, color: AppTheme.inkOf(b)),
                      ),
                    ),
                  ],
                ),
              ),
        ],
      ),
    );
  }

  static String _date(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  /// 把 Markdown 风格 release 说明压平成条目列表。
  static List<String> _parseBody(String body) {
    final out = <String>[];
    for (var raw in body.split('\n')) {
      var line = raw.trim();
      if (line.isEmpty) continue;
      if (line.startsWith('#')) continue;
      if (line.startsWith('![')) continue;
      line = line.replaceFirst(RegExp(r'^[-*+]\s+'), '');
      line = line.replaceFirst(RegExp(r'^\d+\.\s+'), '');
      line = line.replaceAll('**', '').replaceAll('`', '');
      if (line.isEmpty) continue;
      out.add(line);
      if (out.length >= 12) break;
    }
    return out;
  }
}

class _LocalCard extends StatelessWidget {
  final ReleaseNote note;

  const _LocalCard({required this.note});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return SectionCard(
      title: 'v${note.version}',
      trailing: Text(
        note.date,
        style: TextStyle(fontSize: 11, color: AppTheme.ink2Of(b)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final line in [...note.items, ...note.fixes])
            Padding(
              padding: const EdgeInsets.only(bottom: 7),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 6, right: 8),
                    child: Container(
                      width: 4,
                      height: 4,
                      decoration: BoxDecoration(
                        color: AppTheme.brandOf(b),
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      line,
                      style: TextStyle(fontSize: 12.5, height: 1.6, color: AppTheme.inkOf(b)),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
