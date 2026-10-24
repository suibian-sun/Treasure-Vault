import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';

import '../app_version.dart';
import '../core/native_bridge.dart';
import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';

/// 开发工具（7 个）。
final List<ToolDef> devTools = [
  ToolDef(
    id: 'regex_test',
    name: '正则测试',
    desc: '实时匹配、分组捕获与替换预览',
    category: Categories.dev,
    icon: Icons.rule_rounded,
    keywords: ['正则', 'regex', '匹配'],
    build: () => const _RegexTool(),
  ),
  ToolDef(
    id: 'color_convert',
    name: '颜色转换',
    desc: 'HEX / RGB / HSL 互转与预览',
    category: Categories.dev,
    icon: Icons.palette_outlined,
    keywords: ['颜色', 'color', 'hex', 'rgb'],
    build: () => const _ColorTool(),
  ),
  ToolDef(
    id: 'jwt',
    name: 'JWT 解析',
    desc: '解析 Header / Payload 与过期状态',
    category: Categories.dev,
    icon: Icons.key_outlined,
    keywords: ['jwt', 'token', '解析'],
    build: () => const _JwtTool(),
  ),
  ToolDef(
    id: 'http_status',
    name: 'HTTP 状态码',
    desc: '查询状态码含义与类别',
    category: Categories.dev,
    icon: Icons.http_rounded,
    keywords: ['http', '状态码', 'status'],
    build: () => const _HttpStatusTool(),
  ),
  ToolDef(
    id: 'json_to_dart',
    name: 'JSON 转 Dart',
    desc: '根据 JSON 结构生成模型类',
    category: Categories.dev,
    icon: Icons.model_training_outlined,
    keywords: ['json', 'dart', '模型', 'model'],
    build: () => const _JsonToDartTool(),
  ),
  ToolDef(
    id: 'url_parse',
    name: 'URL 解析',
    desc: '拆解协议、域名、路径与查询参数',
    category: Categories.dev,
    icon: Icons.link_outlined,
    keywords: ['url', '解析', '参数'],
    build: () => const _UrlParseTool(),
  ),
  ToolDef(
    id: 'device_info',
    name: '设备信息',
    desc: '型号、系统版本与屏幕参数',
    category: Categories.dev,
    icon: Icons.phone_android_rounded,
    keywords: ['设备', '机型', '系统', '信息'],
    build: () => const _DeviceTool(),
  ),
];

// ---------------------------------------------------------------------------
// 正则
// ---------------------------------------------------------------------------

class _RegexTool extends StatefulWidget {
  const _RegexTool();

  @override
  State<_RegexTool> createState() => _RegexToolState();
}

class _RegexToolState extends State<_RegexTool> {
  final _pattern = TextEditingController(text: r'\d+');
  final _text = TextEditingController(text: '订单 2024 年共 57 个工具，版本 1.0.0');
  bool _caseSensitive = true;
  bool _multiLine = true;

  @override
  void dispose() {
    _pattern.dispose();
    _text.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final rows = <String>[];
    String? error;
    try {
      if (_pattern.text.isNotEmpty) {
        final re = RegExp(_pattern.text, caseSensitive: _caseSensitive, multiLine: _multiLine);
        var i = 0;
        for (final m in re.allMatches(_text.text)) {
          i++;
          final groups = <String>[];
          for (var g = 1; g <= m.groupCount; g++) {
            groups.add('\$$g=${m.group(g) ?? ''}');
          }
          rows.add('$i. "${m.group(0)}" @ ${m.start}-${m.end}'
              '${groups.isEmpty ? '' : '  ${groups.join('  ')}'}');
          if (i >= 200) {
            rows.add('… 结果过多，仅显示前 200 条');
            break;
          }
        }
      }
    } catch (e) {
      error = '正则无效：$e';
    }

    return ToolBody(children: [
      SectionCard(
        title: '正则表达式',
        child: LabeledField(
          label: 'Pattern',
          controller: _pattern,
          hint: r'\d{4}-\d{2}-\d{2}',
          onChanged: (_) => setState(() {}),
        ),
      ),
      SectionCard(
        title: '测试文本',
        child: TextField(
          controller: _text,
          minLines: 5,
          maxLines: 9,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            hintText: '粘贴需要匹配的文本…',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 13.5, height: 1.6),
        ),
      ),
      Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: Wrap(
          spacing: 8,
          children: [
            FilterChip(
              label: const Text('区分大小写'),
              selected: _caseSensitive,
              onSelected: (v) => setState(() => _caseSensitive = v),
            ),
            FilterChip(
              label: const Text('多行模式'),
              selected: _multiLine,
              onSelected: (v) => setState(() => _multiLine = v),
            ),
          ],
        ),
      ),
      SectionCard(
        title: '匹配结果',
        trailing: Text(
          '${rows.length} 条',
          style: TextStyle(fontSize: 11.5, color: AppTheme.brandOf(b)),
        ),
        child: error != null
            ? InfoNote(text: error, icon: Icons.error_outline_rounded)
            : ResultBox(text: rows.join('\n'), label: '按出现顺序'),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 颜色
// ---------------------------------------------------------------------------

class _ColorTool extends StatefulWidget {
  const _ColorTool();

  @override
  State<_ColorTool> createState() => _ColorToolState();
}

class _ColorToolState extends State<_ColorTool> {
  final _c = TextEditingController(text: '#1890FF');

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  Color? get _color => _parseColor(_c.text);

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final c = _color;
    final hex = c == null
        ? '—'
        : '#${c.toARGB32().toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}';

    return ToolBody(children: [
      SectionCard(
        title: '颜色值',
        hint: '支持 #1890FF、1890ff、rgb(24,144,255)、hsl(210,100%,55%)',
        child: LabeledField(
          label: '输入任意格式',
          controller: _c,
          hint: '#1890FF',
          onChanged: (_) => setState(() {}),
        ),
      ),
      if (c != null)
        SectionCard(
          title: '转换结果',
          child: Column(
            children: [
              Container(
                height: 72,
                width: double.infinity,
                decoration: BoxDecoration(
                  color: c,
                  borderRadius: BorderRadius.circular(AppTheme.radMedium),
                  border: Border.all(color: AppTheme.lineOf(b)),
                ),
                alignment: Alignment.center,
                child: Text(
                  hex,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: c.computeLuminance() > 0.55 ? Colors.black87 : Colors.white,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              KVRow(k: 'HEX', v: hex, copyable: true),
              KVRow(k: 'RGB', v: 'rgb(${(c.r * 255).round()}, ${(c.g * 255).round()}, ${(c.b * 255).round()})', copyable: true),
              KVRow(k: 'HSL', v: _toHsl(c), copyable: true),
              KVRow(k: '亮度', v: (c.computeLuminance() * 100).toStringAsFixed(1) + '%'),
            ],
          ),
        )
      else
        Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(
            '无法识别的颜色值',
            style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
          ),
        ),
    ]);
  }

  static String _toHsl(Color c) {
    final r = c.r, g = c.g, bl = c.b;
    final maxV = [r, g, bl].reduce((a, b) => a > b ? a : b);
    final minV = [r, g, bl].reduce((a, b) => a < b ? a : b);
    final l = (maxV + minV) / 2;
    double h = 0, s = 0;
    if (maxV != minV) {
      final d = maxV - minV;
      s = l > 0.5 ? d / (2 - maxV - minV) : d / (maxV + minV);
      if (maxV == r) {
        h = (g - bl) / d + (g < bl ? 6 : 0);
      } else if (maxV == g) {
        h = (bl - r) / d + 2;
      } else {
        h = (r - g) / d + 4;
      }
      h /= 6;
    }
    return 'hsl(${(h * 360).round()}, ${(s * 100).round()}%, ${(l * 100).round()}%)';
  }

  static Color? _parseColor(String raw) {
    var s = raw.trim().toLowerCase();
    if (s.isEmpty) return null;
    if (s.startsWith('#')) s = s.substring(1);
    if (RegExp(r'^[0-9a-f]{6}$').hasMatch(s)) {
      return Color(int.parse('FF$s', radix: 16));
    }
    if (RegExp(r'^[0-9a-f]{3}$').hasMatch(s)) {
      final full = s.split('').map((ch) => '$ch$ch').join();
      return Color(int.parse('FF$full', radix: 16));
    }
    if (RegExp(r'^[0-9a-f]{8}$').hasMatch(s)) {
      return Color(int.parse(s, radix: 16));
    }
    final rgb = RegExp(r'rgba?\(([^)]+)\)').firstMatch(s);
    if (rgb != null) {
      final parts = rgb.group(1)!.split(',').map((e) => e.trim()).toList();
      if (parts.length >= 3) {
        final r = int.tryParse(parts[0]);
        final g = int.tryParse(parts[1]);
        final b = int.tryParse(parts[2]);
        if (r != null && g != null && b != null) {
          return Color.fromARGB(255, r.clamp(0, 255), g.clamp(0, 255), b.clamp(0, 255));
        }
      }
    }
    final hsl = RegExp(r'hsl\(([^)]+)\)').firstMatch(s);
    if (hsl != null) {
      final parts = hsl.group(1)!.split(',').map((e) => e.trim().replaceAll('%', '')).toList();
      if (parts.length >= 3) {
        final h = double.tryParse(parts[0]);
        final sv = double.tryParse(parts[1]);
        final lv = double.tryParse(parts[2]);
        if (h != null && sv != null && lv != null) {
          return HSLColor.fromAHSL(1, h % 360, sv / 100, lv / 100).toColor();
        }
      }
    }
    return null;
  }
}

// ---------------------------------------------------------------------------
// JWT
// ---------------------------------------------------------------------------

class _JwtTool extends StatefulWidget {
  const _JwtTool();

  @override
  State<_JwtTool> createState() => _JwtToolState();
}

class _JwtToolState extends State<_JwtTool> {
  final _c = TextEditingController();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final token = _c.text.trim();
    String header = '';
    String payload = '';
    String? error;
    String expired = '';

    if (token.isNotEmpty) {
      final parts = token.split('.');
      if (parts.length < 2) {
        error = '不是有效的 JWT（应包含 3 段，以 . 分隔）';
      } else {
        try {
          header = _pretty(_b64(parts[0]));
          payload = _pretty(_b64(parts[1]));
          final map = jsonDecode(_b64(parts[1]));
          if (map is Map && map['exp'] is num) {
            final exp = DateTime.fromMillisecondsSinceEpoch((map['exp'] as num).toInt() * 1000);
            final now = DateTime.now();
            expired = exp.isBefore(now)
                ? '已过期（${fmtDateTime(exp)}）'
                : '有效期至 ${fmtDateTime(exp)}';
          }
        } catch (e) {
          error = '解析失败：$e';
        }
      }
    }

    return ToolBody(children: [
      SectionCard(
        title: 'JWT Token',
        child: TextField(
          controller: _c,
          minLines: 4,
          maxLines: 8,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            hintText: '粘贴 eyJhbGciOi... 形式的 Token',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 12.5, fontFamily: 'monospace', height: 1.5),
        ),
      ),
      if (error != null) InfoNote(text: error, icon: Icons.error_outline_rounded),
      if (payload.isNotEmpty) ...[
        if (expired.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: InfoNote(
              text: expired,
              icon: expired.startsWith('已过期') ? Icons.lock_clock : Icons.check_circle_outline,
            ),
          ),
        SectionCard(
          child: ResultBox(text: header, label: 'Header'),
        ),
        SectionCard(
          child: ResultBox(text: payload, label: 'Payload'),
        ),
      ],
      const InfoNote(
        text: '仅做本地解码展示，不验证签名；请勿在不可信环境粘贴真实业务 Token。',
      ),
    ]);
  }

  static String _b64(String s) {
    var t = s.replaceAll('-', '+').replaceAll('_', '/');
    final pad = t.length % 4;
    if (pad != 0) t = t.padRight(t.length + (4 - pad), '=');
    return utf8.decode(base64Decode(t), allowMalformed: true);
  }

  static String _pretty(String s) {
    try {
      return const JsonEncoder.withIndent('  ').convert(jsonDecode(s));
    } catch (_) {
      return s;
    }
  }
}

// ---------------------------------------------------------------------------
// HTTP 状态码
// ---------------------------------------------------------------------------

class _HttpStatusTool extends StatefulWidget {
  const _HttpStatusTool();

  @override
  State<_HttpStatusTool> createState() => _HttpStatusToolState();
}

class _HttpStatusToolState extends State<_HttpStatusTool> {
  final _c = TextEditingController(text: '200');

  static const Map<int, (String, String)> _codes = {
    100: ('Continue', '服务器已收到请求头，客户端应继续发送请求体'),
    101: ('Switching Protocols', '切换协议（如升级到 WebSocket）'),
    200: ('OK', '请求成功，响应体包含结果'),
    201: ('Created', '资源创建成功'),
    202: ('Accepted', '请求已接受，正在异步处理'),
    204: ('No Content', '成功但无响应体'),
    206: ('Partial Content', '返回部分内容（断点续传）'),
    301: ('Moved Permanently', '资源永久重定向'),
    302: ('Found', '临时重定向'),
    303: ('See Other', '应使用 GET 访问新地址'),
    304: ('Not Modified', '缓存仍然有效'),
    307: ('Temporary Redirect', '临时重定向且保持请求方法'),
    308: ('Permanent Redirect', '永久重定向且保持请求方法'),
    400: ('Bad Request', '请求语法错误或参数不合法'),
    401: ('Unauthorized', '未认证，需要登录或 Token'),
    403: ('Forbidden', '已认证但无权限'),
    404: ('Not Found', '资源不存在'),
    405: ('Method Not Allowed', '请求方法不被允许'),
    406: ('Not Acceptable', '无法满足客户端的接受类型'),
    408: ('Request Timeout', '请求超时'),
    409: ('Conflict', '资源冲突（并发写入等）'),
    410: ('Gone', '资源已被永久删除'),
    413: ('Payload Too Large', '请求体过大'),
    414: ('URI Too Long', '请求地址过长'),
    415: ('Unsupported Media Type', '不支持的媒体类型'),
    418: ('I\'m a teapot', '彩蛋状态码'),
    422: ('Unprocessable Entity', '语义错误，无法处理'),
    423: ('Locked', '资源被锁定'),
    429: ('Too Many Requests', '请求过于频繁，被限流'),
    500: ('Internal Server Error', '服务器内部错误'),
    501: ('Not Implemented', '服务器不支持该功能'),
    502: ('Bad Gateway', '网关收到无效响应'),
    503: ('Service Unavailable', '服务暂不可用（过载或维护）'),
    504: ('Gateway Timeout', '网关等待上游超时'),
    505: ('HTTP Version Not Supported', '不支持的 HTTP 版本'),
    507: ('Insufficient Storage', '存储空间不足'),
    508: ('Loop Detected', '检测到循环请求'),
  };

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final code = int.tryParse(_c.text.trim());
    final entry = code == null ? null : _codes[code];
    final color = code == null
        ? AppTheme.ink2Of(b)
        : code < 200
            ? const Color(0xFF0EA5E9)
            : code < 300
                ? const Color(0xFF16A34A)
                : code < 400
                    ? const Color(0xFFD97706)
                    : code < 500
                        ? const Color(0xFFF97316)
                        : const Color(0xFFDC2626);

    return ToolBody(children: [
      SectionCard(
        title: '状态码',
        child: TextField(
          controller: _c,
          keyboardType: TextInputType.number,
          onChanged: (_) => setState(() {}),
          style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w700),
          decoration: const InputDecoration(
            hintText: '200',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
        ),
      ),
      SectionCard(
        title: '含义',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(AppTheme.radSmall),
                  ),
                  child: Text(
                    code == null ? '—' : _category(code),
                    style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: color),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    entry?.$1 ?? (code == null ? '输入状态码' : '未收录的扩展状态码'),
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: AppTheme.inkOf(b),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              entry?.$2 ?? '该状态码可能属于厂商扩展，请参考对应服务端文档。',
              style: TextStyle(fontSize: 13, height: 1.6, color: AppTheme.ink2Of(b)),
            ),
          ],
        ),
      ),
      SectionCard(
        title: '常用状态码',
        child: Column(
          children: [
            for (final c in [200, 301, 304, 400, 401, 403, 404, 429, 500, 502, 503])
              KVRow(k: '$c', v: _codes[c]?.$1 ?? '—'),
          ],
        ),
      ),
    ]);
  }

  static String _category(int code) {
    if (code < 200) return '1xx 信息';
    if (code < 300) return '2xx 成功';
    if (code < 400) return '3xx 重定向';
    if (code < 500) return '4xx 客户端';
    return '5xx 服务端';
  }
}

// ---------------------------------------------------------------------------
// JSON → Dart
// ---------------------------------------------------------------------------

class _JsonToDartTool extends StatefulWidget {
  const _JsonToDartTool();

  @override
  State<_JsonToDartTool> createState() => _JsonToDartToolState();
}

class _JsonToDartToolState extends State<_JsonToDartTool> {
  final _c = TextEditingController();
  final _name = TextEditingController(text: 'Root');
  String _out = '';
  String? _error;

  @override
  void dispose() {
    _c.dispose();
    _name.dispose();
    super.dispose();
  }

  void _run() {
    final src = _c.text.trim();
    if (src.isEmpty) {
      setState(() {
        _out = '';
        _error = null;
      });
      return;
    }
    try {
      final decoded = jsonDecode(src);
      final classes = <String>[];
      _walk(decoded, _pascal(_name.text.isEmpty ? 'Root' : _name.text.trim()), classes);
      setState(() {
        _out = classes.join('\n\n');
        _error = null;
      });
    } catch (e) {
      setState(() {
        _error = 'JSON 解析失败：$e';
        _out = '';
      });
    }
  }

  /// 递归收集类定义（子类先入栈，保证输出顺序可读）。
  static void _walk(Object? node, String className, List<String> out) {
    if (node is List) {
      if (node.isEmpty) return;
      _walk(node.first, _singular(className), out);
      return;
    }
    if (node is! Map<String, dynamic>) return;

    final fields = <String>[];
    final ctor = <String>[];
    for (final entry in node.entries) {
      final key = entry.key;
      final v = entry.value;
      final type = _typeOf(key, v, out);
      final fieldName = _camel(key);
      fields.add('  final $type $fieldName;');
      ctor.add('    this.$fieldName,');
    }

    final def = StringBuffer();
    def.writeln('class $className {');
    for (final f in fields) {
      def.writeln(f);
    }
    def.writeln();
    def.writeln('  const $className({');
    for (final c in ctor) {
      def.writeln(c);
    }
    def.writeln('  });');
    def.writeln();
    def.writeln('  factory $className.fromJson(Map<String, dynamic> json) => $className(');
    for (final key in node.keys) {
      final fieldName = _camel(key);
      final v = node[key];
      def.writeln('        $fieldName: ${_fromJson(key, v, '$fieldName', className)},');
    }
    def.writeln('      );');
    def.writeln('}');
    out.insert(0, def.toString());
  }

  static String _typeOf(String key, Object? v, List<String> out) {
    if (v == null) return 'dynamic';
    if (v is String) return 'String';
    if (v is bool) return 'bool';
    if (v is int) return 'int';
    if (v is double) return 'double';
    if (v is num) return 'num';
    if (v is List) {
      if (v.isEmpty) return 'List<dynamic>';
      final first = v.first;
      final inner = _typeOf(key, first, out);
      return 'List<$inner>';
    }
    if (v is Map<String, dynamic>) {
      final cls = _pascal(_singular(key));
      _walk(v, cls, out);
      return cls;
    }
    return 'dynamic';
  }

  static String _fromJson(String key, Object? v, String field, String parent) {
    if (v is Map<String, dynamic>) {
      return '${_pascal(_singular(key))}.fromJson(json[\'$key\'] as Map<String, dynamic>)';
    }
    if (v is List) {
      if (v.isEmpty) return 'json[\'$key\'] as List<dynamic>';
      final first = v.first;
      if (first is Map<String, dynamic>) {
        return '(json[\'$key\'] as List<dynamic>)\n            .map((e) => ${_pascal(_singular(key))}.fromJson(e as Map<String, dynamic>))\n            .toList()';
      }
      return 'json[\'$key\'] as List<dynamic>';
    }
    if (v is int) return 'json[\'$key\'] as int';
    if (v is double) return 'json[\'$key\'] as double';
    if (v is num) return 'json[\'$key\'] as num';
    if (v is String) return 'json[\'$key\'] as String';
    if (v is bool) return 'json[\'$key\'] as bool';
    return 'json[\'$key\']';
  }

  static String _pascal(String s) {
    final parts = s.split(RegExp(r'[^A-Za-z0-9]+')).where((e) => e.isNotEmpty).toList();
    if (parts.isEmpty) return 'Model';
    final buf = StringBuffer();
    for (final p in parts) {
      buf.write('${p[0].toUpperCase()}${p.substring(1)}');
    }
    return buf.toString();
  }

  static String _camel(String s) {
    final p = _pascal(s);
    return '${p[0].toLowerCase()}${p.substring(1)}';
  }

  static String _singular(String s) {
    if (s.endsWith('ies')) return '${s.substring(0, s.length - 3)}y';
    if (s.endsWith('s') && s.length > 1) return s.substring(0, s.length - 1);
    return s;
  }

  @override
  Widget build(BuildContext context) {
    return ToolBody(children: [
      SectionCard(
        title: '根类名',
        child: LabeledField(label: 'Class 名称', controller: _name, hint: 'User'),
      ),
      SectionCard(
        title: 'JSON 示例',
        child: TextField(
          controller: _c,
          minLines: 7,
          maxLines: 12,
          onChanged: (_) => _run(),
          decoration: const InputDecoration(
            hintText: '{"id":1,"name":"宝藏盒","tags":["工具"]}',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 12.5, fontFamily: 'monospace', height: 1.5),
        ),
      ),
      if (_error != null) InfoNote(text: _error!, icon: Icons.error_outline_rounded),
      SectionCard(child: ResultBox(text: _out, label: '生成的 Dart 代码')),
    ]);
  }
}

// ---------------------------------------------------------------------------
// URL 解析
// ---------------------------------------------------------------------------

class _UrlParseTool extends StatefulWidget {
  const _UrlParseTool();

  @override
  State<_UrlParseTool> createState() => _UrlParseToolState();
}

class _UrlParseToolState extends State<_UrlParseTool> {
  final _c = TextEditingController(text: 'https://example.com:8080/path/page?id=1&lang=zh#top');

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final uri = Uri.tryParse(_c.text.trim());
    final valid = uri != null && uri.hasScheme;
    return ToolBody(children: [
      SectionCard(
        title: 'URL',
        child: TextField(
          controller: _c,
          minLines: 2,
          maxLines: 4,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            hintText: 'https://…',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 13, height: 1.5),
        ),
      ),
      if (!valid)
        const InfoNote(text: '请输入包含协议头的完整 URL，例如 https://example.com', icon: Icons.error_outline_rounded)
      else
        SectionCard(
          title: '解析结果',
          child: Column(
            children: [
              KVRow(k: '协议', v: uri.scheme, copyable: true),
              KVRow(k: '主机', v: uri.host, copyable: true),
              KVRow(k: '端口', v: uri.hasPort ? '${uri.port}' : '默认'),
              KVRow(k: '路径', v: uri.path.isEmpty ? '/' : uri.path, copyable: true),
              KVRow(k: '片段', v: uri.fragment.isEmpty ? '—' : uri.fragment),
              if (uri.queryParameters.isNotEmpty) ...[
                Divider(color: AppTheme.lineOf(Theme.of(context).brightness), height: 18),
                for (final e in uri.queryParameters.entries)
                  KVRow(k: e.key, v: e.value.isEmpty ? '(空)' : e.value, copyable: true),
              ],
            ],
          ),
        ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 设备信息
// ---------------------------------------------------------------------------

class _DeviceTool extends StatefulWidget {
  const _DeviceTool();

  @override
  State<_DeviceTool> createState() => _DeviceToolState();
}

class _DeviceToolState extends State<_DeviceTool> {
  Map<String, String> _info = {};
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final info = await NativeBridge.deviceInfo();
    if (!mounted) return;
    setState(() {
      _info = info;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final size = MediaQuery.sizeOf(context);
    final dpr = MediaQuery.devicePixelRatioOf(context);
    return ToolBody(children: [
      SectionCard(
        title: '系统',
        child: _loading
            ? const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              )
            : Column(
                children: [
                  KVRow(k: '品牌', v: _info['brand'] ?? Platform.operatingSystem),
                  KVRow(k: '型号', v: _info['model'] ?? '—'),
                  KVRow(k: 'Android', v: _info['sdk'] ?? '—'),
                  KVRow(k: 'CPU ABI', v: _info['abi'] ?? '—'),
                  KVRow(k: 'Dart', v: Platform.version.split(' ').first),
                ],
              ),
      ),
      SectionCard(
        title: '屏幕',
        child: Column(
          children: [
            KVRow(k: '逻辑尺寸', v: '${size.width.toStringAsFixed(0)} × ${size.height.toStringAsFixed(0)} dp'),
            KVRow(k: '物理像素', v: '${(size.width * dpr).round()} × ${(size.height * dpr).round()} px'),
            KVRow(k: '像素密度', v: '${dpr.toStringAsFixed(2)}x'),
          ],
        ),
      ),
      SectionCard(
        title: '应用',
        child: Column(
          children: [
            KVRow(k: '名称', v: appName),
            KVRow(k: '版本', v: 'v$appVersion ($appBuildNumber)'),
            KVRow(k: '包名', v: appPackageId),
            KVRow(k: '数据目录', v: _info['dataDir'] ?? '—'),
          ],
        ),
      ),
      Text(
        '以上信息由本机读取，不会上传。',
        style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
      ),
    ]);
  }
}
