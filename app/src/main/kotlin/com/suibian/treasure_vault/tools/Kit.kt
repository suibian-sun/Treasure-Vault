package com.suibian.treasure_vault.tools

import android.graphics.Typeface
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv

/** 工具页常用积木。 */
object Kit {

    fun simple(
        act: ToolActivity,
        body: LinearLayout,
        hint: String,
        modes: List<Pair<String, (String) -> String>>,
        lines: Int = 6,
        outputLabel: String = "结果",
        live: Boolean = true,
    ) {
        var idx = 0
        var input: EditText? = null
        var output: TextView? = null

        fun run() {
            val s = input?.text?.toString().orEmpty()
            val o = output ?: return
            if (s.isEmpty()) {
                o.text = "—"; o.setTextColor(T.ink3); return
            }
            val r = runCatching { modes[idx].second(s) }.getOrElse { "处理失败：${it.message}" }
            o.text = r; o.setTextColor(T.ink)
        }

        body.addView(act.card {
            sectionTitle("输入")
            val e = et(hint, lines)
            if (live) e.doAfterTextChanged { run() }
            input = e
        })
        if (modes.size > 1) {
            body.chips(modes.map { it.first }, 0) { i -> idx = i; run() }
        }
        body.addView(act.card {
            val copy = act.tv("复制", 11.5f, T.primary, true).apply {
                setPadding(act.dp(8), act.dp(2), 0, act.dp(2))
                setOnClickListener { Ui.copy(act, output?.text?.toString().orEmpty()) }
            }
            sectionTitle(outputLabel, copy)
            val o = act.tv("—", 13.5f, T.ink3).apply {
                typeface = Typeface.MONOSPACE
                background = T.fieldBg()
                setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
                setTextIsSelectable(true)
            }
            addView(o)
            output = o
        })
        run()
    }

    /** 数字输入行（标签 + 输入 + 单位）。 */
    fun numberRow(
        act: ToolActivity,
        parent: LinearLayout,
        label: String,
        unit: String = "",
        hint: String = "0",
        onChanged: () -> Unit = {},
    ): EditText {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, act.dp(4), 0, act.dp(4))
        }
        row.addView(act.tv(label, 13f, T.ink2), LinearLayout.LayoutParams(act.dp(84), -2))
        val e = EditText(act).apply {
            this.hint = hint
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(T.ink)
            setHintTextColor(T.ink3)
            background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
            setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            doAfterTextChanged { onChanged() }
        }
        row.addView(e, LinearLayout.LayoutParams(0, -2, 1f))
        if (unit.isNotEmpty()) {
            row.addView(act.tv(unit, 12f, T.ink2).apply { setPadding(act.dp(8), 0, 0, 0) })
        }
        parent.addView(row)
        return e
    }

    fun parse(s: CharSequence?): Double? {
        val t = s?.toString()?.trim()?.replace(",", "")?.removeSuffix("%") ?: return null
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()
    }

    fun fmt(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "—"
        if (v == v.toLong().toDouble() && kotlin.math.abs(v) < 1e15) return v.toLong().toString()
        var s = "%.6f".format(v).trimEnd('0').trimEnd('.')
        if (s == "-0") s = "0"
        return s
    }

    fun big(act: ToolActivity, parent: LinearLayout, value: String, label: String, sub: String? = null) {
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            background = T.fillBg(T.selectedFill, act.dp(T.R_MEDIUM).toFloat())
            setPadding(act.dp(16), act.dp(16), act.dp(16), act.dp(16))
        }
        box.addView(act.tv(label, 11.5f, T.primary, true))
        box.addView(act.tv(value, 24f, T.primary, true).apply { setPadding(0, act.dp(6), 0, 0) })
        if (sub != null) box.addView(act.tv(sub, 11.5f, T.primary).apply { setPadding(0, act.dp(4), 0, 0) })
        parent.addView(box, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(10) })
    }
}
