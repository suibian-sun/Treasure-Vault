import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';

/// 生活娱乐类工具（4 个）。
final List<ToolDef> lifeTools = [
  ToolDef(
    id: 'dice',
    name: '掷骰子',
    desc: 'D6 / D8 / D12 / D20 多颗同掷',
    category: Categories.life,
    icon: Icons.casino_outlined,
    keywords: ['骰子', 'dice', '随机'],
    build: () => const _DiceTool(),
  ),
  ToolDef(
    id: 'picker',
    name: '随机抽签',
    desc: '从名单中抽取，可设置不重复',
    category: Categories.life,
    icon: Icons.card_giftcard_rounded,
    keywords: ['抽签', '抽奖', 'picker', '名单'],
    build: () => const _PickerTool(),
  ),
  ToolDef(
    id: 'coin',
    name: '抛硬币',
    desc: '正反面统计，快速做决定',
    category: Categories.life,
    icon: Icons.monetization_on_outlined,
    keywords: ['硬币', '决定', 'coin'],
    build: () => const _CoinTool(),
  ),
  ToolDef(
    id: 'stopwatch',
    name: '秒表',
    desc: '计时与分段记录',
    category: Categories.life,
    icon: Icons.timer_outlined,
    keywords: ['秒表', '计时', 'stopwatch'],
    build: () => const _StopwatchTool(),
  ),
];

class _DiceTool extends StatefulWidget {
  const _DiceTool();

  @override
  State<_DiceTool> createState() => _DiceToolState();
}

class _DiceToolState extends State<_DiceTool> {
  int _sides = 6;
  int _count = 1;
  List<int> _values = [];
  final _rnd = math.Random.secure();

  void _roll() {
    setState(() {
      _values = List<int>.generate(_count, (_) => _rnd.nextInt(_sides) + 1);
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final total = _values.fold<int>(0, (a, c) => a + c);
    return ToolBody(children: [
      SectionCard(
        title: '骰子设置',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('面数', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
            const SizedBox(height: 8),
            SegChips<int>(
              values: const [4, 6, 8, 12, 20, 100],
              labels: const ['D4', 'D6', 'D8', 'D12', 'D20', 'D100'],
              value: _sides,
              onChanged: (v) => setState(() => _sides = v),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Text('数量', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
                const Spacer(),
                Text(
                  '$_count 颗',
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
              max: 10,
              divisions: 9,
              onChanged: (v) => setState(() => _count = v.round()),
            ),
            PrimaryButton(label: '掷骰子', icon: Icons.casino_rounded, onPressed: _roll),
          ],
        ),
      ),
      if (_values.isNotEmpty)
        SectionCard(
          title: '结果',
          trailing: Text(
            '合计 $total',
            style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: AppTheme.brandOf(b)),
          ),
          child: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              for (final v in _values)
                Container(
                  width: 62,
                  height: 62,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: AppTheme.selectedFillOf(b),
                    borderRadius: BorderRadius.circular(AppTheme.radMedium),
                    border: Border.all(
                      color: AppTheme.brandOf(b).withValues(alpha: 0.25),
                    ),
                  ),
                  child: Text(
                    '$v',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: AppTheme.brandOf(b),
                    ),
                  ),
                ),
            ],
          ),
        ),
      if (_values.isEmpty)
        Text('点击「掷骰子」开始', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
    ]);
  }
}

class _PickerTool extends StatefulWidget {
  const _PickerTool();

  @override
  State<_PickerTool> createState() => _PickerToolState();
}

class _PickerToolState extends State<_PickerTool> {
  final _c = TextEditingController(text: '苹果\n香蕉\n橙子\n葡萄\n西瓜');
  int _count = 1;
  bool _unique = true;
  List<String> _picked = [];

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  void _draw() {
    final items = _c.text
        .split('\n')
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
    if (items.isEmpty) {
      toast(context, '请先填写候选名单', error: true);
      return;
    }
    if (_unique && _count > items.length) {
      toast(context, '候选只有 ${items.length} 项', error: true);
      return;
    }
    final rnd = math.Random.secure();
    final result = <String>[];
    if (_unique) {
      final pool = [...items]..shuffle(rnd);
      result.addAll(pool.take(_count));
    } else {
      for (var i = 0; i < _count; i++) {
        result.add(items[rnd.nextInt(items.length)]);
      }
    }
    setState(() => _picked = result);
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final items = _c.text.split('\n').where((e) => e.trim().isNotEmpty).length;
    return ToolBody(children: [
      SectionCard(
        title: '候选名单（每行一项）',
        trailing: Text('$items 项', style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b))),
        child: TextField(
          controller: _c,
          minLines: 6,
          maxLines: 12,
          decoration: const InputDecoration(
            hintText: '每行一个候选项…',
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
          ),
          style: const TextStyle(fontSize: 14, height: 1.6),
        ),
      ),
      SectionCard(
        title: '抽取设置',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('抽取数量', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
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
              max: 10,
              divisions: 9,
              onChanged: (v) => setState(() => _count = v.round()),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('不重复抽取', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _unique,
              onChanged: (v) => setState(() => _unique = v),
            ),
            PrimaryButton(label: '开始抽取', icon: Icons.auto_awesome_rounded, onPressed: _draw),
          ],
        ),
      ),
      if (_picked.isNotEmpty)
        SectionCard(
          title: '抽取结果',
          child: Column(
            children: [
              for (var i = 0; i < _picked.length; i++)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(
                    children: [
                      Container(
                        width: 24,
                        height: 24,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: AppTheme.brandOf(b),
                          shape: BoxShape.circle,
                        ),
                        child: Text(
                          '${i + 1}',
                          style: const TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          _picked[i],
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: AppTheme.inkOf(b),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
    ]);
  }
}

class _CoinTool extends StatefulWidget {
  const _CoinTool();

  @override
  State<_CoinTool> createState() => _CoinToolState();
}

class _CoinToolState extends State<_CoinTool> {
  int _heads = 0;
  int _tails = 0;
  String _last = '';
  final _rnd = math.Random.secure();

  void _flip() {
    final head = _rnd.nextBool();
    setState(() {
      _last = head ? '正面' : '反面';
      if (head) {
        _heads++;
      } else {
        _tails++;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final total = _heads + _tails;
    return ToolBody(children: [
      SectionCard(
        child: Column(
          children: [
            Container(
              width: 140,
              height: 140,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: _last.isEmpty
                    ? AppTheme.fieldOf(b)
                    : AppTheme.selectedFillOf(b),
                shape: BoxShape.circle,
                border: Border.all(
                  color: AppTheme.brandOf(b).withValues(alpha: 0.3),
                  width: 1,
                ),
              ),
              child: Text(
                _last.isEmpty ? '?' : _last,
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w800,
                  color: _last.isEmpty ? AppTheme.ink3 : AppTheme.brandOf(b),
                ),
              ),
            ),
            const SizedBox(height: 18),
            PrimaryButton(label: '抛一次', icon: Icons.flip_rounded, onPressed: _flip),
            if (total > 0)
              TextButton(
                onPressed: () => setState(() {
                  _heads = 0;
                  _tails = 0;
                  _last = '';
                }),
                child: const Text('重置统计'),
              ),
          ],
        ),
      ),
      if (total > 0)
        SectionCard(
          title: '统计（共 $total 次）',
          child: Row(
            children: [
              Expanded(
                child: StatTile(
                  value: '$_heads',
                  label: '正面 ${(100 * _heads / total).toStringAsFixed(0)}%',
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: StatTile(
                  value: '$_tails',
                  label: '反面 ${(100 * _tails / total).toStringAsFixed(0)}%',
                ),
              ),
            ],
          ),
        ),
    ]);
  }
}

class _StopwatchTool extends StatefulWidget {
  const _StopwatchTool();

  @override
  State<_StopwatchTool> createState() => _StopwatchToolState();
}

class _StopwatchToolState extends State<_StopwatchTool> {
  final _sw = Stopwatch();
  Timer? _timer;
  final List<int> _laps = [];

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _toggle() {
    if (_sw.isRunning) {
      _sw.stop();
      _timer?.cancel();
    } else {
      _sw.start();
      _timer = Timer.periodic(const Duration(milliseconds: 50), (_) {
        if (mounted) setState(() {});
      });
    }
    setState(() {});
  }

  void _lap() {
    if (!_sw.isRunning) return;
    setState(() => _laps.insert(0, _sw.elapsedMilliseconds));
  }

  void _reset() {
    _timer?.cancel();
    _sw.reset();
    setState(_laps.clear);
  }

  static String _fmt(int ms) {
    final h = ms ~/ 3600000;
    final m = (ms % 3600000) ~/ 60000;
    final s = (ms % 60000) ~/ 1000;
    final cs = (ms % 1000) ~/ 10;
    final base = '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}.${cs.toString().padLeft(2, '0')}';
    return h > 0 ? '${h.toString().padLeft(2, '0')}:$base' : base;
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final elapsed = _sw.elapsedMilliseconds;
    return ToolBody(children: [
      SectionCard(
        child: Column(
          children: [
            Text(
              _fmt(elapsed),
              style: TextStyle(
                fontSize: 46,
                fontWeight: FontWeight.w300,
                letterSpacing: 1,
                color: AppTheme.inkOf(b),
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: GhostButton(
                    label: '重置',
                    icon: Icons.refresh_rounded,
                    onPressed: _reset,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: PrimaryButton(
                    label: _sw.isRunning ? '暂停' : '开始',
                    icon: _sw.isRunning ? Icons.pause_rounded : Icons.play_arrow_rounded,
                    onPressed: _toggle,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: GhostButton(
                    label: '分段',
                    icon: Icons.flag_outlined,
                    onPressed: _sw.isRunning ? _lap : null,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      if (_laps.isNotEmpty)
        SectionCard(
          title: '分段记录',
          child: Column(
            children: [
              for (var i = 0; i < _laps.length; i++)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(
                    children: [
                      Text(
                        '#${_laps.length - i}',
                        style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
                      ),
                      const Spacer(),
                      Text(
                        _fmt(_laps[i]),
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: AppTheme.inkOf(b),
                          fontFeatures: const [FontFeature.tabularFigures()],
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
    ]);
  }
}
