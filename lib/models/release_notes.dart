/// 内置更新日志（离线兜底）。
///
/// CI 会校验：当前 `pubspec.yaml` 版本在此必须有条目，且至少一条说明。
/// 运行时「更新记录」页优先展示 GitHub Release 的真实内容，网络不可用时回退到本文件。
class ReleaseNote {
  final String version;
  final String date;
  final List<String> items;
  final List<String> fixes;

  const ReleaseNote({
    required this.version,
    required this.date,
    this.items = const [],
    this.fixes = const [],
  });
}

const List<ReleaseNote> kReleaseNotes = [
  ReleaseNote(
    version: '1.0.0',
    date: '2026-10-23',
    items: [
      '首个正式版本：内置 57 个离线工具，覆盖文本、编码、加密、计算、日期、开发、图片、生活八大类。',
      '首页展示收藏与最近使用，底部「首页 / 全部 / 我的」三页切换。',
      '「全部」页支持关键词搜索与按类型分组浏览，常用工具一键收藏。',
      '应用内检查更新：可关闭的更新弹窗、下载进度实时显示，下载完成后自动拉起系统安装器。',
      '「更新记录」独立页面，在线读取 GitHub Release 说明，离线自动回退到内置日志。',
      '零第三方运行时依赖，全部能力由标准库与自写原生通道实现，安装包与内存占用保持在极低水平。',
      '支持智能加速源，国内网络下检查更新与下载 APK 更稳定。',
    ],
    fixes: [],
  ),
];
