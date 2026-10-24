import 'dart:convert';

import 'package:flutter/material.dart';

import '../core/tool.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';
import 'common.dart';

/// 编码转换类工具（5 个）。
final List<ToolDef> codecTools = [
  ToolDef(
    id: 'base64',
    name: 'Base64 编解码',
    desc: '文本与 Base64 互转，支持 URL 安全变体',
    category: Categories.codec,
    icon: Icons.code_rounded,
    keywords: ['base64', '编码', '解码'],
    build: () => SimpleTextTool(
      hint: '输入文本（编码）或 Base64 字符串（解码）…',
      outputLabel: '输出',
      modes: [
        TextMode('编码', (s) => base64Encode(utf8.encode(s))),
        TextMode('解码', (s) => _b64Decode(s, urlSafe: false)),
        TextMode('URL 安全编码', (s) => base64Url.encode(utf8.encode(s))),
        TextMode('URL 安全解码', (s) => _b64Decode(s, urlSafe: true)),
      ],
    ),
  ),
  ToolDef(
    id: 'url_codec',
    name: 'URL 编解码',
    desc: '百分号编码与还原，适合处理链接参数',
    category: Categories.codec,
    icon: Icons.link_rounded,
    keywords: ['url', 'encode', 'decode', '链接'],
    build: () => SimpleTextTool(
      hint: '粘贴链接或参数值…',
      outputLabel: '输出',
      modes: [
        TextMode('编码（组件）', (s) => Uri.encodeComponent(s)),
        TextMode('解码（组件）', (s) => Uri.decodeComponent(s)),
        TextMode('编码（整段）', (s) => Uri.encodeFull(s)),
        TextMode('解码（整段）', (s) => Uri.decodeFull(s)),
      ],
    ),
  ),
  ToolDef(
    id: 'unicode_codec',
    name: 'Unicode 转义',
    desc: '中文与 \\uXXXX 转义序列互转',
    category: Categories.codec,
    icon: Icons.translate_rounded,
    keywords: ['unicode', '转义', '中文'],
    build: () => SimpleTextTool(
      hint: r'输入中文或 \u4f60\u597d 形式的文本…',
      outputLabel: '输出',
      modes: [
        TextMode('转为 \\uXXXX', (s) => _unicodeEscape(s)),
        TextMode(r'还原 \uXXXX', (s) => _unicodeUnescape(s)),
      ],
    ),
  ),
  ToolDef(
    id: 'html_escape',
    name: 'HTML 转义',
    desc: '尖括号、引号与实体字符互转',
    category: Categories.codec,
    icon: Icons.html_rounded,
    keywords: ['html', '转义', 'entity', 'xss'],
    build: () => SimpleTextTool(
      hint: '输入 HTML 片段或含实体字符的文本…',
      outputLabel: '输出',
      modes: [
        TextMode('转义', (s) => s
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;')),
        TextMode('反转义', (s) => _htmlUnescape(s)),
      ],
    ),
  ),
  ToolDef(
    id: 'json_tool',
    name: 'JSON 格式化',
    desc: '美化 / 压缩 / 校验，错误定位到行列',
    category: Categories.codec,
    icon: Icons.data_object_rounded,
    keywords: ['json', '格式化', '压缩', '校验'],
    build: () => const _JsonTool(),
  ),
];

// ---------------------------------------------------------------------------

String _b64Decode(String input, {required bool urlSafe}) {
  var s = input.replaceAll(RegExp(r'\s'), '');
  if (urlSafe || s.contains('-') || s.contains('_')) {
    s = s.replaceAll('-', '+').replaceAll('_', '/');
  }
  final pad = s.length % 4;
  if (pad != 0) s = s.padRight(s.length + (4 - pad), '=');
  try {
    final bytes = base64Decode(s);
    return utf8.decode(bytes, allowMalformed: true);
  } catch (_) {
    return '解码失败：不是有效的 Base64 字符串';
  }
}

String _unicodeEscape(String s) {
  final buf = StringBuffer();
  for (final unit in s.codeUnits) {
    if (unit < 128) {
      buf.writeCharCode(unit);
    } else {
      buf.write('\\u${unit.toRadixString(16).padLeft(4, '0')}');
    }
  }
  return buf.toString();
}

String _unicodeUnescape(String s) {
  var out = s;
  // \u{1F600}
  out = out.replaceAllMapped(RegExp(r'\\u\{([0-9a-fA-F]{1,6})\}'), (m) {
    final cp = int.tryParse(m.group(1)!, radix: 16);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  // \uXXXX （可能是代理对，连续两个还原后由 Dart 自动合并）
  out = out.replaceAllMapped(RegExp(r'\\u([0-9a-fA-F]{4})'), (m) {
    final cp = int.tryParse(m.group(1)!, radix: 16);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  // \xXX
  out = out.replaceAllMapped(RegExp(r'\\x([0-9a-fA-F]{2})'), (m) {
    final cp = int.tryParse(m.group(1)!, radix: 16);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  // &#123; / &#x1F600;
  out = out.replaceAllMapped(RegExp(r'&#x([0-9a-fA-F]+);'), (m) {
    final cp = int.tryParse(m.group(1)!, radix: 16);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  out = out.replaceAllMapped(RegExp(r'&#(\d+);'), (m) {
    final cp = int.tryParse(m.group(1)!);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  return out;
}

String _htmlUnescape(String s) {
  const named = {
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#39;': "'",
    '&apos;': "'",
    '&nbsp;': ' ',
    '&copy;': '©',
    '&reg;': '®',
    '&trade;': '™',
    '&hellip;': '…',
    '&mdash;': '—',
  };
  var out = s;
  named.forEach((k, v) => out = out.replaceAll(k, v));
  out = out.replaceAllMapped(RegExp(r'&#x([0-9a-fA-F]+);'), (m) {
    final cp = int.tryParse(m.group(1)!, radix: 16);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  out = out.replaceAllMapped(RegExp(r'&#(\d+);'), (m) {
    final cp = int.tryParse(m.group(1)!);
    return cp == null ? m.group(0)! : String.fromCharCode(cp);
  });
  return out;
}

class _JsonTool extends StatefulWidget {
  const _JsonTool();

  @override
  State<_JsonTool> createState() => _JsonToolState();
}

class _JsonToolState extends State<_JsonTool> {
  final _c = TextEditingController();
  int _indent = 0; // 0: 2 空格, 1: 4 空格, 2: 压缩
  String _out = '';
  String? _error;
  String _summary = '';

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  void _run() {
    final src = _c.text.trim();
    if (src.isEmpty) {
      setState(() {
        _out = '';
        _error = null;
        _summary = '';
      });
      return;
    }
    try {
      final decoded = jsonDecode(src);
      final text = _indent == 2
          ? jsonEncode(decoded)
          : const JsonEncoder.withIndent('  ').convert(decoded);
      final encoder = _indent == 1 ? const JsonEncoder.withIndent('    ') : null;
      setState(() {
        _out = encoder == null ? text : encoder.convert(decoded);
        _error = null;
        _summary = _describe(decoded);
      });
    } on FormatException catch (e) {
      final pos = _lineCol(src, e.offset ?? 0);
      setState(() {
        _error = '第 ${pos.$1} 行，第 ${pos.$2} 列：${e.message}';
        _out = '';
        _summary = '';
      });
    } catch (e) {
      setState(() {
        _error = '解析失败：$e';
        _out = '';
      });
    }
  }

  static (int, int) _lineCol(String s, int offset) {
    var line = 1, col = 1;
    final max = offset > s.length ? s.length : offset;
    for (var i = 0; i < max; i++) {
      if (s.codeUnitAt(i) == 10) {
        line++;
        col = 1;
      } else {
        col++;
      }
    }
    return (line, col);
  }

  static String _describe(Object? v) {
    if (v is Map) return '对象 · ${v.length} 个键';
    if (v is List) return '数组 · ${v.length} 项';
    if (v is String) return '字符串 · ${v.length} 字符';
    if (v is num) return '数字';
    if (v is bool) return '布尔值';
    return 'null';
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: 'JSON 内容',
        trailing: _summary.isEmpty
            ? null
            : Text(_summary, style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b))),
        child: TextField(
          controller: _c,
          minLines: 8,
          maxLines: 14,
          onChanged: (_) => _run(),
          decoration: const InputDecoration(
            hintText: '{"name":"宝藏盒","tools":57}',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 13, height: 1.55, fontFamily: 'monospace'),
        ),
      ),
      Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: SegChips<int>(
          values: const [0, 1, 2],
          labels: const ['2 空格缩进', '4 空格缩进', '压缩为一行'],
          value: _indent,
          onChanged: (v) => setState(() {
            _indent = v;
            _run();
          }),
        ),
      ),
      if (_error != null)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: InfoNote(text: _error!, icon: Icons.error_outline_rounded),
        ),
      SectionCard(
        child: ResultBox(text: _out, label: '格式化结果'),
      ),
    ]);
  }
}
