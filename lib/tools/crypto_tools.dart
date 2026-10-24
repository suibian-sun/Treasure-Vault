import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/hash.dart';
import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';
import 'common.dart';

/// 加密安全类工具（5 个）。
final List<ToolDef> cryptoTools = [
  ToolDef(
    id: 'md5',
    name: 'MD5 哈希',
    desc: '32 位十六进制摘要，常用于文件校验',
    category: Categories.crypto,
    icon: Icons.tag_rounded,
    keywords: ['md5', 'hash', '摘要', '校验'],
    build: () => SimpleTextTool(
      hint: '输入需要计算 MD5 的文本…',
      outputLabel: 'MD5',
      modes: [
        TextMode('小写', (s) => md5Hex(s)),
        TextMode('大写', (s) => md5Hex(s).toUpperCase()),
      ],
    ),
  ),
  ToolDef(
    id: 'sha1',
    name: 'SHA-1 哈希',
    desc: '40 位摘要，Git 对象标识常用',
    category: Categories.crypto,
    icon: Icons.fingerprint_rounded,
    keywords: ['sha1', 'hash', '摘要'],
    build: () => SimpleTextTool(
      hint: '输入需要计算 SHA-1 的文本…',
      outputLabel: 'SHA-1',
      modes: [
        TextMode('小写', (s) => sha1Hex(s)),
        TextMode('大写', (s) => sha1Hex(s).toUpperCase()),
      ],
    ),
  ),
  ToolDef(
    id: 'sha256',
    name: 'SHA-256 哈希',
    desc: '64 位摘要，校验下载文件完整性',
    category: Categories.crypto,
    icon: Icons.verified_user_outlined,
    keywords: ['sha256', 'hash', '摘要', '校验'],
    build: () => SimpleTextTool(
      hint: '输入需要计算 SHA-256 的文本…',
      outputLabel: 'SHA-256',
      modes: [
        TextMode('小写', (s) => sha256Hex(s)),
        TextMode('大写', (s) => sha256Hex(s).toUpperCase()),
      ],
    ),
  ),
  ToolDef(
    id: 'password_gen',
    name: '密码生成器',
    desc: '可配置长度与字符集，本地生成不上传',
    category: Categories.crypto,
    icon: Icons.password_rounded,
    keywords: ['密码', 'password', '随机', '强密码'],
    build: () => const _PasswordTool(),
  ),
  ToolDef(
    id: 'uuid',
    name: 'UUID 生成',
    desc: '批量生成 v4 随机 UUID',
    category: Categories.crypto,
    icon: Icons.confirmation_number_outlined,
    keywords: ['uuid', 'guid', '唯一'],
    build: () => const _UuidTool(),
  ),
];

class _PasswordTool extends StatefulWidget {
  const _PasswordTool();

  @override
  State<_PasswordTool> createState() => _PasswordToolState();
}

class _PasswordToolState extends State<_PasswordTool> {
  double _length = 16;
  bool _upper = true;
  bool _lower = true;
  bool _digit = true;
  bool _symbol = false;
  bool _noAmbiguous = true;
  final _out = <String>[];

  void _generate() {
    const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const lower = 'abcdefghijkmnopqrstuvwxyz';
    const digit = '23456789';
    const symbol = '!@#\$%^&*-_=+?';
    const ambiguous = 'Il1O0o';

    var pool = '';
    if (_upper) pool += upper;
    if (_lower) pool += lower;
    if (_digit) pool += digit;
    if (_symbol) pool += symbol;
    if (pool.isEmpty) {
      toast(context, '请至少选择一种字符类型', error: true);
      return;
    }
    if (!_noAmbiguous) pool += ambiguous;

    final rnd = math.Random.secure();
    final list = <String>[];
    for (var i = 0; i < 5; i++) {
      final buf = StringBuffer();
      for (var j = 0; j < _length.round(); j++) {
        buf.write(pool[rnd.nextInt(pool.length)]);
      }
      list.add(buf.toString());
    }
    setState(() {
      _out
        ..clear()
        ..addAll(list);
    });
  }

  int get _strength {
    var score = 0;
    if (_length >= 12) score++;
    if (_length >= 18) score++;
    if (_upper && _lower) score++;
    if (_digit) score++;
    if (_symbol) score++;
    return score;
  }

  String get _strengthLabel {
    if (_strength <= 2) return '较弱';
    if (_strength <= 3) return '中等';
    if (_strength <= 4) return '较强';
    return '很强';
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '生成参数',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('长度', style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(b))),
                const Spacer(),
                Text(
                  '${_length.round()}',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.brandOf(b),
                  ),
                ),
              ],
            ),
            Slider(
              value: _length,
              min: 4,
              max: 48,
              divisions: 44,
              label: '${_length.round()}',
              onChanged: (v) => setState(() => _length = v),
            ),
            _switch('大写字母 A-Z', _upper, (v) => setState(() => _upper = v)),
            _switch('小写字母 a-z', _lower, (v) => setState(() => _lower = v)),
            _switch('数字 0-9', _digit, (v) => setState(() => _digit = v)),
            _switch('特殊符号', _symbol, (v) => setState(() => _symbol = v)),
            _switch('排除易混淆字符（l/1/O/0）', _noAmbiguous, (v) => setState(() => _noAmbiguous = v)),
            const SizedBox(height: 8),
            PrimaryButton(label: '生成 5 组密码', icon: Icons.casino_outlined, onPressed: _generate),
          ],
        ),
      ),
      if (_out.isNotEmpty)
        SectionCard(
          title: '生成结果',
          trailing: Text(
            '强度：$_strengthLabel',
            style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: AppTheme.brandOf(b)),
          ),
          child: Column(
            children: [
              for (final p in _out)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: SelectableText(
                          p,
                          style: TextStyle(
                            fontSize: 14,
                            fontFamily: 'monospace',
                            color: AppTheme.inkOf(b),
                          ),
                        ),
                      ),
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        onPressed: () => copyText(context, p),
                        icon: Icon(Icons.copy_rounded, size: 16, color: AppTheme.ink2Of(b)),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      const InfoNote(text: '密码在设备本地用加密安全随机数生成，不会联网、不会保存。'),
    ]);
  }

  Widget _switch(String label, bool v, ValueChanged<bool> onChanged) {
    final b = Theme.of(context).brightness;
    return SwitchListTile(
      contentPadding: EdgeInsets.zero,
      dense: true,
      visualDensity: const VisualDensity(vertical: -2),
      title: Text(label, style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
      value: v,
      onChanged: onChanged,
    );
  }
}

class _UuidTool extends StatefulWidget {
  const _UuidTool();

  @override
  State<_UuidTool> createState() => _UuidToolState();
}

class _UuidToolState extends State<_UuidTool> {
  int _count = 5;
  bool _upper = false;
  bool _hyphen = true;
  final _out = <String>[];

  static final _rnd = math.Random.secure();

  void _generate() {
    final list = <String>[];
    for (var i = 0; i < _count; i++) {
      list.add(_v4());
    }
    setState(() {
      _out
        ..clear()
        ..addAll(list);
    });
  }

  static String _v4() {
    final bytes = List<int>.generate(16, (_) => _rnd.nextInt(256));
    bytes[6] = (bytes[6] & 0x0F) | 0x40;
    bytes[8] = (bytes[8] & 0x3F) | 0x80;
    final hex = bytes.map((e) => e.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-'
        '${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final text = _out
        .map((u) => _hyphen ? u : u.replaceAll('-', ''))
        .map((u) => _upper ? u.toUpperCase() : u)
        .join('\n');
    return ToolBody(children: [
      SectionCard(
        title: '生成参数',
        child: Column(
          children: [
            Row(
              children: [
                Text('数量', style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(b))),
                const Spacer(),
                Text(
                  '$_count',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.brandOf(b),
                  ),
                ),
              ],
            ),
            Slider(
              value: _count.toDouble(),
              min: 1,
              max: 50,
              divisions: 49,
              label: '$_count',
              onChanged: (v) => setState(() => _count = v.round()),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              visualDensity: const VisualDensity(vertical: -2),
              title: Text('大写', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _upper,
              onChanged: (v) => setState(() => _upper = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              visualDensity: const VisualDensity(vertical: -2),
              title: Text('保留连字符', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _hyphen,
              onChanged: (v) => setState(() => _hyphen = v),
            ),
            const SizedBox(height: 8),
            PrimaryButton(label: '生成 UUID', icon: Icons.bolt_rounded, onPressed: _generate),
          ],
        ),
      ),
      if (text.isNotEmpty)
        SectionCard(
          child: ResultBox(text: text, label: 'UUID v4（共 ${_out.length} 个）'),
        ),
      const InfoNote(text: '基于 Random.secure() 生成，符合 RFC 4122 v4 格式。'),
    ]);
  }
}

/// 供其他工具复用的十六进制输入格式化器（小写 + 去除空格）。
final hexFormatter = FilteringTextInputFormatter.allow(RegExp(r'[0-9a-fA-F\s]'));
