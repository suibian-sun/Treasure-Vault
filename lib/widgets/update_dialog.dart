import 'dart:io';

import 'package:flutter/material.dart';

import '../app_version.dart';
import '../core/app_state.dart';
import '../core/utils.dart';
import '../services/update_downloader.dart';
import '../services/update_service.dart';
import '../theme/app_theme.dart';

/// 弹出更新对话框（可关闭）。
///
/// 交互约定：
/// * 右上角与底部「关闭」都可退出；
/// * 点「更新」后在同一弹窗内显示下载进度（百分比 / 已下载 / 速度）；
/// * 下载完成自动调用系统安装器，无需二次确认；
/// * 下载过程中关闭会中断下载（不残留半包影响下次续传）。
Future<void> showUpdateDialog(
  BuildContext context, {
  required ReleaseInfo release,
  required String proxy,
  String? abi,
  bool force = false,
}) {
  return showDialog<void>(
    context: context,
    barrierDismissible: true,
    builder: (_) => _UpdateDialog(release: release, proxy: proxy, abi: abi),
  );
}

class _UpdateDialog extends StatefulWidget {
  final ReleaseInfo release;
  final String proxy;
  final String? abi;

  const _UpdateDialog({required this.release, required this.proxy, this.abi});

  @override
  State<_UpdateDialog> createState() => _UpdateDialogState();
}

class _UpdateDialogState extends State<_UpdateDialog> {
  bool _downloading = false;
  bool _installing = false;
  bool _finished = false;
  bool _cancelled = false;
  String? _error;
  String? _apkPath;
  DownloadProgress? _progress;

  Future<void> _startDownload() async {
    final url = UpdateService.pickApk(widget.release, widget.abi);
    if (url == null) {
      setState(() => _error = '该版本未提供安装包，请前往发布页手动下载');
      return;
    }
    setState(() {
      _downloading = true;
      _error = null;
      _cancelled = false;
    });

    final dir = '${AppState.instance.dataDir}${Platform.pathSeparator}updates';
    try {
      final path = await UpdateDownloader.download(
        url: UpdateService.applyProxy(url, widget.proxy),
        dir: dir,
        version: widget.release.version,
        onProgress: (p) {
          if (!mounted) return;
          setState(() => _progress = p);
        },
        isCancelled: () async => _cancelled || !mounted,
      );
      if (!mounted) return;
      setState(() {
        _downloading = false;
        _installing = true;
        _apkPath = path;
      });
      final ok = await UpdateDownloader.install(path);
      if (!mounted) return;
      setState(() {
        _installing = false;
        _finished = true;
        if (!ok) _error = '无法拉起安装器，请允许「安装未知应用」后重试';
      });
    } on UpdateException catch (e) {
      if (!mounted) return;
      setState(() {
        _downloading = false;
        _error = e.message;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _downloading = false;
        _error = '下载失败：$e';
      });
    }
  }

  void _close() {
    _cancelled = true;
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final p = _progress;
    final percent = p == null ? 0.0 : p.ratio;

    return Dialog(
      insetPadding: const EdgeInsets.symmetric(horizontal: 22),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 18, 12, 0),
              child: Row(
                children: [
                  Container(
                    width: 34,
                    height: 34,
                    decoration: BoxDecoration(
                      color: AppTheme.brandOf(b).withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(AppTheme.radSmall),
                    ),
                    child: Icon(Icons.system_update_alt_rounded, size: 18, color: AppTheme.brandOf(b)),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '发现新版本 v${widget.release.version}',
                          style: TextStyle(
                            fontSize: 15.5,
                            fontWeight: FontWeight.w700,
                            color: AppTheme.inkOf(b),
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '当前版本 v$appVersion',
                          style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: _close,
                    visualDensity: VisualDensity.compact,
                    icon: const Icon(Icons.close_rounded, size: 18),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '更新内容',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: AppTheme.ink2Of(b),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    constraints: const BoxConstraints(maxHeight: 190),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.fieldOf(b),
                      borderRadius: BorderRadius.circular(AppTheme.radMedium),
                    ),
                    child: SingleChildScrollView(
                      child: Text(
                        _cleanBody(widget.release.body),
                        style: TextStyle(fontSize: 12.5, height: 1.6, color: AppTheme.inkOf(b)),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            if (_downloading || _installing || _finished || _error != null)
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 14, 20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: _installing || _finished ? 1 : percent,
                        minHeight: 6,
                        backgroundColor: AppTheme.brandOf(b).withValues(alpha: 0.12),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _statusText(percent),
                      style: TextStyle(
                        fontSize: 11.5,
                        height: 1.5,
                        color: _error != null ? const Color(0xFFDC2626) : AppTheme.ink2Of(b),
                      ),
                    ),
                  ],
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
              child: Row(
                children: [
                  if (_finished && _error != null)
                    TextButton(
                      onPressed: () async {
                        final path = _apkPath;
                        if (path == null) return;
                        final ok = await UpdateDownloader.install(path);
                        if (!mounted) return;
                        setState(() => _error = ok ? null : '仍无法拉起安装器，请检查系统设置');
                      },
                      child: const Text('重试安装'),
                    ),
                  const Spacer(),
                  TextButton(
                    onPressed: _close,
                    child: Text(_downloading ? '取消' : '关闭'),
                  ),
                  const SizedBox(width: 8),
                  FilledButton(
                    onPressed: (_downloading || _installing || _finished)
                        ? null
                        : _startDownload,
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 11),
                    ),
                    child: Text(
                      _finished
                          ? '已完成'
                          : _downloading
                              ? '下载中'
                              : '更新',
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _statusText(double percent) {
    if (_error != null) return _error!;
    if (_installing) return '下载完成，正在拉起系统安装程序…';
    if (_finished) return '安装包已就绪：${_apkPath ?? ''}';
    final p = _progress;
    if (p == null) return '正在连接下载服务器…';
    final got = formatBytes(p.received);
    final total = p.total > 0 ? formatBytes(p.total) : '未知';
    final speed = p.speed > 0 ? ' · ${formatBytes(p.speed)}/s' : '';
    return '$got / $total（${(percent * 100).toStringAsFixed(1)}%）$speed';
  }

  /// Release body 可能是 Markdown，弹窗内做轻量清洗（去掉图片与标题符号）。
  static String _cleanBody(String body) {
    if (body.trim().isEmpty) return '本次更新包含体验优化与问题修复。';
    return body
        .split('\n')
        .map((l) => l.replaceFirst(RegExp(r'^#{1,6}\s*'), ''))
        .join('\n')
        .replaceAll(RegExp(r'!\[[^\]]*\]\([^)]*\)'), '')
        .trim();
  }
}

/// 手动检查更新：查询 → 弹窗 / 提示。
Future<void> checkUpdateManually(BuildContext context, {bool silent = false}) async {
  final state = AppState.instance;
  final proxy = state.settings.githubProxy;
  if (!silent) toast(context, '正在检查更新…');
  final release = await UpdateService.latest(proxy: proxy);
  if (!context.mounted) return;
  if (release == null) {
    if (!silent) {
      toast(context, '检查失败，请检查网络或在「我的」中切换加速源', error: true);
    }
    return;
  }
  if (UpdateService.isNewer(release.version, appVersion)) {
    state.markChecked(release.version);
    if (context.mounted) {
      await showUpdateDialog(
        context,
        release: release,
        proxy: proxy,
        abi: await _deviceAbi(),
      );
    }
    return;
  }
  state.markChecked('');
  if (!silent) toast(context, '已是最新版本 v$appVersion');
}

/// 读取设备 ABI 用于挑选安装包；失败返回 null（由 pickApk 兜底）。
Future<String?> _deviceAbi() async {
  if (!Platform.isAndroid) return null;
  try {
    final r = await Process.run('getprop', ['ro.product.cpu.abi']);
    final v = (r.stdout as String).trim();
    return v.isEmpty ? null : v;
  } catch (_) {
    return null;
  }
}
