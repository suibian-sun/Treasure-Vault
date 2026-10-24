import 'dart:async';
import 'dart:io';

import '../app_version.dart';
import '../core/native_bridge.dart';

/// 下载进度快照。
class DownloadProgress {
  final int received;
  final int total;
  final double speed; // bytes/s

  const DownloadProgress(this.received, this.total, this.speed);

  double get ratio => total > 0 ? (received / total).clamp(0.0, 1.0) : 0;
  bool get done => total > 0 && received >= total;
}

/// 更新包下载器：断点续传 + 完整性校验 + 自动调用系统安装器。
///
/// 使用 dart:io 的 HttpClient 直接流式写入磁盘，不把 APK 读入内存，
/// 因此 20MB 的安装包下载过程中内存增量接近于 0。
class UpdateDownloader {
  UpdateDownloader._();

  static const Duration _timeout = Duration(seconds: 30);

  /// 下载到 `dir`，返回本地文件路径。
  ///
  /// [onProgress] 会以约 200ms 的节奏回调，避免过于频繁的重建。
  static Future<String> download({
    required String url,
    required String dir,
    required String version,
    String? fileName,
    required void Function(DownloadProgress p) onProgress,
    Future<bool> Function()? isCancelled,
  }) async {
    final uri = Uri.tryParse(url);
    if (uri == null || uri.host.isEmpty || (uri.scheme != 'http' && uri.scheme != 'https')) {
      throw const UpdateException('安装包地址无效');
    }
    final folder = Directory(dir);
    if (!await folder.exists()) await folder.create(recursive: true);

    final name = _safeName(fileName ?? uri.pathSegments.last, version);
    final file = File('${folder.path}${Platform.pathSeparator}$name');
    var existing = 0;
    if (await file.exists()) existing = await file.length();

    final client = HttpClient()..connectionTimeout = _timeout;
    try {
      final req = await client.getUrl(uri).timeout(_timeout);
      req.headers.set('User-Agent', 'TreasureVault/$appVersion');
      req.followRedirects = true;
      req.maxRedirects = 5;
      if (existing > 0) req.headers.set('Range', 'bytes=$existing-');

      final resp = await req.close().timeout(_timeout);
      var total = 0;
      if (resp.statusCode == 206) {
        final cr = resp.headers.value('content-range') ?? '';
        final m = RegExp(r'/(\d+)\s*$').firstMatch(cr);
        total = m == null ? 0 : (int.tryParse(m.group(1)!) ?? 0);
      } else if (resp.statusCode == 200) {
        existing = 0;
        total = resp.contentLength > 0 ? resp.contentLength : 0;
      } else if (resp.statusCode == 416 && existing > 0) {
        // 本地已完整
        onProgress(DownloadProgress(existing, existing, 0));
        return file.path;
      } else {
        await resp.drain<void>();
        throw UpdateException('下载失败（HTTP ${resp.statusCode}）');
      }

      final sink = file.openWrite(mode: existing > 0 ? FileMode.append : FileMode.write);
      var received = existing;
      var lastTick = DateTime.now().millisecondsSinceEpoch;
      var lastBytes = received;
      var speed = 0.0;
      try {
        await for (final chunk in resp.timeout(_timeout)) {
          if (isCancelled != null && await isCancelled()) {
            throw const UpdateException('下载已取消');
          }
          sink.add(chunk);
          received += chunk.length;
          final now = DateTime.now().millisecondsSinceEpoch;
          if (now - lastTick >= 200) {
            final dt = (now - lastTick) / 1000.0;
            if (dt > 0) speed = (received - lastBytes) / dt;
            lastTick = now;
            lastBytes = received;
            onProgress(DownloadProgress(received, total > 0 ? total : received, speed));
          }
        }
      } finally {
        await sink.flush();
        await sink.close();
      }

      final actual = await file.length();
      if (total > 0 && actual < total) {
        throw const UpdateException('下载不完整，请重试');
      }
      if (!await _looksLikeZip(file)) {
        await file.delete();
        throw const UpdateException('安装包校验失败（非有效 APK）');
      }
      onProgress(DownloadProgress(actual, total > 0 ? total : actual, 0));
      return file.path;
    } on UpdateException {
      rethrow;
    } on TimeoutException {
      throw const UpdateException('下载超时，请检查网络后重试');
    } catch (e) {
      throw UpdateException('下载出错：$e');
    } finally {
      client.close(force: true);
    }
  }

  /// 调用系统安装器。
  static Future<bool> install(String path) => NativeBridge.installApk(path);

  /// APK 本质是 ZIP：校验前 4 字节魔数，拒绝被替换为任意内容。
  static Future<bool> _looksLikeZip(File f) async {
    try {
      final raf = await f.open();
      try {
        final head = await raf.read(4);
        if (head.length < 4) return false;
        return head[0] == 0x50 &&
            head[1] == 0x4B &&
            (head[2] == 0x03 || head[2] == 0x05 || head[2] == 0x07);
      } finally {
        await raf.close();
      }
    } catch (_) {
      return false;
    }
  }

  static String _safeName(String raw, String version) {
    var n = raw.split('/').last.trim();
    n = n.replaceAll(RegExp(r'[^\w.\-+]'), '_');
    if (!n.toLowerCase().endsWith('.apk')) {
      n = 'TreasureVault-$version.apk';
    }
    return n;
  }
}

class UpdateException implements Exception {
  final String message;
  const UpdateException(this.message);
  @override
  String toString() => message;
}
