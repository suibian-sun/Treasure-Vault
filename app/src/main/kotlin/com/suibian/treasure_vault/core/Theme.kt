package com.suibian.treasure_vault.core

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * 设计令牌 + 主题预设。
 *
 * 沿用 SunBuilder-grpc 的 Minimal 语言：纯白卡片、浅灰画布、1px 发丝描边、
 * 单一强调色；圆角 20/16/10。所有颜色都由当前预设 + 明暗解析得到，
 * 页面代码只引用 T.xxx，不在任何地方硬编码色值。
 */
object T {

    /** 主题预设：一张卡片一套视觉。 */
    data class Preset(
        val id: String,
        val name: String,
        val desc: String,
        val primary: Int,
        val primaryDark: Int,
        val teal: Int,
    )

    val presets = listOf(
        Preset("minimal", "极简蓝", "纯白卡片 · 浅灰画布 · 亮蓝强调", 0xFF1890FF.toInt(), 0xFF0F6FD1.toInt(), 0xFF14B8A6.toInt()),
        Preset("emerald", "翡翠绿", "清爽通透的绿色主色", 0xFF10B981.toInt(), 0xFF059669.toInt(), 0xFF0EA5E9.toInt()),
        Preset("ocean", "海洋蓝", "冷静克制的深海蓝", 0xFF0EA5E9.toInt(), 0xFF0369A1.toInt(), 0xFF14B8A6.toInt()),
        Preset("sunset", "落日橙", "温暖的橙色调", 0xFFF97316.toInt(), 0xFFEA580C.toInt(), 0xFFF59E0B.toInt()),
        Preset("violet", "紫罗兰", "偏冷的紫罗兰", 0xFF8B5CF6.toInt(), 0xFF7C3AED.toInt(), 0xFF6366F1.toInt()),
        Preset("sakura", "樱花粉", "柔和的粉色点缀", 0xFFEC4899.toInt(), 0xFFDB2777.toInt(), 0xFFF472B6.toInt()),
    )

    var preset: Preset = presets[0]
        private set
    var dark: Boolean = false
        private set

    // 基础中性色
    private const val INK_L = 0xFF1F2329.toInt()
    private const val INK2_L = 0xFF8A9099.toInt()
    private const val INK3_L = 0xFFB7BCC4.toInt()
    private const val PAPER_L = 0xFFFFFFFF.toInt()
    private const val CANVAS_L = 0xFFF5F6F8.toInt()
    private const val LINE_L = 0xFFEBEDF0.toInt()
    private const val FIELD_L = 0xFFF5F6F8.toInt()

    private const val CANVAS_D = 0xFF0F1115.toInt()
    private const val PAPER_D = 0xFF181A1F.toInt()
    private const val LINE_D = 0xFF2A2E36.toInt()
    private const val FIELD_D = 0xFF23262D.toInt()
    private const val INK_D = 0xFFF2F3F5.toInt()
    private const val INK2_D = 0xFF9096A0.toInt()

    // 圆角与间距
    const val R_SMALL = 10f
    const val R_MEDIUM = 16f
    const val R_LARGE = 20f
    const val R_PILL = 999f
    const val STROKE = 1f

    val primary: Int get() = if (dark) lighten(preset.primary) else preset.primary
    val primaryDark: Int get() = preset.primaryDark
    val surface: Int get() = if (dark) PAPER_D else PAPER_L
    val canvas: Int get() = if (dark) CANVAS_D else CANVAS_L
    val ink: Int get() = if (dark) INK_D else INK_L
    val ink2: Int get() = if (dark) INK2_D else INK2_L
    val ink3: Int get() = if (dark) 0xFF6B7280.toInt() else INK3_L
    val line: Int get() = if (dark) LINE_D else LINE_L
    val field: Int get() = if (dark) FIELD_D else FIELD_L
    val ok: Int get() = 0xFF16A34A.toInt()
    val warn: Int get() = 0xFFD97706.toInt()
    val danger: Int get() = if (dark) 0xFFF87171.toInt() else 0xFFDC2626.toInt()

    /** 选中态浅底（12% 主色）。 */
    val selectedFill: Int get() = withAlpha(primary, if (dark) 0x33 else 0x1A)

    fun sync(ctx: Context) {
        val p = Prefs.get(ctx)
        preset = presets.firstOrNull { it.id == p.themePreset } ?: presets[0]
        dark = when (p.themeMode) {
            "light" -> false
            "dark" -> true
            else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun lighten(c: Int): Int = Color.rgb(
        (Color.red(c) + (255 - Color.red(c)) * 0.22f).toInt(),
        (Color.green(c) + (255 - Color.green(c)) * 0.22f).toInt(),
        (Color.blue(c) + (255 - Color.blue(c)) * 0.22f).toInt(),
    )

    /** 统一卡片底：纯色 + 圆角 + 发丝描边（无阴影）。 */
    fun cardBg(radius: Float = R_LARGE, fill: Int = surface): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fill)
            setStroke(STROKE.toInt(), line)
        }

    /** 输入框 / 结果框底。 */
    fun fieldBg(radius: Float = R_MEDIUM): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(field)
            setStroke(STROKE.toInt(), line)
        }

    fun fillBg(color: Int, radius: Float = R_MEDIUM): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }
}
