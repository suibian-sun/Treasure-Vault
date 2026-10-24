package com.suibian.treasure_vault.pages

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.suibian.treasure_vault.MainActivity
import com.suibian.treasure_vault.core.Category
import com.suibian.treasure_vault.core.Gap
import com.suibian.treasure_vault.core.Registry
import com.suibian.treasure_vault.core.Router
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.iconBadge
import com.suibian.treasure_vault.core.tv

/** 全部工具：搜索 + 分类 + 分组列表。 */
class AllPage(private val act: MainActivity) {

    private var query = ""
    private var catId: String? = null
    private lateinit var listBox: LinearLayout

    fun view(): View {
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.canvas)
        }

        // 头部
        val head = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.surface)
            setPadding(act.dp(16), act.dp(12), act.dp(16), act.dp(12))
        }
        head.addView(act.tv("全部工具", 19f, T.ink, true))
        head.addView(
            act.tv("${Registry.tools.size} 个工具 · ${Registry.categories.size} 大分类", 11.5f, T.ink2)
                .apply { setPadding(0, act.dp(3), 0, act.dp(10)) },
        )
        val search = EditText(act).apply {
            hint = "搜索工具名称、用途或关键词"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTextColor(T.ink)
            setHintTextColor(T.ink3)
            background = T.fillBg(T.field, act.dp(T.R_PILL).toFloat())
            setPadding(act.dp(16), act.dp(10), act.dp(16), act.dp(10))
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    query = s?.toString().orEmpty()
                    renderList()
                }
            })
        }
        head.addView(search, LinearLayout.LayoutParams(-1, -2))
        val headWrap = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        headWrap.addView(head)
        headWrap.addView(act.hairline())
        root.addView(headWrap)

        // 分类 chips
        val chipBox = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.surface)
            setPadding(act.dp(16), act.dp(8), act.dp(16), act.dp(6))
        }
        val labels = listOf("全部 ${Registry.tools.size}") +
            Registry.categories.map { "${it.name} ${Registry.of(it).size}" }
        chipBox.chips(labels, if (catId == null) 0 else Registry.categories.indexOfFirst { it.id == catId } + 1) { i ->
            catId = if (i == 0) null else Registry.categories[i - 1].id
            rebuildChips(chipBox)
            renderList()
        }
        root.addView(chipBox)

        listBox = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(16), act.dp(10), act.dp(16), act.dp(28))
        }
        root.addView(
            ScrollView(act).apply {
                isFillViewport = true
                addView(listBox)
            },
            LinearLayout.LayoutParams(-1, 0, 1f),
        )

        renderList()
        return root
    }

    private fun rebuildChips(box: LinearLayout) {
        box.removeAllViews()
        val labels = listOf("全部 ${Registry.tools.size}") +
            Registry.categories.map { "${it.name} ${Registry.of(it).size}" }
        box.chips(labels, if (catId == null) 0 else Registry.categories.indexOfFirst { it.id == catId } + 1) { i ->
            catId = if (i == 0) null else Registry.categories[i - 1].id
            rebuildChips(box)
            renderList()
        }
    }

    private fun renderList() {
        if (!::listBox.isInitialized) return
        listBox.removeAllViews()
        val filtered = Registry.search(query).filter { catId == null || it.category.id == catId }
        if (filtered.isEmpty()) {
            listBox.addView(empty("没有匹配的工具", "换个关键词，或切换到「全部」分类"))
            return
        }
        if (query.isBlank() && catId == null) {
            Registry.categories.forEach { c ->
                val items = Registry.of(c)
                if (items.isEmpty()) return@forEach
                listBox.addView(catHeader(c, items.size))
                listBox.addView(toolCard(items))
            }
        } else {
            listBox.addView(
                act.tv("找到 ${filtered.size} 个工具", 11.5f, T.ink2).apply { setPadding(act.dp(4), 0, 0, act.dp(8)) },
            )
            listBox.addView(toolCard(filtered))
        }
    }

    private fun catHeader(c: Category, count: Int): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dp(4), act.dp(10), act.dp(4), act.dp(8))
        }
        row.addView(act.tv(c.emoji, 14f, T.primary))
        row.addView(act.tv(c.name, 13.5f, T.ink, true).apply { setPadding(act.dp(6), 0, act.dp(6), 0) })
        row.addView(act.tv(c.desc, 11f, T.ink2))
        row.addView(act.tv("$count", 11f, T.ink3), LinearLayout.LayoutParams(0, -2, 1f).also {
            (it as LinearLayout.LayoutParams).gravity = Gravity.END
        })
        return row
    }

    private fun toolCard(items: List<ToolSpec>): View {
        val box = act.card(pad = 0) {
            items.forEachIndexed { i, t ->
                if (i > 0) addView(act.hairline().apply { setPadding(act.dp(60), 0, 0, 0) })
                addView(toolRow(t))
            }
        }
        return box
    }

    private fun toolRow(t: ToolSpec): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dp(14), act.dp(11), act.dp(14), act.dp(11))
            isClickable = true
            setOnClickListener { Router.open(act, t) }
        }
        row.addView(act.iconBadge(t.emoji, 36))
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(12), 0, act.dp(8), 0)
        }
        col.addView(act.tv(t.name, 14f, T.ink, true))
        col.addView(act.tv(t.desc, 11.5f, T.ink2).apply { setPadding(0, act.dp(2), 0, 0) })
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(act.tv("›", 18f, T.ink3))
        return row
    }

    private fun empty(title: String, desc: String): View =
        LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, act.dp(40), 0, act.dp(20))
            addView(act.iconBadge("🔍", 48))
            addView(act.tv(title, 13.5f, T.ink, true).apply {
                gravity = Gravity.CENTER; setPadding(0, act.dp(10), 0, 0)
            })
            addView(act.tv(desc, 11.5f, T.ink2).apply {
                gravity = Gravity.CENTER; setPadding(0, act.dp(4), 0, 0)
            })
        }
}
