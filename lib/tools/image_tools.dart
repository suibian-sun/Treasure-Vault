import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';

import '../core/native_bridge.dart';
import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';

/// 图片工具（3 个）。
///
/// 图片解码与压缩全部在 Kotlin 侧用 BitmapFactory 完成：
/// 原生采样只解码目标尺寸，内存峰值远低于把整图读进 Dart 堆。
final List<ToolDef> imageTools = [
  ToolDef(
    id: 'image_info',
    name: '图片信息',
    desc: '尺寸、体积、格式与比例',
    category: Categories.image,
    icon: Icons.image_search_rounded,
    keywords: ['图片', '尺寸', '信息', 'image'],
    build: () => const _ImageInfoTool(),
  ),
  ToolDef(
    id: 'image_compress',
    name: '图片压缩',
    desc: '按长边与质量压缩，输出 JPEG',
    category: Categories.image,
    icon: Icons.compress_rounded,
    keywords: ['压缩', '图片', 'jpg', '缩小'],
    build: () => const _ImageCompressTool(),
  ),
  ToolDef(
    id: 'image_base64',
    name: '图片转 Base64',
    desc: '生成 data URI，便于内嵌到网页',
    category: Categories.image,
    icon: Icons.code_off_rounded,
    keywords: ['base64', '图片', 'datauri'],
    build: () => const _ImageBase64Tool(),
  ),
];

class _ImageInfoTool extends StatefulWidget {
  const _ImageInfoTool();

  @override
  State<_ImageInfoTool> createState() => _ImageInfoToolState();
}

class _ImageInfoToolState extends State<_ImageInfoTool> {
  String? _path;
  Map<String, dynamic>? _info;
  bool _busy = false;

  Future<void> _pick() async {
    setState(() => _busy = true);
    final path = await NativeBridge.pickFile(mime: 'image/*');
    if (path == null) {
      if (mounted) setState(() => _busy = false);
      return;
    }
    final info = await NativeBridge.imageInfo(path);
    if (!mounted) return;
    setState(() {
      _path = path;
      _info = info;
      _busy = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final name = _path == null ? '' : _path!.split('/').last;
    return ToolBody(children: [
      SectionCard(
        title: '选择图片',
        child: Column(
          children: [
            PrimaryButton(
              label: _busy ? '读取中…' : '从相册 / 文件中选择',
              icon: Icons.add_photo_alternate_outlined,
              onPressed: _busy ? null : _pick,
            ),
            if (_path != null) ...[
              const SizedBox(height: 14),
              ClipRRect(
                borderRadius: BorderRadius.circular(AppTheme.radMedium),
                child: Image.file(
                  File(_path!),
                  height: 180,
                  width: double.infinity,
                  fit: BoxFit.contain,
                  cacheWidth: 600,
                  errorBuilder: (_, __, ___) => const SizedBox.shrink(),
                ),
              ),
            ],
          ],
        ),
      ),
      if (_info != null)
        SectionCard(
          title: '文件信息',
          child: Column(
            children: [
              KVRow(k: '文件名', v: name, copyable: true),
              KVRow(k: '尺寸', v: '${_info!['width']} × ${_info!['height']} px'),
              KVRow(k: '体积', v: formatBytes((_info!['bytes'] as num?) ?? 0), copyable: true),
              KVRow(k: '格式', v: '${_info!['mime'] ?? '未知'}'),
              KVRow(k: '比例', v: _ratio(_info!)),
              KVRow(k: '路径', v: _path!, copyable: true),
            ],
          ),
        )
      else if (!_busy)
        Text(
          '未选择图片。图片仅在本地读取，不会上传。',
          style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
        ),
    ]);
  }

  static String _ratio(Map<String, dynamic> info) {
    final w = (info['width'] as num?)?.toDouble() ?? 0;
    final h = (info['height'] as num?)?.toDouble() ?? 0;
    if (w == 0 || h == 0) return '—';
    final g = _gcd(w.round(), h.round());
    return '${w.round() ~/ g}:${h.round() ~/ g}';
  }

  static int _gcd(int a, int b) => b == 0 ? (a == 0 ? 1 : a) : _gcd(b, a % b);
}

class _ImageCompressTool extends StatefulWidget {
  const _ImageCompressTool();

  @override
  State<_ImageCompressTool> createState() => _ImageCompressToolState();
}

class _ImageCompressToolState extends State<_ImageCompressTool> {
  String? _path;
  Map<String, dynamic>? _result;
  bool _busy = false;
  int _maxDim = 1280;
  double _quality = 80;

  Future<void> _pick() async {
    final path = await NativeBridge.pickFile(mime: 'image/*');
    if (path == null) return;
    setState(() {
      _path = path;
      _result = null;
    });
  }

  Future<void> _compress() async {
    if (_path == null) return;
    setState(() => _busy = true);
    final r = await NativeBridge.compressImage(_path!, maxDim: _maxDim, quality: _quality.round());
    if (!mounted) return;
    setState(() {
      _result = r;
      _busy = false;
    });
    if (r == null) {
      toast(context, '压缩失败，请换一张图片重试', error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final before = (_result?['originalBytes'] as num?) ?? 0;
    final after = (_result?['bytes'] as num?) ?? 0;
    final saved = before > 0 && after > 0 ? (1 - after / before) : 0.0;

    return ToolBody(children: [
      SectionCard(
        title: '原图',
        child: Column(
          children: [
            GhostButton(
              label: _path == null ? '选择图片' : '重新选择',
              icon: Icons.image_outlined,
              onPressed: _pick,
            ),
            if (_path != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  _path!.split('/').last,
                  style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
                ),
              ),
          ],
        ),
      ),
      SectionCard(
        title: '压缩参数',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('长边上限', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
            const SizedBox(height: 8),
            SegChips<int>(
              values: const [720, 1080, 1280, 1920, 2560],
              labels: const ['720', '1080', '1280', '1920', '2560'],
              value: _maxDim,
              onChanged: (v) => setState(() => _maxDim = v),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Text('JPEG 质量', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
                const Spacer(),
                Text(
                  '${_quality.round()}',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.brandOf(b),
                  ),
                ),
              ],
            ),
            Slider(
              value: _quality,
              min: 30,
              max: 95,
              divisions: 13,
              onChanged: (v) => setState(() => _quality = v),
            ),
            PrimaryButton(
              label: _busy ? '压缩中…' : '开始压缩',
              icon: Icons.compress_rounded,
              onPressed: _busy ? null : _compress,
            ),
          ],
        ),
      ),
      if (_result != null)
        SectionCard(
          title: '压缩结果',
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: StatTile(value: formatBytes(before), label: '原图'),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: StatTile(
                      value: formatBytes(after),
                      label: '压缩后',
                      color: AppTheme.brandOf(b),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: StatTile(
                      value: '${(saved * 100).toStringAsFixed(0)}%',
                      label: '节省',
                      color: const Color(0xFF16A34A),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              KVRow(k: '输出尺寸', v: '${_result!['width']} × ${_result!['height']} px'),
              KVRow(k: '输出路径', v: '${_result!['path']}', copyable: true),
              const SizedBox(height: 8),
              Text(
                '压缩后的文件保存在应用私有目录，可从文件管理器中导出。',
                style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
              ),
            ],
          ),
        ),
    ]);
  }
}

class _ImageBase64Tool extends StatefulWidget {
  const _ImageBase64Tool();

  @override
  State<_ImageBase64Tool> createState() => _ImageBase64ToolState();
}

class _ImageBase64ToolState extends State<_ImageBase64Tool> {
  String _result = '';
  bool _busy = false;

  Future<void> _pick() async {
    setState(() => _busy = true);
    final path = await NativeBridge.pickFile(mime: 'image/*');
    if (path == null) {
      if (mounted) setState(() => _busy = false);
      return;
    }
    try {
      final bytes = await File(path).readAsBytes();
      final mime = _mimeOf(path);
      final b64 = base64Encode(bytes);
      if (!mounted) return;
      setState(() {
        _result = 'data:$mime;base64,$b64';
        _busy = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _busy = false);
      toast(context, '读取失败：$e', error: true);
    }
  }

  static String _mimeOf(String path) {
    final p = path.toLowerCase();
    if (p.endsWith('.png')) return 'image/png';
    if (p.endsWith('.webp')) return 'image/webp';
    if (p.endsWith('.gif')) return 'image/gif';
    if (p.endsWith('.bmp')) return 'image/bmp';
    return 'image/jpeg';
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '选择图片',
        child: Column(
          children: [
            PrimaryButton(
              label: _busy ? '编码中…' : '选择并编码',
              icon: Icons.code_rounded,
              onPressed: _busy ? null : _pick,
            ),
            if (_result.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Text(
                  'Base64 长度 ${_result.length} 字符（约 ${formatBytes(_result.length)}）',
                  style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
                ),
              ),
          ],
        ),
      ),
      if (_result.isNotEmpty)
        SectionCard(
          child: ResultBox(text: _result, label: 'data URI'),
        ),
      const InfoNote(
        text: 'Base64 会让体积膨胀约 33%，仅适合小图标内嵌；大图请用「图片压缩」。',
      ),
    ]);
  }
}
