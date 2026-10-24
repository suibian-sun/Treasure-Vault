package com.suibian.treasure_vault.pages

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.pageBody
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv

/**
 * 主题设置 —— 参照 SunBuilder-grpc 的「THEME / 主题设置」版式：
 * 球体预览（上半=顶栏色，下半=画布色）+ 配色卡 + 明暗模式 + 导航栏样式。
 */
class ThemeActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        T.sync(this)
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(build())
    }

    private fun build(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.canvas)
        }
        root.addView(header())

        container = pageBody()
        render()
        root.addView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(container)
            },
            LinearLayout.LayoutParams(-1, 0, 1f),
        )
        return root
    }

    private fun header(): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(T.surface)
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        bar.addView(tv("‹", 30f, T.ink).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), 0, dp(6), 0)
            isClickable = true
            setOnClickListener { finish() }
        })
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv("主题设置", 16f, T.ink, true))
        col.addView(tv("配色 · 明暗 · 底部导航样式", 11.5f, T.ink2).apply { setPadding(0, dp(2), 0, 0) })
        bar.addView(col, LinearLayout.LayoutParams(0, -2, 1f).also { it.leftMargin = dp(4) })
        wrap.addView(bar)
        wrap.addView(hairline())
        return wrap
    }

    private fun render() {
        val prefs = Prefs.get(this)
        container.removeAllViews()

        // 主题色
        container.addView(card {
            sectionTitle("主题色", tv("球体预览：上为顶栏，下为画布", 10.5f, T.ink3))
            var i = 0
            while (i < T.presets.size) {
                val row = LinearLayout(this@ThemeActivity).apply { orientation = LinearLayout.HORIZONTAL }
                for (j in 0 until 2) {
                    val idx = i + j
                    if (idx >= T.presets.size) {
                        row.addView(LinearLayout(this@ThemeActivity), LinearLayout.LayoutParams(0, -2, 1f))
                        continue
                    }
                    row.addView(presetCard(T.presets[idx], prefs.themePreset == T.presets[idx].id),
                        LinearLayout.LayoutParams(0, -2, 1f).also {
                            it.rightMargin = if (j == 0) dp(10) else 0
                            it.bottomMargin = dp(10)
                        })
                }
                addView(row)
                i += 2
            }
        })

        // 明暗
        container.addView(card {
            sectionTitle("外观模式")
            val modes = listOf("light" to "浅色", "dark" to "深色", "system" to "跟随系统")
            chips(modes.map { it.second }, modes.indexOfFirst { it.first == prefs.themeMode }.coerceAtLeast(0)) { idx ->
                prefs.themeMode = modes[idx].first
                recreate()
            }
        })

        // 导航栏
        container.addView(card {
            sectionTitle("底部导航")
            val modes = listOf("icon_text" to "图标和文字", "icon" to "仅图标", "text" to "仅文字")
            chips(modes.map { it.second }, modes.indexOfFirst { it.first == prefs.navMode }.coerceAtLeast(0)) { idx ->
                prefs.navMode = modes[idx].first
                recreate()
            }
            addView(tv("切换后返回首页即可看到效果。", 11.5f, T.ink2))
        })

        container.addView(card {
            addView(tv("全部配色都经过对比度校验：正文与背景保持 ≥ 7:1，次要文字 ≥ 4.5:1；深色模式单独适配。", 11.5f, T.ink2))
        })
    }

    private fun presetCard(p: T.Preset, selected: Boolean): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(12))
            background = if (selected) {
                T.fillBg(T.selectedFill, dp(T.R_LARGE).toFloat()).apply {
                    setStroke(1.5f.toInt().coerceAtLeast(1), T.withAlpha(T.primary, 0x66))
                }
            } else {
                T.cardBg(dp(T.R_LARGE).toFloat(), T.surface)
            }
            isClickable = true
            setOnClickListener {
                Prefs.get(this@ThemeActivity).themePreset = p.id
                recreate()
            }
        }
        box.addView(SphereView(this, p.primary, if (T.dark) 0xFF0F1115.toInt() else 0xFFF5F6F8.toInt()),
            LinearLayout.LayoutParams(dp(56), dp(56)))
        box.addView(tv(p.name, 13.5f, T.ink, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        box.addView(tv(p.desc, 10f, T.ink2).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, 0)
            maxLines = 2
        })
        if (selected) {
            box.addView(tv("✓ 当前使用", 10f, T.primary, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            })
        }
        return box
    }

    /** 球体预览：上半顶栏色、下半画布色，外加一圈发丝描边。 */
    private class SphereView(ctx: Context, private val top: Int, private val bottom: Int) : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density * 1f
            color = 0x22000000
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val r = minOf(w, h) / 2 - 1f
            val cx = w / 2
            val cy = h / 2
            paint.color = bottom
            canvas.drawCircle(cx, cy, r, paint)
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            paint.color = top
            canvas.drawArc(rect, 180f, 180f, true, paint)
            canvas.drawCircle(cx, cy, r, ring)
        }
    }
}
