package com.suibian.treasure_vault.tools

import android.graphics.Color
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 记录管理：记账本 / 记事本 / 纪念日。 */
object DataTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.data, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("ledger", "记账本", "收支记录与月度汇总", "💰", "记账", "账本") { a, b -> ledger(a, b) },
        spec("notes", "记事本", "本地笔记，随时保存", "📝", "笔记", "note") { a, b -> notes(a, b) },
        spec("anniversary", "纪念日", "记录重要日期并倒数", "🎗️", "纪念日", "倒数") { a, b -> anniversary(a, b) },
    )

    // ---------------- 记账 ----------------
    private fun ledger(act: ToolActivity, body: LinearLayout) {
        val prefs = Prefs.get(act)
        val listBox = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val summary = act.tv("", 13f, T.ink2)
        var income = false

        fun load(): MutableList<Triple<String, Double, Boolean>> {
            val raw = prefs.str("ledger.items", "[]")
            val out = mutableListOf<Triple<String, Double, Boolean>>()
            runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(Triple(o.optString("t"), o.optDouble("v"), o.optBoolean("i")))
                }
            }
            return out
        }

        fun save(items: List<Triple<String, Double, Boolean>>) {
            val arr = JSONArray()
            items.forEach {
                arr.put(JSONObject().put("t", it.first).put("v", it.second).put("i", it.third))
            }
            prefs.put("ledger.items", arr.toString())
        }

        fun render() {
            val items = load()
            listBox.removeAllViews()
            if (items.isEmpty()) {
                listBox.addView(act.tv("还没有记录", 12.5f, T.ink2))
            }
            items.take(30).forEachIndexed { idx, it ->
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, act.dp(8), 0, act.dp(8))
                }
                row.addView(act.tv(if (it.third) "收" else "支", 12f,
                    if (it.third) T.ok else T.danger, true))
                row.addView(act.tv(it.first, 13f, T.ink).apply { setPadding(act.dp(10), 0, 0, 0) },
                    LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv((if (it.third) "+" else "-") + "¥" + Kit.fmt(it.second), 13f,
                    if (it.third) T.ok else T.danger, true))
                row.addView(act.tv("✕", 13f, T.ink3).apply {
                    setPadding(act.dp(10), 0, 0, 0)
                    isClickable = true
                    setOnClickListener {
                        val l = load().toMutableList()
                        if (idx < l.size) { l.removeAt(idx); save(l); render() }
                    }
                })
                listBox.addView(row)
                if (idx < 29) listBox.addView(act.hairline())
            }
            val inc = items.filter { it.third }.sumOf { it.second }
            val exp = items.filterNot { it.third }.sumOf { it.second }
            summary.text = "收入 ¥${Kit.fmt(inc)} · 支出 ¥${Kit.fmt(exp)} · 结余 ¥${Kit.fmt(inc - exp)}"
        }

        body.addView(act.card {
            sectionTitle("新增记录")
            val note = EditText(act).apply {
                hint = "备注，例如 午饭"
                setTextColor(T.ink); setHintTextColor(T.ink3)
                background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10))
            }
            addView(note)
            val amount = Kit.numberRow(act, this, "金额", "元")
            val typeRow = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, act.dp(8), 0, 0) }
            val expense = act.tv("支出", 13f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.danger, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(10), 0, act.dp(10))
                isClickable = true
                setOnClickListener { income = false }
            }
            val gain = act.tv("收入", 13f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(10), 0, act.dp(10))
                isClickable = true
                setOnClickListener { income = true }
            }
            typeRow.addView(expense, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            typeRow.addView(gain, LinearLayout.LayoutParams(0, -2, 1f))
            addView(typeRow)
            btn("记一笔") {
                val v = amount.text.toString().toDoubleOrNull()
                if (v == null || v <= 0) { Toast.makeText(act, "请输入金额", Toast.LENGTH_SHORT).show(); return@btn }
                val l = load().toMutableList()
                l.add(0, Triple(note.text.toString().ifBlank { if (income) "收入" else "支出" }, v, income))
                save(l); note.setText(""); amount.setText(""); render()
            }
        })
        body.addView(act.card { sectionTitle("汇总"); addView(summary); addView(act.hairline().apply { setPadding(0, act.dp(8), 0, act.dp(8)) }) })
        body.addView(act.card { sectionTitle("明细（最近 30 条）"); addView(listBox) })
        render()
    }

    // ---------------- 记事本 ----------------
    private fun notes(act: ToolActivity, body: LinearLayout) {
        val prefs = Prefs.get(act)
        var title = ""
        var content = prefs.str("note.content", "")
        val stamp = prefs.str("note.time", "尚未保存")

        body.addView(act.card {
            sectionTitle("标题")
            val e = EditText(act).apply {
                hint = "给这段笔记起个名字"
                setTextColor(T.ink); setHintTextColor(T.ink3)
                background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10))
                setText(prefs.str("note.title", ""))
                doAfterTextChanged { title = it?.toString().orEmpty() }
            }
            addView(e)
            sectionTitle("内容")
            val bodyEt = EditText(act).apply {
                hint = "写点什么…"
                setTextColor(T.ink); setHintTextColor(T.ink3)
                background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10))
                minLines = 8
                gravity = Gravity.TOP
                setText(content)
                doAfterTextChanged { content = it?.toString().orEmpty() }
            }
            addView(bodyEt)
            val info = act.tv("上次保存：$stamp", 11f, T.ink2)
            btn("保存") {
                prefs.put("note.title", title)
                prefs.put("note.content", content)
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
                prefs.put("note.time", now)
                info.text = "上次保存：$now"
                Toast.makeText(act, "已保存", Toast.LENGTH_SHORT).show()
            }
            addView(info)
        })
        body.note("笔记只保存在本机；卸载应用会一并删除。")
    }

    // ---------------- 纪念日 ----------------
    private fun anniversary(act: ToolActivity, body: LinearLayout) {
        val prefs = Prefs.get(act)
        val listBox = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var name = ""
        var month = 0
        var day = 1
        var picking = false

        fun load(): MutableList<Triple<String, Int, Int>> {
            val out = mutableListOf<Triple<String, Int, Int>>()
            runCatching {
                val arr = JSONArray(prefs.str("ann.items", "[]"))
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(Triple(o.optString("n"), o.optInt("m"), o.optInt("d")))
                }
            }
            return out
        }

        fun save(l: List<Triple<String, Int, Int>>) {
            val arr = JSONArray()
            l.forEach { arr.put(JSONObject().put("n", it.first).put("m", it.second).put("d", it.third)) }
            prefs.put("ann.items", arr.toString())
        }

        fun render() {
            listBox.removeAllViews()
            val items = load()
            if (items.isEmpty()) listBox.addView(act.tv("还没有纪念日", 12.5f, T.ink2))
            val today = java.util.Calendar.getInstance()
            items.forEachIndexed { idx, it ->
                val c = java.util.Calendar.getInstance().apply {
                    set(today.get(java.util.Calendar.YEAR), it.second - 1, it.third, 0, 0, 0)
                }
                if (c.before(today)) c.add(java.util.Calendar.YEAR, 1)
                val days = ((c.timeInMillis - today.timeInMillis) / 86400000L).toInt()
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, act.dp(9), 0, act.dp(9))
                }
                row.addView(act.tv(it.first, 13.5f, T.ink, true), LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("${it.second}月${it.third}日 · $days 天", 12f, T.ink2))
                row.addView(act.tv("✕", 13f, T.ink3).apply {
                    setPadding(act.dp(10), 0, 0, 0)
                    isClickable = true
                    setOnClickListener {
                        val l = load().toMutableList()
                        if (idx < l.size) { l.removeAt(idx); save(l); render() }
                    }
                })
                listBox.addView(row)
                listBox.addView(act.hairline())
            }
        }

        body.addView(act.card {
            sectionTitle("添加纪念日")
            val nameEt = EditText(act).apply {
                hint = "名称，例如 在一起"
                setTextColor(T.ink); setHintTextColor(T.ink3)
                background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10))
                doAfterTextChanged { name = it?.toString().orEmpty() }
            }
            addView(nameEt)
            val monthRow = Kit.numberRow(act, this, "月份", "月")
            val dayRow = Kit.numberRow(act, this, "日期", "日")
            monthRow.setText("1"); dayRow.setText("1")
            monthRow.doAfterTextChanged { month = (it?.toString()?.toIntOrNull() ?: 1).coerceIn(1, 12) }
            dayRow.doAfterTextChanged { day = (it?.toString()?.toIntOrNull() ?: 1).coerceIn(1, 31) }
            btn("添加") {
                if (name.isBlank()) { Toast.makeText(act, "请输入名称", Toast.LENGTH_SHORT).show(); return@btn }
                val l = load().toMutableList()
                l.add(Triple(name, month.coerceAtLeast(1), day.coerceAtLeast(1)))
                save(l); render()
            }
        })
        body.addView(act.card { sectionTitle("我的纪念日"); addView(listBox) })
        render()
    }
}
