package com.suibian.treasure_vault

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.suibian.treasure_vault.core.Gap
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.tv
import com.suibian.treasure_vault.pages.AllPage
import com.suibian.treasure_vault.pages.HomePage
import com.suibian.treasure_vault.pages.MinePage

/**
 * 主框架：首页 / 全部 / 我的 三个页面。
 *
 * 只保留当前页面的 View 树（切换即移除旧页），配合原生 View 方案，
 * 常驻内存保持在很低的水平。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var content: FrameLayout
    private lateinit var bar: LinearLayout
    private var index = 0
    private var appliedPreset = ""
    private var appliedDark = false

    override fun onCreate(savedInstanceState: Bundle?) {
        T.sync(this)
        appliedPreset = T.preset.id
        appliedDark = T.dark
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        lightBars(!T.dark)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.canvas)
        }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        bar = buildBar()
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, 0)
            bar.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        show(0, false)
    }

    override fun onResume() {
        super.onResume()
        // 从主题设置返回或系统深色模式变化时，重建以应用新主题
        T.sync(this)
        if (T.preset.id != appliedPreset || T.dark != appliedDark) {
            recreate()
        }
    }

    private fun buildBar(): LinearLayout {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(hairline())
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(T.surface)
        }
        val mode = Prefs.get(this).navMode
        val items = listOf(Triple("🏠", "首页", 0), Triple("▦", "全部", 1), Triple("👤", "我的", 2))
        items.forEach { (icon, label, i) ->
            val tab = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(9), 0, dp(8))
                isClickable = true
                setOnClickListener { show(i, true) }
            }
            tab.addView(tv(icon, 17f, T.ink2).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
            if (mode != "icon") {
                tab.addView(
                    tv(label, 11f, T.ink2).apply { gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0) },
                    LinearLayout.LayoutParams(-1, -2),
                )
            }
            row.addView(tab, LinearLayout.LayoutParams(0, -2, 1f))
        }
        wrap.addView(row)
        return wrap
    }

    private fun refreshBar() {
        val mode = Prefs.get(this).navMode
        (bar.getChildAt(1) as LinearLayout).let { row ->
            for (i in 0 until row.childCount) {
                val tab = row.getChildAt(i) as LinearLayout
                val active = i == index
                for (j in 0 until tab.childCount) {
                    val t = tab.getChildAt(j) as TextView
                    t.setTextColor(if (active) T.primary else T.ink2)
                }
                // 仅图标模式下没有文字子项
                if (mode == "icon" && tab.childCount > 1) tab.removeViewAt(1)
            }
        }
    }

    fun show(i: Int, animate: Boolean) {
        index = i
        content.removeAllViews()
        val page: View = when (i) {
            0 -> HomePage(this).view()
            1 -> AllPage(this).view()
            else -> MinePage(this).view()
        }
        content.addView(page, FrameLayout.LayoutParams(-1, -1))
        refreshBar()
        if (animate && !Prefs.get(this).lowMemory) {
            page.alpha = 0f
            page.translationY = dp(8).toFloat()
            page.animate().alpha(1f).translationY(0f).setDuration(180).start()
        }
    }

    fun goAll() = show(1, true)

    private fun lightBars(light: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                if (light) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
        }
    }
}
