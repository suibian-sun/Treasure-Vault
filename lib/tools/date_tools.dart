import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';
import 'common.dart';

/// 日期时间类工具（5 个）。
final List<ToolDef> dateTools = [
  ToolDef(
    id: 'timestamp',
    name: '时间戳转换',
    desc: '秒 / 毫秒与本地、UTC 时间互转',
    category: Categories.date,
    icon: Icons.access_time_rounded,
    keywords: ['时间戳', 'timestamp', 'unix', '时间'],
    build: () => const _TimestampTool(),
  ),
  ToolDef(
    id: 'date_diff',
    name: '日期相差',
    desc: '两个日期之间的天数 / 周数 / 工作日',
    category: Categories.date,
    icon: Icons.date_range_outlined,
    keywords: ['日期', '相差', '天数', '工作日'],
    build: () => const _DateDiffTool(),
  ),
  ToolDef(
    id: 'age',
    name: '年龄计算',
    desc: '精确到年月日，附生肖与星座',
    category: Categories.date,
    icon: Icons.cake_outlined,
    keywords: ['年龄', '生日', '生肖', '星座'],
    build: () => const _AgeTool(),
  ),
  ToolDef(
    id: 'countdown',
    name: '倒数日',
    desc: '距离目标日期还有多少天',
    category: Categories.date,
    icon: Icons.hourglass_bottom_rounded,
    keywords: ['倒数', '纪念日', 'countdown'],
    build: () => const _CountdownTool(),
  ),
  ToolDef(
    id: 'date_query',
    name: '日期查询',
    desc: '星期、年内周数、闰年判断',
    category: Categories.date,
    icon: Icons.event_note_outlined,
    keywords: ['星期', '周数', '闰年', '日期'],
    build: () => const _DateQueryTool(),
  ),
];

String _weekdayZh(DateTime d) => weekdayZh(d);

class _DateRow extends StatelessWidget {
  final String label;
  final DateTime value;
  final VoidCallback onTap;

  const _DateRow({required this.label, required this.value, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppTheme.radSmall),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        decoration: BoxDecoration(
          color: AppTheme.fieldOf(b),
          borderRadius: BorderRadius.circular(AppTheme.radSmall),
        ),
        child: Row(
          children: [
            Text(label, style: TextStyle(fontSize: 13, color: AppTheme.ink2Of(b))),
            const Spacer(),
            Text(
              fmtDate(value),
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: AppTheme.inkOf(b),
              ),
            ),
            const SizedBox(width: 6),
            Icon(Icons.calendar_today_outlined, size: 14, color: AppTheme.ink2Of(b)),
          ],
        ),
      ),
    );
  }
}

Future<DateTime?> pickDate(BuildContext context, DateTime initial) {
  return showDatePicker(
    context: context,
    initialDate: initial,
    firstDate: DateTime(1900),
    lastDate: DateTime(2200),
    builder: (ctx, child) => Theme(
      data: Theme.of(ctx).copyWith(
        colorScheme: Theme.of(ctx).colorScheme.copyWith(primary: AppTheme.primary),
      ),
      child: child!,
    ),
  );
}

// ---------------------------------------------------------------------------
// 时间戳
// ---------------------------------------------------------------------------

class _TimestampTool extends StatefulWidget {
  const _TimestampTool();

  @override
  State<_TimestampTool> createState() => _TimestampToolState();
}

class _TimestampToolState extends State<_TimestampTool> {
  final _c = TextEditingController();
  DateTime _picked = DateTime.now();
  bool _ms = true;
  String _fromTs = '';

  @override
  void initState() {
    super.initState();
    _c.text = '${DateTime.now().millisecondsSinceEpoch}';
    _convertTs();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  void _convertTs() {
    final raw = _c.text.trim();
    if (raw.isEmpty) {
      setState(() => _fromTs = '');
      return;
    }
    final n = int.tryParse(raw);
    if (n == null) {
      setState(() => _fromTs = '请输入纯数字时间戳');
      return;
    }
    // 10 位视为秒，13 位视为毫秒；其余按数值大小推断
    final isSecond = raw.length <= 10;
    final dt = DateTime.fromMillisecondsSinceEpoch(isSecond ? n * 1000 : n);
    setState(() {
      _fromTs = '本地：${fmtDateTime(dt)}（周${_weekdayZh(dt)}）\n'
          'UTC ：${dt.toUtc().toIso8601String().substring(0, 19).replaceFirst('T', ' ')}\n'
          '秒  ：${dt.millisecondsSinceEpoch ~/ 1000}\n'
          '毫秒：${dt.millisecondsSinceEpoch}';
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final ts = _ms
        ? _picked.millisecondsSinceEpoch
        : _picked.millisecondsSinceEpoch ~/ 1000;

    return ToolBody(children: [
      SectionCard(
        title: '时间戳 → 时间',
        trailing: TextButton(
          onPressed: () {
            _c.text = '${DateTime.now().millisecondsSinceEpoch}';
            _convertTs();
          },
          child: const Text('当前'),
        ),
        child: Column(
          children: [
            LabeledField(
              label: '时间戳（秒或毫秒，自动识别）',
              controller: _c,
              hint: '1730000000',
              keyboardType: TextInputType.number,
              onChanged: (_) => _convertTs(),
              formatters: [FilteringTextInputFormatter.digitsOnly],
            ),
            const SizedBox(height: 12),
            ResultBox(text: _fromTs, label: '转换结果', mono: false),
          ],
        ),
      ),
      SectionCard(
        title: '时间 → 时间戳',
        child: Column(
          children: [
            _DateRow(
              label: '选择日期',
              value: _picked,
              onTap: () async {
                final d = await pickDate(context, _picked);
                if (d == null) return;
                final t = await showTimePicker(
                  context: context,
                  initialTime: TimeOfDay.fromDateTime(_picked),
                );
                setState(() {
                  _picked = DateTime(
                    d.year,
                    d.month,
                    d.day,
                    t?.hour ?? _picked.hour,
                    t?.minute ?? _picked.minute,
                  );
                });
              },
            ),
            const SizedBox(height: 10),
            SegChips<bool>(
              values: const [false, true],
              labels: const ['秒（10 位）', '毫秒（13 位）'],
              value: _ms,
              onChanged: (v) => setState(() => _ms = v),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: SelectableText(
                    '$ts',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                      color: AppTheme.brandOf(b),
                    ),
                  ),
                ),
                IconButton(
                  onPressed: () => copyText(context, '$ts'),
                  icon: const Icon(Icons.copy_rounded, size: 17),
                ),
              ],
            ),
          ],
        ),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 日期相差
// ---------------------------------------------------------------------------

class _DateDiffTool extends StatefulWidget {
  const _DateDiffTool();

  @override
  State<_DateDiffTool> createState() => _DateDiffToolState();
}

class _DateDiffToolState extends State<_DateDiffTool> {
  DateTime _a = DateTime.now();
  DateTime _b = DateTime.now().add(const Duration(days: 30));

  int get _days => _b.difference(_a).inDays.abs();

  int get _workdays {
    final start = _a.isBefore(_b) ? _a : _b;
    final end = _a.isBefore(_b) ? _b : _a;
    var count = 0;
    var cursor = DateTime(start.year, start.month, start.day);
    final last = DateTime(end.year, end.month, end.day);
    while (cursor.isBefore(last)) {
      cursor = cursor.add(const Duration(days: 1));
      if (cursor.weekday != DateTime.saturday && cursor.weekday != DateTime.sunday) {
        count++;
      }
    }
    return count;
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '选择两个日期',
        child: Column(
          children: [
            _DateRow(
              label: '起始',
              value: _a,
              onTap: () async {
                final d = await pickDate(context, _a);
                if (d != null) setState(() => _a = d);
              },
            ),
            const SizedBox(height: 8),
            _DateRow(
              label: '结束',
              value: _b,
              onTap: () async {
                final d = await pickDate(context, _b);
                if (d != null) setState(() => _b = d);
              },
            ),
          ],
        ),
      ),
      SectionCard(
        title: '相差',
        child: Column(
          children: [
            BigResult(value: '$_days 天', label: '总天数', sub: '约 ${(_days / 7).toStringAsFixed(1)} 周'),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: StatTile(value: '$_workdays', label: '工作日（不含节假日）')),
                const SizedBox(width: 8),
                Expanded(child: StatTile(value: '${_days - _workdays}', label: '周末天数')),
              ],
            ),
          ],
        ),
      ),
      Text(
        '工作日仅按周一至周五计算，未扣除法定节假日。',
        style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 年龄
// ---------------------------------------------------------------------------

class _AgeTool extends StatefulWidget {
  const _AgeTool();

  @override
  State<_AgeTool> createState() => _AgeToolState();
}

class _AgeToolState extends State<_AgeTool> {
  DateTime _birth = DateTime(2000, 1, 1);

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final now = DateTime.now();
    var years = now.year - _birth.year;
    var months = now.month - _birth.month;
    var days = now.day - _birth.day;
    if (days < 0) {
      months--;
      days += DateTime(now.year, now.month, 0).day;
    }
    if (months < 0) {
      years--;
      months += 12;
    }
    final totalDays = now.difference(_birth).inDays;
    final nextBirthday = DateTime(now.year, _birth.month, _birth.day);
    final next = nextBirthday.isBefore(DateTime(now.year, now.month, now.day))
        ? DateTime(now.year + 1, _birth.month, _birth.day)
        : nextBirthday;

    const zodiacs = ['鼠', '牛', '虎', '兔', '龙', '蛇', '马', '羊', '猴', '鸡', '狗', '猪'];
    final zodiac = zodiacs[(_birth.year - 1900) % 12];
    final constellation = _constellation(_birth.month, _birth.day);

    return ToolBody(children: [
      SectionCard(
        title: '出生日期',
        child: _DateRow(
          label: '生日',
          value: _birth,
          onTap: () async {
            final d = await pickDate(context, _birth);
            if (d != null) setState(() => _birth = d);
          },
        ),
      ),
      SectionCard(
        title: '年龄',
        child: Column(
          children: [
            BigResult(
              value: '$years 岁',
              label: '周岁',
              sub: '$months 个月 $days 天',
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: StatTile(value: '$totalDays', label: '出生天数')),
                const SizedBox(width: 8),
                Expanded(
                  child: StatTile(
                    value: '${next.difference(DateTime(now.year, now.month, now.day)).inDays}',
                    label: '距下次生日',
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _pill(context, '生肖 · $zodiac'),
                _pill(context, '星座 · $constellation'),
                _pill(context, '${totalDays ~/ 7} 周'),
              ],
            ),
          ],
        ),
      ),
      Text(
        '星座按公历日期近似划分，交界日可能存在一天误差。',
        style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
      ),
    ]);
  }

  static String _constellation(int m, int d) {
    const starts = [20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22];
    final idx = d < starts[m - 1] ? m - 1 : m;
    return const [
      '摩羯座', '水瓶座', '双鱼座', '白羊座', '金牛座', '双子座',
      '巨蟹座', '狮子座', '处女座', '天秤座', '天蝎座', '射手座', '摩羯座',
    ][idx];
  }

  Widget _pill(BuildContext context, String text) {
    final b = Theme.of(context).brightness;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      decoration: BoxDecoration(
        color: AppTheme.fieldOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radPill),
        border: Border.all(color: AppTheme.lineOf(b), width: 1),
      ),
      child: Text(text, style: TextStyle(fontSize: 12, color: AppTheme.inkOf(b))),
    );
  }
}

// ---------------------------------------------------------------------------
// 倒数日
// ---------------------------------------------------------------------------

class _CountdownTool extends StatefulWidget {
  const _CountdownTool();

  @override
  State<_CountdownTool> createState() => _CountdownToolState();
}

class _CountdownToolState extends State<_CountdownTool> {
  DateTime _target = DateTime.now().add(const Duration(days: 30));

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    final base = DateTime(today.year, today.month, today.day);
    final days = DateTime(_target.year, _target.month, _target.day).difference(base).inDays;

    return ToolBody(children: [
      SectionCard(
        title: '目标日期',
        child: _DateRow(
          label: '日期',
          value: _target,
          onTap: () async {
            final d = await pickDate(context, _target);
            if (d != null) setState(() => _target = d);
          },
        ),
      ),
      SectionCard(
        child: Column(
          children: [
            BigResult(
              value: days >= 0 ? '$days 天' : '${-days} 天前',
              label: days >= 0 ? '距离目标还有' : '已经过去',
              sub: '${fmtDate(_target)} · 周${_weekdayZh(_target)}',
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: StatTile(value: '${(days.abs() / 7).toStringAsFixed(1)}', label: '周')),
                const SizedBox(width: 8),
                Expanded(child: StatTile(value: '${(days.abs() / 30).toStringAsFixed(1)}', label: '月（约）')),
              ],
            ),
          ],
        ),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 日期查询
// ---------------------------------------------------------------------------

class _DateQueryTool extends StatefulWidget {
  const _DateQueryTool();

  @override
  State<_DateQueryTool> createState() => _DateQueryToolState();
}

class _DateQueryToolState extends State<_DateQueryTool> {
  DateTime _date = DateTime.now();

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final yearStart = DateTime(_date.year, 1, 1);
    final dayOfYear = _date.difference(yearStart).inDays + 1;
    final isoWeek = ((dayOfYear - _date.weekday + 10) / 7).floor();
    final isLeap = (_date.year % 4 == 0 && _date.year % 100 != 0) || _date.year % 400 == 0;
    final daysInMonth = DateTime(_date.year, _date.month + 1, 0).day;

    return ToolBody(children: [
      SectionCard(
        title: '选择日期',
        child: _DateRow(
          label: '日期',
          value: _date,
          onTap: () async {
            final d = await pickDate(context, _date);
            if (d != null) setState(() => _date = d);
          },
        ),
      ),
      SectionCard(
        title: '查询结果',
        child: Column(
          children: [
            BigResult(
              value: '星期${_weekdayZh(_date)}',
              label: fmtDate(_date),
              sub: '${_date.year} 年 ${_date.month} 月 ${_date.day} 日',
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: StatTile(value: '$dayOfYear', label: '年内第几天')),
                const SizedBox(width: 8),
                Expanded(child: StatTile(value: 'W$isoWeek', label: 'ISO 周数')),
                const SizedBox(width: 8),
                Expanded(child: StatTile(value: isLeap ? '是' : '否', label: '闰年')),
              ],
            ),
            const SizedBox(height: 8),
            Divider(color: AppTheme.lineOf(b), height: 20),
            KVRow(k: '本月天数', v: '$daysInMonth 天'),
            KVRow(k: '季度', v: '第 ${((_date.month - 1) ~/ 3) + 1} 季度'),
            KVRow(k: '星期序号', v: '${_date.weekday}（1 = 周一）'),
          ],
        ),
      ),
    ]);
  }
}
