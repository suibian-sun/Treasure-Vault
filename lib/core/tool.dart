import 'package:flutter/material.dart';

/// 工具分类。顺序即「全部」页面的分组顺序。
class ToolCategory {
  final String id;
  final String name;
  final String desc;
  final IconData icon;

  const ToolCategory(this.id, this.name, this.desc, this.icon);
}

/// 单个工具的元数据 + 懒构建入口。
///
/// `build` 是一个闭包，只有用户真正点开该工具时才会实例化页面，
/// 因此注册 50+ 个工具不会带来任何启动期内存开销。
class ToolDef {
  final String id;
  final String name;
  final String desc;
  final ToolCategory category;
  final IconData icon;
  final List<String> keywords;
  final Widget Function() build;

  const ToolDef({
    required this.id,
    required this.name,
    required this.desc,
    required this.category,
    required this.icon,
    this.keywords = const [],
    required this.build,
  });

  /// 搜索匹配：名称 / 描述 / 关键词 / 分类名（全部小写化）。
  bool matches(String q) {
    if (q.isEmpty) return true;
    final s = q.toLowerCase();
    if (name.toLowerCase().contains(s) || desc.toLowerCase().contains(s)) return true;
    if (category.name.toLowerCase().contains(s)) return true;
    for (final k in keywords) {
      if (k.toLowerCase().contains(s)) return true;
    }
    return false;
  }
}

/// 全部分类（保持稳定顺序）。
class Categories {
  static const text = ToolCategory('text', '文本处理', '清洗、统计、改写文本', Icons.subject_outlined);
  static const codec = ToolCategory('codec', '编码转换', 'Base64 / URL / Unicode / JSON', Icons.swap_horiz_rounded);
  static const crypto = ToolCategory('crypto', '加密安全', '哈希、密码、UUID', Icons.shield_outlined);
  static const calc = ToolCategory('calc', '计算换算', '算式、单位、金融、健康', Icons.calculate_outlined);
  static const date = ToolCategory('date', '日期时间', '时间戳、日期差、倒数日', Icons.event_outlined);
  static const dev = ToolCategory('dev', '开发工具', '正则、颜色、JWT、HTTP', Icons.terminal_rounded);
  static const image = ToolCategory('image', '图片工具', '信息、压缩、Base64', Icons.image_outlined);
  static const life = ToolCategory('life', '生活娱乐', '骰子、抽签、秒表', Icons.emoji_emotions_outlined);

  static const List<ToolCategory> all = [text, codec, crypto, calc, date, dev, image, life];
}
