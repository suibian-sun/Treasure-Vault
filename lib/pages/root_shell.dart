import 'package:flutter/material.dart';

import '../core/app_state.dart';
import '../theme/app_theme.dart';
import '../widgets/update_dialog.dart';
import 'all_tools_page.dart';
import 'home_page.dart';
import 'mine_page.dart';

/// 主框架：底部三个 Tab（首页 / 全部 / 我的）。
///
/// 只构建当前 Tab 的页面（不使用 IndexedStack 常驻三份子树），
/// 这是本应用把常驻内存压到最低的关键手段之一：
/// 切页时旧页面整棵释放，最多只保留一份页面对象。
class RootShell extends StatefulWidget {
  const RootShell({super.key});

  @override
  State<RootShell> createState() => _RootShellState();
}

class _RootShellState extends State<RootShell> {
  int _index = 0;
  bool _updateChecked = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _maybeCheckUpdate());
  }

  Future<void> _maybeCheckUpdate() async {
    if (_updateChecked) return;
    _updateChecked = true;
    final state = AppState.instance;
    // 等待本地设置加载完成，避免用到默认加速源（无轮询，零定时器）
    await state.readyFuture;
    if (!mounted) return;
    if (!state.settings.autoCheckUpdate) return;
    await checkUpdateManually(context, silent: true);
  }

  Widget _page() {
    switch (_index) {
      case 0:
        return HomePage(onGoAll: () => setState(() => _index = 1));
      case 1:
        return const AllToolsPage();
      default:
        return const MinePage();
    }
  }

  @override
  Widget build(BuildContext context) {
    final lowMemory = AppScope.of(context).settings.lowMemoryMode;
    final b = Theme.of(context).brightness;

    return Scaffold(
      body: lowMemory
          ? _page()
          : AnimatedSwitcher(
              duration: const Duration(milliseconds: 160),
              switchInCurve: Curves.easeOut,
              switchOutCurve: Curves.easeIn,
              child: KeyedSubtree(key: ValueKey<int>(_index), child: _page()),
            ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: AppTheme.surfaceOf(b),
          border: Border(
            top: BorderSide(color: AppTheme.lineOf(b), width: AppTheme.stroke),
          ),
        ),
        child: SafeArea(
          top: false,
          child: SizedBox(
            height: 58,
            child: Row(
              children: [
                _NavItem(
                  icon: Icons.home_outlined,
                  activeIcon: Icons.home_rounded,
                  label: '首页',
                  selected: _index == 0,
                  onTap: () => setState(() => _index = 0),
                ),
                _NavItem(
                  icon: Icons.grid_view_outlined,
                  activeIcon: Icons.grid_view_rounded,
                  label: '全部',
                  selected: _index == 1,
                  onTap: () => setState(() => _index = 1),
                ),
                _NavItem(
                  icon: Icons.person_outline_rounded,
                  activeIcon: Icons.person_rounded,
                  label: '我的',
                  selected: _index == 2,
                  onTap: () => setState(() => _index = 2),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final IconData activeIcon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _NavItem({
    required this.icon,
    required this.activeIcon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final color = selected ? AppTheme.brandOf(b) : AppTheme.ink2Of(b);
    return Expanded(
      child: InkWell(
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(selected ? activeIcon : icon, size: 22, color: color),
            const SizedBox(height: 3),
            Text(
              label,
              style: TextStyle(
                fontSize: 11,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
