import '../core/tool.dart';
import 'calc_tools.dart';
import 'codec_tools.dart';
import 'crypto_tools.dart';
import 'date_tools.dart';
import 'dev_tools.dart';
import 'image_tools.dart';
import 'life_tools.dart';
import 'text_tools.dart';

/// 全部工具注册表。
///
/// 每个条目只保存元数据与一个「构建闭包」——页面在用户点开时才实例化，
/// 因此注册 57 个工具不会增加任何启动期内存。
final List<ToolDef> kTools = [
  ...textTools,
  ...codecTools,
  ...cryptoTools,
  ...calcTools,
  ...dateTools,
  ...devTools,
  ...imageTools,
  ...lifeTools,
];

final Map<String, ToolDef> kToolById = {
  for (final t in kTools) t.id: t,
};

/// 按分类取工具（保持注册顺序）。
List<ToolDef> toolsOf(ToolCategory category) =>
    kTools.where((t) => t.category.id == category.id).toList();

/// 关键词搜索。
List<ToolDef> searchTools(String query) {
  final q = query.trim();
  if (q.isEmpty) return kTools;
  return kTools.where((t) => t.matches(q)).toList();
}

bool toolExists(String id) => kToolById.containsKey(id);
