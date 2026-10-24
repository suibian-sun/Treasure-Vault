package com.suibian.treasure_vault.tools

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.label
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import android.util.Base64

/** 开发工具（8 个）。 */
object DevTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.dev, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("regex_test", "正则测试", "实时匹配与分组捕获", "🧪", "正则", "regex") { a, b -> regex(a, b) },
        spec("color_convert", "颜色码转换", "HEX / RGB / HSL 互转与预览", "🎨", "颜色", "颜色码", "hex", "rgb") { a, b -> color(a, b) },
        spec("jwt", "JWT 解析", "解析 Header / Payload 与过期状态", "🪪", "jwt", "token") { a, b -> jwt(a, b) },
        spec("http_status", "HTTP 状态码", "查询状态码含义", "🌐", "http", "状态码") { a, b -> httpStatus(a, b) },
        spec("json_to_dart", "JSON 转 Dart", "根据 JSON 结构生成模型类", "🧬", "json", "dart", "model") { a, b -> jsonToDart(a, b) },
        spec("url_parse", "URL 解析", "拆解协议、域名、路径与参数", "🔗", "url", "解析") { a, b -> urlParse(a, b) },
        spec("device_info", "设备信息", "型号、系统版本与屏幕参数", "📱", "设备", "机型") { a, b -> device(a, b) },
        spec("morse", "摩斯电码", "文本与摩斯电码互转", "📡", "摩斯", "morse") { a, b ->
            Kit.simple(a, b, "输入文本或摩斯电码（用空格分隔）…", listOf(
                "编码" to { s -> Morse.encode(s) },
                "解码" to { s -> Morse.decode(s) },
            ))
        },
    )

    // ---------------- 正则 ----------------
    private fun regex(act: ToolActivity, body: LinearLayout) {
        var pattern = "\\d+"
        var text = "订单 2024 年共 142 个工具，版本 2.0.0"
        var ignoreCase = false
        val out = act.tv("—", 13f, T.ink3).apply {
            typeface = Typeface.MONOSPACE
            background = T.fieldBg()
            setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
        }

        fun run() {
            if (pattern.isEmpty()) { out.text = "—"; return }
            val res = runCatching {
                val re = if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
                val matches = re.findAll(text).take(200).toList()
                if (matches.isEmpty()) "无匹配" else
                    matches.mapIndexed { i, m ->
                        "$i. \"${m.value}\" @ ${m.range.first}-${m.range.last + 1}" +
                            if (m.groupValues.size > 1) "  " + m.groupValues.drop(1)
                                .mapIndexed { j, g -> "\$${j + 1}=$g" }.joinToString("  ") else ""
                    }.joinToString("\n")
            }.getOrElse { "正则无效：${it.message}" }
            out.text = res
            out.setTextColor(T.ink)
        }

        body.addView(act.card {
            sectionTitle("正则表达式")
            val p = et("\\d{4}-\\d{2}-\\d{2}", 1, singleLine = true)
            p.setText(pattern)
            p.doAfterTextChanged { pattern = it?.toString().orEmpty(); run() }
            sectionTitle("测试文本")
            val t = et("粘贴需要匹配的文本…", 5)
            t.setText(text)
            t.doAfterTextChanged { text = it?.toString().orEmpty(); run() }
        })
        body.addView(act.card {
            sectionTitle("匹配结果")
            addView(out)
        })
        run()
    }

    // ---------------- 颜色 ----------------
    private fun color(act: ToolActivity, body: LinearLayout) {
        var input = "#1890FF"
        val preview = android.view.View(act)
        val info = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }

        fun run() {
            val c = parseColor(input)
            preview.setBackgroundColor(c ?: Color.TRANSPARENT)
            info.removeAllViews()
            if (c == null) {
                info.addView(act.tv("无法识别的颜色值", 12f, T.ink2)); return
            }
            val hex = String.format("#%06X", 0xFFFFFF and c)
            val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            val hsl = rgbToHsl(r, g, b)
            info.addView(act.tv("HEX   $hex", 13f, T.ink).apply { typeface = Typeface.MONOSPACE })
            info.addView(act.tv("RGB   rgb($r, $g, $b)", 13f, T.ink).apply { typeface = Typeface.MONOSPACE })
            info.addView(act.tv("HSL   hsl(${hsl[0]}, ${hsl[1]}%, ${hsl[2]}%)", 13f, T.ink).apply { typeface = Typeface.MONOSPACE })
            info.addView(act.tv("亮度   %.0f%%".format(Color.luminance(c) * 100), 12f, T.ink2))
        }

        body.addView(act.card {
            sectionTitle("颜色值")
            val e = et("#1890FF / rgb(24,144,255) / hsl(210,100%,55%)", 1, singleLine = true)
            e.setText(input)
            e.doAfterTextChanged { input = it?.toString().orEmpty(); run() }
            preview.layoutParams = LinearLayout.LayoutParams(-1, act.dp(72)).also { it.topMargin = act.dp(6) }
            addView(preview)
        })
        body.addView(act.card { sectionTitle("转换结果"); addView(info) })
        run()
    }

    private fun parseColor(raw: String): Int? {
        var s = raw.trim().lowercase()
        if (s.isEmpty()) return null
        if (s.startsWith("#")) s = s.substring(1)
        if (Regex("^[0-9a-f]{6}$").matches(s)) return (0xFF000000 or s.toLong(16)).toInt()
        if (Regex("^[0-9a-f]{3}$").matches(s)) {
            val f = s.map { "$it$it" }.joinToString("")
            return (0xFF000000 or f.toLong(16)).toInt()
        }
        Regex("rgba?\\(([^)]+)\\)").find(s)?.let { m ->
            val p = m.groupValues[1].split(",").map { it.trim().toIntOrNull() ?: 0 }
            if (p.size >= 3) return Color.rgb(p[0].coerceIn(0, 255), p[1].coerceIn(0, 255), p[2].coerceIn(0, 255))
        }
        Regex("hsl\\(([^)]+)\\)").find(s)?.let { m ->
            val p = m.groupValues[1].split(",").map { it.trim().removeSuffix("%") }
            if (p.size >= 3) {
                val h = p[0].toFloatOrNull() ?: 0f
                val sat = (p[1].toFloatOrNull() ?: 0f) / 100f
                val l = (p[2].toFloatOrNull() ?: 0f) / 100f
                return hslToColor(h, sat, l)
            }
        }
        return null
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Int {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val hp = (h % 360f) / 60f
        val x = c * (1 - kotlin.math.abs(hp % 2 - 1))
        val (r1, g1, b1) = when {
            hp < 1 -> Triple(c, x, 0f)
            hp < 2 -> Triple(x, c, 0f)
            hp < 3 -> Triple(0f, c, x)
            hp < 4 -> Triple(0f, x, c)
            hp < 5 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2
        return Color.rgb(((r1 + m) * 255).toInt(), ((g1 + m) * 255).toInt(), ((b1 + m) * 255).toInt())
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): IntArray {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
        val l = (max + min) / 2
        var h = 0f; var s = 0f
        if (max != min) {
            val d = max - min
            s = if (l > 0.5f) d / (2 - max - min) else d / (max + min)
            h = when (max) {
                rf -> (gf - bf) / d + if (gf < bf) 6f else 0f
                gf -> (bf - rf) / d + 2f
                else -> (rf - gf) / d + 4f
            } / 6f
        }
        return intArrayOf((h * 360).toInt(), (s * 100).toInt(), (l * 100).toInt())
    }

    // ---------------- JWT ----------------
    private fun jwt(act: ToolActivity, body: LinearLayout) {
        var token = ""
        val out = act.tv("—", 12.5f, T.ink3).apply {
            typeface = Typeface.MONOSPACE
            background = T.fieldBg()
            setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
        }
        fun run() {
            if (token.isBlank()) { out.text = "—"; return }
            val parts = token.split(".")
            if (parts.size < 2) { out.text = "不是有效的 JWT"; return }
            val header = runCatching {
                JSONObject(String(Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_WRAP))).toString(2)
            }.getOrElse { parts[0] }
            val payload = runCatching {
                JSONObject(String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))).toString(2)
            }.getOrElse { parts[1] }
            var exp = ""
            runCatching {
                val o = JSONObject(payload)
                if (o.has("exp")) {
                    val t = o.getLong("exp") * 1000
                    val d = java.util.Date(t)
                    exp = if (t < System.currentTimeMillis()) "已过期：$d" else "有效期至：$d"
                }
            }
            out.text = "Header:\n$header\n\nPayload:\n$payload" + if (exp.isNotEmpty()) "\n\n$exp" else ""
            out.setTextColor(T.ink)
        }
        body.addView(act.card {
            sectionTitle("JWT Token")
            val e = et("粘贴 eyJhbGciOi… 形式的 Token", 5)
            e.doAfterTextChanged { token = it?.toString().orEmpty(); run() }
        })
        body.addView(act.card { sectionTitle("解析结果"); addView(out) })
    }

    // ---------------- HTTP ----------------
    private fun httpStatus(act: ToolActivity, body: LinearLayout) {
        val map = mapOf(
            200 to "OK 请求成功", 201 to "Created 已创建", 204 to "No Content 无内容",
            206 to "Partial Content 部分内容（断点续传）",
            301 to "Moved Permanently 永久重定向", 302 to "Found 临时重定向", 304 to "Not Modified 缓存有效",
            307 to "Temporary Redirect 临时重定向", 400 to "Bad Request 参数错误",
            401 to "Unauthorized 未认证", 403 to "Forbidden 无权限", 404 to "Not Found 资源不存在",
            405 to "Method Not Allowed 方法不允许", 408 to "Request Timeout 超时", 409 to "Conflict 冲突",
            413 to "Payload Too Large 请求体过大", 415 to "Unsupported Media Type 类型不支持",
            422 to "Unprocessable Entity 语义错误", 429 to "Too Many Requests 请求过多",
            500 to "Internal Server Error 服务器错误", 501 to "Not Implemented 未实现",
            502 to "Bad Gateway 网关错误", 503 to "Service Unavailable 服务不可用",
            504 to "Gateway Timeout 网关超时",
        )
        var code = "200"
        val out = act.tv("", 14f, T.ink)
        fun run() {
            val c = code.toIntOrNull()
            out.text = if (c == null) "输入状态码" else "${c}\n\n${map[c] ?: "未收录的扩展状态码"}"
        }
        body.addView(act.card {
            sectionTitle("状态码")
            val e = et("200", 1, singleLine = true)
            e.setText(code)
            e.doAfterTextChanged { code = it?.toString().orEmpty(); run() }
        })
        body.addView(act.card { sectionTitle("含义"); addView(out) })
        run()
    }

    // ---------------- JSON → Dart ----------------
    private fun jsonToDart(act: ToolActivity, body: LinearLayout) {
        var src = ""
        val out = act.tv("—", 12f, T.ink3).apply {
            typeface = Typeface.MONOSPACE
            background = T.fieldBg()
            setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
        }
        fun run() {
            if (src.isBlank()) { out.text = "—"; return }
            val code = runCatching { DartModel.from(JSONObject(src), "Root") }.getOrElse { "解析失败：${it.message}" }
            out.text = code
            out.setTextColor(T.ink)
        }
        body.addView(act.card {
            sectionTitle("JSON 示例")
            val e = et("{\"id\":1,\"name\":\"宝藏盒\"}", 7)
            e.doAfterTextChanged { src = it?.toString().orEmpty(); run() }
        })
        body.addView(act.card { sectionTitle("生成的 Dart 代码"); addView(out) })
    }

    // ---------------- URL ----------------
    private fun urlParse(act: ToolActivity, body: LinearLayout) {
        var url = "https://example.com:8080/path/page?id=1&lang=zh#top"
        val out = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        fun run() {
            out.removeAllViews()
            val u = runCatching { URL(url) }.getOrNull()
            if (u == null) { out.addView(act.tv("请输入完整 URL（含 https://）", 12f, T.ink2)); return }
            fun kv(k: String, v: String) {
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, act.dp(5), 0, act.dp(5)) }
                row.addView(act.tv(k, 12.5f, T.ink2), LinearLayout.LayoutParams(act.dp(72), -2))
                row.addView(act.tv(v, 13f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
                out.addView(row)
            }
            kv("协议", u.protocol)
            kv("主机", u.host)
            kv("端口", if (u.port > 0) u.port.toString() else "默认")
            kv("路径", u.path.ifEmpty { "/" })
            u.query?.split("&")?.forEach { p ->
                val kvp = p.split("=", limit = 2)
                kv(kvp[0], kvp.getOrElse(1) { "" })
            }
            kv("片段", u.ref ?: "—")
        }
        body.addView(act.card {
            sectionTitle("URL")
            val e = et("https://…", 2)
            e.setText(url)
            e.doAfterTextChanged { url = it?.toString().orEmpty(); run() }
        })
        body.addView(act.card { sectionTitle("解析结果"); addView(out) })
        run()
    }

    // ---------------- 设备 ----------------
    private fun device(act: ToolActivity, body: LinearLayout) {
        val dm = act.resources.displayMetrics
        body.addView(act.card {
            sectionTitle("系统")
            kvRow("品牌", Build.BRAND.replaceFirstChar { it.uppercase() })
            kvRow("型号", Build.MODEL)
            kvRow("Android", "API ${Build.VERSION.SDK_INT}（${Build.VERSION.RELEASE}）")
            kvRow("ABI", Build.SUPPORTED_ABIS.firstOrNull() ?: "—")
        })
        body.addView(act.card {
            sectionTitle("屏幕")
            kvRow("逻辑尺寸", "${dm.widthPixels / dm.density} × ${dm.heightPixels / dm.density} dp")
            kvRow("物理像素", "${dm.widthPixels} × ${dm.heightPixels} px")
            kvRow("像素密度", "%.2fx".format(dm.density))
        })
        body.addView(act.card {
            sectionTitle("应用")
            kvRow("版本", "${com.suibian.treasure_vault.BuildConfig.VERSION_NAME} (${com.suibian.treasure_vault.BuildConfig.VERSION_CODE})")
            kvRow("包名", com.suibian.treasure_vault.BuildConfig.APPLICATION_ID)
        })
    }

    private fun LinearLayout.kvRow(k: String, v: String) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, context.dp(5), 0, context.dp(5))
        }
        row.addView(context.tv(k, 12.5f, T.ink2), LinearLayout.LayoutParams(context.dp(84), -2))
        row.addView(context.tv(v, 13f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
        addView(row)
    }

}

/** 简易 Dart 模型生成。 */
private object DartModel {
    fun from(o: JSONObject, name: String): String {
        val sb = StringBuilder()
        sb.append("class ").append(name).append(" {\n")
        o.keys().forEach { k ->
            sb.append("  final ").append(typeOf(o.get(k), k)).append(" ").append(camel(k)).append(";\n")
        }
        sb.append("\n  const ").append(name).append("({\n")
        o.keys().forEach { k -> sb.append("    required this.").append(camel(k)).append(",\n") }
        sb.append("  });\n\n  factory ").append(name).append(".fromJson(Map<String, dynamic> json) => ").append(name).append("(\n")
        o.keys().forEach { k -> sb.append("        ").append(camel(k)).append(": json['").append(k).append("'],\n") }
        sb.append("      );\n}")
        return sb.toString()
    }

    private fun typeOf(v: Any?, key: String): String = when (v) {
        is JSONObject -> "Map<String, dynamic>"
        is JSONArray -> "List<dynamic>"
        is Int, is Long -> "int"
        is Double -> "double"
        is Boolean -> "bool"
        is String -> "String"
        else -> "dynamic"
    }

    private fun camel(k: String): String {
        val p = k.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        if (p.isEmpty()) return "value"
        return p[0].lowercase() + p.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

/** 摩斯电码表。 */
private object Morse {
    private val table = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '!' to "-.-.--", '/' to "-..-.",
    )

    fun encode(s: String): String =
        s.uppercase().map { c ->
            when {
                c == ' ' -> "/"
                table.containsKey(c) -> table[c]
                else -> ""
            }
        }.filterNotNull().filter { it.isNotEmpty() }.joinToString(" ")

    fun decode(s: String): String {
        val rev = table.entries.associate { it.value to it.key }
        return s.trim().split(Regex("\\s+")).joinToString("") { t ->
            if (t == "/") " " else rev[t]?.toString() ?: "?"
        }
    }
}
