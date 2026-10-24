import 'package:flutter/material.dart';

import '../core/app_state.dart';
import '../core/tool.dart';
import '../theme/app_theme.dart';

/// 打开工具详情页。
///
/// 「低内存模式」下使用零动画路由：省掉一次 Hero/过渡层的额外图层，
/// 在低端机上体感更跟手，也少一份过渡期间的离屏缓冲。
Future<void> openTool(BuildContext context, ToolDef tool) async {
  final lowMemory = AppState.instance.settings.lowMemoryMode;
  await Navigator.of(context).push(
    lowMemory
        ? PageRouteBuilder<void>(
            transitionDuration: Duration.zero,
            reverseTransitionDuration: Duration.zero,
            pageBuilder: (_, __, ___) => ToolHostPage(tool: tool),
          )
        : MaterialPageRoute<void>(builder: (_) => ToolHostPage(tool: tool)),
  );
}

/// 工具承载页：统一顶栏（名称 + 说明 + 收藏）+ 工具正文。
class ToolHostPage extends StatefulWidget {
  final ToolDef tool;

  const ToolHostPage({super.key, required this.tool});

  @override
  State<ToolHostPage> createState() => _ToolHostPageState();
}

class _ToolHostPageState extends State<ToolHostPage> {
  @override
  void initState() {
    super.initState();
    // 记录「最近使用」放到首帧之后，避免在 build 期间触发祖先重建
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) AppState.instance.markUsed(widget.tool.id);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final b = Theme.of(context).brightness;
    final fav = state.isFavorite(widget.tool.id);

    return Scaffold(
      appBar: AppBar(
        shape: Border(
          bottom: BorderSide(color: AppTheme.lineOf(b), width: AppTheme.stroke),
        ),
        titleSpacing: 0,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              widget.tool.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: AppTheme.inkOf(b),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              widget.tool.desc,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w400,
                color: AppTheme.ink2Of(b),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: fav ? '取消收藏' : '收藏',
            onPressed: () {
              state.toggleFavorite(widget.tool.id);
              ScaffoldMessenger.of(context)
                ..hideCurrentSnackBar()
                ..showSnackBar(
                  SnackBar(
                    content: Text(fav ? '已取消收藏' : '已加入收藏'),
                    duration: const Duration(milliseconds: 1200),
                    behavior: SnackBarBehavior.floating,
                    margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                  ),
                );
            },
            icon: Icon(
              fav ? Icons.star_rounded : Icons.star_border_rounded,
              color: fav ? const Color(0xFFF59E0B) : AppTheme.ink2Of(b),
            ),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: widget.tool.build(),
    );
  }
}
