import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../widgets/kit.dart';
import 'common.dart';

/// 计算换算类工具（16 个）。
final List<ToolDef> calcTools = [
  ToolDef(
    id: 'calculator',
    name: '科学计算器',
    desc: '支持括号、乘方、阶乘与常用函数',
    category: Categories.calc,
    icon: Icons.calculate_outlined,
    keywords: ['计算器', 'calculator', '算式'],
    build: () => const _CalculatorTool(),
  ),
  ToolDef(
    id: 'base_convert',
    name: '进制转换',
    desc: '二 / 八 / 十 / 十六 / 三十六进制',
    category: Categories.calc,
    icon: Icons.numbers_rounded,
    keywords: ['进制', 'binary', 'hex', 'radix'],
    build: () => const _BaseConvertTool(),
  ),
  ToolDef(
    id: 'unit_length',
    name: '长度换算',
    desc: '米 / 千米 / 英尺 / 英里 / 市里',
    category: Categories.calc,
    icon: Icons.straighten_rounded,
    keywords: ['长度', '单位', '米', '英尺', 'inch'],
    build: () => UnitConverterTool(
      baseNote: '以「米」为基准',
      units: [
        _UnitDef('毫米', 'mm', (v) => v / 1000, (v) => v * 1000),
        _UnitDef('厘米', 'cm', (v) => v / 100, (v) => v * 100),
        _UnitDef('米', 'm', (v) => v, (v) => v),
        _UnitDef('千米', 'km', (v) => v * 1000, (v) => v / 1000),
        _UnitDef('市寸', '寸', (v) => v / 30, (v) => v * 30),
        _UnitDef('市尺', '尺', (v) => v / 3, (v) => v * 3),
        _UnitDef('市里', '里', (v) => v * 500, (v) => v / 500),
        _UnitDef('英寸', 'in', (v) => v * 0.0254, (v) => v / 0.0254),
        _UnitDef('英尺', 'ft', (v) => v * 0.3048, (v) => v / 0.3048),
        _UnitDef('码', 'yd', (v) => v * 0.9144, (v) => v / 0.9144),
        _UnitDef('英里', 'mi', (v) => v * 1609.344, (v) => v / 1609.344),
        _UnitDef('海里', 'nmi', (v) => v * 1852, (v) => v / 1852),
      ],
    ),
  ),
  ToolDef(
    id: 'unit_weight',
    name: '重量换算',
    desc: '千克 / 斤 / 两 / 磅 / 盎司',
    category: Categories.calc,
    icon: Icons.scale_outlined,
    keywords: ['重量', '质量', 'kg', '斤', '磅'],
    build: () => UnitConverterTool(
      baseNote: '以「克」为基准',
      units: [
        _UnitDef('毫克', 'mg', (v) => v / 1000, (v) => v * 1000),
        _UnitDef('克', 'g', (v) => v, (v) => v),
        _UnitDef('千克', 'kg', (v) => v * 1000, (v) => v / 1000),
        _UnitDef('吨', 't', (v) => v * 1000000, (v) => v / 1000000),
        _UnitDef('市斤', '斤', (v) => v * 500, (v) => v / 500),
        _UnitDef('市两', '两', (v) => v * 50, (v) => v / 50),
        _UnitDef('磅', 'lb', (v) => v * 453.59237, (v) => v / 453.59237),
        _UnitDef('盎司', 'oz', (v) => v * 28.349523125, (v) => v / 28.349523125),
      ],
    ),
  ),
  ToolDef(
    id: 'unit_temp',
    name: '温度换算',
    desc: '摄氏度 / 华氏度 / 开尔文 / 兰氏度',
    category: Categories.calc,
    icon: Icons.thermostat_rounded,
    keywords: ['温度', '摄氏', '华氏', '开尔文'],
    build: () => UnitConverterTool(
      baseNote: '以「摄氏度」为基准',
      decimals: 2,
      units: [
        _UnitDef('摄氏度', '°C', (v) => v, (v) => v),
        _UnitDef('华氏度', '°F', (v) => (v - 32) * 5 / 9, (v) => v * 9 / 5 + 32),
        _UnitDef('开尔文', 'K', (v) => v - 273.15, (v) => v + 273.15),
        _UnitDef('兰氏度', '°R', (v) => (v - 491.67) * 5 / 9, (v) => (v + 273.15) * 9 / 5),
      ],
    ),
  ),
  ToolDef(
    id: 'unit_area',
    name: '面积换算',
    desc: '平方米 / 公顷 / 亩 / 平方英尺',
    category: Categories.calc,
    icon: Icons.crop_square_rounded,
    keywords: ['面积', '亩', '公顷', '平方'],
    build: () => UnitConverterTool(
      baseNote: '以「平方米」为基准',
      units: [
        _UnitDef('平方厘米', 'cm²', (v) => v / 10000, (v) => v * 10000),
        _UnitDef('平方米', 'm²', (v) => v, (v) => v),
        _UnitDef('平方千米', 'km²', (v) => v * 1000000, (v) => v / 1000000),
        _UnitDef('公顷', 'ha', (v) => v * 10000, (v) => v / 10000),
        _UnitDef('亩', '亩', (v) => v * 666.6666667, (v) => v / 666.6666667),
        _UnitDef('平方英尺', 'ft²', (v) => v * 0.09290304, (v) => v / 0.09290304),
        _UnitDef('英亩', 'ac', (v) => v * 4046.8564224, (v) => v / 4046.8564224),
      ],
    ),
  ),
  ToolDef(
    id: 'unit_volume',
    name: '体积换算',
    desc: '毫升 / 升 / 立方米 / 加仑',
    category: Categories.calc,
    icon: Icons.local_drink_outlined,
    keywords: ['体积', '容积', '升', '加仑'],
    build: () => UnitConverterTool(
      baseNote: '以「升」为基准',
      units: [
        _UnitDef('毫升', 'mL', (v) => v / 1000, (v) => v * 1000),
        _UnitDef('升', 'L', (v) => v, (v) => v),
        _UnitDef('立方米', 'm³', (v) => v * 1000, (v) => v / 1000),
        _UnitDef('立方厘米', 'cm³', (v) => v / 1000, (v) => v * 1000),
        _UnitDef('美制加仑', 'gal', (v) => v * 3.785411784, (v) => v / 3.785411784),
        _UnitDef('美制品脱', 'pt', (v) => v * 0.473176473, (v) => v / 0.473176473),
      ],
    ),
  ),
  ToolDef(
    id: 'storage_size',
    name: '存储容量换算',
    desc: '字节 / KB / MB / GB / TB（1024 进制）',
    category: Categories.calc,
    icon: Icons.sd_storage_outlined,
    keywords: ['容量', '存储', 'mb', 'gb', '字节'],
    build: () => UnitConverterTool(
      baseNote: '以「字节 Byte」为基准（1024 进制）',
      units: [
        _UnitDef('比特', 'bit', (v) => v / 8, (v) => v * 8),
        _UnitDef('字节', 'B', (v) => v, (v) => v),
        _UnitDef('千字节', 'KB', (v) => v * 1024, (v) => v / 1024),
        _UnitDef('兆字节', 'MB', (v) => v * 1024 * 1024, (v) => v / 1024 / 1024),
        _UnitDef('吉字节', 'GB', (v) => v * 1024 * 1024 * 1024, (v) => v / 1024 / 1024 / 1024),
        _UnitDef('太字节', 'TB', (v) => v * 1024 * 1024 * 1024 * 1024, (v) => v / 1024 / 1024 / 1024 / 1024),
      ],
    ),
  ),
  ToolDef(
    id: 'percent',
    name: '百分比计算',
    desc: '占比 / 百分比值 / 增减幅度',
    category: Categories.calc,
    icon: Icons.percent_rounded,
    keywords: ['百分比', '占比', '涨幅', 'percent'],
    build: () => const _PercentTool(),
  ),
  ToolDef(
    id: 'discount',
    name: '折扣计算',
    desc: '折后价、优惠金额与节省比例',
    category: Categories.calc,
    icon: Icons.local_offer_outlined,
    keywords: ['折扣', '打折', '优惠', 'discount'],
    build: () => const _DiscountTool(),
  ),
  ToolDef(
    id: 'bmi',
    name: 'BMI 计算',
    desc: '身高体重指数与健康区间',
    category: Categories.calc,
    icon: Icons.monitor_heart_outlined,
    keywords: ['bmi', '体重', '健康'],
    build: () => const _BmiTool(),
  ),
  ToolDef(
    id: 'loan',
    name: '贷款计算',
    desc: '等额本息 / 等额本金月供与总利息',
    category: Categories.calc,
    icon: Icons.account_balance_outlined,
    keywords: ['贷款', '房贷', '月供', '利息'],
    build: () => const _LoanTool(),
  ),
  ToolDef(
    id: 'tip',
    name: 'AA 分摊',
    desc: '账单、小费与人数快速分摊',
    category: Categories.calc,
    icon: Icons.restaurant_outlined,
    keywords: ['aa', '分摊', '小费', 'tip'],
    build: () => const _TipTool(),
  ),
  ToolDef(
    id: 'roman',
    name: '罗马数字',
    desc: '阿拉伯数字 ↔ 罗马数字互转',
    category: Categories.calc,
    icon: Icons.history_edu_rounded,
    keywords: ['罗马', 'roman', '数字'],
    build: () => const _RomanTool(),
  ),
  ToolDef(
    id: 'rmb_upper',
    name: '人民币大写',
    desc: '金额转财务大写，适用于票据',
    category: Categories.calc,
    icon: Icons.receipt_long_outlined,
    keywords: ['大写', '人民币', '财务', '金额'],
    build: () => const _RmbTool(),
  ),
  ToolDef(
    id: 'random_number',
    name: '随机数生成',
    desc: '指定区间批量生成随机整数',
    category: Categories.calc,
    icon: Icons.shuffle_rounded,
    keywords: ['随机', 'random', '抽奖'],
    build: () => const _RandomNumberTool(),
  ),
];

// ---------------------------------------------------------------------------
// 通用单位换算
// ---------------------------------------------------------------------------

class _UnitDef {
  final String name;
  final String symbol;
  final double Function(double) toBase;
  final double Function(double) fromBase;

  const _UnitDef(this.name, this.symbol, this.toBase, this.fromBase);
}

class UnitConverterTool extends StatefulWidget {
  final List<_UnitDef> units;
  final String baseNote;
  final int decimals;

  const UnitConverterTool({
    super.key,
    required this.units,
    required this.baseNote,
    this.decimals = 6,
  });

  @override
  State<UnitConverterTool> createState() => _UnitConverterToolState();
}

class _UnitConverterToolState extends State<UnitConverterTool> {
  final _c = TextEditingController(text: '1');
  int _index = 0;

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final raw = parseNum(_c.text);
    double? base;
    if (raw != null) base = widget.units[_index].toBase(raw);

    return ToolBody(children: [
      SectionCard(
        title: '输入数值',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _c,
              keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
              onChanged: (_) => setState(() {}),
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
              decoration: const InputDecoration(
                hintText: '0',
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
              ),
            ),
            const SizedBox(height: 6),
            SegChips<int>(
              values: List<int>.generate(widget.units.length, (i) => i),
              labels: widget.units.map((u) => u.name).toList(),
              value: _index,
              onChanged: (v) => setState(() => _index = v),
            ),
          ],
        ),
      ),
      SectionCard(
        title: '换算结果',
        hint: widget.baseNote,
        child: Column(
          children: [
            for (var i = 0; i < widget.units.length; i++)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Row(
                  children: [
                    SizedBox(
                      width: 76,
                      child: Text(
                        widget.units[i].name,
                        style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(b)),
                      ),
                    ),
                    Expanded(
                      child: SelectableText(
                        base == null ? '—' : trimNumber(widget.units[i].fromBase(base)),
                        style: TextStyle(
                          fontSize: 14.5,
                          fontWeight: i == _index ? FontWeight.w700 : FontWeight.w500,
                          color: i == _index ? AppTheme.brandOf(b) : AppTheme.inkOf(b),
                        ),
                      ),
                    ),
                    Text(
                      widget.units[i].symbol,
                      style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                    ),
                    const SizedBox(width: 6),
                    GestureDetector(
                      onTap: base == null
                          ? null
                          : () => copyText(
                                context,
                                trimNumber(widget.units[i].fromBase(base!)),
                              ),
                      child: Icon(
                        Icons.copy_rounded,
                        size: 13,
                        color: AppTheme.ink3,
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

// ---------------------------------------------------------------------------
// 计算器
// ---------------------------------------------------------------------------

class _CalculatorTool extends StatefulWidget {
  const _CalculatorTool();

  @override
  State<_CalculatorTool> createState() => _CalculatorToolState();
}

class _CalculatorToolState extends State<_CalculatorTool> {
  String _expr = '';
  String _result = '';
  final List<String> _history = [];

  void _press(String v) {
    setState(() {
      if (v == 'C') {
        _expr = '';
        _result = '';
      } else if (v == '⌫') {
        if (_expr.isNotEmpty) _expr = _expr.substring(0, _expr.length - 1);
      } else if (v == '=') {
        if (_expr.trim().isEmpty) return;
        try {
          final r = _eval(_expr);
          final text = trimNumber(r);
          _result = text;
          _history.insert(0, '$_expr = $text');
          if (_history.length > 6) _history.removeLast();
          _expr = text;
        } catch (e) {
          _result = '表达式有误';
        }
      } else {
        _expr += v;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    const keys = [
      ['C', '(', ')', '⌫'],
      ['7', '8', '9', '÷'],
      ['4', '5', '6', '×'],
      ['1', '2', '3', '-'],
      ['0', '.', '^', '+'],
    ];
    return ToolBody(children: [
      SectionCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              reverse: true,
              child: Text(
                _expr.isEmpty ? '0' : _expr,
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.w600, color: AppTheme.inkOf(b)),
              ),
            ),
            const SizedBox(height: 6),
            Text(
              _result.isEmpty ? '输入算式后按 =' : '= $_result',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: _result.isEmpty ? AppTheme.ink3 : AppTheme.brandOf(b),
              ),
            ),
          ],
        ),
      ),
      SectionCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            for (final row in keys)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    for (final k in row)
                      Expanded(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 3),
                          child: _KeyButton(
                            label: k,
                            onTap: () => _press(_mapKey(k)),
                            primary: '+−×÷^'.contains(k) && k != '^',
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            Row(
              children: [
                Expanded(
                  flex: 2,
                  child: _KeyButton(label: '=', onTap: () => _press('='), filled: true),
                ),
                const SizedBox(width: 6),
                Expanded(child: _KeyButton(label: '√', onTap: () => _press('sqrt('))),
                const SizedBox(width: 6),
                Expanded(child: _KeyButton(label: 'π', onTap: () => _press('pi'))),
              ],
            ),
          ],
        ),
      ),
      if (_history.isNotEmpty)
        SectionCard(
          title: '历史记录',
          trailing: TextButton(
            onPressed: () => setState(_history.clear),
            child: const Text('清空'),
          ),
          child: Column(
            children: [
              for (final h in _history)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          h,
                          style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(b)),
                        ),
                      ),
                      GestureDetector(
                        onTap: () => copyText(context, h.split(' = ').last),
                        child: Icon(Icons.copy_rounded, size: 13, color: AppTheme.ink3),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      const InfoNote(
        text: '支持 + - × ÷ % ^ ( )、sqrt/cbrt/sin/cos/tan/log/ln/abs/round 与阶乘 !，三角函数使用弧度。',
      ),
    ]);
  }

  static String _mapKey(String k) {
    switch (k) {
      case '×':
        return '*';
      case '÷':
        return '/';
      default:
        return k;
    }
  }
}

class _KeyButton extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  final bool primary;
  final bool filled;

  const _KeyButton({
    required this.label,
    required this.onTap,
    this.primary = false,
    this.filled = false,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final brand = AppTheme.brandOf(b);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppTheme.radSmall),
      child: Container(
        height: 46,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: filled
              ? brand
              : (primary ? AppTheme.selectedFillOf(b) : AppTheme.fieldOf(b)),
          borderRadius: BorderRadius.circular(AppTheme.radSmall),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: filled ? Colors.white : (primary ? brand : AppTheme.inkOf(b)),
          ),
        ),
      ),
    );
  }
}

/// 表达式求值（递归下降）。
double _eval(String src) {
  final p = _Parser(src);
  final v = p.parseExpression();
  p.skipSpaces();
  if (!p.atEnd) throw FormatException('无法识别：${src.substring(p.pos)}');
  if (v.isNaN || v.isInfinite) throw FormatException('结果无效');
  return v;
}

class _Parser {
  final String s;
  int pos = 0;

  _Parser(this.s);

  bool get atEnd => pos >= s.length;

  void skipSpaces() {
    while (pos < s.length && (s[pos] == ' ' || s[pos] == '\t')) {
      pos++;
    }
  }

  bool eat(String c) {
    skipSpaces();
    if (pos < s.length && s[pos] == c) {
      pos++;
      return true;
    }
    return false;
  }

  double parseExpression() {
    var v = _term();
    while (true) {
      skipSpaces();
      if (eat('+')) {
        v += _term();
      } else if (eat('-')) {
        v -= _term();
      } else {
        break;
      }
    }
    return v;
  }

  double _term() {
    var v = _factor();
    while (true) {
      skipSpaces();
      if (pos < s.length && s[pos] == '*') {
        pos++;
        v *= _factor();
      } else if (pos < s.length && s[pos] == '/') {
        pos++;
        final d = _factor();
        if (d == 0) throw const FormatException('除数不能为 0');
        v /= d;
      } else if (pos < s.length && s[pos] == '%') {
        pos++;
        final d = _factor();
        if (d == 0) throw const FormatException('除数不能为 0');
        v %= d;
      } else {
        break;
      }
    }
    return v;
  }

  double _factor() {
    var v = _unary();
    skipSpaces();
    if (eat('^')) {
      final e = _factor();
      v = math.pow(v, e).toDouble();
    }
    if (eat('!')) {
      v = _factorial(v);
    }
    return v;
  }

  double _unary() {
    skipSpaces();
    if (eat('-')) return -_unary();
    if (eat('+')) return _unary();
    return _primary();
  }

  double _primary() {
    skipSpaces();
    if (eat('(')) {
      final v = parseExpression();
      if (!eat(')')) throw const FormatException('括号不匹配');
      return v;
    }
    if (pos < s.length && (RegExp(r'[0-9.]').hasMatch(s[pos]))) {
      final start = pos;
      while (pos < s.length && RegExp(r'[0-9.]').hasMatch(s[pos])) {
        pos++;
      }
      final numText = s.substring(start, pos);
      final v = double.tryParse(numText);
      if (v == null) throw FormatException('数字无效：$numText');
      return v;
    }
    if (pos < s.length && RegExp(r'[a-zA-Z]').hasMatch(s[pos])) {
      final start = pos;
      while (pos < s.length && RegExp(r'[a-zA-Z]').hasMatch(s[pos])) {
        pos++;
      }
      final name = s.substring(start, pos).toLowerCase();
      if (name == 'pi') return math.pi;
      if (name == 'e') return math.e;
      if (!eat('(')) throw FormatException('函数 $name 需要括号');
      final arg = parseExpression();
      if (!eat(')')) throw const FormatException('括号不匹配');
      return _apply(name, arg);
    }
    throw const FormatException('表达式不完整');
  }

  static double _apply(String name, double x) {
    switch (name) {
      case 'sqrt':
        return math.sqrt(x);
      case 'cbrt':
        return math.pow(x, 1 / 3).toDouble();
      case 'sin':
        return math.sin(x);
      case 'cos':
        return math.cos(x);
      case 'tan':
        return math.tan(x);
      case 'asin':
        return math.asin(x);
      case 'acos':
        return math.acos(x);
      case 'atan':
        return math.atan(x);
      case 'log':
        return math.log(x) / math.ln10;
      case 'ln':
        return math.log(x);
      case 'abs':
        return x.abs();
      case 'round':
        return x.roundToDouble();
      case 'floor':
        return x.floorToDouble();
      case 'ceil':
        return x.ceilToDouble();
      case 'exp':
        return math.exp(x);
      default:
        throw FormatException('未知函数：$name');
    }
  }

  static double _factorial(double x) {
    if (x < 0 || x != x.roundToDouble() || x > 170) {
      throw const FormatException('阶乘仅支持 0-170 的整数');
    }
    var r = 1.0;
    for (var i = 2; i <= x.round(); i++) {
      r *= i;
    }
    return r;
  }
}

// ---------------------------------------------------------------------------
// 进制转换
// ---------------------------------------------------------------------------

class _BaseConvertTool extends StatefulWidget {
  const _BaseConvertTool();

  @override
  State<_BaseConvertTool> createState() => _BaseConvertToolState();
}

class _BaseConvertToolState extends State<_BaseConvertTool> {
  final _c = TextEditingController(text: '255');
  int _from = 2; // 基数

  static const _bases = [2, 8, 10, 16, 36];

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  BigInt? get _value {
    final s = _c.text.trim().replaceAll(' ', '').replaceAll('_', '');
    if (s.isEmpty) return null;
    try {
      return BigInt.parse(s, radix: _from);
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final v = _value;
    return ToolBody(children: [
      SectionCard(
        title: '输入数值',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _c,
              onChanged: (_) => setState(() {}),
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600, fontFamily: 'monospace'),
              decoration: const InputDecoration(
                hintText: '输入数字',
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
              ),
            ),
            const SizedBox(height: 8),
            SegChips<int>(
              values: _bases,
              labels: _bases.map((b) => '$b 进制').toList(),
              value: _from,
              onChanged: (v) => setState(() => _from = v),
            ),
          ],
        ),
      ),
      SectionCard(
        title: '各进制结果',
        hint: '支持超大整数（BigInt），不会丢精度。',
        child: Column(
          children: [
            for (final base in _bases)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 7),
                child: Row(
                  children: [
                    SizedBox(
                      width: 62,
                      child: Text(
                        '${base}进制',
                        style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
                      ),
                    ),
                    Expanded(
                      child: SelectableText(
                        v == null ? '—' : v.toRadixString(base).toUpperCase(),
                        style: TextStyle(
                          fontSize: 13.5,
                          fontFamily: 'monospace',
                          color: base == _from ? AppTheme.brandOf(b) : AppTheme.inkOf(b),
                          fontWeight: base == _from ? FontWeight.w700 : FontWeight.w400,
                        ),
                      ),
                    ),
                    GestureDetector(
                      onTap: v == null
                          ? null
                          : () => copyText(context, v.toRadixString(base).toUpperCase()),
                      child: Icon(Icons.copy_rounded, size: 13, color: AppTheme.ink3),
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

// ---------------------------------------------------------------------------
// 百分比
// ---------------------------------------------------------------------------

class _PercentTool extends StatefulWidget {
  const _PercentTool();

  @override
  State<_PercentTool> createState() => _PercentToolState();
}

class _PercentToolState extends State<_PercentTool> {
  final _a = TextEditingController();
  final _b = TextEditingController();
  int _mode = 0;

  @override
  void dispose() {
    _a.dispose();
    _b.dispose();
    super.dispose();
  }

  String get _labelsA => ['部分值', '数值', '变化前'][_mode];
  String get _labelsB => ['总量', '百分比', '变化后'][_mode];

  String get _result {
    final a = parseNum(_a.text);
    final b = parseNum(_b.text);
    if (a == null || b == null) return '—';
    switch (_mode) {
      case 0:
        final r = safeDiv(a, b);
        return r == null ? '—' : '${trimNumber(r * 100)}%';
      case 1:
        return trimNumber(a * b);
      default:
        final diff = b - a;
        final ratio = safeDiv(diff, a);
        if (ratio == null) return '—';
        final sign = ratio >= 0 ? '+' : '';
        return '$sign${trimNumber(ratio * 100)}%（${diff >= 0 ? '增加' : '减少'} ${trimNumber(diff.abs())}）';
    }
  }

  @override
  Widget build(BuildContext context) {
    return ToolBody(children: [
      SectionCard(
        title: '计算方式',
        child: SegChips<int>(
          values: const [0, 1, 2],
          labels: const ['占比', '百分比值', '增减幅度'],
          value: _mode,
          onChanged: (v) => setState(() => _mode = v),
        ),
      ),
      SectionCard(
        title: '输入',
        child: Column(
          children: [
            NumberField(label: _labelsA, unit: '', controller: _a, onChanged: (_) => setState(() {})),
            NumberField(label: _labelsB, unit: '%', controller: _b, onChanged: (_) => setState(() {})),
          ],
        ),
      ),
      SectionCard(
        child: BigResult(
          value: _result,
          label: _mode == 0
              ? '占比结果'
              : _mode == 1
                  ? '计算结果'
                  : '变化幅度',
          sub: _mode == 1 ? '${_a.text} 的 ${_b.text}%' : null,
        ),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 折扣
// ---------------------------------------------------------------------------

class _DiscountTool extends StatefulWidget {
  const _DiscountTool();

  @override
  State<_DiscountTool> createState() => _DiscountToolState();
}

class _DiscountToolState extends State<_DiscountTool> {
  final _price = TextEditingController();
  final _discount = TextEditingController(text: '8.5');
  final _coupon = TextEditingController();

  @override
  void dispose() {
    _price.dispose();
    _discount.dispose();
    _coupon.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final price = parseNum(_price.text);
    final discount = parseNum(_discount.text);
    final coupon = parseNum(_coupon.text) ?? 0;

    double? afterDiscount;
    if (price != null && discount != null) {
      afterDiscount = price * discount / 10;
    }
    final pay = afterDiscount == null ? null : math.max(0.0, afterDiscount - coupon);
    final saved = (price != null && pay != null) ? price - pay : null;

    return ToolBody(children: [
      SectionCard(
        title: '输入',
        child: Column(
          children: [
            NumberField(label: '原价', unit: '元', controller: _price, onChanged: (_) => setState(() {})),
            NumberField(label: '折扣', unit: '折', controller: _discount, onChanged: (_) => setState(() {})),
            NumberField(label: '优惠券', unit: '元', controller: _coupon, onChanged: (_) => setState(() {})),
          ],
        ),
        hint: '折扣按「几折」填写，例如 8.5 表示原价的 85%。',
      ),
      if (pay != null)
        SectionCard(
          child: Column(
            children: [
              BigResult(
                value: '¥ ${trimNumber(pay)}',
                label: '实付金额',
                sub: saved == null ? null : '已省 ¥ ${trimNumber(saved)}',
                onCopy: () => copyText(context, trimNumber(pay)),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: StatTile(
                      value: afterDiscount == null ? '—' : '¥${trimNumber(afterDiscount)}',
                      label: '折后价',
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: StatTile(
                      value: saved == null ? '—' : '¥${trimNumber(saved)}',
                      label: '共节省',
                    ),
                  ),
                ],
              ),
            ],
          ),
        )
      else
        Text(
          '填写原价与折扣后自动计算',
          style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
        ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// BMI
// ---------------------------------------------------------------------------

class _BmiTool extends StatefulWidget {
  const _BmiTool();

  @override
  State<_BmiTool> createState() => _BmiToolState();
}

class _BmiToolState extends State<_BmiTool> {
  final _h = TextEditingController(text: '170');
  final _w = TextEditingController(text: '65');

  @override
  void dispose() {
    _h.dispose();
    _w.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final h = parseNum(_h.text);
    final w = parseNum(_w.text);
    double? bmi;
    if (h != null && w != null && h > 0) {
      final m = h / 100;
      bmi = w / (m * m);
    }
    String label = '—';
    Color color = AppTheme.ink2Of(b);
    if (bmi != null) {
      if (bmi < 18.5) {
        label = '偏瘦';
        color = const Color(0xFF3B82F6);
      } else if (bmi < 24) {
        label = '正常';
        color = const Color(0xFF16A34A);
      } else if (bmi < 28) {
        label = '超重';
        color = const Color(0xFFD97706);
      } else {
        label = '肥胖';
        color = const Color(0xFFDC2626);
      }
    }
    final minW = h == null ? null : 18.5 * math.pow(h / 100, 2);
    final maxW = h == null ? null : 23.9 * math.pow(h / 100, 2);

    return ToolBody(children: [
      SectionCard(
        title: '身体数据',
        child: Column(
          children: [
            NumberField(label: '身高', unit: 'cm', controller: _h, onChanged: (_) => setState(() {})),
            NumberField(label: '体重', unit: 'kg', controller: _w, onChanged: (_) => setState(() {})),
          ],
        ),
      ),
      SectionCard(
        title: '计算结果',
        child: Column(
          children: [
            BigResult(
              value: bmi == null ? '—' : bmi.toStringAsFixed(1),
              label: 'BMI 指数',
              sub: '体型：$label',
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: StatTile(
                    value: minW == null ? '—' : '${minW.toStringAsFixed(1)} kg',
                    label: '正常下限',
                    color: color,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: StatTile(
                    value: maxW == null ? '—' : '${maxW.toStringAsFixed(1)} kg',
                    label: '正常上限',
                    color: color,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      const InfoNote(text: 'BMI 仅作粗略参考，运动员、孕妇与未成年人不宜直接套用。'),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 贷款
// ---------------------------------------------------------------------------

class _LoanTool extends StatefulWidget {
  const _LoanTool();

  @override
  State<_LoanTool> createState() => _LoanToolState();
}

class _LoanToolState extends State<_LoanTool> {
  final _amount = TextEditingController(text: '100');
  final _rate = TextEditingController(text: '3.1');
  final _months = TextEditingController(text: '360');
  int _type = 0;

  @override
  void dispose() {
    _amount.dispose();
    _rate.dispose();
    _months.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final amountWan = parseNum(_amount.text);
    final rate = parseNum(_rate.text);
    final months = parseNum(_months.text);

    double? monthly, totalInterest, totalPay, firstMonth, lastMonth;
    if (amountWan != null && rate != null && months != null && months > 0 && amountWan > 0) {
      final p = amountWan * 10000;
      final r = rate / 100 / 12;
      final n = months.round();
      if (_type == 0) {
        if (r == 0) {
          monthly = p / n;
        } else {
          final f = math.pow(1 + r, n).toDouble();
          monthly = p * r * f / (f - 1);
        }
        totalPay = monthly * n;
        totalInterest = totalPay - p;
        firstMonth = monthly;
        lastMonth = monthly;
      } else {
        final principal = p / n;
        firstMonth = principal + p * r;
        lastMonth = principal + principal * r;
        totalInterest = p * r * (n + 1) / 2;
        totalPay = p + totalInterest;
        monthly = firstMonth;
      }
    }

    return ToolBody(children: [
      SectionCard(
        title: '还款方式',
        child: SegChips<int>(
          values: const [0, 1],
          labels: const ['等额本息', '等额本金'],
          value: _type,
          onChanged: (v) => setState(() => _type = v),
        ),
      ),
      SectionCard(
        title: '贷款参数',
        child: Column(
          children: [
            NumberField(label: '贷款金额', unit: '万元', controller: _amount, onChanged: (_) => setState(() {})),
            NumberField(label: '年利率', unit: '%', controller: _rate, onChanged: (_) => setState(() {})),
            NumberField(label: '期限', unit: '月', controller: _months, onChanged: (_) => setState(() {})),
          ],
        ),
      ),
      if (monthly != null)
        SectionCard(
          title: '还款测算',
          child: Column(
            children: [
              BigResult(
                value: '¥ ${thousands(monthly)}',
                label: _type == 0 ? '每月月供' : '首月月供',
                sub: _type == 1 && lastMonth != null ? '末月 ¥ ${thousands(lastMonth)}' : null,
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: StatTile(
                      value: '¥ ${thousands(totalInterest ?? 0)}',
                      label: '利息总额',
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: StatTile(
                      value: '¥ ${thousands(totalPay ?? 0)}',
                      label: '还款总额',
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      Text(
        '按固定利率、按月计息估算，实际以银行合同为准。',
        style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// AA 分摊
// ---------------------------------------------------------------------------

class _TipTool extends StatefulWidget {
  const _TipTool();

  @override
  State<_TipTool> createState() => _TipToolState();
}

class _TipToolState extends State<_TipTool> {
  final _bill = TextEditingController();
  final _people = TextEditingController(text: '2');
  final _tip = TextEditingController(text: '10');

  @override
  void dispose() {
    _bill.dispose();
    _people.dispose();
    _tip.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final bill = parseNum(_bill.text);
    final people = parseNum(_people.text);
    final tip = parseNum(_tip.text) ?? 0;

    double? perPerson, tipAmount, total;
    if (bill != null && people != null && people > 0) {
      tipAmount = bill * tip;
      total = bill + tipAmount;
      perPerson = total / people;
    }

    return ToolBody(children: [
      SectionCard(
        title: '账单信息',
        child: Column(
          children: [
            NumberField(label: '账单金额', unit: '元', controller: _bill, onChanged: (_) => setState(() {})),
            NumberField(label: '小费比例', unit: '%', controller: _tip, onChanged: (_) => setState(() {})),
            NumberField(label: '人数', unit: '人', controller: _people, onChanged: (_) => setState(() {})),
          ],
        ),
      ),
      if (perPerson != null)
        SectionCard(
          child: Column(
            children: [
              BigResult(
                value: '¥ ${trimNumber(roundTo(perPerson, 2))}',
                label: '每人应付',
                onCopy: () => copyText(context, perPerson!.toStringAsFixed(2)),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: StatTile(value: '¥ ${trimNumber(roundTo(tipAmount ?? 0, 2))}', label: '小费'),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: StatTile(value: '¥ ${trimNumber(roundTo(total ?? 0, 2))}', label: '合计'),
                  ),
                ],
              ),
            ],
          ),
        )
      else
        Text('填写账单金额与人数即可分摊', style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b))),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 罗马数字
// ---------------------------------------------------------------------------

class _RomanTool extends StatefulWidget {
  const _RomanTool();

  @override
  State<_RomanTool> createState() => _RomanToolState();
}

class _RomanToolState extends State<_RomanTool> {
  final _c = TextEditingController();
  int _mode = 0;

  static const _pairs = [
    [1000, 'M'], [900, 'CM'], [500, 'D'], [400, 'CD'],
    [100, 'C'], [90, 'XC'], [50, 'L'], [40, 'XL'],
    [10, 'X'], [9, 'IX'], [5, 'V'], [4, 'IV'], [1, 'I'],
  ];

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  String get _result {
    final s = _c.text.trim();
    if (s.isEmpty) return '';
    if (_mode == 0) {
      final n = int.tryParse(s);
      if (n == null || n <= 0 || n > 3999) return '请输入 1-3999 的整数';
      return _toRoman(n);
    }
    final v = _fromRoman(s.toUpperCase());
    return v == null ? '不是有效的罗马数字' : '$v';
  }

  static String _toRoman(int n) {
    final buf = StringBuffer();
    var rest = n;
    for (final p in _pairs) {
      final value = p[0] as int;
      final sym = p[1] as String;
      while (rest >= value) {
        buf.write(sym);
        rest -= value;
      }
    }
    return buf.toString();
  }

  static int? _fromRoman(String s) {
    const map = {
      'I': 1, 'V': 5, 'X': 10, 'L': 50, 'C': 100, 'D': 500, 'M': 1000,
    };
    var total = 0;
    var prev = 0;
    for (var i = s.length - 1; i >= 0; i--) {
      final v = map[s[i]];
      if (v == null) return null;
      if (v < prev) {
        total -= v;
      } else {
        total += v;
        prev = v;
      }
    }
    // 反向校验：转回罗马数字应完全一致
    if (total <= 0 || total > 3999) return null;
    if (_toRoman(total) != s) return null;
    return total;
  }

  @override
  Widget build(BuildContext context) {
    return ToolBody(children: [
      SectionCard(
        title: '转换方向',
        child: SegChips<int>(
          values: const [0, 1],
          labels: const ['数字 → 罗马', '罗马 → 数字'],
          value: _mode,
          onChanged: (v) => setState(() {
            _mode = v;
            _c.clear();
          }),
        ),
      ),
      SectionCard(
        child: Column(
          children: [
            LabeledField(
              label: _mode == 0 ? '阿拉伯数字（1-3999）' : '罗马数字',
              controller: _c,
              hint: _mode == 0 ? '2026' : 'MMXXVI',
              onChanged: (_) => setState(() {}),
              keyboardType: _mode == 0 ? TextInputType.number : TextInputType.text,
            ),
            const SizedBox(height: 14),
            ResultBox(text: _result, label: '结果'),
          ],
        ),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 人民币大写
// ---------------------------------------------------------------------------

class _RmbTool extends StatefulWidget {
  const _RmbTool();

  @override
  State<_RmbTool> createState() => _RmbToolState();
}

class _RmbToolState extends State<_RmbTool> {
  final _c = TextEditingController();

  static const _digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'];
  static const _units = ['', '拾', '佰', '仟'];
  static const _bigUnits = ['', '万', '亿', '万亿'];

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  String get _result {
    final v = parseNum(_c.text);
    if (v == null) return '';
    if (v < 0) return '金额不能为负';
    if (v >= 1e16) return '金额过大';
    return _convert(v);
  }

  static String _convert(double value) {
    final cents = (value * 100).round();
    final yuan = cents ~/ 100;
    final jiao = (cents % 100) ~/ 10;
    final fen = cents % 10;

    var intPart = '零元';
    if (yuan > 0) {
      final groups = <String>[];
      var rest = yuan;
      var groupIndex = 0;
      while (rest > 0) {
        final g = rest % 10000;
        if (g > 0) {
          groups.insert(0, '${_four(g)}${_bigUnits[groupIndex]}');
        } else if (groups.isNotEmpty && groups.first.isNotEmpty) {
          // 组间补零（如 100000005）
          if (!groups.first.startsWith('零')) groups.insert(0, '零');
        }
        rest ~/= 10000;
        groupIndex++;
      }
      intPart = '${groups.join().replaceAll(RegExp('零+'), '零').replaceAll(RegExp(r'零$'), '')}元';
    }

    final buf = StringBuffer(intPart);
    if (jiao == 0 && fen == 0) {
      buf.write('整');
    } else {
      if (jiao > 0) {
        buf.write('${_digits[jiao]}角');
      } else if (yuan > 0 && fen > 0) {
        buf.write('零');
      }
      if (fen > 0) buf.write('${_digits[fen]}分');
    }
    return buf.toString();
  }

  static String _four(int n) {
    final buf = StringBuffer();
    var zero = false;
    for (var pos = 3; pos >= 0; pos--) {
      final d = (n ~/ math.pow(10, pos).toInt()) % 10;
      if (d == 0) {
        zero = buf.isNotEmpty;
      } else {
        if (zero) buf.write('零');
        zero = false;
        buf.write('${_digits[d]}${_units[pos]}');
      }
    }
    return buf.toString();
  }

  @override
  Widget build(BuildContext context) {
    return ToolBody(children: [
      SectionCard(
        title: '金额',
        child: Column(
          children: [
            LabeledField(
              label: '人民币金额（元）',
              controller: _c,
              hint: '例如 12345.67',
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 14),
            ResultBox(text: _result, label: '大写金额'),
          ],
        ),
      ),
      const InfoNote(text: '按财务票据规范转换，支持到「万亿」位；角分为 0 时补「整」。'),
    ]);
  }
}

// ---------------------------------------------------------------------------
// 随机数
// ---------------------------------------------------------------------------

class _RandomNumberTool extends StatefulWidget {
  const _RandomNumberTool();

  @override
  State<_RandomNumberTool> createState() => _RandomNumberToolState();
}

class _RandomNumberToolState extends State<_RandomNumberTool> {
  final _min = TextEditingController(text: '1');
  final _max = TextEditingController(text: '100');
  final _count = TextEditingController(text: '5');
  bool _unique = false;
  List<int> _out = [];

  @override
  void dispose() {
    _min.dispose();
    _max.dispose();
    _count.dispose();
    super.dispose();
  }

  void _run() {
    final lo = int.tryParse(_min.text.trim());
    final hi = int.tryParse(_max.text.trim());
    final n = int.tryParse(_count.text.trim());
    if (lo == null || hi == null || n == null || n <= 0) {
      toast(context, '请填写有效的整数', error: true);
      return;
    }
    final a = math.min(lo, hi);
    final b = math.max(lo, hi);
    final range = b - a + 1;
    if (_unique && n > range) {
      toast(context, '不重复模式最多 $range 个', error: true);
      return;
    }
    final rnd = math.Random.secure();
    final list = <int>[];
    if (_unique) {
      final pool = List<int>.generate(range, (i) => a + i)..shuffle(rnd);
      list.addAll(pool.take(n));
    } else {
      for (var i = 0; i < n; i++) {
        list.add(a + rnd.nextInt(range));
      }
    }
    setState(() => _out = list);
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return ToolBody(children: [
      SectionCard(
        title: '参数',
        child: Column(
          children: [
            NumberField(label: '最小值', unit: '', controller: _min, onChanged: (_) => setState(() {})),
            NumberField(label: '最大值', unit: '', controller: _max, onChanged: (_) => setState(() {})),
            NumberField(label: '数量', unit: '个', controller: _count, onChanged: (_) => setState(() {})),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('结果不重复', style: TextStyle(fontSize: 13.5, color: AppTheme.inkOf(b))),
              value: _unique,
              onChanged: (v) => setState(() => _unique = v),
            ),
            const SizedBox(height: 6),
            PrimaryButton(label: '生成', icon: Icons.shuffle_rounded, onPressed: _run),
          ],
        ),
      ),
      if (_out.isNotEmpty)
        SectionCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    '生成结果',
                    style: TextStyle(fontSize: 12, color: AppTheme.ink2Of(b)),
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: () => copyText(context, _out.join(', ')),
                    child: const Text('复制全部'),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final n in _out)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppTheme.selectedFillOf(b),
                        borderRadius: BorderRadius.circular(AppTheme.radSmall),
                      ),
                      child: Text(
                        '$n',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                          color: AppTheme.brandOf(b),
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
    ]);
  }
}
