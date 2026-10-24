import 'package:flutter/material.dart';

import '../core/app_state.dart';
import '../core/tool.dart';
import '../theme/app_theme.dart';
import '../tools/registry.dart';
import '../widgets/kit.dart';
import '../widgets/page_header.dart';
import 'tool_host.dart';

/// 首页：收藏 + 最近使用。
class HomePage extends StatelessWidget {
  final VoidCallback onGoAll;

  const HomePage({super.key, required this.onGoAll});

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final b = Theme.of(context).brightness;

    final favTools = state.favorites
        .map((id) => kToolById[id])
        .whereType<ToolDef>()
        .toList();
    final recentTools = state.recents
        .map((id) => kToolById[id])
        .whereType<ToolDef>()
        .toList();

    final hour = DateTime.now().hour;
    final greeting = hour < 6
        ? '夜深了'
        : hour < 11
            ? '早上好'
            : hour < 14
                ? '中午好'
                : hour < 18
                    ? '下午好'
                    : '晚上好';

    return Column(
      children: [
        PageHeader(
          large: true,
          title: '宝藏盒',
          subtitle: '$greeting · ${kTools.length} 个工具，全部离线可用',
          actions: [
            IconButton(
              tooltip: '搜索工具',
              onPressed: onGoAll,
              icon: const Icon(Icons.search_rounded, size: 22),
            ),
          ],
        ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
            children: [
              const _HeroCard(),
              SectionCard(
                title: '我的收藏',
                trailing: favTools.isEmpty
                    ? null
                    : Text(
                        '${favTools.length} 个',
                        style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                      ),
                child: favTools.isEmpty
                    ? InkWell(
                        onTap: onGoAll,
                        borderRadius: BorderRadius.circular(AppTheme.radMedium),
                        child: const EmptyState(
                          icon: Icons.star_border_rounded,
                          title: '还没有收藏',
                          desc: '在工具页右上角点击星标，常用工具会固定在这里',
                        ),
                      )
                    : _ToolGrid(tools: favTools),
              ),
              if (state.settings.showRecents)
                SectionCard(
                  title: '最近使用',
                  trailing: recentTools.isEmpty
                      ? null
                      : TextButton(
                          onPressed: state.clearRecents,
                          child: const Text('清空'),
                        ),
                  child: recentTools.isEmpty
                      ? const EmptyState(
                          icon: Icons.history_rounded,
                          title: '暂无使用记录',
                          desc: '打开任意工具后，这里会出现快捷入口',
                        )
                      : Column(
                          children: [
                            for (var i = 0; i < recentTools.length; i++)
                              _RecentRow(
                                tool: recentTools[i],
                                last: i == recentTools.length - 1,
                              ),
                          ],
                        ),
                ),
              const SizedBox(height: 4),
              Text(
                '全部工具均在本机运行，不上传任何数据。',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 11, color: AppTheme.ink3),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard();

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.surfaceOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radLarge),
        border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
      ),
      child: Row(
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: AppTheme.brandOf(b),
              borderRadius: BorderRadius.circular(AppTheme.radMedium),
            ),
            alignment: Alignment.center,
            child: const Icon(Icons.inventory_2_rounded, color: Colors.white, size: 26),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '轻装上阵的工具箱',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.inkOf(b),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${kTools.length} 个工具 · 8 大分类 · 零第三方依赖',
                  style: TextStyle(fontSize: 11.5, height: 1.5, color: AppTheme.ink2Of(b)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ToolGrid extends StatelessWidget {
  final List<ToolDef> tools;

  const _ToolGrid({required this.tools});

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      padding: EdgeInsets.zero,
      itemCount: tools.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        mainAxisSpacing: 10,
        crossAxisSpacing: 6,
        childAspectRatio: 0.82,
      ),
      itemBuilder: (_, i) => _ToolTile(tool: tools[i]),
    );
  }
}

class _ToolTile extends StatelessWidget {
  final ToolDef tool;

  const _ToolTile({required this.tool});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return InkWell(
      onTap: () => openTool(context, tool),
      borderRadius: BorderRadius.circular(AppTheme.radMedium),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: AppTheme.fieldOf(b),
              borderRadius: BorderRadius.circular(AppTheme.radMedium),
              border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
            ),
            child: Icon(tool.icon, size: 20, color: AppTheme.brandOf(b)),
          ),
          const SizedBox(height: 6),
          Text(
            tool.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 11.5, color: AppTheme.inkOf(b)),
          ),
        ],
      ),
    );
  }
}

class _RecentRow extends StatelessWidget {
  final ToolDef tool;
  final bool last;

  const _RecentRow({required this.tool, required this.last});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Column(
      children: [
        InkWell(
          onTap: () => openTool(context, tool),
          borderRadius: BorderRadius.circular(AppTheme.radSmall),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 9),
            child: Row(
              children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: AppTheme.fieldOf(b),
                    borderRadius: BorderRadius.circular(AppTheme.radSmall),
                  ),
                  child: Icon(tool.icon, size: 17, color: AppTheme.brandOf(b)),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        tool.name,
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w600,
                          color: AppTheme.inkOf(b),
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        tool.category.name,
                        style: TextStyle(fontSize: 11, color: AppTheme.ink2Of(b)),
                      ),
                    ],
                  ),
                ),
                Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
              ],
            ),
          ),
        ),
        if (!last) Divider(height: 1, color: AppTheme.lineOf(b), indent: 46),
      ],
    );
  }
}
