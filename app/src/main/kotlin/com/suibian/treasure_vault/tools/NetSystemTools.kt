package com.suibian.treasure_vault.tools

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Toast
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 网络查询 + 系统工具（全部使用免 Key 接口）。 */
object NetSystemTools {

    private fun spec(cat: com.suibian.treasure_vault.core.Category, id: String, name: String, desc: String,
                     emoji: String, vararg kw: String, build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, cat, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec(Cats.net, "translate", "翻译", "中英互译，免 Key 接口", "🌍", "翻译", "translate") { a, b -> translate(a, b) },
        spec(Cats.net, "currency", "汇率转换", "实时汇率，支持常用币种", "💱", "汇率", "currency") { a, b -> currency(a, b) },
        spec(Cats.net, "ip_query", "全球 IP 查询", "查询 IP 归属与运营商", "🌐", "ip", "归属地") { a, b -> ipQuery(a, b) },
        spec(Cats.net, "domain", "域名信息查询", "RDAP 查询域名注册信息", "🔖", "域名", "rdap") { a, b -> domain(a, b) },
        spec(Cats.net, "history_today", "历史上的今天", "查看今天发生的大事", "📜", "历史", "今天") { a, b -> historyToday(a, b) },
        spec(Cats.system, "apps", "应用管理", "查看、打开、卸载已安装应用", "📦", "应用", "卸载") { a, b -> apps(a, b) },
        spec(Cats.system, "vibrator", "震动器", "几种震动模式", "📳", "震动", "vibrate") { a, b -> vibrator(a, b) },
        spec(Cats.system, "wallpaper", "设置壁纸", "把图片设为桌面 / 锁屏壁纸", "🖼️", "壁纸", "wallpaper") { a, b -> wallpaper(a, b) },
        spec(Cats.system, "garbage", "垃圾分类查询", "常见垃圾分类速查", "♻️", "垃圾", "分类") { a, b -> garbage(a, b) },
    )

    // ---------------- 网络基础 ----------------
    private fun http(url: String, timeout: Int = 12000): String? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeout
        c.readTimeout = timeout
        c.setRequestProperty("User-Agent", "TreasureVault/2.0")
        if (c.responseCode != 200) return null
        c.inputStream.bufferedReader().readText()
    }.getOrNull()

    private val ui = Handler(Looper.getMainLooper())

    private fun runAsync(act: ToolActivity, out: android.widget.TextView, loading: String = "查询中…", block: () -> String) {
        out.text = loading
        Thread {
            val r = runCatching(block).getOrElse { "查询失败：${it.message}" }
            ui.post { out.text = r }
        }.start()
    }

    // ---------------- 翻译 ----------------
    private fun translate(act: ToolActivity, body: LinearLayout) {
        var to = "en"
        val out = act.tv("—", 13.5f, T.ink).apply {
            background = T.fieldBg()
            setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
            setTextIsSelectable(true)
        }
        body.addView(act.card {
            sectionTitle("翻译")
            val input = et("输入要翻译的文本…", 5)
            chips(listOf("中 → 英", "英 → 中", "自动 → 中"), 0) { i ->
                to = listOf("en", "zh-CN", "zh-CN")[i]
            }
            btn("翻译") {
                val text = input.text?.toString()?.trim().orEmpty()
                if (text.isEmpty()) { Toast.makeText(act, "请先输入内容", Toast.LENGTH_SHORT).show(); return@btn }
                runAsync(act, out) {
                    val q = URLEncoder.encode(text, "UTF-8")
                    val json = http("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$to&dt=t&q=$q")
                        ?: return@runAsync "网络不可用，请稍后重试"
                    val arr = org.json.JSONArray(json).getJSONArray(0)
                    buildString {
                        for (i in 0 until arr.length()) {
                            val seg = arr.optJSONArray(i) ?: continue
                            append(seg.optString(0))
                        }
                    }.ifBlank { "没有拿到翻译结果" }
                }
            }
            addView(act.tv("译文", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, act.dp(6)) })
            addView(out)
        })
        body.note("使用免 Key 的公共翻译接口，长文本可能被截断；敏感内容请勿提交。")
    }

    // ---------------- 汇率 ----------------
    private fun currency(act: ToolActivity, body: LinearLayout) {
        val codes = listOf("CNY", "USD", "EUR", "JPY", "HKD", "GBP", "KRW")
        var from = 0
        var to = 1
        val out = act.tv("—", 22f, T.primary, true)
        val rateLine = act.tv("", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("币种")
            chips(codes, from) { i -> from = i }
            chips(codes, to) { i -> to = i }
            val amount = Kit.numberRow(act, this, "金额", codes[from])
            amount.setText("100")
            btn("换算") {
                runAsync(act, out, "获取汇率…") {
                    val a = amount.text.toString().toDoubleOrNull() ?: 1.0
                    val json = http("https://api.frankfurter.app/latest?from=${codes[from]}&to=${codes[to]}")
                        ?: return@runAsync "汇率接口不可用"
                    val o = JSONObject(json)
                    val rate = o.getJSONObject("rates").getDouble(codes[to])
                    rateLine.text = "1 ${codes[from]} = ${Kit.fmt(rate)} ${codes[to]}（${o.optString("date")}）"
                    "%.2f".format(a * rate) + " " + codes[to]
                }
            }
            addView(act.tv("结果", 11.5f, T.ink2).apply { setPadding(0, act.dp(10), 0, act.dp(6)) })
            addView(out)
            addView(rateLine)
        })
        body.note("数据来自欧洲央行公开汇率（frankfurter.app），不含人民币以外的部分小币种。")
    }

    // ---------------- IP ----------------
    private fun ipQuery(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("—", 13f, T.ink)
        body.addView(act.card {
            sectionTitle("IP / 本机")
            val input = et("留空查询本机公网 IP", 1, singleLine = true)
            btn("查询") {
                runAsync(act, out) {
                    val q = input.text?.toString()?.trim().orEmpty()
                    val url = if (q.isEmpty()) "https://ipapi.co/json/" else "https://ipapi.co/$q/json/"
                    val json = http(url) ?: return@runAsync "查询失败，请检查网络"
                    val o = JSONObject(json)
                    if (o.has("error")) return@runAsync "查询失败：${o.optString("reason")}"
                    buildString {
                        append("IP：").append(o.optString("ip")).append('\n')
                        append("城市：").append(o.optString("city")).append(' ')
                        append(o.optString("region")).append(' ')
                        append(o.optString("country_name")).append('\n')
                        append("运营商：").append(o.optString("org")).append('\n')
                        append("时区：").append(o.optString("timezone"))
                    }
                }
            }
            addView(out)
        })
    }

    // ---------------- 域名 ----------------
    private fun domain(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("—", 12.5f, T.ink).apply {
            background = T.fieldBg(); setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
        }
        body.addView(act.card {
            sectionTitle("域名")
            val input = et("example.com", 1, singleLine = true)
            btn("查询 RDAP") {
                val d = input.text?.toString()?.trim().orEmpty()
                if (d.isEmpty()) return@btn
                runAsync(act, out) {
                    val json = http("https://rdap.org/domain/$d", 15000) ?: return@runAsync "RDAP 查询失败"
                    val o = JSONObject(json)
                    val sb = StringBuilder()
                    sb.append("域名：").append(o.optString("ldhName")).append('\n')
                    val events = o.optJSONArray("events")
                    if (events != null) {
                        for (i in 0 until events.length()) {
                            val e = events.getJSONObject(i)
                            sb.append(e.optString("eventAction")).append("：")
                                .append(e.optString("eventDate").take(10)).append('\n')
                        }
                    }
                    val ns = o.optJSONArray("nameservers")
                    if (ns != null && ns.length() > 0) {
                        sb.append("NS：")
                        for (i in 0 until ns.length()) {
                            sb.append(ns.getJSONObject(i).optString("ldhName")).append("  ")
                        }
                    }
                    sb.toString().trim()
                }
            }
            addView(out)
        })
    }

    // ---------------- 历史上的今天 ----------------
    private fun historyToday(act: ToolActivity, body: LinearLayout) {
        val out = act.tv("点击下方按钮加载", 12.5f, T.ink).apply { setPadding(0, act.dp(6), 0, 0) }
        body.addView(act.card {
            sectionTitle("历史上的今天")
            btn("加载今天的事件") {
                runAsync(act, out) {
                    val json = http("https://api.oick.cn/lishi/api.php") ?: return@runAsync "接口暂不可用"
                    val o = JSONObject(json)
                    val arr = o.optJSONArray("result") ?: return@runAsync "暂无数据"
                    val sb = StringBuilder()
                    for (i in 0 until minOf(arr.length(), 15)) {
                        val item = arr.optJSONObject(i) ?: continue
                        sb.append("· ").append(item.optString("date")).append('\n')
                            .append("  ").append(item.optString("title")).append("\n\n")
                    }
                    sb.toString().trim().ifEmpty { "暂无数据" }
                }
            }
            addView(out)
        })
    }

    // ---------------- 应用管理 ----------------
    private fun apps(act: ToolActivity, body: LinearLayout) {
        val pm = act.packageManager
        val list = pm.getInstalledApplications(0).sortedBy { pm.getApplicationLabel(it).toString() }
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var shown = 0
        lateinit var addBatch: () -> Unit
        val more = act.tv("加载更多", 13f, T.primary, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, act.dp(10), 0, act.dp(10))
            isClickable = true
            setOnClickListener { addBatch() }
        }
        addBatch = {
            val end = minOf(shown + 30, list.size)
            for (i in shown until end) {
                val info = list[i]
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, act.dp(8), 0, act.dp(8))
                }
                row.addView(act.tv(pm.getApplicationLabel(info).toString(), 13f, T.ink),
                    LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("打开", 12f, T.primary, true).apply {
                    setPadding(act.dp(10), 0, act.dp(10), 0)
                    isClickable = true
                    setOnClickListener {
                        runCatching {
                            act.startActivity(pm.getLaunchIntentForPackage(info.packageName))
                        }
                    }
                })
                row.addView(act.tv("卸载", 12f, T.danger, true).apply {
                    isClickable = true
                    setOnClickListener {
                        runCatching {
                            act.startActivity(
                                Intent(Intent.ACTION_DELETE, Uri.parse("package:${info.packageName}")),
                            )
                        }
                    }
                })
                box.addView(row)
            }
            shown = end
            if (shown >= list.size) more.text = "已全部显示 ${list.size} 个应用"
        }
        body.addView(act.card {
            sectionTitle("已安装应用（${list.size}）")
            addView(box)
            addView(more)
        })
        addBatch()
    }

    // ---------------- 震动 ----------------
    private fun vibrator(act: ToolActivity, body: LinearLayout) {
        val v = act.getSystemService(Vibrator::class.java)
        fun doVibrate(pattern: LongArray, repeat: Int = -1) {
            if (v == null || !v.hasVibrator()) { Ui.toast(act, "设备不支持震动"); return }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createWaveform(pattern, repeat))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, repeat)
            }
        }
        body.addView(act.card {
            sectionTitle("震动模式")
            listOf(
                "短震" to longArrayOf(80),
                "长震" to longArrayOf(600),
                "双震" to longArrayOf(0, 120, 120, 120),
                "心跳" to longArrayOf(0, 60, 80, 60, 300, 60, 80, 60),
            ).forEach { (name, p) ->
                btn(name) { doVibrate(p) }
            }
            btn("停止") { v?.cancel() }
        })
    }

    // ---------------- 壁纸 ----------------
    private fun wallpaper(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("设置壁纸")
            addView(act.tv("从相册选一张图，设为桌面或锁屏壁纸。", 12.5f, T.ink2))
            btn("选择图片并设为壁纸") {
                val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                act.startActivityForResult(i, 0x51)
            }
        })
        body.note("系统可能弹出裁剪界面；部分机型仅支持设为桌面壁纸。")
    }

    // ---------------- 垃圾分类 ----------------
    private fun garbage(act: ToolActivity, body: LinearLayout) {
        val map = mapOf(
            "电池" to "有害垃圾", "灯管" to "有害垃圾", "药品" to "有害垃圾", "油漆" to "有害垃圾",
            "玻璃" to "可回收物", "纸张" to "可回收物", "塑料瓶" to "可回收物", "金属" to "可回收物",
            "衣服" to "可回收物", "纸箱" to "可回收物",
            "剩饭" to "厨余垃圾", "果皮" to "厨余垃圾", "菜叶" to "厨余垃圾", "骨头" to "厨余垃圾",
            "烟头" to "其他垃圾", "纸巾" to "其他垃圾", "陶瓷" to "其他垃圾", "一次性餐具" to "其他垃圾",
        )
        val out = act.tv("—", 15f, T.primary, true)
        body.addView(act.card {
            sectionTitle("查询")
            val input = et("输入物品名称，如 电池", 1, singleLine = true)
            btn("查询") {
                val q = input.text?.toString()?.trim().orEmpty()
                val hit = map.entries.firstOrNull { q.isNotEmpty() && (q.contains(it.key) || it.key.contains(q)) }
                out.text = hit?.value ?: if (q.isEmpty()) "请输入物品" else "未收录，建议查询当地分类指南"
            }
            addView(out)
        })
        body.addView(act.card {
            sectionTitle("常见对照")
            map.entries.chunked(2).forEach { pair ->
                addView(act.tv(pair.joinToString("   ") { "${it.key} → ${it.value}" }, 11.5f, T.ink2)
                    .apply { setPadding(0, act.dp(4), 0, act.dp(4)) })
            }
        })
    }
}
