import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/utils.dart';
import '../theme/app_theme.dart';

/// 统一的工具页面骨架：顶栏（返回 + 标题 + 副标题 + 右侧动作）+ 可滚动内容。
///
/// 所有工具页都使用同一骨架，保证版式与参考应用（SunBuilder-grpc）一致。
class ToolScaffold extends StatelessWidget {
  final String title;
  final String? subtitle;
  final List<Widget> children;
  final List<Widget> actions;
  final Widget? bottomBar;

  const ToolScaffold({
    super.key,
    required this.title,
    this.subtitle,
    required this.children,
    this.actions = const [],
    this.bottomBar,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final line = AppTheme.lineOf(b);
    return Scaffold(
      appBar: AppBar(
        shape: Border(bottom: BorderSide(color: line, width: AppTheme.stroke)),
        titleSpacing: 0,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(title, maxLines: 1, overflow: TextOverflow.ellipsis),
            if (subtitle != null)
              Padding(
                padding: const EdgeInsets.only(top: 2),
                child: Text(
                  subtitle!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w400,
                    color: AppTheme.ink2Of(b),
                  ),
                ),
              ),
          ],
        ),
        actions: actions,
      ),
      body: SafeArea(
        top: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
          children: children,
        ),
      ),
      bottomNavigationBar: bottomBar,
    );
  }
}

/// 工具页面正文容器：统一边距与滚动行为。
///
/// 各工具只返回内容，由 `ToolHostPage` 负责 Scaffold / 顶栏 / 收藏按钮，
/// 这样 57 个工具共享同一套版式，代码量也最省。
class ToolBody extends StatelessWidget {
  final List<Widget> children;

  const ToolBody({super.key, required this.children});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
      children: children,
    );
  }
}

/// 白底 + 发丝描边的分组卡片（Minimal 的核心容器）。
class SectionCard extends StatelessWidget {
  final String? title;
  final String? hint;
  final Widget child;
  final EdgeInsetsGeometry padding;
  final Widget? trailing;

  const SectionCard({
    super.key,
    this.title,
    this.hint,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: AppTheme.surfaceOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radLarge),
        border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
      ),
      child: Padding(
        padding: padding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (title != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        title!,
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w700,
                          color: AppTheme.inkOf(b),
                        ),
                      ),
                    ),
                    if (trailing != null) trailing!,
                  ],
                ),
              ),
            child,
            if (hint != null)
              Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Text(
                  hint!,
                  style: TextStyle(fontSize: 11.5, height: 1.5, color: AppTheme.ink2Of(b)),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// 字段标题 + 输入框。
class LabeledField extends StatelessWidget {
  final String label;
  final TextEditingController? controller;
  final String hint;
  final int minLines;
  final int maxLines;
  final TextInputType? keyboardType;
  final ValueChanged<String>? onChanged;
  final List<TextInputFormatter>? formatters;
  final Widget? suffix;

  const LabeledField({
    super.key,
    required this.label,
    this.controller,
    this.hint = '',
    this.minLines = 1,
    this.maxLines = 1,
    this.keyboardType,
    this.onChanged,
    this.formatters,
    this.suffix,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: AppTheme.ink2Of(b),
          ),
        ),
        const SizedBox(height: 7),
        TextField(
          controller: controller,
          minLines: minLines,
          maxLines: maxLines,
          keyboardType: keyboardType,
          onChanged: onChanged,
          inputFormatters: formatters,
          style: TextStyle(fontSize: 14, height: 1.45, color: AppTheme.inkOf(b)),
          decoration: InputDecoration(hintText: hint, suffixIcon: suffix),
        ),
      ],
    );
  }
}

/// 结果展示区：等宽字体、可选中、右上角一键复制。
class ResultBox extends StatelessWidget {
  final String text;
  final String? label;
  final bool mono;
  final Color? color;

  const ResultBox({
    super.key,
    required this.text,
    this.label,
    this.mono = true,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final empty = text.isEmpty;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                label ?? '结果',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.ink2Of(b),
                ),
              ),
            ),
            if (!empty)
              _MiniAction(
                icon: Icons.copy_rounded,
                label: '复制',
                onTap: () => copyText(context, text),
              ),
          ],
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(13),
          decoration: BoxDecoration(
            color: AppTheme.fieldOf(b),
            borderRadius: BorderRadius.circular(AppTheme.radMedium),
            border: Border.all(color: AppTheme.lineOf(b), width: AppTheme.stroke),
          ),
          child: SelectableText(
            empty ? '—' : text,
            style: TextStyle(
              fontSize: 13.5,
              height: 1.55,
              color: color ?? (empty ? AppTheme.ink3 : AppTheme.inkOf(b)),
              fontFamily: mono ? 'monospace' : null,
              fontFamilyFallback: const ['monospace', 'Roboto Mono', 'Courier New'],
            ),
          ),
        ),
      ],
    );
  }
}

class _MiniAction extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _MiniAction({required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppTheme.radSmall),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 13, color: AppTheme.brandOf(b)),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w600,
                color: AppTheme.brandOf(b),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 横向可滚动的分段选择器（选中项 = 浅蓝底 + 蓝字）。
class SegChips<T> extends StatelessWidget {
  final List<T> values;
  final List<String> labels;
  final T value;
  final ValueChanged<T> onChanged;

  const SegChips({
    super.key,
    required this.values,
    required this.labels,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final sel = AppTheme.selectedFillOf(b);
    final brand = AppTheme.brandOf(b);
    return SizedBox(
      height: 34,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: values.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (_, i) {
          final active = values[i] == value;
          return InkWell(
            borderRadius: BorderRadius.circular(AppTheme.radPill),
            onTap: () => onChanged(values[i]),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14),
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: active ? sel : AppTheme.surfaceOf(b),
                borderRadius: BorderRadius.circular(AppTheme.radPill),
                border: Border.all(
                  color: active ? brand.withValues(alpha: 0.35) : AppTheme.lineOf(b),
                  width: AppTheme.stroke,
                ),
              ),
              child: Text(
                labels[i],
                style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: active ? FontWeight.w600 : FontWeight.w500,
                  color: active ? brand : AppTheme.ink2Of(b),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 主按钮（整行，胶囊/大圆角）。
class PrimaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;

  const PrimaryButton({super.key, required this.label, this.onPressed, this.icon});

  @override
  Widget build(BuildContext context) {
    if (icon == null) {
      return SizedBox(
        width: double.infinity,
        child: FilledButton(onPressed: onPressed, child: Text(label)),
      );
    }
    return SizedBox(
      width: double.infinity,
      child: FilledButton.icon(
        onPressed: onPressed,
        icon: Icon(icon, size: 17),
        label: Text(label),
      ),
    );
  }
}

class GhostButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;

  const GhostButton({super.key, required this.label, this.onPressed, this.icon});

  @override
  Widget build(BuildContext context) {
    if (icon == null) {
      return OutlinedButton(onPressed: onPressed, child: Text(label));
    }
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 16),
      label: Text(label),
    );
  }
}

/// 统计格子（数字 + 说明）。
class StatTile extends StatelessWidget {
  final String value;
  final String label;
  final Color? color;

  const StatTile({super.key, required this.value, required this.label, this.color});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
      decoration: BoxDecoration(
        color: AppTheme.fieldOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radSmall),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: color ?? AppTheme.inkOf(b),
              fontFeatures: const [FontFeature.tabularFigures()],
            ),
          ),
          const SizedBox(height: 3),
          Text(
            label,
            style: TextStyle(fontSize: 11, color: AppTheme.ink2Of(b)),
          ),
        ],
      ),
    );
  }
}

/// 键值行（详情展示）。
class KVRow extends StatelessWidget {
  final String k;
  final String v;
  final bool copyable;

  const KVRow({super.key, required this.k, required this.v, this.copyable = false});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 96,
            child: Text(k, style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(b))),
          ),
          Expanded(
            child: SelectableText(
              v,
              style: TextStyle(fontSize: 13, height: 1.4, color: AppTheme.inkOf(b)),
            ),
          ),
          if (copyable)
            GestureDetector(
              onTap: () => copyText(context, v),
              child: Padding(
                padding: const EdgeInsets.only(left: 8),
                child: Icon(Icons.copy_rounded, size: 14, color: AppTheme.ink2Of(b)),
              ),
            ),
        ],
      ),
    );
  }
}

/// 说明条（浅蓝底，用于工具内提示）。
class InfoNote extends StatelessWidget {
  final String text;
  final IconData icon;

  const InfoNote({super.key, required this.text, this.icon = Icons.info_outline_rounded});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppTheme.selectedFillOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radMedium),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 15, color: AppTheme.brandOf(b)),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: TextStyle(
                fontSize: 12,
                height: 1.55,
                color: AppTheme.brandOf(b),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 空状态。
class EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String desc;

  const EmptyState({super.key, required this.icon, required this.title, required this.desc});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 26),
      child: Column(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: AppTheme.fieldOf(b),
              borderRadius: BorderRadius.circular(AppTheme.radMedium),
            ),
            child: Icon(icon, size: 22, color: AppTheme.ink3),
          ),
          const SizedBox(height: 12),
          Text(
            title,
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: AppTheme.inkOf(b),
            ),
          ),
          const SizedBox(height: 5),
          Text(
            desc,
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, height: 1.5, color: AppTheme.ink2Of(b)),
          ),
        ],
      ),
    );
  }
}

/// 设置项行：图标 + 标题 + 副标题 + 右侧控件。
class SettingRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;
  final bool showDivider;

  const SettingRow({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.trailing,
    this.onTap,
    this.showDivider = false,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final content = Padding(
      padding: const EdgeInsets.symmetric(vertical: 11),
      child: Row(
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: AppTheme.fieldOf(b),
              borderRadius: BorderRadius.circular(AppTheme.radSmall),
            ),
            child: Icon(icon, size: 17, color: AppTheme.brandOf(b)),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.inkOf(b),
                  ),
                ),
                if (subtitle != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      subtitle!,
                      style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
                    ),
                  ),
              ],
            ),
          ),
          if (trailing != null) trailing!,
        ],
      ),
    );
    return Column(
      children: [
        InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppTheme.radSmall),
          child: content,
        ),
        if (showDivider)
          Divider(height: 1, thickness: 1, color: AppTheme.lineOf(b), indent: 44),
      ],
    );
  }
}
