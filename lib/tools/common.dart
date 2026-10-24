import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';
import '../widgets/kit.dart';

/// 文本变换函数。
typedef TextFn = String Function(String input);

/// 一种处理模式（模式切换即换算法）。
class TextMode {
  final String label;
  final TextFn fn;
  const TextMode(this.label, this.fn);
}

/// 通用「输入 → 一键/实时转换 → 结果」文本工具页面。
///
/// 文本类工具大量共用此骨架，减少重复代码，也保证版式完全一致。
class SimpleTextTool extends StatefulWidget {
  final String hint;
  final List<TextMode> modes;
  final String inputLabel;
  final String outputLabel;
  final bool live;
  final int inputLines;
  final List<Widget> Function(String input)? extraBuilder;

  const SimpleTextTool({
    super.key,
    required this.hint,
    required this.modes,
    this.inputLabel = '输入内容',
    this.outputLabel = '结果',
    this.live = true,
    this.inputLines = 7,
    this.extraBuilder,
  });

  @override
  State<SimpleTextTool> createState() => _SimpleTextToolState();
}

class _SimpleTextToolState extends State<SimpleTextTool> {
  final _controller = TextEditingController();
  int _index = 0;
  String _output = '';

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _run() {
    final input = _controller.text;
    if (input.isEmpty) {
      setState(() => _output = '');
      return;
    }
    try {
      final fn = widget.modes[_index].fn;
      setState(() => _output = fn(input));
    } catch (e) {
      setState(() => _output = '处理失败：$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return ToolBody(
      children: [
        SectionCard(
          title: widget.inputLabel,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: _controller,
                minLines: widget.inputLines,
                maxLines: widget.inputLines * 2,
                keyboardType: TextInputType.multiline,
                onChanged: (_) => widget.live ? _run() : setState(() {}),
                style: const TextStyle(fontSize: 14, height: 1.5),
                decoration: InputDecoration(
                  hintText: widget.hint,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppTheme.radMedium),
                    borderSide: BorderSide.none,
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppTheme.radMedium),
                    borderSide: BorderSide.none,
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppTheme.radMedium),
                    borderSide: BorderSide(
                      color: AppTheme.brandOf(Theme.of(context).brightness),
                      width: 1.2,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Text(
                    '${_controller.text.length} 字符 · ${_controller.text.isEmpty ? 0 : _controller.text.split('\n').length} 行',
                    style: TextStyle(
                      fontSize: 11.5,
                      color: AppTheme.ink2Of(Theme.of(context).brightness),
                    ),
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: () {
                      _controller.clear();
                      setState(() => _output = '');
                    },
                    child: const Text('清空'),
                  ),
                  TextButton(
                    onPressed: () async {
                      final data = await Clipboard.getData(Clipboard.kTextPlain);
                      if (data?.text != null) {
                        _controller.text = data!.text!;
                        _run();
                      }
                    },
                    child: const Text('粘贴'),
                  ),
                ],
              ),
            ],
          ),
        ),
        if (widget.modes.length > 1)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: SegChips<int>(
              values: List<int>.generate(widget.modes.length, (i) => i),
              labels: widget.modes.map((m) => m.label).toList(),
              value: _index,
              onChanged: (v) {
                _index = v;
                _run();
              },
            ),
          ),
        if (widget.extraBuilder != null) ...widget.extraBuilder!(_controller.text),
        if (!widget.live)
          Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: PrimaryButton(
              label: '开始处理',
              icon: Icons.auto_fix_high_rounded,
              onPressed: _run,
            ),
          ),
        const SizedBox(height: 8),
        SectionCard(
          child: ResultBox(text: _output, label: widget.outputLabel),
        ),
        if (_output.isNotEmpty) _OutputStats(input: _controller.text, output: _output),
      ],
    );
  }
}

/// 结果概览（字符 / 行数变化）。
class _OutputStats extends StatelessWidget {
  final String input;
  final String output;

  const _OutputStats({required this.input, required this.output});

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    final outLines = output.isEmpty ? 0 : output.split('\n').length;
    return Padding(
      padding: const EdgeInsets.only(top: 2, left: 4),
      child: Text(
        '输出 ${output.length} 字符 · $outLines 行'
        '${input.length != output.length ? '（${output.length - input.length >= 0 ? '+' : ''}${output.length - input.length}）' : ''}',
        style: TextStyle(fontSize: 11.5, color: AppTheme.ink2Of(b)),
      ),
    );
  }
}

/// 数字输入行（左标签 + 右单位）。
class NumberField extends StatelessWidget {
  final String label;
  final String unit;
  final TextEditingController controller;
  final ValueChanged<String>? onChanged;
  final String hint;

  const NumberField({
    super.key,
    required this.label,
    required this.unit,
    required this.controller,
    this.onChanged,
    this.hint = '0',
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          SizedBox(
            width: 84,
            child: Text(
              label,
              style: TextStyle(fontSize: 13, color: AppTheme.ink2Of(b)),
            ),
          ),
          Expanded(
            child: TextField(
              controller: controller,
              onChanged: onChanged,
              keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
              style: const TextStyle(fontSize: 14),
              decoration: InputDecoration(
                hintText: hint,
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppTheme.radSmall),
                  borderSide: BorderSide.none,
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppTheme.radSmall),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppTheme.radSmall),
                  borderSide: BorderSide(color: AppTheme.brandOf(b), width: 1.2),
                ),
              ),
            ),
          ),
          if (unit.isNotEmpty)
            SizedBox(
              width: 46,
              child: Text(
                unit,
                textAlign: TextAlign.right,
                style: TextStyle(fontSize: 12.5, color: AppTheme.ink2Of(b)),
              ),
            ),
        ],
      ),
    );
  }
}

/// 大号结果展示（计算类工具用）。
class BigResult extends StatelessWidget {
  final String value;
  final String label;
  final String? sub;
  final VoidCallback? onCopy;

  const BigResult({
    super.key,
    required this.value,
    this.label = '结果',
    this.sub,
    this.onCopy,
  });

  @override
  Widget build(BuildContext context) {
    final b = Theme.of(context).brightness;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
      decoration: BoxDecoration(
        color: AppTheme.selectedFillOf(b),
        borderRadius: BorderRadius.circular(AppTheme.radMedium),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w600,
              color: AppTheme.brandOf(b),
            ),
          ),
          const SizedBox(height: 6),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: SelectableText(
                  value,
                  maxLines: 3,
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                    height: 1.2,
                    color: AppTheme.brandOf(b),
                    fontFeatures: const [FontFeature.tabularFigures()],
                  ),
                ),
              ),
              if (onCopy != null)
                IconButton(
                  onPressed: onCopy,
                  icon: Icon(Icons.copy_rounded, size: 17, color: AppTheme.brandOf(b)),
                  visualDensity: VisualDensity.compact,
                ),
            ],
          ),
          if (sub != null)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text(
                sub!,
                style: TextStyle(fontSize: 11.5, color: AppTheme.brandOf(b)),
              ),
            ),
        ],
      ),
    );
  }
}
