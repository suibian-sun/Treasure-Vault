package com.suibian.treasure_vault.tools

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.tv
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 手持弹幕：全屏黑底滚动文字。 */
class MarqueeActivity : AppCompatActivity() {

    private lateinit var stage: LinearLayout
    private var running: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        hideSystemUi()

        stage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        setContentView(stage)
        showSetup()
    }

    private fun showSetup() {
        stage.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val input = EditText(this).apply {
            hint = "输入要滚动显示的文字"
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF94A3B8.toInt())
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = T.fillBg(0xFF1E293B.toInt(), dp(12).toFloat())
            setText("欢迎来我的摊位！")
        }
        box.addView(input, LinearLayout.LayoutParams(-1, -2))
        val start = tv("开始滚动", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(0xFF1890FF.toInt(), dp(12).toFloat())
            setPadding(0, dp(14), 0, dp(14))
            isClickable = true
            setOnClickListener {
                val text = input.text?.toString().orEmpty().ifBlank { "手持弹幕" }
                startMarquee(text)
            }
        }
        box.addView(start, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = dp(12) })
        box.addView(tv("提示：点屏幕任意位置可返回设置", 12f, 0xFF94A3B8.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })
        stage.addView(box)
    }

    private fun startMarquee(text: String) {
        stage.removeAllViews()
        val label = tv(text, 64f, Color.WHITE, true).apply {
            setSingleLine(true)
        }
        val scroller = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.BLACK)
            addView(label)
        }
        stage.addView(scroller, LinearLayout.LayoutParams(-1, -1))
        scroller.setOnClickListener { showSetup() }
        label.post {
            val w = label.width.toFloat().coerceAtLeast(1f)
            val anim = android.view.animation.TranslateAnimation(
                scroller.width.toFloat(), -w, 0f, 0f,
            ).apply {
                duration = ((scroller.width + w) / 260f * 1000).toLong().coerceAtLeast(3000)
                repeatCount = android.view.animation.Animation.INFINITE
            }
            label.startAnimation(anim)
        }
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}

/** 自拍补光灯：全屏纯色，点击切换色温。 */
class LightActivity : AppCompatActivity() {
    private val colors = listOf(
        0xFFFFFFFF.toInt() to "冷白",
        0xFFFFF1E0.toInt() to "暖白",
        0xFFFFE0C0.toInt() to "暖黄",
        0xFFE3F2FD.toInt() to "冷蓝",
    )
    private var index = 0
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { cycle() }
        }
        setContentView(root)
        apply()
    }

    private fun apply() {
        val (c, name) = colors[index]
        root.setBackgroundColor(c)
        root.removeAllViews()
        root.addView(tv(name, 15f, 0x66000000, true).apply {
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = T.fillBg(0x33FFFFFF, dp(99).toFloat())
        })
        root.addView(tv("点击切换色温 · 返回键退出", 12f, 0x66000000).apply {
            setPadding(0, dp(12), 0, 0)
        })
        runCatching {
            window.attributes = window.attributes.apply { screenBrightness = 1f }
        }
    }

    private fun cycle() {
        index = (index + 1) % colors.size
        apply()
    }
}

/** 时间屏幕：全屏时钟，可当床头钟。 */
class TimeScreenActivity : AppCompatActivity() {
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val fmtTime = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
    private val fmtDate = SimpleDateFormat("yyyy 年 M 月 d 日 EEEE", Locale.CHINA)

    private val tick = object : Runnable {
        override fun run() {
            val now = Date()
            clock.text = fmtTime.format(now)
            date.text = fmtDate.format(now)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        clock = tv("00:00:00", 64f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        date = tv("", 15f, 0xFF94A3B8.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        root.addView(clock)
        root.addView(date)
        root.addView(tv("轻点屏幕切换 12/24 小时", 11f, 0xFF64748B.toInt()).apply {
            setPadding(0, dp(24), 0, 0)
        })
        root.setOnClickListener {
            fmtTime.applyPattern(if (fmtTime.toPattern().startsWith("HH")) "hh:mm:ss a" else "HH:mm:ss")
            Ui.toast(this, "已切换")
        }
        setContentView(root)
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
