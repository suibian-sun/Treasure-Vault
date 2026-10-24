package com.suibian.treasure_vault.tools

import android.graphics.Color
import android.text.InputType
import android.view.Gravity
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
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.label
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.out
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import java.text.Normalizer

/** 文本增强工具：编辑器、拼音、词典、成语、藏头诗、摘要等。 */
object TextExtraTools {

    private fun spec(id: String, name: String, desc: String, cat: com.suibian.treasure_vault.core.Category,
                     emoji: String, vararg kw: String, build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, cat, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("editor", "文本编辑器", "大文本编辑，实时统计并保存到文件", Cats.text, "📝", "编辑", "记事") { a, b -> editor(a, b) },
        spec("split_words", "拆分选词", "按标点 / 字数切分，统计词频", Cats.text, "✂️", "拆分", "词频") { a, b -> splitWords(a, b) },
        spec("pinyin", "中文转拼音", "带声调、不带声调、首字母三种输出", Cats.text, "🈶", "拼音", "pinyin") { a, b -> pinyin(a, b) },
        spec("dict", "汉语词典", "查字词读音与释义", Cats.text, "📖", "词典", "释义") { a, b -> dict(a, b) },
        spec("idiom", "成语查询", "按关键词查成语释义", Cats.text, "🀄", "成语", "词典") { a, b -> idiom(a, b) },
        spec("synonym", "近反义词查询", "查常见词的近义词与反义词", Cats.text, "🔀", "近义", "反义") { a, b -> synonym(a, b) },
        spec("acrostic", "藏头诗生成", "把想说的话藏进每句开头", Cats.text, "🖋️", "藏头", "诗") { a, b -> acrostic(a, b) },
        spec("couplet", "对联生成", "名联库 + 自动对句", Cats.text, "🧧", "对联", "春联") { a, b -> couplet(a, b) },
        spec("summarize", "文章摘要生成", "TextRank 抽取式摘要", Cats.text, "📄", "摘要", "总结") { a, b -> summarize(a, b) },
    )

    // ---------------- 文本编辑器 ----------------

    private fun editor(act: ToolActivity, body: LinearLayout) {
        var text = ""
        var stats: android.widget.TextView? = null
        var editRef: android.widget.EditText? = null
        fun refresh() {
            stats?.text = "${text.length} 字 · ${text.count { it == '\n' } + 1} 行 · ${text.count { !it.isWhitespace() }} 有效字符"
        }
        body.addView(act.card {
            sectionTitle("编辑")
            val e = et("在这里输入或粘贴文本…", 14)
            e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            editRef = e
            val t = act.tv("", 11.5f, T.ink2)
            addView(t)
            stats = t
            e.doAfterTextChanged { text = it?.toString().orEmpty(); refresh() }
            refresh()
        })
        body.addView(act.card {
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            fun mk(label: String, primary: Boolean, action: () -> Unit) = act.tv(label, 13f, if (primary) Color.WHITE else T.ink, true).apply {
                gravity = Gravity.CENTER
                background = if (primary) T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                else T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener { action() }
            }
            row.addView(mk("保存", true) {
                if (text.isBlank()) { Ui.toast(act, "内容为空"); return@mk }
                val dir = java.io.File(act.getExternalFilesDir(null), "notes").apply { mkdirs() }
                val f = java.io.File(dir, "note_${System.currentTimeMillis()}.txt")
                runCatching { f.writeText(text) }
                    .onSuccess { Ui.toast(act, "已保存：${f.name}") }
                    .onFailure { Ui.toast(act, "保存失败：${it.message}") }
            }, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            row.addView(mk("复制", false) { Ui.copy(act, text) },
                LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            row.addView(mk("清空", false) {
                editRef?.setText("")
                text = ""
                refresh()
                Ui.toast(act, "已清空")
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(row)
            note("文本保存在应用专属目录，无需存储权限。")
        })
    }

    // ---------------- 拆分选词 ----------------

    private fun splitWords(act: ToolActivity, body: LinearLayout) {
        var byPunct = true
        var n = 2

        val result = act.tv("—", 13f, T.ink3).apply {
            background = T.fieldBg()
            setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
            setTextIsSelectable(true)
        }
        body.addView(act.card {
            sectionTitle("输入")
            val e = et("粘贴要拆分的中文段落", 8)
            label("拆分方式")
            chips(listOf("按标点/空格", "按字数", "去重统计"), 0) { i -> byPunct = i == 0 }
            label("每段字数")
            chips(listOf("2 字", "3 字", "4 字"), 0) { i -> n = i + 2 }
            btn("开始拆分") {
                val s = e.text?.toString().orEmpty()
                result.text = when {
                    s.isBlank() -> "—"
                    byPunct -> s.split(Regex("[\\s,，。.、;；:：!！?？\\n\\r\\t()（）\\[\\]【】\"'“”]+"))
                        .filter { it.isNotBlank() }
                        .joinToString("\n") { "$it\t(${it.length} 字)" }
                    else -> {
                        val parts = ArrayList<String>()
                        var i = 0
                        while (i < s.length) {
                            parts.add(s.substring(i, minOf(i + n, s.length)))
                            i += n
                        }
                        parts.joinToString("\n")
                    }
                }
                result.setTextColor(if (result.text == "—") T.ink3 else T.ink)
            }
            btn("词频统计", primary = false) {
                val s = e.text?.toString().orEmpty()
                if (s.isBlank()) { Ui.toast(act, "先输入文本"); return@btn }
                val counts = s.filter { it.isLetter() }.windowed(n, 1).groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }.take(60)
                result.text = counts.joinToString("\n") { "${it.key}\t${it.value}" }
                result.setTextColor(T.ink)
            }
        })
        body.addView(act.card {
            sectionTitle("结果", act.tv("复制", 12f, T.primary, true).apply {
                isClickable = true
                setOnClickListener { Ui.copy(act, result.text.toString()) }
            })
            addView(result)
        })
    }

    // ---------------- 拼音 ----------------

    private fun pinyin(act: ToolActivity, body: LinearLayout) {
        var mode = 0
        body.addView(act.card {
            sectionTitle("拼音转换")
            chips(listOf("带声调", "不带声调", "首字母"), 0) { i -> mode = i }
            val e = et("输入中文，如：宝藏盒", 4)
            val outView = act.tv("—", 14f, T.ink3).apply {
                background = T.fieldBg()
                setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
                setTextIsSelectable(true)
            }
            btn("转换") {
                val s = e.text?.toString().orEmpty()
                if (s.isBlank()) { Ui.toast(act, "先输入中文"); return@btn }
                val r = runCatching { toPinyin(s, mode) }.getOrElse { "转换失败：${it.message}" }
                outView.text = r
                outView.setTextColor(T.ink)
            }
            btn("复制结果", primary = false) { Ui.copy(act, outView.text.toString()) }
            addView(outView)
            note("使用系统内置 ICU 转换，多音字按常用读音处理。")
        })
    }

    fun toPinyin(text: String, mode: Int): String {
        val t = android.icu.text.Transliterator.getInstance("Han-Latin")
        val raw = t.transliterate(text)
        return raw.split(" ").joinToString(" ") { word ->
            var w = word
            // 去声调
            w = Normalizer.normalize(w, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            w = w.replace("ü", "v").replace("Ü", "V")
            when (mode) {
                1 -> w.lowercase()
                2 -> w.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                else -> word   // 原样（带声调）
            }
        }.replace(Regex("\\s+"), " ").trim()
    }

    // ---------------- 词典 ----------------

    private fun dict(act: ToolActivity, body: LinearLayout) {
        body.note("内置常用词条离线可查；未收录的词会联网查询（免 Key）。")
        body.addView(act.card {
            sectionTitle("查询")
            val e = et("输入汉字、词语或英文", singleLine = true)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            btn("查询") {
                val q = e.text?.toString()?.trim().orEmpty()
                if (q.isEmpty()) { Ui.toast(act, "先输入要查的词"); return@btn }
                result.removeAllViews()
                result.addView(act.tv("查询中…", 13f, T.ink2))
                Thread {
                    val local = DictData.lookup(q)
                    val pinyin = runCatching { toPinyin(q, 0) }.getOrNull().orEmpty()
                    val online = if (local == null) DictData.online(q) else null
                    act.runOnUiThread {
                        result.removeAllViews()
                        if (local != null) {
                            result.addView(act.tv("读音", 11.5f, T.ink2, true))
                            result.addView(act.tv(pinyin, 15f, T.primary, true).apply { setPadding(0, act.dp(4), 0, act.dp(10)) })
                            result.addView(act.tv(local, 13.5f, T.ink))
                        } else if (online != null) {
                            result.addView(act.tv("读音", 11.5f, T.ink2, true))
                            result.addView(act.tv(pinyin, 15f, T.primary, true).apply { setPadding(0, act.dp(4), 0, act.dp(10)) })
                            result.addView(act.tv(online, 13.5f, T.ink))
                            result.addView(act.tv("来源：网络词典", 10.5f, T.ink3).apply { setPadding(0, act.dp(8), 0, 0) })
                        } else {
                            result.addView(act.tv("没有查到「$q」。\n\n内置词库为精选常用词，未收录时可尝试更常见的词，或检查网络。", 13f, T.ink2))
                            result.addView(act.tv(pinyin, 13f, T.ink3).apply { setPadding(0, act.dp(8), 0, 0) })
                        }
                    }
                }.start()
            }
            addView(result)
        })
    }

    // ---------------- 成语 ----------------

    private fun idiom(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("成语查询")
            val e = et("输入成语或关键词，如：一心 / 水", singleLine = true)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            btn("查询") {
                val q = e.text?.toString()?.trim().orEmpty()
                result.removeAllViews()
                val hits = DictData.searchIdiom(q)
                if (hits.isEmpty()) {
                    result.addView(act.tv("没有找到相关成语，换个关键词试试。", 13f, T.ink2))
                } else {
                    hits.take(30).forEach { (word, mean) ->
                        val box = LinearLayout(act).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(0, act.dp(8), 0, act.dp(8))
                        }
                        box.addView(act.tv(word, 15f, T.primary, true))
                        box.addView(act.tv(mean, 12.5f, T.ink).apply { setPadding(0, act.dp(3), 0, 0) })
                        result.addView(box)
                        result.addView(act.hairline())
                    }
                    result.addView(act.tv("共 ${hits.size} 条", 11f, T.ink3).apply { setPadding(0, act.dp(8), 0, 0) })
                }
            }
            addView(result)
            note("内置 ${DictData.idiomCount} 条常用成语，支持按字或释义搜索。")
        })
    }

    // ---------------- 近反义词 ----------------

    private fun synonym(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("近反义词")
            val e = et("输入词语，如：高兴", singleLine = true)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            btn("查询") {
                val q = e.text?.toString()?.trim().orEmpty()
                result.removeAllViews()
                if (q.isEmpty()) { Ui.toast(act, "先输入词语"); return@btn }
                val hit = DictData.synonym(q)
                if (hit == null) {
                    result.addView(act.tv("内置词库没有「$q」。\n可尝试：高兴、美丽、安静、勤奋、危险…", 13f, T.ink2))
                } else {
                    result.addView(act.tv("近义词", 12f, T.ink2, true))
                    result.addView(act.tv(hit.first.ifBlank { "—" }, 14f, T.primary).apply { setPadding(0, act.dp(4), 0, act.dp(12)) })
                    result.addView(act.tv("反义词", 12f, T.ink2, true))
                    result.addView(act.tv(hit.second.ifBlank { "—" }, 14f, T.ink).apply { setPadding(0, act.dp(4), 0, 0) })
                }
            }
            addView(result)
            note("内置 ${DictData.synonymCount} 组常见近反义词。")
        })
    }

    // ---------------- 藏头诗 ----------------

    private fun acrostic(act: ToolActivity, body: LinearLayout) {
        var lines = 5
        body.addView(act.card {
            sectionTitle("藏头诗")
            val e = et("输入 2–8 个字，如：宝藏盒子", singleLine = true)
            label("每句字数")
            chips(listOf("五言", "七言"), 0) { i -> lines = if (i == 0) 5 else 7 }
            val outView = act.tv("—", 15f, T.ink3).apply {
                background = T.fieldBg()
                setPadding(act.dp(14), act.dp(14), act.dp(14), act.dp(14))
                setTextIsSelectable(true)
                setLineSpacing(act.dp(6).toFloat(), 1f)
            }
            btn("作诗") {
                val s = e.text?.toString()?.replace(Regex("\\s"), "").orEmpty()
                if (s.length !in 2..8) { Ui.toast(act, "请输入 2–8 个字"); return@btn }
                outView.text = Poem.acrostic(s, lines)
                outView.setTextColor(T.ink)
            }
            btn("再来一首", primary = false) {
                val s = e.text?.toString()?.replace(Regex("\\s"), "").orEmpty()
                if (s.length in 2..8) {
                    outView.text = Poem.acrostic(s, lines)
                    outView.setTextColor(T.ink)
                }
            }
            addView(outView)
            note("按意象词库拼句，追求可读与意境，非古典格律诗。")
        })
    }

    // ---------------- 对联 ----------------

    private fun couplet(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("对联")
            val e = et("输入上联（留空则随机名联）", singleLine = true)
            val outView = act.tv("—", 15f, T.ink3).apply {
                background = T.fieldBg()
                setPadding(act.dp(14), act.dp(14), act.dp(14), act.dp(14))
                setTextIsSelectable(true)
                setLineSpacing(act.dp(6).toFloat(), 1f)
            }
            btn("对下联") {
                val up = e.text?.toString()?.trim().orEmpty()
                outView.text = if (up.isEmpty()) Poem.randomCouplet()
                else Poem.replyCouplet(up)
                outView.setTextColor(T.ink)
            }
            btn("随机一副名联", primary = false) {
                outView.text = Poem.randomCouplet()
                outView.setTextColor(T.ink)
            }
            addView(outView)
            note("名联取自内置经典对联库；自拟下联按字数与词性拼句，仅供娱乐。")
        })
    }

    // ---------------- 摘要 ----------------

    private fun summarize(act: ToolActivity, body: LinearLayout) {
        var count = 3
        body.addView(act.card {
            sectionTitle("文章摘要")
            val e = et("粘贴文章正文（建议 200 字以上）", 12)
            label("摘要句数")
            chips(listOf("3 句", "5 句", "8 句"), 0) { i -> count = listOf(3, 5, 8)[i] }
            val outView = act.tv("—", 13.5f, T.ink3).apply {
                background = T.fieldBg()
                setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(12))
                setTextIsSelectable(true)
            }
            btn("生成摘要") {
                val s = e.text?.toString().orEmpty()
                if (s.length < 50) { Ui.toast(act, "文本太短，至少 50 字"); return@btn }
                outView.text = TextRank.summarize(s, count)
                outView.setTextColor(T.ink)
            }
            addView(outView)
            note("TextRank 抽取式算法：按词语共现给句子打分，挑出最能代表全文的句子。")
        })
    }
}
