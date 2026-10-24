import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// 复制到剪贴板并给出轻提示。
Future<void> copyText(BuildContext context, String text, {String? tip}) async {
  if (text.isEmpty) {
    toast(context, '没有可复制的内容');
    return;
  }
  await Clipboard.setData(ClipboardData(text: text));
  if (context.mounted) toast(context, tip ?? '已复制到剪贴板');
}

/// 轻量提示（浮动 SnackBar，2 秒后自动消失，不占用页面层级）。
void toast(BuildContext context, String msg, {bool error = false}) {
  final messenger = ScaffoldMessenger.maybeOf(context);
  if (messenger == null) return;
  messenger.hideCurrentSnackBar();
  messenger.showSnackBar(
    SnackBar(
      content: Text(msg),
      duration: const Duration(milliseconds: 1800),
      backgroundColor: error ? const Color(0xFFB42318) : null,
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
    ),
  );
}

/// 格式化字节数（1024 进制）。
String formatBytes(num bytes, {int digits = 2}) {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  var v = bytes.toDouble();
  var i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i++;
  }
  final s = i == 0 ? v.toStringAsFixed(0) : v.toStringAsFixed(v >= 100 ? 0 : digits);
  return '$s ${units[i]}';
}

/// 数字精简显示：最多 8 位有效小数，去掉尾随 0。
String trimNumber(double v) {
  if (v.isNaN) return 'NaN';
  if (v.isInfinite) return v.isNegative ? '-∞' : '∞';
  if (v == v.roundToDouble() && v.abs() < 1e15) {
    return v.toInt().toString();
  }
  var s = v.toStringAsPrecision(10);
  if (s.contains('e')) return v.toString();
  if (s.contains('.')) {
    s = s.replaceFirst(RegExp(r'0+$'), '');
    s = s.replaceFirst(RegExp(r'\.$'), '');
  }
  return s;
}

/// 千分位。
String thousands(num v) {
  final neg = v < 0;
  final s = v.abs().toStringAsFixed(v == v.roundToDouble() ? 0 : 2);
  final parts = s.split('.');
  final buf = StringBuffer();
  final intPart = parts[0];
  for (var i = 0; i < intPart.length; i++) {
    if (i > 0 && (intPart.length - i) % 3 == 0) buf.write(',');
    buf.write(intPart[i]);
  }
  final out = parts.length > 1 ? '$buf.${parts[1]}' : buf.toString();
  return neg ? '-$out' : out;
}

/// 根据输入框内容推断数值（支持百分号、逗号、全角字符）。
double? parseNum(String s) {
  var t = s.trim();
  if (t.isEmpty) return null;
  t = t.replaceAll(',', '').replaceAll('，', '').replaceAll('％', '%');
  var percent = false;
  if (t.endsWith('%')) {
    percent = true;
    t = t.substring(0, t.length - 1);
  }
  final v = double.tryParse(t);
  if (v == null) return null;
  return percent ? v / 100 : v;
}

/// 安全除法（除数为 0 返回 null）。
double? safeDiv(double a, double b) => b == 0 ? null : a / b;

/// 简易四舍五入到指定小数位。
double roundTo(double v, int digits) {
  final p = math.pow(10, digits).toDouble();
  return (v * p).roundToDouble() / p;
}

/// 日期格式化（yyyy-MM-dd）。
String fmtDate(DateTime d) =>
    '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

/// 日期时间格式化（yyyy-MM-dd HH:mm:ss）。
String fmtDateTime(DateTime d) =>
    '${fmtDate(d)} ${d.hour.toString().padLeft(2, '0')}:'
    '${d.minute.toString().padLeft(2, '0')}:${d.second.toString().padLeft(2, '0')}';

/// 中文星期（一 ~ 日）。
String weekdayZh(DateTime d) => const ['一', '二', '三', '四', '五', '六', '日'][d.weekday - 1];

/// 截断长文本用于列表展示。
String ellipsis(String s, int max) {
  final flat = s.replaceAll(RegExp(r'\s+'), ' ').trim();
  if (flat.length <= max) return flat;
  return '${flat.substring(0, max)}…';
}
