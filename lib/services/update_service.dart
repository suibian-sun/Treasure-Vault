import 'dart:convert';
import 'dart:io';

import '../app_version.dart';

/// 远程版本信息（来自 GitHub Releases）。
class ReleaseInfo {
  final String version;
  final String tag;
  final String body;
  final String htmlUrl;
  final DateTime? publishedAt;
  final Map<String, String> assets;

  const ReleaseInfo({
    required this.version,
    required this.tag,
    required this.body,
    required this.htmlUrl,
    this.publishedAt,
    this.assets = const {},
  });
}

/// 更新检查服务：只依赖 dart:io，无 http 包依赖。
class UpdateService {
  UpdateService._();

  static const String _apiBase = 'https://api.github.com';
  static const Duration _timeout = Duration(seconds: 15);

  /// 规范化加速源前缀，非法输入返回空串（= 直连）。
  ///
  /// 仅允许 https：更新包是可执行文件，明文代理存在被替换风险。
  static String normalizeProxy(String raw) {
    var v = raw.trim();
    if (v.isEmpty) return '';
    if (RegExp(r'[\x00-\x1f\x7f]').hasMatch(v)) return '';
    if (!v.contains('://')) v = 'https://$v';
    final uri = Uri.tryParse(v);
    if (uri == null || uri.scheme != 'https' || uri.host.isEmpty) return '';
    if (uri.userInfo.isNotEmpty) return '';
    final buf = StringBuffer('${uri.scheme}://${uri.host}');
    if (uri.hasPort) buf.write(':${uri.port}');
    if (uri.path.isNotEmpty && uri.path != '/') buf.write(uri.path);
    var s = buf.toString();
    if (!s.endsWith('/')) s = '$s/';
    return s;
  }

  /// 给 GitHub 直链套用加速源；非可信 GitHub 域名原样返回。
  static String applyProxy(String url, String proxy) {
    final p = normalizeProxy(proxy);
    if (p.isEmpty) return url;
    if (!_trustedHost(url)) return url;
    if (url.startsWith(p)) return url;
    return '$p$url';
  }

  static bool _trustedHost(String url) {
    final uri = Uri.tryParse(url);
    if (uri == null || uri.scheme != 'https') return false;
    const allowed = {
      'github.com',
      'api.github.com',
      'raw.githubusercontent.com',
      'objects.githubusercontent.com',
      'codeload.github.com',
    };
    return allowed.contains(uri.host.toLowerCase());
  }

  /// 查询最新正式版本；失败返回 null。
  static Future<ReleaseInfo?> latest({String proxy = '', bool direct = false}) async {
    final p = direct ? '' : normalizeProxy(proxy);
    final uri = Uri.parse('${p.isEmpty ? '' : p}$_apiBase/repos/$releaseRepo/releases/latest');
    final json = await _getJson(uri);
    if (json == null) {
      // 加速源失败时自动回退直连，提升可达性
      if (p.isNotEmpty) {
        return _fetchOne(Uri.parse('$_apiBase/repos/$releaseRepo/releases/latest'));
      }
      return null;
    }
    return _parse(json);
  }

  /// 查询历史版本列表（用于「更新记录」页）。
  static Future<List<ReleaseInfo>> history({String proxy = '', int limit = 30}) async {
    final p = normalizeProxy(proxy);
    final uri = Uri.parse('${p.isEmpty ? '' : p}$_apiBase/repos/$releaseRepo/releases?per_page=$limit');
    var list = await _getJsonList(uri);
    if (list == null && p.isNotEmpty) {
      list = await _getJsonList(
        Uri.parse('$_apiBase/repos/$releaseRepo/releases?per_page=$limit'),
      );
    }
    if (list == null) return const [];
    final out = <ReleaseInfo>[];
    for (final e in list) {
      if (e is Map<String, dynamic>) {
        final r = _parse(e);
        if (r != null) out.add(r);
      }
    }
    out.sort((a, b) => compareVersions(b.version, a.version));
    return out;
  }

  static Future<ReleaseInfo?> _fetchOne(Uri uri) async {
    final json = await _getJson(uri);
    return json == null ? null : _parse(json);
  }

  static Future<Map<String, dynamic>?> _getJson(Uri uri) async {
    final body = await _get(uri);
    if (body == null) return null;
    try {
      final v = jsonDecode(body);
      return v is Map<String, dynamic> ? v : null;
    } catch (_) {
      return null;
    }
  }

  static Future<List<dynamic>?> _getJsonList(Uri uri) async {
    final body = await _get(uri);
    if (body == null) return null;
    try {
      final v = jsonDecode(body);
      return v is List ? v : null;
    } catch (_) {
      return null;
    }
  }

  static Future<String?> _get(Uri uri) async {
    HttpClient? client;
    try {
      client = HttpClient()..connectionTimeout = _timeout;
      final req = await client.getUrl(uri).timeout(_timeout);
      req.headers.set('User-Agent', 'TreasureVault/$appVersion');
      req.headers.set('Accept', 'application/vnd.github+json');
      final resp = await req.close().timeout(_timeout);
      if (resp.statusCode != 200) {
        await resp.drain<void>();
        return null;
      }
      final body = await resp.transform(utf8.decoder).join().timeout(_timeout);
      if (body.trimLeft().startsWith('<')) return null; // 加速源返回错误页
      return body;
    } catch (_) {
      return null;
    } finally {
      client?.close(force: true);
    }
  }

  static ReleaseInfo? _parse(Map<String, dynamic> json) {
    final tag = (json['tag_name'] as String?)?.trim() ?? '';
    if (tag.isEmpty) return null;
    final assets = <String, String>{};
    final raw = json['assets'];
    if (raw is List) {
      for (final a in raw) {
        if (a is Map<String, dynamic>) {
          final name = (a['name'] as String?)?.trim() ?? '';
          final url = (a['browser_download_url'] as String?)?.trim() ?? '';
          if (name.isEmpty || url.isEmpty) continue;
          if (!_trustedHost(url)) continue;
          assets[name] = url;
        }
      }
    }
    DateTime? published;
    final p = json['published_at'] as String?;
    if (p != null) published = DateTime.tryParse(p)?.toLocal();
    return ReleaseInfo(
      version: stripV(tag),
      tag: tag,
      body: (json['body'] as String?)?.trim() ?? '',
      htmlUrl: (json['html_url'] as String?)?.trim() ?? '',
      publishedAt: published,
      assets: assets,
    );
  }

  static String stripV(String tag) {
    final t = tag.trim();
    if (t.startsWith('v') || t.startsWith('V')) return t.substring(1);
    return t;
  }

  /// 挑出适配当前设备 ABI 的 APK；找不到则退化为任意 APK。
  static String? pickApk(ReleaseInfo info, String? abi) {
    if (info.assets.isEmpty) return null;
    final a = (abi ?? '').trim();
    if (a.isNotEmpty) {
      for (final e in info.assets.entries) {
        if (e.key.contains(a)) return e.value;
      }
    }
    // 常见 ABI 兜底顺序
    const fallback = ['arm64-v8a', 'armeabi-v7a', 'x86_64'];
    for (final f in fallback) {
      for (final e in info.assets.entries) {
        if (e.key.contains(f)) return e.value;
      }
    }
    for (final e in info.assets.entries) {
      if (e.key.toLowerCase().endsWith('.apk')) return e.value;
    }
    return null;
  }

  /// 版本号逐段比较。
  static bool isNewer(String remote, String local) => compareVersions(remote, local) > 0;

  static int compareVersions(String a, String b) {
    final x = _segments(a);
    final y = _segments(b);
    final n = x.length > y.length ? x.length : y.length;
    for (var i = 0; i < n; i++) {
      final xv = i < x.length ? x[i] : 0;
      final yv = i < y.length ? y[i] : 0;
      if (xv != yv) return xv.compareTo(yv);
    }
    return 0;
  }

  static List<int> _segments(String v) {
    return v
        .trim()
        .split(RegExp(r'[.\-_+]'))
        .where((e) => e.isNotEmpty)
        .map((e) => int.tryParse(RegExp(r'\d+').firstMatch(e)?.group(0) ?? '') ?? 0)
        .toList();
  }
}
