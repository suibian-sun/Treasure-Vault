package com.suibian.treasure_vault.tools

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.Router
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import kotlin.random.Random

/** 生活娱乐类：骰子 / 抽签 / 硬币都带真实动画。 */
object LifeTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.life, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("dice", "掷骰子", "D4~D100，多颗同掷，带滚动动画", "🎲", "骰子", "dice") { a, b -> dice(a, b) },
        spec("picker", "随机抽签", "从名单抽取，带滚动抽签动画", "🎟️", "抽签", "抽奖") { a, b -> picker(a, b) },
        spec("coin", "抛硬币", "3D 翻转动画与正反统计", "🪙", "硬币", "coin") { a, b -> coin(a, b) },
        spec("decide", "做个决定", "选项轮盘，交给运气", "🤔", "决定", "选择") { a, b -> decide(a, b) },
        spec("scoreboard", "记分牌", "两队计分，可保存", "🏅", "记分", "比分") { a, b -> scoreboard(a, b) },
        spec("reaction", "反应力测试", "颜色变化瞬间点击，测毫秒", "⚡", "反应", "测试") { a, b -> reaction(a, b) },
        spec("life_grid", "人生小格", "以周为单位看一生", "🟦", "人生", "格子") { a, b -> lifeGrid(a, b) },
        spec("stopwatch", "秒表", "计时与分段记录", "⏲️", "秒表", "计时") { a, b -> stopwatch(a, b) },
        spec("marquee", "手持弹幕", "全屏滚动字幕，接机应援", "📣", "弹幕", "应援") { a, b ->
            body(a, b, "手持弹幕", "全屏滚动文字，可调速度与字号", "🏮") {
                Router.openActivity(a, MarqueeActivity::class.java)
            }
        },
        spec("light", "自拍补光灯", "全屏白光 / 暖光补光", "💡", "补光", "自拍") { a, b ->
            body(a, b, "自拍补光灯", "屏幕变身为补光灯，支持冷暖色温", "🔆") {
                Router.openActivity(a, LightActivity::class.java)
            }
        },
    )

    private fun body(act: ToolActivity, parent: LinearLayout, title: String, desc: String, emoji: String, onOpen: () -> Unit) {
        parent.addView(act.card {
            sectionTitle(title)
            addView(act.tv(desc, 12.5f, T.ink2))
            btn("打开 $title") { onOpen() }
        })
    }

    // ------------------------------------------------------------------
    // 骰子（滚动动画）
    // ------------------------------------------------------------------

    private fun dice(act: ToolActivity, body: LinearLayout) {
        var sides = 6
        var count = 1
        val faces = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, act.dp(10), 0, act.dp(10))
        }
        val total = act.tv("合计 —", 13f, T.ink2)

        fun roll() {
            row.removeAllViews()
            val results = (1..count).map { Random.nextInt(1, sides + 1) }
            val views = results.map { v ->
                act.tv(if (sides == 6) faces[v - 1] else "$v", if (sides == 6) 54f else 30f, T.primary, true)
                    .apply {
                        gravity = Gravity.CENTER
                        background = T.fillBg(T.selectedFill, act.dp(T.R_MEDIUM).toFloat())
                    }
            }
            views.forEach { row.addView(it, LinearLayout.LayoutParams(act.dp(72), act.dp(72)).also { lp -> lp.rightMargin = act.dp(8) }) }
            total.text = "合计 ${results.sum()}"

            // 滚动：快速切换面值 + 缩放回弹
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 700
                interpolator = DecelerateInterpolator()
                addUpdateListener { a ->
                    val p = a.animatedValue as Float
                    views.forEachIndexed { i, v ->
                        val face = if (sides == 6) faces[Random.nextInt(6)] else "${Random.nextInt(1, sides + 1)}"
                        v.text = if (p < 0.92f) face else if (sides == 6) faces[results[i] - 1] else "${results[i]}"
                        val s = 1f + 0.12f * (1f - p)
                        v.scaleX = s; v.scaleY = s
                        v.rotation = (1f - p) * 12f * (if (i % 2 == 0) 1 else -1)
                    }
                }
            }
            anim.start()
        }

        body.addView(act.card {
            sectionTitle("骰子设置")
            addView(act.tv("面数", 12f, T.ink2).apply { setPadding(0, 0, 0, act.dp(6)) })
            chips(listOf("D4", "D6", "D8", "D12", "D20", "D100"), 1) { i ->
                sides = listOf(4, 6, 8, 12, 20, 100)[i]
            }
            addView(act.tv("数量", 12f, T.ink2).apply { setPadding(0, act.dp(8), 0, act.dp(6)) })
            chips(listOf("1", "2", "3", "4", "5"), 0) { i -> count = i + 1 }
            btn("掷骰子") { roll() }
        })
        body.addView(act.card {
            sectionTitle("结果")
            addView(row)
            addView(total)
        })
        roll()
    }

    // ------------------------------------------------------------------
    // 抽签（老虎机动画）
    // ------------------------------------------------------------------

    private fun picker(act: ToolActivity, body: LinearLayout) {
        var count = 1
        var sequential = true
        val display = act.tv("?", 30f, T.primary, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.selectedFill, act.dp(T.R_LARGE).toFloat())
            setPadding(0, act.dp(24), 0, act.dp(24))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(10) }
        }
        val resultBox = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var input: android.widget.EditText? = null
        var busy = false

        fun options(): List<String> =
            input?.text?.toString().orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }

        fun draw() {
            val items = options()
            if (items.isEmpty()) { Ui.toast(act, "请先填写候选名单"); return }
            if (busy) return
            busy = true
            resultBox.removeAllViews()
            val picks = if (sequential) items.shuffled().take(count.coerceAtMost(items.size))
            else (1..count).map { items.random() }
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900
                interpolator = DecelerateInterpolator()
                addUpdateListener { a ->
                    val p = a.animatedValue as Float
                    if (p < 0.9f) {
                        display.text = items.random()
                        display.alpha = 0.5f + 0.5f * p
                    } else {
                        display.text = picks.firstOrNull() ?: "?"
                        display.alpha = 1f
                    }
                }
            }
            anim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    busy = false
                    display.text = picks.firstOrNull() ?: "?"
                    picks.forEachIndexed { i, s ->
                        val row = LinearLayout(act).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(0, act.dp(5), 0, act.dp(5))
                        }
                        row.addView(act.tv("${i + 1}.", 12f, T.ink2), LinearLayout.LayoutParams(act.dp(26), -2))
                        row.addView(act.tv(s, 14f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
                        resultBox.addView(row)
                    }
                }
            })
            anim.start()
        }

        body.addView(act.card {
            sectionTitle("候选名单（每行一项）")
            val e = et("苹果\n香蕉\n橙子\n葡萄\n西瓜", 7)
            e.setText("苹果\n香蕉\n橙子\n葡萄\n西瓜")
            input = e
            addView(act.tv("抽取数量", 12f, T.ink2).apply { setPadding(0, act.dp(6), 0, act.dp(6)) })
            chips(listOf("1", "2", "3", "5"), 0) { i -> count = listOf(1, 2, 3, 5)[i] }
            addView(act.tv("模式", 12f, T.ink2).apply { setPadding(0, act.dp(6), 0, act.dp(6)) })
            chips(listOf("不重复", "可重复"), 0) { i -> sequential = i == 0 }
            btn("开始抽取") { draw() }
        })
        body.addView(act.card {
            sectionTitle("抽签结果")
            addView(display)
            addView(resultBox)
        })
        draw()
    }

    // ------------------------------------------------------------------
    // 硬币（3D 翻转）
    // ------------------------------------------------------------------

    private fun coin(act: ToolActivity, body: LinearLayout) {
        var heads = 0
        var tails = 0
        var busy = false
        val face = act.tv("🪙", 60f, T.primary, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.selectedFill, act.dp(T.R_LARGE).toFloat())
            setPadding(0, act.dp(26), 0, act.dp(26))
        }
        val label = act.tv("点击下方按钮抛硬币", 12.5f, T.ink2).apply { gravity = Gravity.CENTER }
        val stat = act.tv("正面 0 · 反面 0", 12.5f, T.ink2).apply { gravity = Gravity.CENTER }

        fun flip() {
            if (busy) return
            busy = true
            val isHead = Random.nextBoolean()
            face.cameraDistance = 12000f
            val anim = ValueAnimator.ofFloat(0f, 1800f).apply {
                duration = 900
                interpolator = DecelerateInterpolator()
                addUpdateListener { a ->
                    val v = a.animatedValue as Float
                    face.rotationY = v
                    face.text = if ((v / 180).toInt() % 2 == 0) "正" else "反"
                    face.scaleX = 1f - 0.12f * kotlin.math.abs(kotlin.math.sin(Math.toRadians(v.toDouble())).toFloat())
                }
            }
            anim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    face.rotationY = 0f
                    face.scaleX = 1f
                    face.text = if (isHead) "正" else "反"
                    if (isHead) heads++ else tails++
                    label.text = if (isHead) "正面朝上" else "反面朝上"
                    stat.text = "正面 $heads · 反面 $tails"
                    busy = false
                }
            })
            anim.start()
        }

        body.addView(act.card {
            addView(face, LinearLayout.LayoutParams(-1, -2))
            addView(label, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
            btn("抛硬币") { flip() }
            addView(stat)
        })
        body.note("动画为 3D 翻转；统计只保存在当前页面。")
    }

    // ------------------------------------------------------------------
    // 做个决定
    // ------------------------------------------------------------------

    private fun decide(act: ToolActivity, body: LinearLayout) {
        val display = act.tv("?", 28f, T.primary, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.selectedFill, act.dp(T.R_LARGE).toFloat())
            setPadding(0, act.dp(26), 0, act.dp(26))
        }
        var input: android.widget.EditText? = null
        fun spin() {
            val items = input?.text?.toString().orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (items.isEmpty()) { Ui.toast(act, "先写几个选项"); return }
            val pick = items.random()
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800
                interpolator = DecelerateInterpolator()
                addUpdateListener { a ->
                    val p = a.animatedValue as Float
                    display.text = if (p < 0.9f) items.random() else pick
                }
            }
            anim.start()
        }
        body.addView(act.card {
            sectionTitle("选项（每行一个）")
            val e = et("吃火锅\n吃烧烤\n点外卖", 5)
            e.setText("吃火锅\n吃烧烤\n点外卖")
            input = e
            btn("帮我决定") { spin() }
        })
        body.addView(act.card { sectionTitle("结果"); addView(display) })
    }

    // ------------------------------------------------------------------
    // 记分牌
    // ------------------------------------------------------------------

    private fun scoreboard(act: ToolActivity, body: LinearLayout) {
        val prefs = Prefs.get(act)
        var a = prefs.str("score.a", "0").toIntOrNull() ?: 0
        var b = prefs.str("score.b", "0").toIntOrNull() ?: 0
        val aText = act.tv("$a", 40f, T.primary, true).apply { gravity = Gravity.CENTER }
        val bText = act.tv("$b", 40f, T.ink, true).apply { gravity = Gravity.CENTER }
        fun save() {
            prefs.put("score.a", "$a"); prefs.put("score.b", "$b")
            aText.text = "$a"; bText.text = "$b"
        }

        fun team(name: String, isA: Boolean): View {
            val col = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = T.fillBg(T.field, act.dp(T.R_MEDIUM).toFloat())
                setPadding(act.dp(10), act.dp(14), act.dp(10), act.dp(14))
            }
            col.addView(act.tv(name, 12.5f, T.ink2))
            col.addView(if (isA) aText else bText)
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            listOf("-1", "+1", "+2").forEach { s ->
                val delta = s.toInt()
                row.addView(act.tv(s, 13f, T.ink, true).apply {
                    gravity = Gravity.CENTER
                    background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.surface)
                    setPadding(act.dp(10), act.dp(7), act.dp(10), act.dp(7))
                    isClickable = true
                    setOnClickListener {
                        if (isA) a = (a + delta).coerceAtLeast(0) else b = (b + delta).coerceAtLeast(0)
                        save()
                    }
                }, LinearLayout.LayoutParams(-2, -2).also { it.rightMargin = act.dp(6) })
            }
            col.addView(row)
            return col
        }

        body.addView(act.card {
            sectionTitle("比分")
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(team("A 队", true), LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(10) })
            row.addView(team("B 队", false), LinearLayout.LayoutParams(0, -2, 1f))
            addView(row)
            btn("清零") { a = 0; b = 0; save() }
        })
        body.note("比分自动保存，下次打开继续。")
    }

    // ------------------------------------------------------------------
    // 反应力测试
    // ------------------------------------------------------------------

    private fun reaction(act: ToolActivity, body: LinearLayout) {
        val area = act.tv("点击开始", 18f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(0xFF64748B.toInt(), act.dp(T.R_LARGE).toFloat())
            setPadding(0, act.dp(60), 0, act.dp(60))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        var startAt = 0L
        var waiting = false
        val handler = Handler(Looper.getMainLooper())
        var best = 0

        area.setOnClickListener {
            when {
                !waiting && startAt == 0L -> {
                    area.text = "等待变绿…"
                    area.background = T.fillBg(0xFFDC2626.toInt(), act.dp(T.R_LARGE).toFloat())
                    waiting = true
                    handler.postDelayed({
                        startAt = SystemClock.elapsedRealtime()
                        area.text = "点！"
                        area.background = T.fillBg(T.ok, act.dp(T.R_LARGE).toFloat())
                    }, Random.nextLong(1200, 3500))
                }
                waiting && startAt == 0L -> {
                    handler.removeCallbacksAndMessages(null)
                    waiting = false
                    area.text = "太早了，重新开始"
                    area.background = T.fillBg(0xFF64748B.toInt(), act.dp(T.R_LARGE).toFloat())
                }
                else -> {
                    val ms = SystemClock.elapsedRealtime() - startAt
                    if (best == 0 || ms < best) best = ms.toInt()
                    area.text = "反应 ${ms}ms\n最快 ${best}ms\n点击再来一次"
                    area.background = T.fillBg(T.primary, act.dp(T.R_LARGE).toFloat())
                    startAt = 0L; waiting = false
                }
            }
        }
        body.addView(act.card { sectionTitle("测试"); addView(area) })
        body.note("屏幕变绿后立刻点击，测量从变绿到点击的毫秒数。")
    }

    // ------------------------------------------------------------------
    // 人生小格
    // ------------------------------------------------------------------

    private fun lifeGrid(act: ToolActivity, body: LinearLayout) {
        val view = object : View(act) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                val cols = 52
                val rows = 80
                val gap = width / (cols * 1.15f)
                val cell = gap * 0.85f
                val top = 0f
                val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val age = 30
                val passedWeeks = age * 52
                for (y in 0 until rows) {
                    for (w in 0 until cols) {
                        val idx = y * 52 + w
                        paint.color = when {
                            idx < passedWeeks -> 0xFF1890FF.toInt()
                            y < age + 10 -> T.withAlpha(T.primary, 0x55)
                            else -> T.line
                        }
                        val x = w * (cell + gap * 0.15f)
                        val yy = top + y * (cell + gap * 0.15f)
                        canvas.drawRoundRect(x, yy, x + cell, yy + cell, 1f, 1f, paint)
                    }
                }
            }
        }
        body.addView(act.card {
            sectionTitle("你的一生（80 年 × 52 周）")
            addView(view, LinearLayout.LayoutParams(-1, act.dp(420)))
            addView(act.tv("蓝色：已走过（按 30 岁估算）；浅蓝：未来 10 年；灰色：剩余时间。", 11f, T.ink2)
                .apply { setPadding(0, act.dp(8), 0, 0) })
        })
    }

    // ------------------------------------------------------------------
    // 秒表
    // ------------------------------------------------------------------

    private fun stopwatch(act: ToolActivity, body: LinearLayout) {
        var running = false
        var startAt = 0L
        var acc = 0L
        val laps = mutableListOf<Long>()
        val display = act.tv("00:00.00", 44f, T.ink).apply { gravity = Gravity.CENTER }
        val lapBox = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val handler = Handler(Looper.getMainLooper())

        fun elapsed(): Long = acc + if (running) SystemClock.elapsedRealtime() - startAt else 0

        fun fmt(ms: Long): String {
            val m = ms / 60000
            val s = ms % 60000 / 1000
            val cs = ms % 1000 / 10
            return "%02d:%02d.%02d".format(m, s, cs)
        }

        val tick = object : Runnable {
            override fun run() {
                display.text = fmt(elapsed())
                if (running) handler.postDelayed(this, 50)
            }
        }

        val toggle = act.tv("开始", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
            setPadding(0, act.dp(12), 0, act.dp(12))
            isClickable = true
            setOnClickListener {
                if (running) {
                    acc = elapsed(); running = false; text = "继续"
                } else {
                    running = true; startAt = SystemClock.elapsedRealtime(); text = "暂停"
                    handler.post(tick)
                }
            }
        }
        val reset = act.tv("重置", 14f, T.ink, true).apply {
            gravity = Gravity.CENTER
            background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.surface)
            setPadding(0, act.dp(12), 0, act.dp(12))
            isClickable = true
            setOnClickListener {
                running = false; acc = 0; startAt = 0
                display.text = "00:00.00"; laps.clear(); lapBox.removeAllViews(); text = "开始"
            }
        }
        val lap = act.tv("分段", 14f, T.ink, true).apply {
            gravity = Gravity.CENTER
            background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.surface)
            setPadding(0, act.dp(12), 0, act.dp(12))
            isClickable = true
            setOnClickListener {
                if (running) {
                    val v = elapsed()
                    laps.add(0, v)
                    lapBox.removeAllViews()
                    laps.take(12).forEachIndexed { i, ms ->
                        lapBox.addView(act.tv("#${laps.size - i}   ${fmt(ms)}", 12.5f, T.ink2)
                            .apply { setPadding(0, act.dp(4), 0, 0) })
                    }
                }
            }
        }

        body.addView(act.card {
            sectionTitle("秒表")
            addView(display, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(12) })
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(reset, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            row.addView(toggle, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            row.addView(lap, LinearLayout.LayoutParams(0, -2, 1f))
            addView(row)
        })
        body.addView(act.card { sectionTitle("分段记录"); addView(lapBox) })
    }
}
