import 'package:flutter/material.dart';

import '../core/tool.dart';
import '../core/utils.dart';
import '../theme/app_theme.dart';
import '../tools/registry.dart';
import '../widgets/kit.dart';
import '../widgets/page_header.dart';
import 'tool_host.dart';

/// 全部工具：搜索 + 分类筛选 + 分组展示。
class AllToolsPage extends StatefulWidget {
  const AllToolsPage({super.key});

  @override
  State<AllToolsPage> createState() => _AllToolsPageState();
}

class _AllToolsPageState extends State<AllToolsPage> {
  final _search = TextEditingController();
  String? _categoryId; // null = 全部

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final q = _search.text.trim();
    final filtered = searchTools(q)
        .where((t) => _categoryId == null || t.category.id == _categoryId)
        .toList();

    // 未搜索且未筛分类时，按分类分组展示
    final grouped = q.isEmpty && _categoryId == null;

    return Column(
      children: [
        PageHeader(
          title: '全部工具',
          subtitle: '${kTools.length} 个工具 · ${Categories.all.length} 大分类',
          bottom: TextField(
            controller: _search,
            onChanged: (_) => setState(() {}),
            textInputAction: TextInputAction.search,
            style: const TextStyle(fontSize: 14),
            decoration: InputDecoration(
              hintText: '搜索工具名称、用途或关键词',
              prefixIcon: Icon(Icons.search_rounded, size: 19, color: AppTheme.ink2Of(b)),
              suffixIcon: q.isEmpty
                  ? null
                  : IconButton(
                      onPressed: () {
                        _search.clear();
                        setState(() {});
                      },
                      icon: const Icon(Icons.close_rounded, size: 17),
                    ),
              isDense: true,
              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppTheme.radPill),
                borderSide: BorderSide.none,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppTheme.radPill),
                borderSide: BorderSide.none,
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppTheme.radPill),
                borderSide: BorderSide(color: AppTheme.brandOf(b), width: 1.2),
              ),
            ),
          ),
        ),
        SizedBox(
          height: 50,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
            children: [
              _chip(context, '全部', null, count: kTools.length),
              for (final c in Categories.all)
                _chip(context, c.name, c.id, count: toolsOf(c).length),
            ],
          ),
        ),
        Expanded(
          child: filtered.isEmpty
              ? ListView(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 16),
                  children: const [
                    EmptyState(
                      icon: Icons.search_off_rounded,
                      title: '没有匹配的工具',
                      desc: '换个关键词，或切换到「全部」分类再看看',
                    ),
                  ],
                )
              : ListView(
                  padding: const EdgeInsets.fromLTRB(16, 2, 16, 28),
                  children: grouped
                      ? [
                          for (final c in Categories.all)
                            if (toolsOf(c).isNotEmpty) ...[
                              _CategoryHeader(category: c, count: toolsOf(c).length),
                              _ToolList(tools: toolsOf(c)),
                              const SizedBox(height: 6),
                            ],
                        ]
                      : [
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8, top: 4),
                            child: Text(
                              '找到 ${filtered.length} 个工具',
                              style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                            ),
                          ),
                          _ToolList(tools: filtered),
                        ],
                ),
        ),
      ],
    );
  }

  Widget _chip(BuildContext context, String label, String? id, {required int count}) {
    final b = Theme.of(context).brightness;
    final active = _categoryId == id;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: InkWell(
        borderRadius: BorderRadius.circular(AppTheme.radPill),
        onTap: () => setState(() => _categoryId = id),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: active ? AppTheme.selectedFillOf(b) : AppTheme.fieldOf(b),
            borderRadius: BorderRadius.circular(AppTheme.radPill),
            border: Border.all(
              color: active
                  ? AppTheme.brandOf(b).withValues(alpha: 0.35)
                  : AppTheme.lineOf(b),
              width: AppTheme.stroke,
            ),
          ),
          child: Text(
            '$label $count',
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: active ? FontWeight.w600 : FontWeight.w500,
              color: active ? AppTheme.brandOf(b) : AppTheme.ink2Of(b),
            ),
          ),
        ),
      ),
    );
  }
}

class _CategoryHeader extends StatelessWidget {
  final ToolCategory category;
  final int count;

  const _CategoryHeader({required this.category, required this.count});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 10, 4, 8),
      child: Row(
        children: [
          Icon(category.icon, size: 15, color: AppTheme.brandOf(b)),
          const SizedBox(width: 6),
          Text(
            category.name,
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w700,
              color: AppTheme.inkOf(b),
            ),
          ),
          const SizedBox(width: 6),
          Text(
            category.desc,
            style: TextStyle(fontSize: 11, color: AppTheme.ink2Of(b)),
          ),
          const Spacer(),
          Text('$count', style: TextStyle(fontSize: 11, color: AppTheme.ink3)),
        ],
      ),
    );
  }
}

class _ToolList extends StatelessWidget {
  final List<ToolDef> tools;

  const _ToolList({required this.tools});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surfaceOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radLarge),
        border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
      ),
      child: Column(
        children: [
          for (var i = 0; i < tools.length; i++) ...[
            if (i > 0)
              Divider(height: 1, color: AppTheme.lineOf(b), indent: 62, endIndent: 14),
            _ToolRow(tool: tools[i]),
          ],
        ],
      ),
    );
  }
}

class _ToolRow extends StatelessWidget {
  final ToolDef tool;

  const _ToolRow({required this.tool});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return InkWell(
      onTap: () => openTool(context, tool),
      borderRadius: BorderRadius.circular(AppTheme.radLarge),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
        child: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: AppTheme.fieldOf(b),
                borderRadius: BorderRadius.circular(AppTheme.radSmall),
                border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
              ),
              child: Icon(tool.icon, size: 18, color: AppTheme.brandOf(b)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    tool.name,
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: AppTheme.inkOf(b),
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    ellipsis(tool.desc, 40),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.ink3),
          ],
        ),
      ),
    );
  }
}
