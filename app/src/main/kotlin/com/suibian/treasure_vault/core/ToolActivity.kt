package com.suibian.treasure_vault.core

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 所有工具的宿主页面。
 *
 * 只负责：顶栏（返回 / 标题 / 说明 / 收藏）、内容容器与打开/关闭过渡动画。
 * 工具本身只关心往 [body] 里塞控件，因此 140 个工具共享同一套版式。
 */
class ToolActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "tool_id"
    }

    lateinit var spec: ToolSpec
    lateinit var body: LinearLayout
    private lateinit var star: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        T.sync(this)
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        spec = Registry.byId(id) ?: run { finish(); return }
        Prefs.get(this).markUsed(spec.id)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setLightBars(!T.dark)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.canvas)
        }
        root.addView(header())

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        body = content
        spec.build(this, content)

        val bodyView: View = if (spec.scroll) {
            ScrollView(this).apply {
                isFillViewport = true
                addView(content)
            }
        } else {
            content.setPadding(0, 0, 0, 0)
            content
        }
        root.addView(bodyView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        playEnter()
    }

    private fun header(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(T.surface)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        val back = tv("‹", 30f, T.ink).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), 0, dp(6), 0)
            isClickable = true
            setOnClickListener { finish() }
        }
        bar.addView(back, LinearLayout.LayoutParams(-2, -2))

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv(spec.name, 16f, T.ink, true))
        col.addView(tv(spec.desc, 11.5f, T.ink2).apply { setPadding(0, dp(2), 0, 0) })
        bar.addView(col, LinearLayout.LayoutParams(0, -2, 1f).also {
            it.leftMargin = dp(4); it.rightMargin = dp(8)
        })

        star = tv(if (Prefs.get(this).isFavorite(spec.id)) "★" else "☆", 22f, T.warn).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            isClickable = true
            setOnClickListener {
                val added = Prefs.get(this@ToolActivity).toggleFavorite(spec.id)
                text = if (added) "★" else "☆"
                Ui.toast(this@ToolActivity, if (added) "已加入收藏" else "已取消收藏")
            }
        }
        bar.addView(star, LinearLayout.LayoutParams(-2, -2))

        val wrap = FrameLayout(this).apply { addView(bar) }
        wrap.addView(View(this).apply { setBackgroundColor(T.line) },
            FrameLayout.LayoutParams(-1, dp(1), Gravity.BOTTOM))
        return wrap
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 0x51 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        runCatching {
            val wm = getSystemService(android.app.WallpaperManager::class.java)
            contentResolver.openInputStream(uri)?.use { wm.setStream(it) }
            Ui.toast(this, "壁纸已设置")
        }.onFailure { Ui.toast(this, "设置失败：${it.message}") }
    }

    override fun finish() {
        super.finish()
        if (Prefs.get(this).lowMemory) {
            overridePendingTransition(0, 0)
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun playEnter() {
        if (Prefs.get(this).lowMemory) return
        val v = window.decorView
        v.alpha = 0f
        v.animate().alpha(1f).setDuration(160).start()
    }

    private fun setLightBars(light: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (light) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                0
            }
        }
    }
}
