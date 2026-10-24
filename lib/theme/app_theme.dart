import 'package:flutter/material.dart';

/// 宝藏盒主题 —— 沿用 SunBuilder-grpc 的「Minimal 极简」设计语言。
///
/// 视觉约定（与参考应用保持一致）：
/// * **纯白卡片 + 浅灰画布**：卡片/顶栏/底栏 `#FFFFFF`，页面底 `#F5F6F8`，
///   用色差而非阴影区分层次；
/// * **亮蓝强调色 `#1890FF`**：唯一彩度来源，用于主按钮、选中态、链接；
/// * **1px 发丝描边**：所有卡片、输入框以 `#EBEDF0` 描边界定，不使用投影；
/// * **克制圆角**：卡片 20 / 控件 16 / 小组件 10，搜索框与主按钮为胶囊形；
/// * **清晰文字层级**：主文字近黑、次要信息中灰，用字号字重区分优先级。
///
/// 深浅两套主题共用同一套语义令牌，仅替换色值。
class AppTheme {
  AppTheme._();

  // ---------------- 品牌色 ----------------
  static const Color primary = Color(0xFF1890FF);
  static const Color primaryDark = Color(0xFF0F6FD1);
  static const Color primaryLight = Color(0xFF4BA7FF);

  // ---------------- 中性色（浅色） ----------------
  static const Color ink = Color(0xFF1F2329);
  static const Color ink2 = Color(0xFF8A9099);
  static const Color ink3 = Color(0xFFB7BCC4);
  static const Color paper = Color(0xFFFFFFFF);
  static const Color canvas = Color(0xFFF5F6F8);
  static const Color hairline = Color(0xFFEBEDF0);
  static const Color fieldBg = Color(0xFFF5F6F8);

  // ---------------- 中性色（深色） ----------------
  static const Color canvasDark = Color(0xFF0F1115);
  static const Color paperDark = Color(0xFF181A1F);
  static const Color hairlineDark = Color(0xFF2A2E36);
  static const Color fieldBgDark = Color(0xFF23262D);
  static const Color inkTextDark = Color(0xFFF2F3F5);
  static const Color ink2TextDark = Color(0xFF9096A0);

  // ---------------- 圆角与描边 ----------------
  static const double radSmall = 10;
  static const double radMedium = 16;
  static const double radLarge = 20;
  static const double radExtra = 24;
  static const double radPill = 999;
  static const double stroke = 1;

  // ---------------- 语义取色（按明暗） ----------------
  static Color canvasOf(Brightness b) => b == Brightness.light ? canvas : canvasDark;
  static Color surfaceOf(Brightness b) => b == Brightness.light ? paper : paperDark;
  static Color inkOf(Brightness b) => b == Brightness.light ? ink : inkTextDark;
  static Color ink2Of(Brightness b) => b == Brightness.light ? ink2 : ink2TextDark;
  static Color lineOf(Brightness b) => b == Brightness.light ? hairline : hairlineDark;
  static Color fieldOf(Brightness b) => b == Brightness.light ? fieldBg : fieldBgDark;
  static Color brandOf(Brightness b) => b == Brightness.light ? primary : primaryLight;

  /// 当前选中项的浅底（12% 品牌色）。
  static Color selectedFillOf(Brightness b) =>
      brandOf(b).withValues(alpha: b == Brightness.light ? 0.10 : 0.20);

  static ColorScheme _scheme(Brightness brightness) {
    final isLight = brightness == Brightness.light;
    final brand = brandOf(brightness);
    return ColorScheme(
      brightness: brightness,
      primary: brand,
      onPrimary: isLight ? Colors.white : const Color(0xFF08111F),
      primaryContainer: isLight ? const Color(0xFFE8F3FF) : const Color(0xFF132338),
      onPrimaryContainer: isLight ? primaryDark : primaryLight,
      secondary: isLight ? ink : inkTextDark,
      onSecondary: isLight ? paper : canvasDark,
      secondaryContainer: fieldOf(brightness),
      onSecondaryContainer: inkOf(brightness),
      tertiary: brand,
      onTertiary: isLight ? Colors.white : const Color(0xFF08111F),
      error: isLight ? const Color(0xFFDC2626) : const Color(0xFFF87171),
      onError: Colors.white,
      errorContainer: isLight ? const Color(0xFFFEF2F2) : const Color(0xFF3B1214),
      onErrorContainer: isLight ? const Color(0xFFB91C1C) : const Color(0xFFFCA5A5),
      surface: canvasOf(brightness),
      onSurface: inkOf(brightness),
      surfaceContainerLowest: isLight ? paper : const Color(0xFF14161B),
      surfaceContainerLow: surfaceOf(brightness),
      surfaceContainer: surfaceOf(brightness),
      surfaceContainerHigh: fieldOf(brightness),
      surfaceContainerHighest: fieldOf(brightness),
      onSurfaceVariant: ink2Of(brightness),
      outline: isLight ? ink3 : const Color(0xFF6B7280),
      outlineVariant: lineOf(brightness),
      shadow: const Color(0x00000000),
      scrim: const Color(0x8C000000),
      inverseSurface: isLight ? ink : inkTextDark,
      onInverseSurface: isLight ? Colors.white : canvasDark,
      inversePrimary: isLight ? primaryLight : primaryDark,
      surfaceTint: Colors.transparent,
    );
  }

  static ThemeData light() => _build(Brightness.light);

  static ThemeData dark() => _build(Brightness.dark);

  static ThemeData _build(Brightness brightness) {
    final isLight = brightness == Brightness.light;
    final scheme = _scheme(brightness);
    final base = (isLight ? Typography.material2021().black : Typography.material2021().white)
        .apply(bodyColor: scheme.onSurface, displayColor: scheme.onSurface);
    final borderMd = BorderRadius.circular(radMedium);
    final borderSm = BorderRadius.circular(radSmall);
    final pill = BorderRadius.circular(radPill);
    final line = BorderSide(color: scheme.outlineVariant, width: stroke);

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      textTheme: base,
      scaffoldBackgroundColor: scheme.surface,
      splashFactory: InkSparkle.splashFactory,
      splashColor: scheme.primary.withValues(alpha: 0.06),
      highlightColor: scheme.onSurface.withValues(alpha: 0.03),
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.surfaceContainerLow,
        foregroundColor: scheme.onSurface,
        surfaceTintColor: Colors.transparent,
        scrolledUnderElevation: 0,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          fontSize: 16.5,
          fontWeight: FontWeight.w700,
          color: scheme.onSurface,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 13),
          shape: RoundedRectangleBorder(borderRadius: borderMd),
          textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: scheme.onSurface,
          side: line,
          backgroundColor: scheme.surfaceContainerLow,
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          shape: RoundedRectangleBorder(borderRadius: borderMd),
          textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: scheme.primary,
          shape: RoundedRectangleBorder(borderRadius: borderSm),
          textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5),
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(foregroundColor: scheme.onSurfaceVariant),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: isLight ? fieldBg : fieldBgDark,
        hintStyle: TextStyle(fontSize: 13.5, color: scheme.onSurfaceVariant),
        labelStyle: TextStyle(fontSize: 13, color: scheme.onSurfaceVariant),
        errorStyle: TextStyle(fontSize: 11.5, color: scheme.error),
        border: OutlineInputBorder(borderRadius: pill, borderSide: BorderSide.none),
        enabledBorder: OutlineInputBorder(borderRadius: pill, borderSide: BorderSide.none),
        focusedBorder: OutlineInputBorder(
          borderRadius: pill,
          borderSide: BorderSide(color: scheme.primary, width: 1.2),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: scheme.surfaceContainerLow,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radLarge),
          side: line,
        ),
        titleTextStyle: TextStyle(
          fontSize: 16.5,
          fontWeight: FontWeight.w700,
          color: scheme.onSurface,
        ),
        contentTextStyle: TextStyle(fontSize: 13.5, color: scheme.onSurfaceVariant),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: scheme.surfaceContainerLow,
        modalBackgroundColor: scheme.surfaceContainerLow,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        showDragHandle: false,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: scheme.inverseSurface,
        elevation: 0,
        contentTextStyle: TextStyle(color: scheme.onInverseSurface, fontSize: 13.5),
        shape: RoundedRectangleBorder(borderRadius: borderMd),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: scheme.primary,
        linearTrackColor: scheme.primary.withValues(alpha: 0.12),
        circularTrackColor: scheme.primary.withValues(alpha: 0.12),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? Colors.white : scheme.outline,
        ),
        trackColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected)
              ? scheme.primary
              : scheme.surfaceContainerHighest,
        ),
        trackOutlineColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? Colors.transparent : scheme.outlineVariant,
        ),
      ),
      dividerTheme: DividerThemeData(color: scheme.outlineVariant, space: 1, thickness: 1),
      listTileTheme: ListTileThemeData(
        iconColor: scheme.onSurfaceVariant,
        textColor: scheme.onSurface,
        shape: RoundedRectangleBorder(borderRadius: borderSm),
        titleTextStyle: TextStyle(
          fontSize: 14.5,
          fontWeight: FontWeight.w600,
          color: scheme.onSurface,
        ),
        subtitleTextStyle: TextStyle(fontSize: 12.5, color: scheme.onSurfaceVariant),
      ),
      popupMenuTheme: PopupMenuThemeData(
        color: scheme.surfaceContainerLow,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: borderMd, side: line),
        textStyle: TextStyle(fontSize: 13.5, color: scheme.onSurface),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: scheme.surfaceContainerLow,
        selectedColor: scheme.primary,
        disabledColor: scheme.surfaceContainerHighest,
        labelStyle: TextStyle(fontSize: 12.5, color: scheme.onSurface),
        secondaryLabelStyle: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant),
        side: line,
        shape: RoundedRectangleBorder(borderRadius: borderSm),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      ),
    );
  }
}
