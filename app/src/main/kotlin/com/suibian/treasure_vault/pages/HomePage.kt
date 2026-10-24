package com.suibian.treasure_vault.pages

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.suibian.treasure_vault.MainActivity
import com.suibian.treasure_vault.core.Gap
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.Registry
import com.suibian.treasure_vault.core.Router
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.iconBadge
import com.suibian.treasure_vault.core.pageBody
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv

/** 首页：收藏 + 最近使用。 */
class HomePage(private val act: MainActivity) {

    fun view(): View {
        val prefs = Prefs.get(act)
        val col = act.pageBody()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greet = when {
            hour < 6 -> "夜深了"
            hour < 11 -> "早上好"
            hour < 14 -> "中午好"
            hour < 18 -> "下午好"
            else -> "晚上好"
        }

        // 顶部标题
        val head = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(4), act.dp(6), act.dp(4), act.dp(14))
        }
        head.addView(act.tv("宝藏盒", 24f, T.ink, true))
        head.addView(
            act.tv("$greet · ${Registry.tools.size} 个工具，全部离线可用", 11.5f, T.ink2)
                .apply { setPadding(0, act.dp(4), 0, 0) },
        )
        col.addView(head)

        // 概览卡
        col.addView(act.card { 
            val row = LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(act.tv("📦", 26f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_MEDIUM).toFloat())
                layoutParams = LinearLayout.LayoutParams(act.dp(52), act.dp(52))
            })
            val texts = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(act.dp(14), 0, 0, 0)
            }
            texts.addView(act.tv("轻装上阵的工具箱", 15f, T.ink, true))
            texts.addView(
                act.tv("${Registry.tools.size} 个工具 · ${Registry.categories.size} 大分类 · 零第三方 SDK", 11.5f, T.ink2)
                    .apply { setPadding(0, act.dp(4), 0, 0) },
            )
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
            addView(row)
        })

        // 收藏
        val favIds = prefs.favorites()
        val favs = favIds.mapNotNull { Registry.byId(it) }
        col.addView(act.card {
            sectionTitle("我的收藏")
            if (favs.isEmpty()) {
                addView(emptyHint("★", "还没有收藏", "在工具页右上角点星标，常用工具会固定在这里"))
            } else {
                addView(grid(favs))
            }
        })

        // 最近
        val recents = prefs.recents().mapNotNull { Registry.byId(it) }
        col.addView(act.card {
            sectionTitle("最近使用")
            if (recents.isEmpty()) {
                addView(emptyHint("🕘", "暂无使用记录", "打开任意工具后，这里会出现快捷入口"))
            } else {
                recents.take(8).forEachIndexed { i, t ->
                    addView(recentRow(t, i == recents.take(8).lastIndex))
                }
            }
        })

        col.addView(
            act.tv("全部工具均在本机运行，不上传任何数据。", 10.5f, T.ink3)
                .apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(6) },
        )

        return ScrollView(act).apply {
            isFillViewport = true
            addView(col)
        }
    }

    private fun grid(tools: List<ToolSpec>): View {
        val wrap = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var i = 0
        while (i < tools.size) {
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            for (j in 0 until 4) {
                val idx = i + j
                val cell = LinearLayout(act).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    if (idx < tools.size) {
                        val t = tools[idx]
                        addView(act.iconBadge(t.emoji, 44))
                        addView(act.tv(t.name, 11f, T.ink).apply {
                            gravity = Gravity.CENTER
                            setPadding(0, act.dp(6), 0, 0)
                            maxLines = 1
                        })
                        isClickable = true
                        setOnClickListener { Router.open(act, t) }
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).also {
                    it.bottomMargin = act.dp(10)
                })
            }
            wrap.addView(row)
            i += 4
        }
        return wrap
    }

    private fun recentRow(t: ToolSpec, last: Boolean): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, act.dp(9), 0, act.dp(9))
            isClickable = true
            setOnClickListener { Router.open(act, t) }
        }
        row.addView(act.iconBadge(t.emoji, 34))
        val texts = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(12), 0, 0, 0)
        }
        texts.addView(act.tv(t.name, 13.5f, T.ink, true))
        texts.addView(act.tv(t.category.name, 11f, T.ink2).apply { setPadding(0, act.dp(2), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(act.tv("›", 18f, T.ink3))
        val wrap = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        if (!last) wrap.addView(act.hairline().apply {
            setPadding(act.dp(46), 0, 0, 0)
        })
        return wrap
    }

    private fun emptyHint(icon: String, title: String, desc: String): View =
        LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, act.dp(18), 0, act.dp(18))
            addView(act.iconBadge(icon, 48))
            addView(act.tv(title, 13f, T.ink, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, act.dp(10), 0, 0)
            })
            addView(act.tv(desc, 11.5f, T.ink2).apply {
                gravity = Gravity.CENTER
                setPadding(0, act.dp(4), 0, 0)
            })
        }
}
