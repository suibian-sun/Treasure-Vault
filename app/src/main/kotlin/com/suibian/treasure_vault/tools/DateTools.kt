package com.suibian.treasure_vault.tools

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 日期时间类（7 个）。 */
object DateTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.date, emoji, kw.toList(), build = build)

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    val all: List<ToolSpec> = listOf(
        spec("timestamp", "时间戳转换", "秒 / 毫秒与本地时间互转", "⏱️", "时间戳", "unix") { a, b -> timestamp(a, b) },
        spec("date_diff", "日期相差", "天数 / 周数 / 工作日", "📆", "日期差") { a, b -> dateDiff(a, b) },
        spec("age", "年龄计算", "精确到年月日，附生肖星座", "🎂", "年龄", "生日") { a, b -> age(a, b) },
        spec("countdown", "倒数日", "距离目标日期还有多少天", "⏳", "倒数", "纪念日") { a, b -> countdown(a, b) },
        spec("date_query", "日期查询", "星期、周数、闰年", "🗓️", "星期", "闰年") { a, b -> dateQuery(a, b) },
        spec("date_calc", "日期计算器", "日期加减天数 / 推算星期", "➕", "日期", "计算") { a, b -> dateCalc(a, b) },
        spec("time_calc", "时间计算器", "两个时刻相减 / 时长累加", "🕰️", "时间", "时长") { a, b -> timeCalc(a, b) },
    )

    private fun pickDate(act: ToolActivity, cal: Calendar, onPick: (Calendar) -> Unit) {
        DatePickerDialog(
            act,
            { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d)
                onPick(c)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun dateRow(act: ToolActivity, parent: LinearLayout, label: String, cal: Calendar, onPick: () -> Unit) {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
            setPadding(act.dp(14), act.dp(12), act.dp(14), act.dp(12))
            isClickable = true
            setOnClickListener { onPick() }
        }
        row.addView(act.tv(label, 13f, T.ink2), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(act.tv(dayFmt.format(cal.time), 14f, T.ink, true))
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(8) })
    }

    private fun timestamp(act: ToolActivity, body: LinearLayout) {
        var input = "${System.currentTimeMillis()}"
        var isMs = true
        val out = act.tv("", 13f, T.ink).apply {
            typeface = Typeface.MONOSPACE
            background = T.fieldBg()
            setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
        }
        val picked = Calendar.getInstance()

        fun runTs() {
            val n = input.toLongOrNull()
            if (n == null) { out.text = "请输入数字时间戳"; return }
            val ms = if (input.length <= 10) n * 1000 else n
            val d = Date(ms)
            out.text = "本地：${fmt.format(d)}\nUTC ：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(d)}\n秒  ：${ms / 1000}\n毫秒：$ms"
        }

        body.addView(act.card {
            sectionTitle("时间戳 → 时间")
            val e = et("1730000000", 1, singleLine = true)
            e.setText(input)
            e.doAfterTextChanged { input = it?.toString().orEmpty(); runTs() }
            btn("使用当前时间") {
                input = "${System.currentTimeMillis()}"; e.setText(input); runTs()
            }
        })
        body.addView(act.card { sectionTitle("转换结果"); addView(out) })

        val dateOut = act.tv("", 18f, T.primary, true)
        body.addView(act.card {
            sectionTitle("时间 → 时间戳")
            dateRow(act, this, "选择日期", picked) {
                pickDate(act, picked) {
                    picked.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH),
                        picked.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.MINUTE))
                    act.recreate()
                }
            }
            chips(listOf("秒（10 位）", "毫秒（13 位）"), 1) { i -> isMs = i == 1; act.recreate() }
            val ts = if (isMs) picked.timeInMillis else picked.timeInMillis / 1000
            dateOut.text = "$ts"
            addView(dateOut)
        })
        runTs()
    }

    private fun dateDiff(act: ToolActivity, body: LinearLayout) {
        val a = Calendar.getInstance()
        val b = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }
        val out = act.tv("", 20f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)

        fun days(): Long {
            val start = minOf(a.timeInMillis, b.timeInMillis)
            val end = maxOf(a.timeInMillis, b.timeInMillis)
            return (end - start) / 86400000L
        }

        fun workdays(): Int {
            val start = if (a.before(b)) a else b
            val end = if (a.before(b)) b else a
            val c = Calendar.getInstance().apply {
                timeInMillis = start.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            val last = Calendar.getInstance().apply {
                timeInMillis = end.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            var count = 0
            while (c.before(last)) {
                c.add(Calendar.DAY_OF_MONTH, 1)
                val dow = c.get(Calendar.DAY_OF_WEEK)
                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) count++
            }
            return count
        }

        fun refresh() {
            out.text = "${days()} 天"
            sub.text = "约 ${"%.1f".format(days() / 7.0)} 周 · 工作日 ${workdays()} 天"
        }

        body.addView(act.card {
            sectionTitle("选择两个日期")
            dateRow(act, this, "起始", a) { pickDate(act, a) { a.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH)); refresh() } }
            dateRow(act, this, "结束", b) { pickDate(act, b) { b.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH)); refresh() } }
        })
        body.addView(act.card {
            sectionTitle("相差")
            addView(out)
            addView(sub)
        })
        body.note("工作日仅按周一至周五计算，未扣除法定节假日。")
        refresh()
    }

    private fun age(act: ToolActivity, body: LinearLayout) {
        val birth = Calendar.getInstance().apply { set(2000, 0, 1) }
        val out = act.tv("", 22f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)

        fun refresh() {
            val now = Calendar.getInstance()
            var years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            var months = now.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
            var days = now.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)
            if (days < 0) { months--; days += 30 }
            if (months < 0) { years--; months += 12 }
            val totalDays = ((now.timeInMillis - birth.timeInMillis) / 86400000L).toInt()
            val zodiac = "鼠牛虎兔龙蛇马羊猴鸡狗猪"[(birth.get(Calendar.YEAR) - 1900) % 12]
            out.text = "$years 岁"
            sub.text = "$months 个月 $days 天 · 共 $totalDays 天 · 生肖 $zodiac"
        }

        body.addView(act.card {
            sectionTitle("出生日期")
            dateRow(act, this, "生日", birth) {
                pickDate(act, birth) {
                    birth.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
                    refresh()
                }
            }
        })
        body.addView(act.card { sectionTitle("年龄"); addView(out); addView(sub) })
        refresh()
    }

    private fun countdown(act: ToolActivity, body: LinearLayout) {
        val target = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }
        val out = act.tv("", 24f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)
        fun refresh() {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            val d = ((target.timeInMillis - today.timeInMillis) / 86400000L).toInt()
            out.text = if (d >= 0) "$d 天" else "${-d} 天前"
            sub.text = if (d >= 0) "距离目标还有" else "已经过去"
        }
        body.addView(act.card {
            sectionTitle("目标日期")
            dateRow(act, this, "日期", target) {
                pickDate(act, target) {
                    target.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
                    refresh()
                }
            }
        })
        body.addView(act.card { sectionTitle("倒数"); addView(out); addView(sub) })
        refresh()
    }

    private fun dateQuery(act: ToolActivity, body: LinearLayout) {
        val date = Calendar.getInstance()
        val out = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        fun refresh() {
            out.removeAllViews()
            val week = "日一二三四五六"[date.get(Calendar.DAY_OF_WEEK) - 1]
            val doy = date.get(Calendar.DAY_OF_YEAR)
            val weeks = (doy + 6) / 7
            fun kv(k: String, v: String) {
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, act.dp(6), 0, act.dp(6)) }
                row.addView(act.tv(k, 12.5f, T.ink2), LinearLayout.LayoutParams(act.dp(90), -2))
                row.addView(act.tv(v, 13.5f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
                out.addView(row)
            }
            kv("星期", "星期$week")
            kv("年内第几天", "$doy")
            kv("第几周", "第 $weeks 周")
            kv("本月天数", "${date.getActualMaximum(Calendar.DAY_OF_MONTH)} 天")
            kv("闰年", if (date.getActualMaximum(Calendar.DAY_OF_YEAR) == 366) "是" else "否")
        }
        body.addView(act.card {
            sectionTitle("选择日期")
            dateRow(act, this, "日期", date) {
                pickDate(act, date) {
                    date.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
                    refresh()
                }
            }
        })
        body.addView(act.card { sectionTitle("查询结果"); addView(out) })
        refresh()
    }

    private fun dateCalc(act: ToolActivity, body: LinearLayout) {
        val base = Calendar.getInstance()
        var offset = 30
        val out = act.tv("", 20f, T.primary, true)
        fun refresh() {
            val c = Calendar.getInstance().apply {
                timeInMillis = base.timeInMillis
                add(Calendar.DAY_OF_MONTH, offset)
            }
            val week = "日一二三四五六"[c.get(Calendar.DAY_OF_WEEK) - 1]
            out.text = dayFmt.format(c.time) + "  星期$week"
        }
        body.addView(act.card {
            sectionTitle("基准日期")
            dateRow(act, this, "日期", base) {
                pickDate(act, base) {
                    base.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
                    refresh()
                }
            }
            sectionTitle("偏移天数")
            chips(listOf("+7", "+30", "+100", "-7", "-30"), 1) { i ->
                offset = listOf(7, 30, 100, -7, -30)[i]; refresh()
            }
            val e = et("30", 1, singleLine = true)
            e.doAfterTextChanged { offset = it?.toString()?.toIntOrNull() ?: 0; refresh() }
        })
        body.addView(act.card { sectionTitle("结果"); addView(out) })
        refresh()
    }

    private fun timeCalc(act: ToolActivity, body: LinearLayout) {
        var h1 = "09:00"; var h2 = "18:30"
        val out = act.tv("", 20f, T.primary, true)
        fun mins(s: String): Int? {
            val p = s.split(":")
            val h = p.getOrNull(0)?.toIntOrNull() ?: return null
            val m = p.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }
        fun refresh() {
            val a = mins(h1) ?: return
            val b = mins(h2) ?: return
            val diff = (b - a + 1440) % 1440
            out.text = "${diff / 60} 小时 ${diff % 60} 分钟"
        }
        body.addView(act.card {
            sectionTitle("两个时刻")
            val e1 = et("09:00", 1, singleLine = true)
            val e2 = et("18:30", 1, singleLine = true)
            e1.setText(h1); e2.setText(h2)
            e1.doAfterTextChanged { h1 = it?.toString().orEmpty(); refresh() }
            e2.doAfterTextChanged { h2 = it?.toString().orEmpty(); refresh() }
        })
        body.addView(act.card { sectionTitle("时长"); addView(out) })
        body.note("跨零点自动按次日计算（结果对 24 小时取模）。")
        refresh()
    }
}
