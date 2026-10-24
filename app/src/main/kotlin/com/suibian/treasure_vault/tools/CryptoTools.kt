package com.suibian.treasure_vault.tools

import android.graphics.Typeface
import android.widget.LinearLayout
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.switchRow
import com.suibian.treasure_vault.core.tv
import androidx.core.widget.doAfterTextChanged
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/** 加密安全类（5 个）。 */
object CryptoTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (com.suibian.treasure_vault.core.ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.crypto, emoji, kw.toList(), build = build)

    private fun hash(algo: String, input: String): String =
        MessageDigest.getInstance(algo).digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    val all: List<ToolSpec> = listOf(
        spec("md5", "MD5 哈希", "32 位摘要，常用于校验", "🏷️", "md5", "hash") { a, b ->
            Kit.simple(a, b, "输入需要计算 MD5 的文本…", listOf(
                "小写" to { s -> hash("MD5", s) },
                "大写" to { s -> hash("MD5", s).uppercase() },
            ), outputLabel = "MD5")
        },
        spec("sha1", "SHA-1 哈希", "40 位摘要", "🔑", "sha1", "hash") { a, b ->
            Kit.simple(a, b, "输入需要计算 SHA-1 的文本…", listOf(
                "小写" to { s -> hash("SHA-1", s) },
                "大写" to { s -> hash("SHA-1", s).uppercase() },
            ), outputLabel = "SHA-1")
        },
        spec("sha256", "SHA-256 哈希", "64 位摘要，校验文件完整性", "🛡️", "sha256", "hash") { a, b ->
            Kit.simple(a, b, "输入需要计算 SHA-256 的文本…", listOf(
                "小写" to { s -> hash("SHA-256", s) },
                "大写" to { s -> hash("SHA-256", s).uppercase() },
            ), outputLabel = "SHA-256")
        },
        spec("password_gen", "密码生成器", "可配置字符集，本地生成", "🔒", "密码", "password") { a, b ->
            password(a, b)
        },
        spec("uuid", "UUID 生成", "批量生成 v4 UUID", "🆔", "uuid", "guid") { a, b -> uuid(a, b) },
    )

    private fun password(act: com.suibian.treasure_vault.core.ToolActivity, body: LinearLayout) {
        var len = 16
        var upper = true
        var lower = true
        var digit = true
        var symbol = false
        var noAmbiguous = true
        val results = mutableListOf<String>()
        val holder = LinearLayout(act)

        fun gen() {
            var pool = ""
            if (upper) pool += "ABCDEFGHJKLMNPQRSTUVWXYZ"
            if (lower) pool += "abcdefghijkmnopqrstuvwxyz"
            if (digit) pool += "23456789"
            if (symbol) pool += "!@#\$%^&*-_=+?"
            if (!noAmbiguous) pool += "Il1O0o"
            if (pool.isEmpty()) {
                Ui.toast(act, "请至少选择一类字符")
                return
            }
            val rnd = SecureRandom()
            results.clear()
            repeat(5) {
                results.add((1..len).map { pool[rnd.nextInt(pool.length)] }.joinToString(""))
            }
            holder.removeAllViews()
            results.forEach { p ->
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, act.dp(6), 0, act.dp(6))
                }
                row.addView(act.tv(p, 14f, T.ink).apply { typeface = Typeface.MONOSPACE },
                    LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("复制", 12f, T.primary, true).apply {
                    setPadding(act.dp(8), 0, 0, 0)
                    setOnClickListener { Ui.copy(act, p) }
                })
                holder.addView(row)
            }
        }

        body.addView(act.card {
            sectionTitle("生成参数")
            val lenInput = Kit.numberRow(act, this, "长度", "位", "16") { }
            lenInput.setText("16")
            lenInput.doAfterTextChanged {
                len = (lenInput.text.toString().toIntOrNull() ?: 16).coerceIn(4, 64)
            }
            switchRow("大写字母", null, upper) { v -> upper = v }
            switchRow("小写字母", null, lower) { v -> lower = v }
            switchRow("数字", null, digit) { v -> digit = v }
            switchRow("特殊符号", null, symbol) { v -> symbol = v }
            switchRow("排除易混淆字符（l/1/O/0）", null, noAmbiguous) { v -> noAmbiguous = v }
            btn("生成 5 组密码") { gen() }
        })
        body.addView(act.card {
            sectionTitle("生成结果")
            addView(holder)
        })
        gen()
    }

    private fun uuid(act: com.suibian.treasure_vault.core.ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act)
        fun gen() {
            holder.removeAllViews()
            repeat(5) {
                val u = UUID.randomUUID().toString()
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, act.dp(6), 0, act.dp(6))
                }
                row.addView(act.tv(u, 13f, T.ink).apply { typeface = Typeface.MONOSPACE },
                    LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("复制", 12f, T.primary, true).apply {
                    setPadding(act.dp(8), 0, 0, 0)
                    setOnClickListener { Ui.copy(act, u) }
                })
                holder.addView(row)
            }
        }
        body.addView(act.card {
            sectionTitle("UUID v4")
            addView(holder)
            btn("重新生成") { gen() }
        })
        gen()
    }
}
