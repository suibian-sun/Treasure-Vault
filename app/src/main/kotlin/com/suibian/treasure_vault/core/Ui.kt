package com.suibian.treasure_vault.core

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** dp → px */
fun Context.dp(v: Number): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

private fun Context.fpx(v: Number): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics)

/** 统一间距。 */
object Gap {
    const val PAGE = 16
    const val CARD = 16
    const val ITEM = 12
    const val TINY = 8
}

fun Context.tv(
    text: String,
    size: Float = 14f,
    color: Int = T.ink,
    bold: Boolean = false,
): TextView = TextView(this).apply {
    this.text = text
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    setTextColor(color)
    if (bold) typeface = Typeface.DEFAULT_BOLD
    includeFontPadding = false
}

fun Context.hairline(): View = View(this).apply {
    setBackgroundColor(T.line)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
}

/** 白底卡片，内容由 block 填充。 */
fun Context.card(pad: Int = Gap.CARD, block: LinearLayout.() -> Unit = {}): LinearLayout {
    val box = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = T.cardBg()
        setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).also { it.bottomMargin = dp(12) }
    }
    box.block()
    return box
}

/** 卡片内的小节标题（可带右侧说明或控件）。 */
fun LinearLayout.sectionTitle(text: String, trailing: View? = null): LinearLayout {
    val ctx = context
    val row = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    row.addView(ctx.tv(text, 13.5f, T.ink, true), LinearLayout.LayoutParams(0, -2, 1f))
    if (trailing != null) row.addView(trailing)
    row.setPadding(0, 0, 0, context.dp(10))
    addView(row)
    return row
}

/** 字段标签。 */
fun LinearLayout.label(text: String): TextView {
    val ctx = context
    val t = ctx.tv(text, 12f, T.ink2)
    t.setPadding(0, 0, 0, ctx.dp(6))
    addView(t)
    return t
}

/** 输入框（默认多行）。 */
fun LinearLayout.et(
    hint: String = "",
    minLines: Int = 4,
    singleLine: Boolean = false,
    inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
): EditText {
    val ctx = context
    val e = EditText(ctx).apply {
        this.hint = hint
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(T.ink)
        setHintTextColor(T.ink3)
        background = T.fillBg(T.field, ctx.dp(T.R_MEDIUM).toFloat())
        setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
        if (singleLine) {
            isSingleLine = true
            this.inputType = inputType
        } else {
            this.inputType = inputType
            this.minLines = minLines
            gravity = Gravity.TOP
        }
    }
    e.layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = ctx.dp(10) }
    addView(e)
    return e
}

/** 主按钮。 */
fun LinearLayout.btn(text: String, primary: Boolean = true, onClick: () -> Unit): TextView {
    val ctx = context
    val b = ctx.tv(text, 14f, if (primary) Color.WHITE else T.ink, true).apply {
        gravity = Gravity.CENTER
        background = if (primary) T.fillBg(T.primary, ctx.dp(T.R_MEDIUM).toFloat())
        else T.cardBg(ctx.dp(T.R_MEDIUM).toFloat(), T.surface)
        setPadding(0, ctx.dp(13), 0, ctx.dp(13))
        isClickable = true
        setOnClickListener { onClick() }
    }
    b.layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = ctx.dp(10) }
    addView(b)
    return b
}

/** 横向可滚动选择器，选中项为浅蓝底 + 主色文字。 */
fun LinearLayout.chips(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val ctx = context
    val scroll = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false }
    val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
    labels.forEachIndexed { i, s ->
        val active = i == selected
        val chip = ctx.tv(s, 12.5f, if (active) T.primary else T.ink2, active).apply {
            gravity = Gravity.CENTER
            background = if (active) {
                T.fillBg(T.selectedFill, ctx.dp(T.R_PILL).toFloat()).apply {
                    setStroke(1, T.withAlpha(T.primary, 0x59))
                }
            } else {
                T.fillBg(T.field, ctx.dp(T.R_PILL).toFloat())
            }
            setPadding(ctx.dp(14), ctx.dp(8), ctx.dp(14), ctx.dp(8))
            isClickable = true
            setOnClickListener { onPick(i) }
        }
        row.addView(chip, LinearLayout.LayoutParams(-2, -2).also { it.rightMargin = ctx.dp(8) })
    }
    scroll.addView(row)
    addView(scroll, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = ctx.dp(10) })
}

/** 结果展示框（等宽字体、可复制）。 */
fun LinearLayout.out(text: String, label: String = "结果"): TextView {
    val ctx = context
    if (text.isNotBlank()) {
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ctx.tv(label, 12f, T.ink2), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(ctx.tv("复制", 12f, T.primary, true).apply {
            setPadding(ctx.dp(8), ctx.dp(2), 0, ctx.dp(2))
            isClickable = true
            setOnClickListener { Ui.copy(ctx, text) }
        })
        head.setPadding(0, 0, 0, ctx.dp(6))
        addView(head)
    }
    val v = ctx.tv(text.ifBlank { "—" }, 13.5f, if (text.isBlank()) T.ink3 else T.ink).apply {
        typeface = Typeface.MONOSPACE
        background = T.fieldBg()
        setPadding(ctx.dp(12), ctx.dp(12), ctx.dp(12), ctx.dp(12))
        setTextIsSelectable(true)
    }
    v.layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = ctx.dp(6) }
    addView(v)
    return v
}

/** 键值行。 */
fun LinearLayout.kv(k: String, v: String) {
    val ctx = context
    val row = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, ctx.dp(6), 0, ctx.dp(6))
    }
    row.addView(ctx.tv(k, 12.5f, T.ink2), LinearLayout.LayoutParams(ctx.dp(96), -2))
    row.addView(ctx.tv(v, 13f, T.ink).apply { setTextIsSelectable(true) }, LinearLayout.LayoutParams(0, -2, 1f))
    addView(row)
}

/** 说明条。 */
fun LinearLayout.note(text: String, color: Int = T.primary) {
    val ctx = context
    val box = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        background = T.fillBg(T.withAlpha(color, if (T.dark) 0x33 else 0x14), ctx.dp(T.R_MEDIUM).toFloat())
        setPadding(ctx.dp(12), ctx.dp(12), ctx.dp(12), ctx.dp(12))
    }
    box.addView(ctx.tv("ⓘ", 13f, color))
    box.addView(ctx.tv(text, 12f, color).apply { setPadding(ctx.dp(8), 0, 0, 0) },
        LinearLayout.LayoutParams(0, -2, 1f))
    addView(box, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = ctx.dp(10) })
}

fun LinearLayout.space(h: Int = Gap.TINY) {
    addView(View(context), LinearLayout.LayoutParams(1, context.dp(h)))
}

/** 开关行。 */
fun LinearLayout.switchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val ctx = context
    val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    col.addView(ctx.tv(title, 14f, T.ink))
    if (subtitle != null) col.addView(ctx.tv(subtitle, 11.5f, T.ink2).apply { setPadding(0, ctx.dp(2), 0, 0) })
    row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
    val sw = android.widget.Switch(ctx).apply {
        isChecked = checked
        setOnCheckedChangeListener { _, v -> onChange(v) }
    }
    row.addView(sw)
    row.setPadding(0, ctx.dp(4), 0, ctx.dp(4))
    addView(row)
}

/** 页面正文容器（带统一左右边距）。 */
fun Context.pageBody(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(Gap.PAGE), dp(14), dp(Gap.PAGE), dp(28))
}

fun Context.scroll(inner: View): ScrollView = ScrollView(this).apply {
    isFillViewport = true
    addView(inner)
}

/** 方形图标底（emoji 图标，零资源）。 */
fun Context.iconBadge(emoji: String, size: Int = 36, bg: Int = T.field): TextView =
    tv(emoji, (size * 0.42f), T.ink).apply {
        gravity = Gravity.CENTER
        background = T.fillBg(bg, dp(T.R_SMALL).toFloat())
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

object Ui {
    fun copy(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("treasure", text))
        toast(ctx, "已复制")
    }

    fun toast(ctx: Context, msg: String) {
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    /** 把内容放进可滚动卡片并返回外层容器（工具页常用）。 */
    fun cardScroll(ctx: Context, block: LinearLayout.() -> Unit): ScrollView {
        val col = ctx.pageBody()
        col.addView(ctx.card { this.block() })
        return ctx.scroll(col)
    }

    fun frame(ctx: Context, child: View): FrameLayout =
        FrameLayout(ctx).apply { addView(child, FrameLayout.LayoutParams(-1, -1)) }
}

/** 设置行：图标 + 标题（+副标题）+ 右侧控件。 */
fun LinearLayout.settingRow(
    emoji: String,
    title: String,
    subtitle: String? = null,
    trailing: View? = null,
    onClick: (() -> Unit)? = null,
): LinearLayout {
    val ctx = context
    val row = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, ctx.dp(11), 0, ctx.dp(11))
    }
    row.addView(ctx.iconBadge(emoji, 32))
    val col = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(12), 0, ctx.dp(8), 0)
    }
    col.addView(ctx.tv(title, 14f, T.ink))
    if (subtitle != null) {
        col.addView(ctx.tv(subtitle, 11.5f, T.ink2).apply { setPadding(0, ctx.dp(2), 0, 0) })
    }
    row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
    if (trailing != null) {
        row.addView(trailing)
    } else if (onClick != null) {
        row.addView(ctx.tv("›", 18f, T.ink3))
    }
    if (onClick != null) {
        row.isClickable = true
        row.setOnClickListener { onClick() }
    }
    addView(row)
    return row
}

/** 带顶部说明的设置卡片。 */
fun Context.settingsCard(title: String, block: LinearLayout.() -> Unit): LinearLayout =
    card(pad = 12) {
        val t = tv(title, 12f, T.ink2, true)
        t.setPadding(dp(4), dp(2), 0, dp(6))
        addView(t)
        block()
    }
