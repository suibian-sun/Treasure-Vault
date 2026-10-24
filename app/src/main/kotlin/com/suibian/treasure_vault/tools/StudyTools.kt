package com.suibian.treasure_vault.tools

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.Router
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import kotlin.random.Random

/** 学习专注类。 */
object StudyTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.study, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("pomodoro", "番茄时钟", "25 分钟专注 + 5 分钟休息", "🍅", "番茄", "专注") { a, b -> pomodoro(a, b) },
        spec("sudoku", "数独", "内置题库，点击填数", "🔢", "数独", "sudoku") { a, b -> sudoku(a, b) },
        spec("schulte", "舒尔特方格", "5×5 按顺序点击，测注意力", "🔳", "舒尔特", "注意力") { a, b -> schulte(a, b) },
        spec("mbti", "MBTI 性格测试", "16 题快速版，得出四字母类型", "🧠", "mbti", "性格") { a, b -> mbti(a, b) },
        spec("time_screen", "时间屏幕", "全屏时钟，床头 / 桌面都合适", "🕛", "时钟", "全屏") { a, b ->
            b.addView(a.card {
                sectionTitle("时间屏幕")
                addView(a.tv("全屏显示时间，保持屏幕常亮，可切换 12/24 小时。", 12.5f, T.ink2))
                btn("进入全屏时钟") { Router.openActivity(a, TimeScreenActivity::class.java) }
            })
        },
    )

    // ---------------- 番茄钟 ----------------
    private fun pomodoro(act: ToolActivity, body: LinearLayout) {
        var focusMin = 25
        var breakMin = 5
        var remain = 25 * 60
        var running = false
        var onBreak = false
        val handler = Handler(Looper.getMainLooper())
        val display = act.tv("25:00", 52f, T.ink, true).apply { gravity = Gravity.CENTER }
        val phase = act.tv("专注中", 12.5f, T.primary).apply { gravity = Gravity.CENTER }
        val progress = android.widget.ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal)
            .apply { max = 100 }
        val count = act.tv("已完成 0 个番茄", 12f, T.ink2).apply { gravity = Gravity.CENTER }
        var done = 0

        fun updateUi() {
            display.text = "%02d:%02d".format(remain / 60, remain % 60)
            phase.text = if (onBreak) "休息中" else "专注中"
            val total = (if (onBreak) breakMin else focusMin) * 60
            progress.progress = if (total == 0) 0 else ((total - remain) * 100 / total)
        }

        val tick = object : Runnable {
            override fun run() {
                if (!running) return
                remain--
                if (remain <= 0) {
                    running = false
                    if (!onBreak) done++
                    onBreak = !onBreak
                    remain = (if (onBreak) breakMin else focusMin) * 60
                    Ui.toast(act, if (onBreak) "专注结束，休息一下" else "休息结束，继续专注")
                    count.text = "已完成 $done 个番茄"
                }
                updateUi()
                if (running) handler.postDelayed(this, 1000)
            }
        }

        val toggle = act.tv("开始", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
            setPadding(0, act.dp(12), 0, act.dp(12))
            isClickable = true
            setOnClickListener {
                running = !running
                text = if (running) "暂停" else "继续"
                if (running) handler.post(tick)
            }
        }
        val reset = act.tv("重置", 14f, T.ink, true).apply {
            gravity = Gravity.CENTER
            background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.surface)
            setPadding(0, act.dp(12), 0, act.dp(12))
            isClickable = true
            setOnClickListener {
                running = false
                handler.removeCallbacksAndMessages(null)
                onBreak = false
                remain = focusMin * 60
                toggle.text = "开始"
                updateUi()
            }
        }

        body.addView(act.card {
            sectionTitle("番茄时钟")
            addView(phase)
            addView(display, LinearLayout.LayoutParams(-1, -2))
            addView(progress, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(reset, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            row.addView(toggle, LinearLayout.LayoutParams(0, -2, 2f))
            addView(row, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(12) })
            addView(count, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
        })
        body.addView(act.card {
            sectionTitle("时长设置")
            chips(listOf("25/5", "45/10", "50/10"), 0) { i ->
                val pair = listOf(25 to 5, 45 to 10, 50 to 10)[i]
                focusMin = pair.first; breakMin = pair.second
                if (!running) { onBreak = false; remain = focusMin * 60; updateUi() }
            }
        })
        updateUi()
    }

    // ---------------- 数独 ----------------
    private val puzzles = listOf(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
        "000260701680070090190004500820100040004602900050003028009300074040050036703018000",
        "100007090030020008009600500005300900010080002600004000300000010040000007007000300",
    )
    private val solutions = listOf(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179",
        "435269781682571493197834562826195347374682915951743628519326874248957136763418259",
        "162857493534129678789643521475312986913586742628794135356478219241935867897261354",
    )

    private fun sudoku(act: ToolActivity, body: LinearLayout) {
        var puzzleIdx = 0
        var selected = -1
        var cells = IntArray(81)
        val grid = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val cellViews = arrayOfNulls<TextView>(81)

        fun highlight() {
            for (i in 0 until 81) {
                val v = cellViews[i] ?: continue
                v.background = if (i == selected) T.fillBg(T.selectedFill, act.dp(4).toFloat())
                else T.fieldBg(act.dp(4).toFloat())
            }
        }

        fun load(idx: Int) {
            puzzleIdx = idx
            selected = -1
            cells = puzzles[idx].map { it - '0' }.toIntArray()
            grid.removeAllViews()
            for (r in 0 until 9) {
                val row = LinearLayout(act)
                for (c in 0 until 9) {
                    val i = r * 9 + c
                    val fixed = cells[i] != 0
                    val cell = act.tv(
                        if (cells[i] == 0) "" else "${cells[i]}",
                        18f,
                        if (fixed) T.ink else T.primary,
                        fixed,
                    ).apply {
                        gravity = Gravity.CENTER
                        background = T.fieldBg(act.dp(4).toFloat())
                        setPadding(0, act.dp(10), 0, act.dp(10))
                        isClickable = true
                        setOnClickListener {
                            if (fixed) return@setOnClickListener
                            selected = i
                            highlight()
                        }
                    }
                    cellViews[i] = cell
                    row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(3) })
                }
                grid.addView(row, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(3) })
            }
        }

        fun put(n: Int) {
            if (selected < 0) return
            cells[selected] = n
            cellViews[selected]?.text = if (n == 0) "" else "$n"
            highlight()
        }

        body.addView(act.card {
            sectionTitle("数独")
            addView(grid)
            addView(act.tv("点选空格后，从下方数字键盘填入", 11f, T.ink2).apply { setPadding(0, act.dp(8), 0, act.dp(8)) })
            val pad = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            for (n in 1..9) {
                pad.addView(act.tv("$n", 16f, T.ink, true).apply {
                    gravity = Gravity.CENTER
                    background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                    setPadding(0, act.dp(10), 0, act.dp(10))
                    isClickable = true
                    setOnClickListener { put(n) }
                }, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(3) })
            }
            addView(pad)
            val actions = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(act.tv("擦除", 13.5f, T.ink2, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.surface)
                setPadding(0, act.dp(10), 0, act.dp(10))
                isClickable = true
                setOnClickListener { put(0) }
            }, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            actions.addView(act.tv("检查", 13.5f, T.ink2, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.surface)
                setPadding(0, act.dp(10), 0, act.dp(10))
                isClickable = true
                setOnClickListener {
                    val sol = solutions[puzzleIdx]
                    var wrong = 0
                    for (i in 0 until 81) {
                        if (cells[i] != 0 && cells[i].toString() != sol[i].toString()) {
                            wrong++
                            cellViews[i]?.setTextColor(T.danger)
                        }
                    }
                    if (cells.contains(0)) Ui.toast(act, if (wrong > 0) "$wrong 处错误，还有空格" else "目前都正确，继续加油")
                    else Ui.toast(act, if (wrong == 0) "全部正确！" else "有 $wrong 处错误")
                }
            }, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8) })
            actions.addView(act.tv("换一题", 13.5f, T.primary, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.selectedFill, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(10), 0, act.dp(10))
                isClickable = true
                setOnClickListener { load((puzzleIdx + 1) % puzzles.size) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(actions, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
        })
        load(0)
    }

    // ---------------- 舒尔特 ----------------
    private fun schulte(act: ToolActivity, body: LinearLayout) {
        var next = 1
        var startAt = 0L
        val time = act.tv("点击 1 开始计时", 13f, T.ink2)
        val grid = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }

        fun newGame() {
            next = 1
            startAt = 0
            time.text = "点击 1 开始计时"
            grid.removeAllViews()
            val nums = (1..25).shuffled()
            for (r in 0 until 5) {
                val row = LinearLayout(act)
                for (c in 0 until 5) {
                    val n = nums[r * 5 + c]
                    val cell = act.tv("$n", 18f, T.ink, true).apply {
                        gravity = Gravity.CENTER
                        background = T.fieldBg(act.dp(T.R_SMALL).toFloat())
                        setPadding(0, act.dp(15), 0, act.dp(15))
                        isClickable = true
                        setOnClickListener {
                            if (n != next) return@setOnClickListener
                            if (next == 1) startAt = System.currentTimeMillis()
                            background = T.fillBg(T.selectedFill, act.dp(T.R_SMALL).toFloat())
                            setTextColor(T.primary)
                            next++
                            if (next > 25) {
                                val ms = System.currentTimeMillis() - startAt
                                time.text = "完成！用时 ${ms / 1000.0} 秒 · 点击「重新开始」再来一局"
                                Ui.toast(act, "完成，用时 ${ms / 1000.0} 秒")
                            } else {
                                time.text = "下一个：$next"
                            }
                        }
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(4) })
                }
                grid.addView(row, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(4) })
            }
        }

        body.addView(act.card {
            sectionTitle("舒尔特方格 5×5")
            addView(time)
            addView(grid, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
            btn("重新开始") { newGame() }
        })
        body.note("按 1→25 的顺序尽快点完；每天练几组，注意力会明显提升。")
        newGame()
    }

    // ---------------- MBTI ----------------
    private data class Q(val text: String, val dim: Char)

    private val questions = listOf(
        Q("聚会结束后，你通常感觉精力被耗尽", 'E'),
        Q("你更喜欢先行动再思考", 'S'),
        Q("做决定时你更依赖逻辑而非感受", 'T'),
        Q("你喜欢提前把计划排好", 'J'),
        Q("独处能让你恢复能量", 'I'),
        Q("你常常沉浸在对未来的想象里", 'N'),
        Q("你很容易察觉别人的情绪", 'F'),
        Q("你更愿意随性安排行程", 'P'),
        Q("你常常主动和陌生人搭话", 'E'),
        Q("你更相信具体经验而非直觉", 'S'),
        Q("争论时你更在意谁的道理对", 'T'),
        Q("你的桌面和文件通常很有条理", 'J'),
        Q("人多时你更愿意听别人说", 'I'),
        Q("你常常想到事物之间的隐含联系", 'N'),
        Q("朋友难过时你会先安慰而不是分析", 'F'),
        Q("你常常临时改变计划", 'P'),
    )

    private fun mbti(act: ToolActivity, body: LinearLayout) {
        val answers = IntArray(questions.size) { -1 }
        val result = act.tv("", 15f, T.primary, true).apply { gravity = Gravity.CENTER }
        val desc = act.tv("", 12.5f, T.ink2)

        fun compute() {
            if (answers.any { it < 0 }) { result.text = "还有 ${answers.count { it < 0 }} 题未答"; return }
            var e = 0; var s = 0; var t = 0; var j = 0
            questions.forEachIndexed { i, q ->
                val yes = answers[i] == 1
                when (q.dim) {
                    'E' -> if (yes) e++ else e--
                    'S' -> if (yes) s++ else s--
                    'T' -> if (yes) t++ else t--
                    'J' -> if (yes) j++ else j--
                    else -> {}
                }
            }
            val type = (if (e > 0) "E" else "I") + (if (s > 0) "S" else "N") +
                (if (t > 0) "T" else "F") + (if (j > 0) "J" else "P")
            result.text = type
            desc.text = MBTI_DESC[type] ?: "你的类型是 $type"
        }

        body.addView(act.card {
            sectionTitle("16 题快速版")
            questions.forEachIndexed { i, q ->
                addView(act.tv("${i + 1}. ${q.text}", 13f, T.ink).apply { setPadding(0, act.dp(8), 0, act.dp(6)) })
                chips(listOf("是", "否"), -1) { idx ->
                    answers[i] = if (idx == 0) 1 else 0
                    compute()
                }
            }
        })
        body.addView(act.card {
            sectionTitle("结果")
            addView(result)
            addView(desc, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(8) })
            btn("重新开始") { answers.fill(-1); result.text = ""; desc.text = "" }
        })
        body.note("快速版只做倾向性参考，不等同于正式 MBTI 测评。")
    }

    private val MBTI_DESC = mapOf(
        "INTJ" to "建筑师：独立、理性，喜欢长期规划。",
        "INTP" to "逻辑学家：好奇、爱分析，沉迷原理。",
        "ENTJ" to "指挥官：果断、目标导向，擅长组织。",
        "ENTP" to "辩论家：机敏、爱挑战，点子很多。",
        "INFJ" to "提倡者：洞察力强，重视意义感。",
        "INFP" to "调停者：理想主义，重视内心价值。",
        "ENFJ" to "主人公：有感染力，善于带动他人。",
        "ENFP" to "竞选者：热情、好奇，喜欢新鲜事。",
        "ISTJ" to "物流师：务实、可靠，重视秩序。",
        "ISFJ" to "守卫者：细致、体贴，责任感强。",
        "ESTJ" to "总经理：直接、高效，擅长执行。",
        "ESFJ" to "执政官：热心、重视关系与和谐。",
        "ISTP" to "鉴赏家：冷静、动手能力强。",
        "ISFP" to "探险家：敏感、审美在线，活在当下。",
        "ESTP" to "企业家：行动派，擅长应变。",
        "ESFP" to "表演者：外向、乐观，享受当下。",
    )
}
