package com.suibian.treasure_vault.tools

import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** 计算换算类。 */
object CalcTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.calc, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("calculator", "科学计算器", "括号、乘方、函数与阶乘", "🧮", "计算器") { a, b -> calculator(a, b) },
        spec("base_convert", "进制转换", "二 / 八 / 十 / 十六 / 三十六进制", "🔢", "进制", "hex") { a, b -> baseConvert(a, b) },
        spec("unit_length", "长度换算", "米 / 英尺 / 市里 / 海里", "📏", "长度", "单位") { a, b ->
            units(a, b, "米", listOf(
                "毫米" to 0.001, "厘米" to 0.01, "米" to 1.0, "千米" to 1000.0,
                "市寸" to 1 / 30.0, "市尺" to 1 / 3.0, "市里" to 500.0,
                "英寸" to 0.0254, "英尺" to 0.3048, "码" to 0.9144, "英里" to 1609.344, "海里" to 1852.0,
            ))
        },
        spec("unit_weight", "重量换算", "千克 / 斤 / 两 / 磅 / 盎司", "⚖️", "重量", "kg", "斤") { a, b ->
            units(a, b, "克", listOf(
                "毫克" to 0.001, "克" to 1.0, "千克" to 1000.0, "吨" to 1e6,
                "市斤" to 500.0, "市两" to 50.0, "磅" to 453.59237, "盎司" to 28.349523125,
            ))
        },
        spec("unit_temp", "温度换算", "摄氏度 / 华氏度 / 开尔文", "🌡️", "温度", "摄氏", "华氏") { a, b -> temp(a, b) },
        spec("unit_area", "面积换算", "平方米 / 公顷 / 亩 / 英亩", "🟦", "面积", "亩") { a, b ->
            units(a, b, "平方米", listOf(
                "平方厘米" to 0.0001, "平方米" to 1.0, "平方千米" to 1e6,
                "公顷" to 10000.0, "亩" to 666.6666667, "平方英尺" to 0.09290304, "英亩" to 4046.8564224,
            ))
        },
        spec("unit_volume", "体积换算", "毫升 / 升 / 立方米 / 加仑", "🧴", "体积", "升") { a, b ->
            units(a, b, "升", listOf(
                "毫升" to 0.001, "升" to 1.0, "立方米" to 1000.0, "立方厘米" to 0.001,
                "美制加仑" to 3.785411784, "美制品脱" to 0.473176473,
            ))
        },
        spec("storage_size", "存储容量换算", "字节 / KB / MB / GB / TB", "💾", "容量", "gb") { a, b ->
            units(a, b, "字节", listOf(
                "比特" to 0.125, "字节" to 1.0, "KB" to 1024.0, "MB" to 1024.0 * 1024,
                "GB" to 1024.0 * 1024 * 1024, "TB" to 1024.0 * 1024 * 1024 * 1024,
            ))
        },
        spec("unit_all", "全能单位转换", "长度 / 重量 / 面积 / 体积 / 存储", "📐", "单位", "换算", "全能") { a, b ->
            units(a, b, "米", listOf(
                "毫米" to 0.001, "厘米" to 0.01, "米" to 1.0, "千米" to 1000.0,
                "克" to 0.001, "千克" to 1.0, "吨" to 1000.0,
                "平方米" to 1.0, "公顷" to 10000.0,
                "毫升" to 0.001, "升" to 1.0,
            ), noteText = "跨量纲仅作数值演示，请按需选择同类单位。")
        },
        spec("percent", "百分比计算", "占比 / 百分比值 / 增减幅度", "％", "百分比", "占比") { a, b -> percent(a, b) },
        spec("discount", "折扣计算", "折后价与节省金额", "🏷️", "折扣", "优惠") { a, b -> discount(a, b) },
        spec("bmi", "BMI 计算", "身高体重指数与健康区间", "❤️", "bmi", "体重") { a, b -> bmi(a, b) },
        spec("loan", "贷款计算", "等额本息 / 等额本金", "🏦", "贷款", "房贷", "月供") { a, b -> loan(a, b) },
        spec("tip", "AA 分摊", "账单、小费与人数分摊", "🍽️", "aa", "分摊") { a, b -> tip(a, b) },
        spec("roman", "罗马数字", "阿拉伯数字 ↔ 罗马数字", "🏛️", "罗马", "roman") { a, b ->
            Kit.simple(a, b, "输入 1-3999 的整数，或 IVXLCDM…", listOf(
                "数字 → 罗马" to { s -> runCatching { roman(s.trim().toInt()) }.getOrElse { "请输入 1-3999 的整数" } },
                "罗马 → 数字" to { s -> runCatching { fromRoman(s.trim().uppercase()).toString() }.getOrElse { "不是有效的罗马数字" } },
            ), lines = 3)
        },
        spec("rmb_upper", "人民币大写", "金额转财务大写", "💴", "大写", "人民币") { a, b -> rmb(a, b) },
        spec("random_number", "随机数生成", "指定区间批量生成", "🎰", "随机", "random") { a, b -> randomNumber(a, b) },
        spec("work_value", "工作性价比", "时薪与通勤折算", "💼", "时薪", "工作") { a, b -> workValue(a, b) },
        spec("number_cn", "数字转中文", "阿拉伯数字转中文读法", "🔤", "中文", "数字") { a, b ->
            Kit.simple(a, b, "输入整数或小数，例如 12345.67", listOf(
                "中文读法" to { s -> NumberCn.read(s) },
            ), lines = 3)
        },
        spec("kinship", "亲戚称呼", "推算家庭称谓", "👪", "亲戚", "称呼") { a, b -> kinship(a, b) },
    )

    // ------------------------------------------------------------------
    // 单位换算
    // ------------------------------------------------------------------

    private fun units(
        act: ToolActivity,
        body: LinearLayout,
        baseName: String,
        list: List<Pair<String, Double>>,
        noteText: String? = null,
    ) {
        var idx = 2.coerceAtMost(list.size - 1)
        var value = "1"
        val out = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var chipHost: LinearLayout? = null

        fun run() {
            out.removeAllViews()
            val v = value.toDoubleOrNull()
            if (v == null) { out.addView(act.tv("请输入数值", 12f, T.ink2)); return }
            val base = v * list[idx].second
            list.forEachIndexed { i, (name, factor) ->
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, act.dp(6), 0, act.dp(6))
                }
                row.addView(act.tv(name, 12.5f, T.ink2), LinearLayout.LayoutParams(act.dp(80), -2))
                row.addView(act.tv(Kit.fmt(base / factor), 14f, if (i == idx) T.primary else T.ink, i == idx),
                    LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("复制", 11.5f, T.primary).apply {
                    setPadding(act.dp(8), 0, 0, 0)
                    setOnClickListener { Ui.copy(act, Kit.fmt(base / factor)) }
                })
                out.addView(row)
            }
        }

        body.addView(act.card {
            sectionTitle("输入数值")
            val e = et("1", 1, singleLine = true)
            e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            e.doAfterTextChanged { value = it?.toString().orEmpty(); run() }
            sectionTitle("单位")
            chipsHost(this, list.map { it.first }, idx) { i -> idx = i; run() }
        })
        body.addView(act.card { sectionTitle("换算结果（以$baseName 为基准）"); addView(out) })
        if (noteText != null) body.note(noteText)
        run()
    }

    private fun chipsHost(parent: LinearLayout, labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
        // 单位较多，直接复用 chips（自动横向滚动）
        parent.chips(labels, selected, onPick)
    }

    private fun temp(act: ToolActivity, body: LinearLayout) {
        var unit = 0
        var value = "25"
        val out = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        fun run() {
            val v = value.toDoubleOrNull() ?: return
            val c = when (unit) {
                0 -> v
                1 -> (v - 32) * 5 / 9
                else -> v - 273.15
            }
            out.removeAllViews()
            listOf(
                "摄氏度" to Kit.fmt(c),
                "华氏度" to Kit.fmt(c * 9 / 5 + 32),
                "开尔文" to Kit.fmt(c + 273.15),
                "兰氏度" to Kit.fmt((c + 273.15) * 9 / 5),
            ).forEach { (n, v2) ->
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, act.dp(6), 0, act.dp(6)) }
                row.addView(act.tv(n, 12.5f, T.ink2), LinearLayout.LayoutParams(act.dp(80), -2))
                row.addView(act.tv(v2, 14f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
                out.addView(row)
            }
        }
        body.addView(act.card {
            sectionTitle("温度")
            val e = et("25", 1, singleLine = true)
            e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            e.doAfterTextChanged { value = it?.toString().orEmpty(); run() }
            chips(listOf("摄氏度", "华氏度", "开尔文"), 0) { i -> unit = i; run() }
        })
        body.addView(act.card { sectionTitle("结果"); addView(out) })
        run()
    }

    // ------------------------------------------------------------------
    // 计算器
    // ------------------------------------------------------------------

    private fun calculator(act: ToolActivity, body: LinearLayout) {
        val display = act.tv("0", 26f, T.ink, true).apply { gravity = Gravity.END }
        val sub = act.tv("输入算式后按 =", 13f, T.ink3).apply { gravity = Gravity.END }
        var expr = ""
        val history = mutableListOf<String>()

        fun refresh() {
            display.text = expr.ifEmpty { "0" }
        }

        fun press(k: String) {
            when (k) {
                "C" -> { expr = ""; sub.text = "输入算式后按 ="; sub.setTextColor(T.ink3) }
                "⌫" -> if (expr.isNotEmpty()) expr = expr.dropLast(1)
                "=" -> {
                    val r = runCatching { Evaluator.eval(expr) }
                    r.onSuccess {
                        sub.text = "= ${Kit.fmt(it)}"
                        sub.setTextColor(T.primary)
                        history.add(0, "$expr = ${Kit.fmt(it)}")
                        expr = Kit.fmt(it)
                    }.onFailure {
                        sub.text = it.message ?: "算式有误"
                        sub.setTextColor(T.danger)
                    }
                }
                "×" -> expr += "*"
                "÷" -> expr += "/"
                else -> expr += k
            }
            refresh()
        }

        body.addView(act.card {
            addView(display, LinearLayout.LayoutParams(-1, -2))
            addView(sub, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(6) })
            addView(act.tv("", 1f))
        })

        val rows = listOf(
            listOf("C", "(", ")", "⌫"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "^", "+"),
        )
        body.addView(act.card {
            rows.forEach { rowKeys ->
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                rowKeys.forEach { k ->
                    val isOp = "+-×÷^".contains(k) && k != "^"
                    val cell = act.tv(k, 17f, if (isOp) T.primary else T.ink, true).apply {
                        gravity = Gravity.CENTER
                        background = if (isOp) T.fillBg(T.selectedFill, act.dp(T.R_SMALL).toFloat())
                        else T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                        setPadding(0, act.dp(12), 0, act.dp(12))
                        isClickable = true
                        setOnClickListener { press(k) }
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).also {
                        it.rightMargin = act.dp(6); it.bottomMargin = act.dp(6)
                    })
                }
                addView(row)
            }
            val last = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            fun wide(label: String, weight: Float, primary: Boolean, onClick: () -> Unit) {
                val c = act.tv(label, 17f, if (primary) android.graphics.Color.WHITE else T.ink, true).apply {
                    gravity = Gravity.CENTER
                    background = if (primary) T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                    else T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                    setPadding(0, act.dp(12), 0, act.dp(12))
                    isClickable = true
                    setOnClickListener { onClick() }
                }
                last.addView(c, LinearLayout.LayoutParams(0, -2, weight).also { it.rightMargin = act.dp(6) })
            }
            wide("=", 2f, true) { press("=") }
            wide("√", 1f, false) { press("sqrt(") }
            wide("π", 1f, false) { press("pi") }
            addView(last)
        })
        if (history.isNotEmpty()) {
            body.addView(act.card {
                sectionTitle("历史记录", act.tv("清空", 12f, T.primary, true).apply {
                    isClickable = true
                    setOnClickListener { history.clear(); act.recreate() }
                })
                history.take(6).forEach { h -> addView(act.tv(h, 12.5f, T.ink2).apply { setPadding(0, act.dp(4), 0, 0) }) }
            })
        }
        body.note("支持 + - × ÷ % ^ ( )、sqrt/cbrt/sin/cos/tan/log/ln/abs，三角函数使用弧度。")
        refresh()
    }

    // ------------------------------------------------------------------
    // 百分比 / 折扣 / BMI / 贷款 / 分摊 / 工作性价比
    // ------------------------------------------------------------------

    private fun percent(act: ToolActivity, body: LinearLayout) {
        var mode = 0
        val out = act.tv("—", 20f, T.primary, true)
        fun a() = 0.0
        body.addView(act.card {
            sectionTitle("计算方式")
            chips(listOf("占比", "百分比值", "增减幅度"), 0) { i -> mode = i }
            val x = Kit.numberRow(act, this, "数值 A") { }
            val y = Kit.numberRow(act, this, "数值 B") { }
            val update = {
                val va = x.text.toString().toDoubleOrNull()
                val vb = y.text.toString().toDoubleOrNull()
                out.text = when {
                    va == null || vb == null -> "—"
                    mode == 0 -> if (vb == 0.0) "—" else "${Kit.fmt(va / vb * 100)}%"
                    mode == 1 -> Kit.fmt(va * vb / 100)
                    else -> if (va == 0.0) "—" else {
                        val r = (vb - va) / va * 100
                        "${if (r >= 0) "+" else ""}${Kit.fmt(r)}%"
                    }
                }
            }
            x.doAfterTextChanged { update() }
            y.doAfterTextChanged { update() }
            addView(act.tv("结果", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, 0) })
            addView(out)
        })
    }

    private fun discount(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("—", 24f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("输入")
            val price = Kit.numberRow(act, this, "原价", "元")
            val disc = Kit.numberRow(act, this, "折扣", "折")
            val coupon = Kit.numberRow(act, this, "优惠券", "元")
            disc.setText("8.5")
            fun update() {
                val p = price.text.toString().toDoubleOrNull()
                val d = disc.text.toString().toDoubleOrNull()
                val c = coupon.text.toString().toDoubleOrNull() ?: 0.0
                if (p == null || d == null) { out.text = "—"; sub.text = ""; return }
                val after = p * d / 10
                val pay = (after - c).coerceAtLeast(0.0)
                out.text = "¥ ${Kit.fmt(pay)}"
                sub.text = "折后 ¥${Kit.fmt(after)} · 共省 ¥${Kit.fmt(p - pay)}"
            }
            listOf(price, disc, coupon).forEach { it.doAfterTextChanged { update() } }
            addView(act.tv("实付金额", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, 0) })
            addView(out)
            addView(sub)
        })
    }

    private fun bmi(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("—", 24f, T.primary, true)
        val tag = act.tv("", 13f, T.ink2)
        body.addView(act.card {
            sectionTitle("身体数据")
            val h = Kit.numberRow(act, this, "身高", "cm")
            val w = Kit.numberRow(act, this, "体重", "kg")
            h.setText("170"); w.setText("65")
            fun update() {
                val hv = h.text.toString().toDoubleOrNull()
                val wv = w.text.toString().toDoubleOrNull()
                if (hv == null || wv == null || hv <= 0) { out.text = "—"; tag.text = ""; return }
                val m = hv / 100
                val b = wv / (m * m)
                out.text = "%.1f".format(b)
                tag.text = when {
                    b < 18.5 -> "偏瘦（< 18.5）"
                    b < 24 -> "正常（18.5 – 23.9）"
                    b < 28 -> "超重（24 – 27.9）"
                    else -> "肥胖（≥ 28）"
                }
            }
            h.doAfterTextChanged { update() }
            w.doAfterTextChanged { update() }
            addView(act.tv("BMI 指数", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, 0) })
            addView(out); addView(tag)
        })
        body.note("BMI 仅作粗略参考，运动员、孕妇与未成年人不宜直接套用。")
    }

    private fun loan(act: ToolActivity, body: LinearLayout) {
        var type = 0
        val out = act.tv("—", 22f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("还款方式")
            chips(listOf("等额本息", "等额本金"), 0) { i -> type = i }
            val amount = Kit.numberRow(act, this, "金额", "万元")
            val rate = Kit.numberRow(act, this, "年利率", "%")
            val months = Kit.numberRow(act, this, "期限", "月")
            amount.setText("100"); rate.setText("3.1"); months.setText("360")
            fun update() {
                val p0 = amount.text.toString().toDoubleOrNull() ?: 0.0
                val r0 = rate.text.toString().toDoubleOrNull() ?: 0.0
                val n = months.text.toString().toIntOrNull() ?: 0
                if (p0 <= 0 || n <= 0) { out.text = "—"; sub.text = ""; return }
                val p = p0 * 10000
                val r = r0 / 100 / 12
                if (type == 0) {
                    val m = if (r == 0.0) p / n else p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
                    val total = m * n
                    out.text = "¥ ${fmtMoney(m)}"
                    sub.text = "总利息 ¥${fmtMoney(total - p)} · 还款总额 ¥${fmtMoney(total)}"
                } else {
                    val principal = p / n
                    val first = principal + p * r
                    val last = principal + principal * r
                    val interest = p * r * (n + 1) / 2
                    out.text = "¥ ${fmtMoney(first)}"
                    sub.text = "首月，末月 ¥${fmtMoney(last)} · 总利息 ¥${fmtMoney(interest)}"
                }
            }
            listOf(amount, rate, months).forEach { it.doAfterTextChanged { update() } }
            addView(act.tv("月供", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, 0) })
            addView(out); addView(sub)
        })
    }

    private fun tip(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("—", 24f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("账单")
            val bill = Kit.numberRow(act, this, "金额", "元")
            val percent = Kit.numberRow(act, this, "小费", "%")
            val people = Kit.numberRow(act, this, "人数", "人")
            percent.setText("10"); people.setText("2")
            fun update() {
                val b = bill.text.toString().toDoubleOrNull()
                val t = percent.text.toString().toDoubleOrNull() ?: 0.0
                val n = people.text.toString().toIntOrNull() ?: 1
                if (b == null || n <= 0) { out.text = "—"; sub.text = ""; return }
                val total = b * (1 + t / 100)
                out.text = "¥ ${fmtMoney(total / n)}"
                sub.text = "合计 ¥${fmtMoney(total)} · 小费 ¥${fmtMoney(b * t / 100)}"
            }
            listOf(bill, percent, people).forEach { it.doAfterTextChanged { update() } }
            addView(act.tv("每人应付", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, 0) })
            addView(out); addView(sub)
        })
    }

    private fun workValue(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("—", 24f, T.primary, true)
        val sub = act.tv("", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("工作参数")
            val salary = Kit.numberRow(act, this, "月薪", "元")
            val days = Kit.numberRow(act, this, "月工作", "天")
            val hours = Kit.numberRow(act, this, "每天", "小时")
            val commute = Kit.numberRow(act, this, "通勤", "小时")
            salary.setText("10000"); days.setText("22"); hours.setText("8"); commute.setText("1.5")
            fun update() {
                val s = salary.text.toString().toDoubleOrNull()
                val d = days.text.toString().toDoubleOrNull() ?: 0.0
                val h = hours.text.toString().toDoubleOrNull() ?: 0.0
                val c = commute.text.toString().toDoubleOrNull() ?: 0.0
                if (s == null || d <= 0 || h + c <= 0) { out.text = "—"; sub.text = ""; return }
                val hourly = s / (d * (h + c))
                out.text = "¥ ${fmtMoney(hourly)} / 小时"
                sub.text = "按含通勤 ${Kit.fmt(h + c)} 小时折算；不含通勤 ¥${fmtMoney(s / (d * h))}/小时"
            }
            listOf(salary, days, hours, commute).forEach { it.doAfterTextChanged { update() } }
            addView(act.tv("时间单价", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, 0) })
            addView(out); addView(sub)
        })
    }

    // ------------------------------------------------------------------
    // 进制 / 大写 / 随机 / 亲戚
    // ------------------------------------------------------------------

    private fun baseConvert(act: ToolActivity, body: LinearLayout) {
        var from = 10
        val out = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var input: EditText? = null
        fun run() {
            out.removeAllViews()
            val s = input?.text?.toString()?.trim()?.replace(" ", "") ?: return
            if (s.isEmpty()) return
            val v = runCatching { java.math.BigInteger(s, from) }.getOrNull()
            if (v == null) { out.addView(act.tv("当前进制下无法解析该数字", 12f, T.ink2)); return }
            listOf(2, 8, 10, 16, 36).forEach { b ->
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, act.dp(6), 0, act.dp(6)) }
                row.addView(act.tv("$b 进制", 12f, T.ink2), LinearLayout.LayoutParams(act.dp(62), -2))
                val txt = v.toString(b).uppercase()
                row.addView(act.tv(txt, 13.5f, if (b == from) T.primary else T.ink, b == from),
                    LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("复制", 11.5f, T.primary).apply {
                    setPadding(act.dp(8), 0, 0, 0)
                    setOnClickListener { Ui.copy(act, txt) }
                })
                out.addView(row)
            }
        }
        body.addView(act.card {
            sectionTitle("输入")
            val e = et("255", 1, singleLine = true)
            e.doAfterTextChanged { run() }
            input = e
            chips(listOf("2", "8", "10", "16", "36"), 2) { i ->
                from = listOf(2, 8, 10, 16, 36)[i]; run()
            }
        })
        body.addView(act.card { sectionTitle("各进制结果"); addView(out) })
        run()
    }

    private fun rmb(act: ToolActivity, body: LinearLayout) {
        Kit.simple(act, body, "输入金额，例如 12345.67", listOf(
            "人民币大写" to { s -> NumberCn.rmb(s.toDoubleOrNull() ?: 0.0) },
        ), lines = 2, outputLabel = "大写金额")
    }

    private fun randomNumber(act: ToolActivity, body: LinearLayout) {
        val out = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("参数")
            val min = Kit.numberRow(act, this, "最小值")
            val max = Kit.numberRow(act, this, "最大值")
            val count = Kit.numberRow(act, this, "数量")
            min.setText("1"); max.setText("100"); count.setText("5")
            btn("生成") {
                out.removeAllViews()
                val a = min.text.toString().toIntOrNull() ?: 1
                val b = max.text.toString().toIntOrNull() ?: 100
                val n = (count.text.toString().toIntOrNull() ?: 5).coerceIn(1, 200)
                val lo = minOf(a, b); val hi = maxOf(a, b)
                val rnd = java.util.Random()
                val listV = (1..n).map { lo + rnd.nextInt(hi - lo + 1) }
                val wrap = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                listV.chunked(4).forEach { chunk ->
                    val row = LinearLayout(act)
                    chunk.forEach { num ->
                        row.addView(act.tv("$num", 16f, T.primary, true).apply {
                            gravity = Gravity.CENTER
                            background = T.fillBg(T.selectedFill, act.dp(T.R_SMALL).toFloat())
                            setPadding(act.dp(12), act.dp(8), act.dp(12), act.dp(8))
                        }, LinearLayout.LayoutParams(-2, -2).also { it.rightMargin = act.dp(8) })
                    }
                    out.addView(row, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(8) })
                }
            }
        })
        body.addView(act.card { sectionTitle("结果"); addView(out) })
    }

    private fun kinship(act: ToolActivity, body: LinearLayout) {
        Kit.simple(act, body, "例如：爸爸的哥哥、妈妈的妹妹的女儿、爷爷的弟弟", listOf(
            "推算称呼" to { s -> Kinship.calc(s) },
        ), lines = 2, outputLabel = "称谓")
        body.note("支持父母、兄弟姐妹、子女、配偶的组合推导；超出词表会给出最接近的称谓。")
    }

    private fun fmtMoney(v: Double): String {
        val scaled = (v * 100).roundToLong() / 100.0
        return if (scaled == scaled.toLong().toDouble()) "%,d".format(scaled.toLong())
        else "%,.2f".format(scaled)
    }

    private fun roman(n: Int): String {
        require(n in 1..3999) { "out of range" }
        val map = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
        )
        var v = n
        val sb = StringBuilder()
        for ((value, sym) in map) {
            while (v >= value) { sb.append(sym); v -= value }
        }
        return sb.toString()
    }

    private fun fromRoman(s: String): Int {
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var total = 0
        var prev = 0
        for (c in s.reversed()) {
            val v = map[c] ?: error("bad char")
            total += if (v < prev) -v else v
            if (v > prev) prev = v
        }
        require(total in 1..3999)
        return total
    }
}

/** 表达式求值（递归下降）。 */
object Evaluator {
    fun eval(src: String): Double {
        val p = P(src)
        val v = p.expr()
        p.skip()
        require(p.atEnd) { "无法识别：${src.substring(p.pos)}" }
        require(!v.isNaN() && !v.isInfinite()) { "结果无效" }
        return v
    }

    private class P(val s: String) {
        var pos = 0
        val atEnd get() = pos >= s.length
        fun skip() { while (pos < s.length && s[pos] == ' ') pos++ }
        fun eat(c: Char): Boolean { skip(); if (pos < s.length && s[pos] == c) { pos++; return true }; return false }

        fun expr(): Double {
            var v = term()
            while (true) {
                skip()
                when {
                    eat('+') -> v += term()
                    eat('-') -> v -= term()
                    else -> return v
                }
            }
        }

        fun term(): Double {
            var v = factor()
            while (true) {
                skip()
                if (pos >= s.length) return v
                when (s[pos]) {
                    '*' -> { pos++; v *= factor() }
                    '/' -> { pos++; val d = factor(); require(d != 0.0) { "除数不能为 0" }; v /= d }
                    '%' -> { pos++; val d = factor(); require(d != 0.0) { "除数不能为 0" }; v %= d }
                    else -> return v
                }
            }
        }

        fun factor(): Double {
            var v = unary()
            skip()
            if (eat('^')) v = Math.pow(v, factor())
            if (eat('!')) v = factorial(v)
            return v
        }

        fun unary(): Double {
            skip()
            if (eat('-')) return -unary()
            if (eat('+')) return unary()
            return primary()
        }

        fun primary(): Double {
            skip()
            if (eat('(')) { val v = expr(); require(eat(')')) { "括号不匹配" }; return v }
            if (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) {
                val start = pos
                while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
                return s.substring(start, pos).toDoubleOrNull() ?: error("数字无效")
            }
            if (pos < s.length && s[pos].isLetter()) {
                val start = pos
                while (pos < s.length && s[pos].isLetter()) pos++
                val name = s.substring(start, pos).lowercase()
                if (name == "pi") return Math.PI
                if (name == "e") return Math.E
                require(eat('(')) { "函数 $name 需要括号" }
                val arg = expr()
                require(eat(')')) { "括号不匹配" }
                return apply(name, arg)
            }
            error("表达式不完整")
        }

        fun apply(name: String, x: Double): Double = when (name) {
            "sqrt" -> Math.sqrt(x)
            "cbrt" -> Math.cbrt(x)
            "sin" -> Math.sin(x)
            "cos" -> Math.cos(x)
            "tan" -> Math.tan(x)
            "log" -> Math.log10(x)
            "ln" -> Math.log(x)
            "abs" -> Math.abs(x)
            "round" -> Math.round(x).toDouble()
            "floor" -> Math.floor(x)
            "ceil" -> Math.ceil(x)
            "exp" -> Math.exp(x)
            else -> error("未知函数：$name")
        }

        fun factorial(x: Double): Double {
            require(x >= 0 && x == Math.floor(x) && x <= 170) { "阶乘仅支持 0-170 的整数" }
            var r = 1.0
            for (i in 2..x.toInt()) r *= i
            return r
        }
    }
}

/** 数字与人民币中文转换。 */
object NumberCn {
    private val digits = "零一二三四五六七八九"

    fun read(raw: String): String {
        val v = raw.toDoubleOrNull() ?: return "请输入数字"
        val neg = v < 0
        val abs = abs(v)
        val intPart = abs.toLong()
        val decPart = abs - intPart
        val sb = StringBuilder(if (neg) "负" else "")
        sb.append(intCn(intPart))
        if (decPart > 1e-9) {
            sb.append("点")
            val ds = "%.4f".format(decPart).removePrefix("0").removeSuffix("0")
            ds.drop(1).forEach { sb.append(digits[it - '0']) }
        }
        return sb.toString()
    }

    private fun intCn(n: Long): String {
        if (n == 0L) return "零"
        val units = listOf("", "十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千")
        val s = n.toString()
        val sb = StringBuilder()
        var zero = false
        s.forEachIndexed { i, c ->
            val d = c - '0'
            val unitIdx = s.length - i - 1
            if (d == 0) {
                zero = sb.isNotEmpty()
            } else {
                if (zero) { sb.append("零"); zero = false }
                sb.append(digits[d])
                if (unitIdx < units.size) sb.append(units[unitIdx])
            }
            if (unitIdx == 4 && sb.isNotEmpty()) sb.append("万")
            if (unitIdx == 8 && sb.isNotEmpty()) sb.append("亿")
        }
        return sb.toString()
    }

    fun rmb(v: Double): String {
        if (v < 0) return "金额不能为负"
        if (v >= 1e16) return "金额过大"
        val cents = Math.round(v * 100)
        val yuan = cents / 100
        val jiao = (cents % 100 / 10).toInt()
        val fen = (cents % 10).toInt()
        val cap = "零壹贰叁肆伍陆柒捌玖"
        fun upper(n: Long): String {
            if (n == 0L) return "零"
            val units = listOf("", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿")
            val s = n.toString()
            val sb = StringBuilder()
            var zero = false
            s.forEachIndexed { i, c ->
                val d = c - '0'
                val ui = s.length - i - 1
                if (d == 0) { zero = sb.isNotEmpty() } else {
                    if (zero) { sb.append("零"); zero = false }
                    sb.append(cap[d])
                    if (ui < units.size) sb.append(units[ui])
                }
            }
            return sb.toString().trimEnd('零')
        }
        val sb = StringBuilder()
        sb.append(if (yuan == 0L) "零元" else upper(yuan) + "元")
        if (jiao == 0 && fen == 0) sb.append("整")
        else {
            if (jiao > 0) sb.append(cap[jiao]).append("角")
            else if (yuan > 0 && fen > 0) sb.append("零")
            if (fen > 0) sb.append(cap[fen]).append("分")
        }
        return sb.toString()
    }
}

/** 极简亲戚称呼推算：沿称呼链走关系图。 */
object Kinship {
    private val graph = mapOf(
        "爸爸" to "father", "父亲" to "father", "妈妈" to "mother", "母亲" to "mother",
        "哥哥" to "brother", "弟弟" to "brother", "姐姐" to "sister", "妹妹" to "sister",
        "儿子" to "son", "女儿" to "daughter", "老公" to "husband", "丈夫" to "husband",
        "老婆" to "wife", "妻子" to "wife",
    )

    private val names = mapOf(
        "father" to "爸爸", "mother" to "妈妈", "brother" to "兄弟", "sister" to "姐妹",
        "son" to "儿子", "daughter" to "女儿", "husband" to "丈夫", "wife" to "妻子",
        "father's father" to "爷爷", "father's mother" to "奶奶",
        "mother's father" to "外公", "mother's mother" to "外婆",
        "father's brother" to "叔伯", "father's sister" to "姑姑",
        "mother's brother" to "舅舅", "mother's sister" to "姨妈",
        "brother's son" to "侄子", "brother's daughter" to "侄女",
        "sister's son" to "外甥", "sister's daughter" to "外甥女",
        "son's son" to "孙子", "son's daughter" to "孙女",
        "daughter's son" to "外孙", "daughter's daughter" to "外孙女",
    )

    fun calc(input: String): String {
        val chain = input.split("的").map { it.trim() }.filter { it.isNotEmpty() }
        if (chain.isEmpty()) return "请输入称呼关系"
        val path = chain.map { graph[it] ?: return "暂不认识的称呼：$it" }
        if (path.size == 1) return names[path[0]] ?: path[0]
        val key = path.take(2).joinToString("'s ")
        names[key]?.let { return it }
        // 直接亲属的叠加
        val first = path[0]
        val second = path[1]
        val special = when {
            first == "father" && second == "father" -> "爷爷"
            first == "father" && second == "mother" -> "奶奶"
            first == "mother" && second == "father" -> "外公"
            first == "mother" && second == "mother" -> "外婆"
            first == "father" && second == "brother" -> "叔伯"
            first == "father" && second == "sister" -> "姑姑"
            first == "mother" && second == "brother" -> "舅舅"
            first == "mother" && second == "sister" -> "姨妈"
            first == "brother" && second == "son" -> "侄子"
            first == "brother" && second == "daughter" -> "侄女"
            first == "sister" && second == "son" -> "外甥"
            first == "sister" && second == "daughter" -> "外甥女"
            first == "son" && second == "son" -> "孙子"
            first == "son" && second == "daughter" -> "孙女"
            first == "daughter" && second == "son" -> "外孙"
            first == "daughter" && second == "daughter" -> "外孙女"
            else -> null
        }
        if (special != null) return special
        return "较远亲属（${path.joinToString(" → ") { names[it] ?: it }}）"
    }
}
