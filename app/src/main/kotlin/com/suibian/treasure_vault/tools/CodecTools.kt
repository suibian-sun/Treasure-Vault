package com.suibian.treasure_vault.tools

import android.util.Base64
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.ToolSpec
import java.net.URLDecoder
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/** 编码转换类（5 个）。 */
object CodecTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (com.suibian.treasure_vault.core.ToolActivity, android.widget.LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.codec, emoji, kw.toList(), build = build)

    private val b64 = Base64.NO_WRAP
    private val b64url = Base64.NO_WRAP or Base64.URL_SAFE

    val all: List<ToolSpec> = listOf(
        spec("base64", "Base64 编解码", "文本与 Base64 互转，含 URL 安全变体", "🔐", "base64", "编码") { a, b ->
            Kit.simple(a, b, "输入文本（编码）或 Base64 字符串（解码）…", listOf(
                "编码" to { s -> Base64.encodeToString(s.toByteArray(), b64) },
                "解码" to { s -> runCatching { String(Base64.decode(s.filter { !it.isWhitespace() }, b64)) }.getOrElse { "解码失败：不是有效的 Base64" } },
                "URL 安全编码" to { s -> Base64.encodeToString(s.toByteArray(), b64url) },
                "URL 安全解码" to { s -> runCatching { String(Base64.decode(s.trim(), b64url)) }.getOrElse { "解码失败" } },
            ))
        },
        spec("url_codec", "URL 编解码", "百分号编码与还原", "🔗", "url", "encode") { a, b ->
            Kit.simple(a, b, "粘贴链接或参数值…", listOf(
                "编码（组件）" to { s -> URLEncoder.encode(s, "UTF-8").replace("+", "%20") },
                "解码（组件）" to { s -> runCatching { URLDecoder.decode(s, "UTF-8") }.getOrElse { "解码失败" } },
                "编码（整段）" to { s -> URLEncoder.encode(s, "UTF-8").replace("+", "%20") },
                "解码（整段）" to { s -> runCatching { URLDecoder.decode(s, "UTF-8") }.getOrElse { "解码失败" } },
            ))
        },
        spec("unicode_codec", "Unicode 转义", "中文与 \\uXXXX 互转", "🈯", "unicode", "转义") { a, b ->
            Kit.simple(a, b, "输入中文，或 \\u4f60\\u597d 形式…", listOf(
                "转为 \\uXXXX" to { s -> s.map { c -> if (c.code < 128) c.toString() else "\\u%04x".format(c.code) }.joinToString("") },
                "还原 \\uXXXX" to { s ->
                    Regex("\\\\u([0-9a-fA-F]{4})").replace(s) { m ->
                        m.groupValues[1].toInt(16).toChar().toString()
                    }
                },
            ))
        },
        spec("html_escape", "HTML 转义", "尖括号、引号与实体互转", "📐", "html", "entity") { a, b ->
            Kit.simple(a, b, "输入 HTML 片段…", listOf(
                "转义" to { s ->
                    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        .replace("\"", "&quot;").replace("'", "&#39;")
                },
                "反转义" to { s ->
                    var t = s
                    mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
                        "&#39;" to "'", "&nbsp;" to " ", "&copy;" to "©", "&hellip;" to "…")
                        .forEach { (k, v) -> t = t.replace(k, v) }
                    t
                },
            ))
        },
        spec("json_tool", "JSON 格式化", "美化 / 压缩 / 校验", "🧩", "json", "格式化") { a, b ->
            Kit.simple(a, b, "{\"name\":\"宝藏盒\"}", listOf(
                "格式化（2 空格）" to { s ->
                    val o = JSONObject(s)
                    o.toString(2)
                },
                "压缩" to { s -> JSONObject(s).toString() },
                "数组格式化" to { s -> JSONArray(s).toString(2) },
            ), lines = 8, outputLabel = "结果")
        },
    )
}
