import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/tool.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';
import 'common.dart';

/// 文本处理类工具（12 个）。
final List<ToolDef> textTools = [
  ToolDef(
    id: 'word_count',
    name: '字数统计',
    desc: '字符 / 中文 / 单词 / 行数 / 阅读时长',
    category: Categories.text,
    icon: Icons.pin_outlined,
    keywords: ['字数', '统计', 'count', '字符'],
    build: () => const _WordCountTool(),
  ),
  ToolDef(
    id: 'case_convert',
    name: '大小写转换',
    desc: '大写 / 小写 / 驼峰 / 下划线 / 中划线',
    category: Categories.text,
    icon: Icons.text_format_rounded,
    keywords: ['大小写', 'camel', 'snake', '命名'],
    build: () => SimpleTextTool(
      hint: '粘贴需要转换命名的文本…',
      inputLabel: '原始文本',
      outputLabel: '转换结果',
      modes: [
        TextMode('全部大写', (s) => s.toUpperCase()),
        TextMode('全部小写', (s) => s.toLowerCase()),
        TextMode('首字母大写', _titleCase),
        TextMode('句首大写', _sentenceCase),
        TextMode('驼峰 camelCase', (s) => _camel(s, upperFirst: false)),
        TextMode('帕斯卡 PascalCase', (s) => _camel(s, upperFirst: true)),
        TextMode('下划线 snake_case', (s) => _joinWords(s, '_', lower: true)),
        TextMode('中划线 kebab-case', (s) => _joinWords(s, '-', lower: true)),
        TextMode('常量 UPPER_SNAKE', (s) => _joinWords(s, '_', lower: false)),
      ],
    ),
  ),
  ToolDef(
    id: 'dedupe',
    name: '文本去重',
    desc: '按行去重，可忽略大小写、保留顺序',
    category: Categories.text,
    icon: Icons.filter_alt_outlined,
    keywords: ['去重', 'duplicate', 'unique'],
    build: () => const _DedupeTool(),
  ),
  ToolDef(
    id: 'sort_lines',
    name: '文本排序',
    desc: '字典序 / 数字 / 长度 / 随机打乱',
    category: Categories.text,
    icon: Icons.sort_rounded,
    keywords: ['排序', 'sort', '随机'],
    build: () => SimpleTextTool(
      hint: '每行一条记录，例如：\nbanana\napple\ncherry',
      modes: [
        TextMode('字典升序', (s) => _sorted(s, (a, b) => a.toLowerCase().compareTo(b.toLowerCase()))),
        TextMode('字典降序', (s) => _sorted(s, (a, b) => b.toLowerCase().compareTo(a.toLowerCase()))),
        TextMode('按数字升序', (s) => _sorted(s, (a, b) => _numOf(a).compareTo(_numOf(b)))),
        TextMode('按数字降序', (s) => _sorted(s, (a, b) => _numOf(b).compareTo(_numOf(a)))),
        TextMode('按长度', (s) => _sorted(s, (a, b) => a.length.compareTo(b.length))),
        TextMode('随机打乱', (s) {
          final list = _lines(s);
          list.shuffle(math.Random());
          return list.join('\n');
        }),
      ],
    ),
  ),
  ToolDef(
    id: 'find_replace',
    name: '查找替换',
    desc: '支持正则、大小写敏感与替换计数',
    category: Categories.text,
    icon: Icons.find_replace_rounded,
    keywords: ['替换', 'replace', '正则'],
    build: () => const _FindReplaceTool(),
  ),
  ToolDef(
    id: 'remove_blank',
    name: '去除空行',
    desc: '删除空行 / 合并连续空行 / 清理缩进',
    category: Categories.text,
    icon: Icons.format_line_spacing_rounded,
    keywords: ['空行', '空白', 'trim'],
    build: () => SimpleTextTool(
      hint: '粘贴含有多余空行或缩进的文本…',
      modes: [
        TextMode('删除所有空行', (s) => s.split('\n').where((l) => l.trim().isNotEmpty).join('\n')),
        TextMode('合并连续空行', (s) => s.replaceAll(RegExp(r'\n\s*\n\s*\n+'), '\n\n')),
        TextMode('删除行首尾空白', (s) => s.split('\n').map((l) => l.trim()).join('\n')),
        TextMode('删除行首空白', (s) => s.split('\n').map((l) => l.replaceFirst(RegExp(r'^[ \t]+'), '')).join('\n')),
      ],
    ),
  ),
  ToolDef(
    id: 'remove_spaces',
    name: '去除空白',
    desc: '删除全部空白或合并多余空格',
    category: Categories.text,
    icon: Icons.space_bar_rounded,
    keywords: ['空格', 'space', '空白'],
    build: () => SimpleTextTool(
      hint: '粘贴含有大量空格 / 换行的文本…',
      modes: [
        TextMode('删除全部空白', (s) => s.replaceAll(RegExp(r'\s+'), '')),
        TextMode('合并连续空格', (s) => s.replaceAll(RegExp(r'[ \t]+'), ' ')),
        TextMode('换行变空格', (s) => s.replaceAll(RegExp(r'\s*\n\s*'), ' ')),
        TextMode('全角转半角', _toHalfWidth),
      ],
    ),
  ),
  ToolDef(
    id: 'line_numbers',
    name: '添加行号',
    desc: '为每行加上序号，可自定义格式',
    category: Categories.text,
    icon: Icons.format_list_numbered_rounded,
    keywords: ['行号', 'number', '序号'],
    build: () => SimpleTextTool(
      hint: '每行一条，自动编号…',
      live: true,
      modes: [
        TextMode('1. 2. 3.', (s) => _numbered(s, (i) => '${i + 1}. ')),
        TextMode('1、2、3、', (s) => _numbered(s, (i) => '${i + 1}、')),
        TextMode('[1] [2]', (s) => _numbered(s, (i) => '[${i + 1}] ')),
        TextMode('01 02 03', (s) => _numbered(s, (i) => '${(i + 1).toString().padLeft(2, '0')} ')),
      ],
    ),
  ),
  ToolDef(
    id: 'text_reverse',
    name: '文本反转',
    desc: '反转字符 / 行序 / 词序',
    category: Categories.text,
    icon: Icons.swap_vert_rounded,
    keywords: ['反转', 'reverse', '倒序'],
    build: () => SimpleTextTool(
      hint: '粘贴文本…',
      modes: [
        TextMode('反转字符', (s) => String.fromCharCodes(s.runes.toList().reversed)),
        TextMode('每行反转', (s) => s.split('\n').map((l) => String.fromCharCodes(l.runes.toList().reversed)).join('\n')),
        TextMode('反转行序', (s) => s.split('\n').reversed.join('\n')),
        TextMode('反转词序', (s) => s.split(RegExp(r'\s+')).reversed.join(' ')),
      ],
    ),
  ),
  ToolDef(
    id: 'delimiter',
    name: '分隔符转换',
    desc: '换行 / 逗号 / 分号 / 制表符互转',
    category: Categories.text,
    icon: Icons.view_column_outlined,
    keywords: ['分隔符', 'delimiter', 'csv', '逗号'],
    build: () => const _DelimiterTool(),
  ),
  ToolDef(
    id: 'text_diff',
    name: '文本对比',
    desc: '逐行比较两段文本的差异',
    category: Categories.text,
    icon: Icons.difference_outlined,
    keywords: ['对比', 'diff', '差异'],
    build: () => const _DiffTool(),
  ),
  ToolDef(
    id: 'extract',
    name: '内容提取',
    desc: '提取邮箱 / 手机号 / 链接 / 数字 / IP',
    category: Categories.text,
    icon: Icons.manage_search_rounded,
    keywords: ['提取', '正则', '邮箱', '手机号', 'url'],
    build: () => SimpleTextTool(
      hint: '粘贴一段混杂文本，自动提取目标内容…',
      outputLabel: '提取结果（按出现顺序去重）',
      modes: [
        TextMode('邮箱', (s) => _extract(s, r'[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}')),
        TextMode('手机号', (s) => _extract(s, r'(?<!\d)1[3-9]\d{9}(?!\d)')),
        TextMode('网址', (s) => _extract(s, r'https?://[^\s<>"\)\]，。；]+')),
        TextMode('数字', (s) => _extract(s, r'-?\d+(?:\.\d+)?')),
        TextMode('IPv4', (s) => _extract(s, r'(?<!\d)(?:\d{1,3}\.){3}\d{1,3}(?!\d)')),
        TextMode('中文', (s) => _extract(s, r'[\u4e00-\u9fa5]{2,}')),
      ],
    ),
  ),
];

// ---------------------------------------------------------------------------
// 实现
// ---------------------------------------------------------------------------

class _WordCountTool extends StatefulWidget {
  const _WordCountTool();

  @override
  State<_WordCountTool> createState() => _WordCountToolState();
}

class _WordCountToolState extends State<_WordCountTool> {
  final _c = TextEditingController();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = _c.text;
    final chars = s.runes.length;
    final noSpace = s.replaceAll(RegExp(r'\s'), '').runes.length;
    final cjk = RegExp(r'[\u4e00-\u9fa5]').allMatches(s).length;
    final words = RegExp(r"[A-Za-z0-9_']+").allMatches(s).length;
    final lines = s.isEmpty ? 0 : s.split('\n').length;
    final paragraphs = s.trim().isEmpty
        ? 0
        : s.trim().split(RegExp(r'\n\s*\n')).where((p) => p.trim().isNotEmpty).length;
    final minutes = cjk / 300 + words / 200;
    final readTime = minutes < 1
        ? '${(minutes * 60).round()} 秒'
        : '${minutes.toStringAsFixed(minutes < 10 ? 1 : 0)} 分钟';

    return ToolBody(children: [
      SectionCard(
        title: '输入文本',
        child: Column(
          children: [
            TextField(
              controller: _c,
              minLines: 8,
              maxLines: 14,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                hintText: '在此粘贴或输入文本，统计结果实时更新…',
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
              ),
              style: const TextStyle(fontSize: 14, height: 1.6),
            ),
            Row(
              children: [
                const Spacer(),
                TextButton(
                  onPressed: () {
                    _c.clear();
                    setState(() {});
                  },
                  child: const Text('清空'),
                ),
              ],
            ),
          ],
        ),
      ),
      SectionCard(
        title: '统计结果',
        child: Column(
          children: [
            GridView.count(
              crossAxisCount: 3,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: 1.75,
              children: [
                StatTile(value: '$chars', label: '总字符'),
                StatTile(value: '$noSpace', label: '不含空白'),
                StatTile(value: '$cjk', label: '中文字'),
                StatTile(value: '$words', label: '英文单词'),
                StatTile(value: '$lines', label: '行数'),
                StatTile(value: '$paragraphs', label: '段落'),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Icon(Icons.schedule_rounded, size: 15, color: AppTheme.ink2Of(Theme.of(context).brightness)),
                const SizedBox(width: 6),
                Text(
                  '预计阅读 $readTime',
                  style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(Theme.of(context).brightness)),
                ),
              ],
            ),
          ],
        ),
      ),
    ]);
  }
}

class _DedupeTool extends StatefulWidget {
  const _DedupeTool();

  @override
  State<_DedupeTool> createState() => _DedupeToolState();
}

class _DedupeToolState extends State<_DedupeTool> {
  final _c = TextEditingController();
  int _mode = 0;
  bool _ignoreCase = false;
  bool _trim = true;
  bool _dropEmpty = true;

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  String get _result {
    var lines = _c.text.split('\n');
    if (_trim) lines = lines.map((e) => e.trim()).toList();
    if (_dropEmpty) lines = lines.where((e) => e.isNotEmpty).toList();
    final seen = <String>{};
    final out = <String>[];
    if (_mode == 1) {
      // 保留最后一次出现
      for (var i = lines.length - 1; i >= 0; i--) {
        final k = _ignoreCase ? lines[i].toLowerCase() : lines[i];
        if (seen.add(k)) out.add(lines[i]);
      }
      return out.reversed.join('\n');
    }
    for (final l in lines) {
      final k = _ignoreCase ? l.toLowerCase() : l;
      if (seen.add(k)) out.add(l);
    }
    if (_mode == 2) {
      out.sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    }
    return out.join('\n');
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final out = _result;
    final before = _c.text.isEmpty ? 0 : _c.text.split('\n').length;
    final after = out.isEmpty ? 0 : out.split('\n').length;

    return ToolBody(children: [
      SectionCard(
        title: '原始文本',
        child: Column(
          children: [
            TextField(
              controller: _c,
              minLines: 6,
              maxLines: 10,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                hintText: '每行一条记录…',
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
              ),
              style: const TextStyle(fontSize: 14, height: 1.55),
            ),
            const SizedBox(height: 4),
            SegChips<int>(
              values: const [0, 1, 2],
              labels: const ['保留首次', '保留最后', '去重并排序'],
              value: _mode,
              onChanged: (v) => setState(() => _mode = v),
            ),
            const SizedBox(height: 4),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('忽略大小写', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _ignoreCase,
              onChanged: (v) => setState(() => _ignoreCase = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('去除行首尾空白', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _trim,
              onChanged: (v) => setState(() => _trim = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('删除空行', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _dropEmpty,
              onChanged: (v) => setState(() => _dropEmpty = v),
            ),
          ],
        ),
      ),
      SectionCard(
        title: '去重结果',
        trailing: Text(
          '${_c.text.isEmpty ? 0 : before - after} 条重复已移除',
          style: TextStyle(fontSize: 11.5, color: AppTheme.brandOf(b)),
        ),
        child: ResultBox(text: out, label: '$after 条记录'),
      ),
    ]);
  }
}

class _FindReplaceTool extends StatefulWidget {
  const _FindReplaceTool();

  @override
  State<_FindReplaceTool> createState() => _FindReplaceToolState();
}

class _FindReplaceToolState extends State<_FindReplaceTool> {
  final _src = TextEditingController();
  final _find = TextEditingController();
  final _rep = TextEditingController();
  bool _regex = false;
  bool _caseSensitive = false;
  String _out = '';
  int _count = 0;
  String? _error;

  @override
  void dispose() {
    _src.dispose();
    _find.dispose();
    _rep.dispose();
    super.dispose();
  }

  void _run() {
    final find = _find.text;
    if (find.isEmpty) {
      setState(() {
        _error = '请填写要查找的内容';
        _out = '';
      });
      return;
    }
    try {
      var count = 0;
      late String result;
      if (_regex) {
        final re = RegExp(find, caseSensitive: _caseSensitive, multiLine: true);
        result = _src.text.replaceAllMapped(re, (m) {
          count++;
          return _rep.text;
        });
      } else {
        final source = _src.text;
        final buf = StringBuffer();
        var i = 0;
        while (true) {
          final idx = _indexOf(source, find, i, _caseSensitive);
          if (idx < 0) {
            buf.write(source.substring(i));
            break;
          }
          buf.write(source.substring(i, idx));
          buf.write(_rep.text);
          count++;
          i = idx + find.length;
        }
        result = buf.toString();
      }
      setState(() {
        _out = result;
        _count = count;
        _error = null;
      });
    } catch (e) {
      setState(() {
        _error = '正则表达式无效：$e';
        _out = '';
      });
    }
  }

  static int _indexOf(String s, String p, int start, bool caseSensitive) {
    if (caseSensitive) return s.indexOf(p, start);
    return s.toLowerCase().indexOf(p.toLowerCase(), start);
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '原文',
        child: TextField(
          controller: _src,
          minLines: 6,
          maxLines: 10,
          decoration: const InputDecoration(
            hintText: '粘贴需要处理的文本…',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 14, height: 1.55),
        ),
      ),
      SectionCard(
        title: '查找与替换',
        child: Column(
          children: [
            LabeledField(label: '查找', controller: _find, hint: _regex ? r'\d{3}-\d{4}' : '需要查找的内容'),
            const SizedBox(height: 12),
            LabeledField(label: '替换为', controller: _rep, hint: '留空表示删除'),
            const SizedBox(height: 6),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('使用正则表达式', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              subtitle: Text(
                r'支持 $1 分组引用',
                style: TextStyle(fontSize: 11, color: AppTheme.ink2Of(b)),
              ),
              value: _regex,
              onChanged: (v) => setState(() => _regex = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('区分大小写', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _caseSensitive,
              onChanged: (v) => setState(() => _caseSensitive = v),
            ),
            const SizedBox(height: 8),
            PrimaryButton(label: '执行替换', icon: Icons.find_replace_rounded, onPressed: _run),
          ],
        ),
      ),
      if (_error != null)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: InfoNote(text: _error!, icon: Icons.error_outline_rounded),
        ),
      SectionCard(child: ResultBox(text: _out, label: '替换结果（共 $_count 处）')),
    ]);
  }
}

class _DelimiterTool extends StatefulWidget {
  const _DelimiterTool();

  @override
  State<_DelimiterTool> createState() => _DelimiterToolState();
}

class _DelimiterToolState extends State<_DelimiterTool> {
  final _c = TextEditingController();
  int _from = 0;
  int _to = 1;
  bool _dropEmpty = true;

  static const _labels = ['换行', '逗号', '分号', '空格', '制表符'];
  static const _values = ['\n', ',', ';', ' ', '\t'];

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  String get _result {
    var items = _c.text.split(_values[_from]);
    if (_dropEmpty) items = items.where((e) => e.trim().isNotEmpty).toList();
    return items.map((e) => e.trim()).join(_values[_to]);
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '原始内容',
        child: TextField(
          controller: _c,
          minLines: 6,
          maxLines: 10,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            hintText: '例如：苹果,香蕉,橙子',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 14, height: 1.55),
        ),
      ),
      SectionCard(
        title: '分隔符',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('原分隔符', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
            const SizedBox(height: 8),
            SegChips<int>(
              values: const [0, 1, 2, 3, 4],
              labels: _labels,
              value: _from,
              onChanged: (v) => setState(() => _from = v),
            ),
            const SizedBox(height: 14),
            Text('转换为', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
            const SizedBox(height: 8),
            SegChips<int>(
              values: const [0, 1, 2, 3, 4],
              labels: _labels,
              value: _to,
              onChanged: (v) => setState(() => _to = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('丢弃空项', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _dropEmpty,
              onChanged: (v) => setState(() => _dropEmpty = v),
            ),
          ],
        ),
      ),
      SectionCard(child: ResultBox(text: _result, label: '转换结果')),
    ]);
  }
}

class _DiffTool extends StatefulWidget {
  const _DiffTool();

  @override
  State<_DiffTool> createState() => _DiffToolState();
}

class _DiffToolState extends State<_DiffTool> {
  final _a = TextEditingController();
  final _b = TextEditingController();
  String _out = '';
  int _added = 0;
  int _removed = 0;
  bool _done = false;

  @override
  void dispose() {
    _a.dispose();
    _b.dispose();
    super.dispose();
  }

  void _run() {
    final left = _a.text.split('\n');
    final right = _b.text.split('\n');
    const cap = 600;
    final l = left.length > cap ? left.sublist(0, cap) : left;
    final r = right.length > cap ? right.sublist(0, cap) : right;

    // LCS 动态规划（行级）
    final n = l.length, m = r.length;
    final dp = List.generate(n + 1, (_) => List<int>.filled(m + 1, 0));
    for (var i = n - 1; i >= 0; i--) {
      for (var j = m - 1; j >= 0; j--) {
        dp[i][j] = l[i] == r[j] ? dp[i + 1][j + 1] + 1 : math.max(dp[i + 1][j], dp[i][j + 1]);
      }
    }
    final buf = StringBuffer();
    var i = 0, j = 0, added = 0, removed = 0;
    while (i < n && j < m) {
      if (l[i] == r[j]) {
        buf.writeln('  ${l[i]}');
        i++;
        j++;
      } else if (dp[i + 1][j] >= dp[i][j + 1]) {
        buf.writeln('- ${l[i]}');
        removed++;
        i++;
      } else {
        buf.writeln('+ ${r[j]}');
        added++;
        j++;
      }
    }
    while (i < n) {
      buf.writeln('- ${l[i++]}');
      removed++;
    }
    while (j < m) {
      buf.writeln('+ ${r[j++]}');
      added++;
    }
    setState(() {
      _out = buf.toString().trimRight();
      _added = added;
      _removed = removed;
      _done = true;
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '原文（左）',
        child: TextField(
          controller: _a,
          minLines: 5,
          maxLines: 8,
          decoration: const InputDecoration(
            hintText: '第一段文本…',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 13.5, height: 1.5),
        ),
      ),
      SectionCard(
        title: '对比文（右）',
        child: TextField(
          controller: _b,
          minLines: 5,
          maxLines: 8,
          decoration: const InputDecoration(
            hintText: '第二段文本…',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 13.5, height: 1.5),
        ),
      ),
      PrimaryButton(label: '开始对比', icon: Icons.difference_outlined, onPressed: _run),
      const SizedBox(height: 12),
      if (_done)
        SectionCard(
          title: '差异结果',
          trailing: Text(
            '-$_removed  +$_added',
            style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.brandOf(b)),
          ),
          child: ResultBox(text: _out, label: '逐行差异（- 仅左侧 / + 仅右侧）'),
        ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 纯函数
// ---------------------------------------------------------------------------

List<String> _lines(String s) => s.split('\n');

String _sorted(String s, int Function(String a, String b) cmp) {
  final list = _lines(s);
  list.sort(cmp);
  return list.join('\n');
}

double _numOf(String s) {
  final m = RegExp(r'-?\d+(?:\.\d+)?').firstMatch(s);
  return m == null ? 0 : (double.tryParse(m.group(0)!) ?? 0);
}

String _numbered(String s, String Function(int i) prefix) {
  final list = _lines(s);
  final buf = StringBuffer();
  for (var i = 0; i < list.length; i++) {
    buf.writeln('${prefix(i)}${list[i]}');
  }
  return buf.toString().trimRight();
}

List<String> _words(String s) {
  final spaced = s.replaceAllMapped(RegExp(r'([a-z0-9])([A-Z])'), (m) => '${m[1]} ${m[2]}');
  return spaced
      .split(RegExp(r'[^A-Za-z0-9\u4e00-\u9fa5]+'))
      .where((w) => w.isNotEmpty)
      .toList();
}

String _joinWords(String s, String sep, {required bool lower}) {
  final ws = _words(s);
  return ws.map((w) => lower ? w.toLowerCase() : w.toUpperCase()).join(sep);
}

String _camel(String s, {required bool upperFirst}) {
  final ws = _words(s);
  if (ws.isEmpty) return '';
  final buf = StringBuffer();
  for (var i = 0; i < ws.length; i++) {
    final w = ws[i].toLowerCase();
    if (i == 0 && !upperFirst) {
      buf.write(w);
    } else {
      buf.write(w.isEmpty ? w : '${w[0].toUpperCase()}${w.substring(1)}');
    }
  }
  return buf.toString();
}

String _titleCase(String s) {
  return s.split('\n').map((line) {
    return line.replaceAllMapped(RegExp(r'(\S)(\S*)'), (m) {
      final word = m.group(0)!;
      return '${word[0].toUpperCase()}${word.substring(1).toLowerCase()}';
    });
  }).join('\n');
}

String _sentenceCase(String s) {
  final lower = s.toLowerCase();
  final buf = StringBuffer();
  var cap = true;
  for (var i = 0; i < lower.length; i++) {
    final ch = lower[i];
    if (cap && RegExp(r'[a-z]').hasMatch(ch)) {
      buf.write(ch.toUpperCase());
      cap = false;
    } else {
      buf.write(ch);
      if ('.!?。！？\n'.contains(ch)) cap = true;
    }
  }
  return buf.toString();
}

String _extract(String s, String pattern) {
  final re = RegExp(pattern);
  final seen = <String>{};
  final out = <String>[];
  for (final m in re.allMatches(s)) {
    final v = m.group(0)!;
    if (seen.add(v)) out.add(v);
  }
  return out.join('\n');
}

String _toHalfWidth(String s) {
  final buf = StringBuffer();
  for (final r in s.runes) {
    if (r == 0x3000) {
      buf.write(' ');
    } else if (r >= 0xFF01 && r <= 0xFF5E) {
      buf.writeCharCode(r - 0xFEE0);
    } else {
      buf.writeCharCode(r);
    }
  }
  return buf.toString();
}
