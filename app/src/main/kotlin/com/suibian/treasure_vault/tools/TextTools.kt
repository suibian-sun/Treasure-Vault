package com.suibian.treasure_vault.tools

import android.graphics.Typeface
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.label
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.out
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.switchRow
import com.suibian.treasure_vault.core.tv

/** 文本处理类工具（12 个）。 */
object TextTools {

    private fun spec(
        id: String,
        name: String,
        desc: String,
        emoji: String,
        keywords: List<String> = emptyList(),
        build: (ToolActivity, LinearLayout) -> Unit,
    ) = ToolSpec(id, name, desc, Cats.text, emoji, keywords, build = build)

    val all: List<ToolSpec> = listOf(
        spec("word_count", "字数统计", "字符 / 中文 / 单词 / 行数 / 阅读时长", "🔢",
            listOf("字数", "统计")) { act, body -> wordCount(act, body) },
        spec("case_convert", "大小写转换", "大写 / 小写 / 驼峰 / 下划线 / 中划线", "🔠",
            listOf("大小写", "camel", "snake")) { act, body ->
            simple(act, body, "粘贴需要转换命名的文本…", listOf(
                "全部大写" to { s: String -> s.uppercase() },
                "全部小写" to { s: String -> s.lowercase() },
                "首字母大写" to { s: String -> titleCase(s) },
                "句首大写" to { s: String -> sentenceCase(s) },
                "驼峰 camelCase" to { s: String -> camel(s, false) },
                "帕斯卡 PascalCase" to { s: String -> camel(s, true) },
                "下划线 snake_case" to { s: String -> joinWords(s, "_", true) },
                "中划线 kebab-case" to { s: String -> joinWords(s, "-", true) },
                "常量 UPPER_SNAKE" to { s: String -> joinWords(s, "_", false) },
            ))
        },
        spec("dedupe", "文本去重", "按行去重，可忽略大小写", "🧹",
            listOf("去重", "unique")) { act, body ->
            simple(act, body, "每行一条记录…", listOf(
                "保留首次" to { s: String -> dedupe(s, false, false) },
                "忽略大小写" to { s: String -> dedupe(s, false, true) },
                "保留最后" to { s: String -> dedupe(s, true, false) },
                "去重并排序" to { s: String -> dedupe(s, false, false).lines().sorted().joinToString("\n") },
            ), lines = 7)
        },
        spec("sort_lines", "文本排序", "字典序 / 数字 / 长度 / 打乱", "↕️",
            listOf("排序", "sort")) { act, body ->
            simple(act, body, "每行一条记录…", listOf(
                "字典升序" to { s: String -> linesOf(s).sortedBy { it.lowercase() }.joinToString("\n") },
                "字典降序" to { s: String -> linesOf(s).sortedByDescending { it.lowercase() }.joinToString("\n") },
                "数字升序" to { s: String -> linesOf(s).sortedBy { numOf(it) }.joinToString("\n") },
                "数字降序" to { s: String -> linesOf(s).sortedByDescending { numOf(it) }.joinToString("\n") },
                "按长度" to { s: String -> linesOf(s).sortedBy { it.length }.joinToString("\n") },
                "随机打乱" to { s: String -> linesOf(s).shuffled().joinToString("\n") },
            ))
        },
        spec("find_replace", "查找替换", "支持正则与替换计数", "🔎",
            listOf("替换", "replace", "正则")) { act, body -> findReplace(act, body) },
        spec("remove_blank", "去除空行", "删除空行 / 合并连续空行 / 清理缩进", "📄",
            listOf("空行", "trim")) { act, body ->
            simple(act, body, "粘贴含有多余空行的文本…", listOf(
                "删除所有空行" to { s: String -> s.lines().filter { it.isNotBlank() }.joinToString("\n") },
                "合并连续空行" to { s: String -> s.replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n") },
                "删除行首尾空白" to { s: String -> s.lines().joinToString("\n") { it.trim() } },
                "删除行首空白" to { s: String -> s.lines().joinToString("\n") { it.trimStart() } },
            ))
        },
        spec("remove_spaces", "去除空白", "删除全部空白或合并多余空格", "␣",
            listOf("空格", "space")) { act, body ->
            simple(act, body, "粘贴含有大量空格的文本…", listOf(
                "删除全部空白" to { s: String -> s.replace(Regex("\\s+"), "") },
                "合并连续空格" to { s: String -> s.replace(Regex("[ \\t]+"), " ") },
                "换行变空格" to { s: String -> s.replace(Regex("\\s*\\n\\s*"), " ") },
                "全角转半角" to { s: String -> halfWidth(s) },
            ))
        },
        spec("line_numbers", "添加行号", "为每行加上序号，可自定义格式", "🔖",
            listOf("行号", "number")) { act, body ->
            simple(act, body, "每行一条，自动编号…", listOf(
                "1. 2. 3." to { s: String -> numbered(s) { "${it + 1}. " } },
                "1、2、3、" to { s: String -> numbered(s) { "${it + 1}、" } },
                "[1] [2]" to { s: String -> numbered(s) { "[${it + 1}] " } },
                "01 02 03" to { s: String -> numbered(s) { (it + 1).toString().padStart(2, '0') + " " } },
            ))
        },
        spec("text_reverse", "文本反转", "反转字符 / 行序 / 词序", "🔃",
            listOf("反转", "reverse")) { act, body ->
            simple(act, body, "粘贴文本…", listOf(
                "反转字符" to { s: String -> s.reversed() },
                "每行反转" to { s: String -> s.lines().joinToString("\n") { it.reversed() } },
                "反转行序" to { s: String -> s.lines().reversed().joinToString("\n") },
                "反转词序" to { s: String -> s.split(Regex("\\s+")).reversed().joinToString(" ") },
            ))
        },
        spec("delimiter", "分隔符转换", "换行 / 逗号 / 分号 / 制表符互转", "🔀",
            listOf("分隔符", "csv", "逗号")) { act, body -> delimiter(act, body) },
        spec("text_diff", "文本对比", "逐行比较两段文本的差异", "🆚",
            listOf("对比", "diff")) { act, body -> diff(act, body) },
        spec("extract", "内容提取", "提取邮箱 / 手机号 / 链接 / 数字 / IP", "🎯",
            listOf("提取", "regex", "邮箱")) { act, body ->
            simple(act, body, "粘贴一段混杂文本，自动提取目标内容…", listOf(
                "邮箱" to { s: String -> extract(s, "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}") },
                "手机号" to { s: String -> extract(s, "(?<!\\d)1[3-9]\\d{9}(?!\\d)") },
                "网址" to { s: String -> extract(s, "https?://[^\\s<>\")\\]]+") },
                "数字" to { s: String -> extract(s, "-?\\d+(?:\\.\\d+)?") },
                "IPv4" to { s: String -> extract(s, "(?<!\\d)(?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d)") },
            ), outputLabel = "提取结果")
        },
    )

    // ------------------------------------------------------------------
    // 通用骨架
    // ------------------------------------------------------------------

    private fun simple(
        act: ToolActivity,
        body: LinearLayout,
        hint: String,
        modes: List<Pair<String, (String) -> String>>,
        lines: Int = 6,
        outputLabel: String = "结果",
    ) {
        var idx = 0
        var input: EditText? = null
        var output: TextView? = null

        fun run() {
            val s = input?.text?.toString().orEmpty()
            val o = output ?: return
            if (s.isEmpty()) {
                o.text = "—"
                o.setTextColor(T.ink3)
                return
            }
            val r = runCatching { modes[idx].second(s) }.getOrElse { "处理失败：${it.message}" }
            o.text = r
            o.setTextColor(T.ink)
        }

        body.addView(act.card {
            sectionTitle("输入内容")
            val e = et(hint, lines)
            e.doAfterTextChanged { run() }
            input = e
        })
        body.chips(modes.map { it.first }, 0) { i ->
            idx = i
            run()
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
            addView(o, LinearLayout.LayoutParams(-1, -2))
            output = o
        })
        run()
    }

    // ------------------------------------------------------------------
    // 字数统计
    // ------------------------------------------------------------------

    private fun wordCount(act: ToolActivity, body: LinearLayout) {
        val stats = arrayOfNulls<TextView>(6)
        val read = act.tv("预计阅读 —", 12f, T.ink2)
        var input: EditText? = null

        fun update() {
            val s = input?.text?.toString().orEmpty()
            val chars = s.codePointCount(0, s.length)
            val noSpace = s.replace(Regex("\\s"), "").length
            val cjk = Regex("[\\u4e00-\\u9fa5]").findAll(s).count()
            val words = Regex("[A-Za-z0-9_']+").findAll(s).count()
            val lines = if (s.isEmpty()) 0 else s.lines().size
            val paras = if (s.isBlank()) 0 else s.trim().split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }
            stats[0]?.text = "$chars"
            stats[1]?.text = "$noSpace"
            stats[2]?.text = "$cjk"
            stats[3]?.text = "$words"
            stats[4]?.text = "$lines"
            stats[5]?.text = "$paras"
            val minutes = cjk / 300.0 + words / 200.0
            read.text = if (minutes < 1) "预计阅读 ${(minutes * 60).toInt()} 秒"
            else "预计阅读 ${"%.1f".format(minutes)} 分钟"
        }

        body.addView(act.card {
            sectionTitle("输入文本")
            val e = et("在此粘贴或输入文本，统计结果实时更新…", 8)
            e.doAfterTextChanged { update() }
            input = e
        })

        val labels = listOf("总字符", "不含空白", "中文字", "英文单词", "行数", "段落")
        body.addView(act.card {
            sectionTitle("统计结果")
            var i = 0
            while (i < 6) {
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                for (j in 0 until 3) {
                    val idx = i + j
                    val cell = LinearLayout(act).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        background = T.fieldBg(act.dp(T.R_SMALL).toFloat())
                        setPadding(0, act.dp(12), 0, act.dp(12))
                        val num = act.tv("0", 16f, T.ink, true)
                        addView(num)
                        addView(act.tv(labels[idx], 11f, T.ink2).apply { setPadding(0, act.dp(3), 0, 0) })
                        stats[idx] = num
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).also {
                        it.rightMargin = if (j < 2) act.dp(8) else 0
                    })
                }
                addView(row, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(8) })
                i += 3
            }
            addView(read)
        })
        update()
    }

    // ------------------------------------------------------------------
    // 查找替换
    // ------------------------------------------------------------------

    private fun findReplace(act: ToolActivity, body: LinearLayout) {
        var regex = false
        var caseSensitive = false
        var src: EditText? = null
        var find: EditText? = null
        var rep: EditText? = null
        var outView: TextView? = null

        fun run() {
            val s = src?.text?.toString().orEmpty()
            val f = find?.text?.toString().orEmpty()
            val r = rep?.text?.toString().orEmpty()
            if (f.isEmpty()) {
                outView?.text = "请填写要查找的内容"
                outView?.setTextColor(T.ink3)
                return
            }
            var count = 0
            val result = runCatching {
                if (regex) {
                    val re = Regex(f, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
                    s.replace(re) { count++; r }
                } else {
                    val sb = StringBuilder()
                    var i = 0
                    while (true) {
                        val idx = if (caseSensitive) s.indexOf(f, i)
                        else s.lowercase().indexOf(f.lowercase(), i)
                        if (idx < 0) { sb.append(s.substring(i)); break }
                        sb.append(s, i, idx).append(r)
                        count++
                        i = idx + f.length
                    }
                    sb.toString()
                }
            }.getOrElse { "正则表达式无效：${it.message}" }
            outView?.text = result
            outView?.setTextColor(T.ink)
            (outView?.tag as? TextView)?.text = "替换结果（共 $count 处）"
        }

        body.addView(act.card {
            sectionTitle("原文")
            val e = et("粘贴需要处理的文本…", 6)
            e.doAfterTextChanged { run() }
            src = e
        })
        body.addView(act.card {
            sectionTitle("查找与替换")
            label("查找")
            val f = et(if (regex) "\\d{3}-\\d{4}" else "需要查找的内容", 1, singleLine = true)
            f.doAfterTextChanged { run() }
            find = f
            label("替换为")
            val r = et("留空表示删除", 1, singleLine = true)
            r.doAfterTextChanged { run() }
            rep = r
            switchRow("使用正则表达式", null, regex) { v -> regex = v; run() }
            switchRow("区分大小写", null, caseSensitive) { v -> caseSensitive = v; run() }
        })
        body.addView(act.card {
            sectionTitle("替换结果")
            val o = act.tv("—", 13.5f, T.ink3).apply {
                typeface = Typeface.MONOSPACE
                background = T.fieldBg()
                setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
                setTextIsSelectable(true)
            }
            addView(o)
            outView = o
        })
    }

    // ------------------------------------------------------------------
    // 分隔符转换
    // ------------------------------------------------------------------

    private fun delimiter(act: ToolActivity, body: LinearLayout) {
        val labels = listOf("换行", "逗号", "分号", "空格", "制表符")
        val values = listOf("\n", ",", ";", " ", "\t")
        var from = 0
        var to = 1
        var input: EditText? = null
        var outView: TextView? = null

        fun run() {
            val s = input?.text?.toString().orEmpty()
            val parts = s.split(values[from]).map { it.trim() }.filter { it.isNotEmpty() }
            outView?.text = parts.joinToString(values[to]).ifEmpty { "—" }
        }

        body.addView(act.card {
            sectionTitle("原始内容")
            val e = et("例如：苹果,香蕉,橙子", 6)
            e.doAfterTextChanged { run() }
            input = e
        })
        body.addView(act.card {
            sectionTitle("从")
            chips(labels, from) { i -> from = i; run(); }
            sectionTitle("转换为")
            chips(labels, to) { i -> to = i; run(); }
        })
        body.addView(act.card {
            sectionTitle("结果")
            val o = act.tv("—", 13.5f, T.ink3).apply {
                typeface = Typeface.MONOSPACE
                background = T.fieldBg()
                setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
            }
            addView(o)
            outView = o
        })
    }

    // ------------------------------------------------------------------
    // 文本对比
    // ------------------------------------------------------------------

    private fun diff(act: ToolActivity, body: LinearLayout) {
        var a: EditText? = null
        var b: EditText? = null
        var outView: TextView? = null

        fun run() {
            val left = a?.text?.toString().orEmpty().lines()
            val right = b?.text?.toString().orEmpty().lines()
            val n = left.size
            val m = right.size
            val dp = Array(n + 1) { IntArray(m + 1) }
            for (i in n - 1 downTo 0) {
                for (j in m - 1 downTo 0) {
                    dp[i][j] = if (left[i] == right[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
            val sb = StringBuilder()
            var i = 0
            var j = 0
            while (i < n && j < m) {
                when {
                    left[i] == right[j] -> { sb.append("  ").append(left[i]).append('\n'); i++; j++ }
                    dp[i + 1][j] >= dp[i][j + 1] -> { sb.append("- ").append(left[i]).append('\n'); i++ }
                    else -> { sb.append("+ ").append(right[j]).append('\n'); j++ }
                }
            }
            while (i < n) { sb.append("- ").append(left[i++]).append('\n') }
            while (j < m) { sb.append("+ ").append(right[j++]).append('\n') }
            outView?.text = sb.toString().trimEnd().ifEmpty { "—" }
        }

        body.addView(act.card {
            sectionTitle("原文（左）")
            val e = et("第一段文本…", 5)
            e.doAfterTextChanged { run() }
            a = e
        })
        body.addView(act.card {
            sectionTitle("对比文（右）")
            val e = et("第二段文本…", 5)
            e.doAfterTextChanged { run() }
            b = e
        })
        body.addView(act.card {
            sectionTitle("差异（- 仅左 / + 仅右）")
            val o = act.tv("—", 12.5f, T.ink3).apply {
                typeface = Typeface.MONOSPACE
                background = T.fieldBg()
                setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
            }
            addView(o)
            outView = o
        })
        body.note("逐行 LCS 对比；文本过长时建议分段比较。")
    }

    // ------------------------------------------------------------------
    // 纯函数
    // ------------------------------------------------------------------

    private fun linesOf(s: String) = s.split("\n")

    private fun numOf(s: String): Double =
        Regex("-?\\d+(?:\\.\\d+)?").find(s)?.value?.toDoubleOrNull() ?: 0.0

    private fun numbered(s: String, prefix: (Int) -> String): String =
        s.lines().mapIndexed { i, l -> prefix(i) + l }.joinToString("\n")

    private fun dedupe(s: String, keepLast: Boolean, ignoreCase: Boolean): String {
        val list = s.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val seen = HashMap<String, Int>()
        val out = ArrayList<String>()
        if (keepLast) {
            for (i in list.indices.reversed()) {
                val k = if (ignoreCase) list[i].lowercase() else list[i]
                if (!seen.containsKey(k)) { seen[k] = i; out.add(list[i]) }
            }
            return out.reversed().joinToString("\n")
        }
        for (item in list) {
            val k = if (ignoreCase) item.lowercase() else item
            if (!seen.containsKey(k)) { seen[k] = 0; out.add(item) }
        }
        return out.joinToString("\n")
    }

    private fun words(s: String): List<String> =
        s.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .split(Regex("[^A-Za-z0-9\\u4e00-\\u9fa5]+"))
            .filter { it.isNotEmpty() }

    private fun camel(s: String, upperFirst: Boolean): String {
        val ws = words(s)
        return ws.mapIndexed { i, w ->
            val lw = w.lowercase()
            if (i == 0 && !upperFirst) lw else lw.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }

    private fun joinWords(s: String, sep: String, lower: Boolean): String =
        words(s).joinToString(sep) { if (lower) it.lowercase() else it.uppercase() }

    private fun titleCase(s: String): String =
        s.lines().joinToString("\n") { line ->
            line.split(Regex("(\\s+)")).joinToString("") { w ->
                if (w.isBlank()) w else w.replaceFirstChar { c -> c.uppercase() }
            }
        }

    private fun sentenceCase(s: String): String {
        val lower = s.lowercase()
        val sb = StringBuilder()
        var cap = true
        for (ch in lower) {
            if (cap && ch in 'a'..'z') { sb.append(ch.uppercaseChar()); cap = false }
            else {
                sb.append(ch)
                if (ch in ".!?。！？\n") cap = true
            }
        }
        return sb.toString()
    }

    private fun halfWidth(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when {
                c.code == 0x3000 -> sb.append(' ')
                c.code in 0xFF01..0xFF5E -> sb.append((c.code - 0xFEE0).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun extract(s: String, pattern: String): String =
        Regex(pattern).findAll(s).map { it.value }.distinct().joinToString("\n")
}
