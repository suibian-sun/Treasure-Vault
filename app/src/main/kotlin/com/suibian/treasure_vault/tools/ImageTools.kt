package com.suibian.treasure_vault.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.kv
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.label
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import java.io.File
import java.util.concurrent.Executors

/** 图片编辑类工具（水印 / 拼接 / 九宫格 / 放大 / 锐化 / 上色 / 取色 …）。 */
object ImageTools {

    private val pool = Executors.newSingleThreadExecutor { r -> Thread(r, "img-worker").apply { priority = Thread.MIN_PRIORITY } }

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.image, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("watermark", "图片水印", "加文字水印，可选位置与透明度", "💧", "水印", "watermark") { a, b -> watermark(a, b) },
        spec("nine_grid", "九格切图", "一键切成九宫格，适合朋友圈九图", "🔲", "九宫格", "切图") { a, b -> nineGrid(a, b) },
        spec("collage", "图片拼接", "多张图纵向 / 横向拼成一张", "🧩", "拼接", "长图") { a, b -> collage(a, b) },
        spec("upscale", "图片放大", "2× / 3× / 4× 放大并保边", "🔍", "放大", "upscale") { a, b -> upscale(a, b) },
        spec("sharpen", "清晰度提升", "USM 锐化，让模糊图更清楚", "✨", "锐化", "清晰") { a, b -> sharpen(a, b) },
        spec("stretch_fix", "拉伸修复", "把被拉变形的图还原成正确比例", "📐", "拉伸", "变形") { a, b -> stretchFix(a, b) },
        spec("colorize", "黑白图片上色", "给灰度图上双色调，多种风格", "🎨", "上色", "黑白") { a, b -> colorize(a, b) },
        spec("color_picker", "取色器", "点图片任意位置取色，复制色值", "💉", "取色", "吸管") { a, b -> colorPicker(a, b) },
        spec("palette", "配色助手", "由主色生成整套配色方案", "🌈", "配色", "色板") { a, b -> palette(a, b) },
        spec("exif_edit", "图片信息编辑", "查看拍摄参数，一键清除隐私信息", "🗂️", "exif", "信息") { a, b -> exifEdit(a, b) },
        spec("id_photo", "证件照制作", "裁剪成一寸 / 二寸并加纯色底", "🪪", "证件照", "一寸") { a, b -> idPhoto(a, b) },
        spec("meme", "表情制作", "图片上下加字，做成表情包", "😂", "表情包", "meme") { a, b -> meme(a, b) },
        spec("device_frame", "带壳截图", "给截图加上手机外壳效果", "📱", "带壳", "截图") { a, b -> deviceFrame(a, b) },
        spec("text_to_image", "文字转图", "把一段文字渲染成图片", "🔤", "文字转图", "长文") { a, b -> textToImage(a, b) },
        spec("gradient_image", "渐变图制作", "生成渐变壁纸 / 背景图", "🌅", "渐变", "壁纸") { a, b -> gradientImage(a, b) },
        spec("bead", "拼豆图纸生成", "把图片转成拼豆格子图纸", "🧶", "拼豆", "图纸") { a, b -> bead(a, b) },
        spec("stego", "隐藏图", "把一段文字藏进图片像素里", "🕵️", "隐写", "藏字") { a, b -> stego(a, b) },
        spec("gif_tool", "GIF 合成分解", "多图合成 GIF，或把 GIF 拆成帧", "🎞️", "gif", "动图") { a, b -> gifTool(a, b) },
        spec("canvas_board", "画板", "随手涂鸦并保存为图片", "🖌️", "画板", "涂鸦") { a, b -> board(a, b) },
    )

    // ---------------- 公共积木 ----------------

    /** 一行「选择图片 / 拍照」按钮。 */
    private fun pickRow(act: ToolActivity, body: LinearLayout, multi: Boolean = false,
                        onPicked: (List<android.net.Uri>) -> Unit) {
        body.addView(act.card(pad = 12) {
            val row = LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val pick = act.tv(if (multi) "选择多张图片" else "选择图片", 13.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                setPadding(act.dp(16), act.dp(11), act.dp(16), act.dp(11))
                isClickable = true
                setOnClickListener {
                    if (multi) act.pickImages { onPicked(it) } else act.pickImage { u -> onPicked(if (u == null) emptyList() else listOf(u)) }
                }
            }
            row.addView(pick, LinearLayout.LayoutParams(0, -2, 1f))
            val cam = act.tv("拍照", 13.5f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(act.dp(16), act.dp(11), act.dp(16), act.dp(11))
                isClickable = true
                setOnClickListener {
                    act.capture { u -> onPicked(if (u == null) emptyList() else listOf(u)) }
                }
            }
            row.addView(cam, LinearLayout.LayoutParams(0, -2, 1f).also { it.leftMargin = act.dp(10) })
            addView(row)
        })
    }

    /** 后台线程处理 Bitmap，完成后回主线程。 */
    private fun work(act: ToolActivity, hint: String = "处理中…", block: () -> Bitmap?, done: (Bitmap?) -> Unit) {
        Ui.toast(act, hint)
        pool.execute {
            val r = runCatching { block() }.getOrElse { null }
            act.runOnUiThread {
                if (r == null) Ui.toast(act, "处理失败，请换一张图试试") else done(r)
            }
        }
    }

    /** 结果区：预览 + 保存 + 分享。 */
    private fun showResult(act: ToolActivity, holder: LinearLayout, bmp: Bitmap, fileName: String = "tv") {
        holder.removeAllViews()
        holder.addView(act.card {
            sectionTitle("效果")
            addView(ImageKit.preview(act, bmp, 260))
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            val save = act.tv("保存到相册", 13.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener {
                    val uri = ImageKit.save(act, bmp, fileName)
                    if (uri == null) Ui.toast(act, "保存失败") else {
                        Ui.toast(act, "已保存到相册")
                        runCatching {
                            act.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                        }
                    }
                }
            }
            row.addView(save, LinearLayout.LayoutParams(0, -2, 1f))
            val share = act.tv("分享", 13.5f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener {
                    val uri = ImageKit.shareable(act, bmp)
                    if (uri == null) Ui.toast(act, "准备分享失败") else ImageKit.share(act, uri)
                }
            }
            row.addView(share, LinearLayout.LayoutParams(0, -2, 1f).also { it.leftMargin = act.dp(10) })
            addView(row)
            addView(act.tv("${bmp.width} × ${bmp.height}", 11f, T.ink3).apply { setPadding(0, act.dp(8), 0, 0) })
        })
    }

    /** 在图片上叠加文字（水印 / 表情包共用）。 */
    private fun drawTextOn(src: Bitmap, text: String, sizeRatio: Float, color: Int,
                           stroke: Boolean, pos: Int): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val paint = ImageKit.textPaint(out.width * sizeRatio, color, bold = true)
        paint.textAlign = Paint.Align.CENTER
        if (stroke) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = out.width * 0.006f
            paint.color = Color.BLACK
        }
        val lines = text.split("\n")
        val lh = paint.textSize * 1.2f
        val totalH = lh * lines.size
        val baseY = when (pos) {
            0 -> totalH + out.height * 0.03f            // 上
            2 -> out.height * 0.94f                     // 下
            else -> (out.height + totalH) / 2f          // 中
        }
        lines.forEachIndexed { i, line ->
            val y = baseY + i * lh - (lines.size - 1) * lh
            if (stroke) {
                c.drawText(line, out.width / 2f, y, paint)
                paint.style = Paint.Style.FILL
                paint.color = color
                c.drawText(line, out.width / 2f, y, paint)
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
            } else {
                c.drawText(line, out.width / 2f, y, paint)
            }
        }
        return out
    }

    private fun colorOf(hex: String, def: Int): Int = runCatching {
        val s = hex.trim().removePrefix("#")
        when (s.length) {
            3 -> Color.rgb(
                Integer.parseInt("${s[0]}${s[0]}", 16),
                Integer.parseInt("${s[1]}${s[1]}", 16),
                Integer.parseInt("${s[2]}${s[2]}", 16),
            )
            6 -> Color.rgb(
                Integer.parseInt(s.substring(0, 2), 16),
                Integer.parseInt(s.substring(2, 4), 16),
                Integer.parseInt(s.substring(4, 6), 16),
            )
            8 -> Color.parseColor("#$s")
            else -> def
        }
    }.getOrDefault(def)

    private fun hex(c: Int): String = String.format("#%06X", 0xFFFFFF and c)

    // ---------------- 各工具 ----------------

    private fun watermark(act: ToolActivity, body: LinearLayout) {
        var text = "宝藏盒"
        var size = 0.06f
        var alpha = 0.75f
        var pos = 2
        var current: Bitmap? = null
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }

        var refresh: () -> Unit = {}
        var load: (Bitmap) -> Unit = {}

        body.addView(act.card {
            sectionTitle("水印设置")
            label("水印文字")
            val e = et("输入水印文字", singleLine = true)
            e.setText(text)
            e.doAfterTextChanged { text = it?.toString().orEmpty(); refresh() }
            label("字号")
            chips(listOf("小", "中", "大"), 1) { i -> size = listOf(0.045f, 0.06f, 0.09f)[i]; refresh() }
            label("位置")
            chips(listOf("顶部", "居中", "底部"), pos) { i -> pos = i; refresh() }
            label("透明度")
            chips(listOf("25%", "50%", "75%", "100%"), 2) { i -> alpha = listOf(0.25f, 0.5f, 0.75f, 1f)[i]; refresh() }
        })
        pickRow(act, body) { list ->
            list.firstOrNull() ?: return@pickRow
            val b = ImageKit.decode(act, list.first()) ?: return@pickRow
            current = b
            load(b)
        }
        body.addView(holder)

        load = { b ->
            holder.removeAllViews()
            holder.addView(ImageKit.preview(act, b, 200))
            refresh()
        }
        refresh = {
            val src = current
            if (src == null || text.isBlank()) {
                if (src != null) showResult(act, holder, src, "watermark")
            } else {
                work(act, "生成中…", {
                    drawTextOn(src, text, size, T.withAlpha(Color.WHITE, (alpha * 255).toInt()), true, pos)
                }) { showResult(act, holder, it!!, "watermark") }
            }
        }
    }

    private fun nineGrid(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.note("切成 3×3 九张等分图，保存后可在相册「Pictures/TreasureVault」找到。")
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val bmp = ImageKit.decode(act, u) ?: return@let
            work(act, "切图中…", {
                val side = minOf(bmp.width, bmp.height)
                val square = ImageKit.centerCrop(bmp, side, side)
                val cell = side / 3
                val parts = ArrayList<Bitmap>(9)
                for (r in 0 until 3) for (c in 0 until 3) {
                    parts.add(Bitmap.createBitmap(square, c * cell, r * cell, cell, cell))
                }
                parts.first()
            }) { first ->
                // 重新生成并保存全部
                val side = minOf(bmp.width, bmp.height)
                val square = ImageKit.centerCrop(bmp, side, side)
                val cell = side / 3
                holder.removeAllViews()
                holder.addView(act.card {
                    sectionTitle("九宫格预览")
                    val grid = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                    for (r in 0 until 3) {
                        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                        for (c in 0 until 3) {
                            val piece = Bitmap.createBitmap(square, c * cell, r * cell, cell, cell)
                            val iv = ImageView(act).apply {
                                setImageBitmap(piece)
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                            row.addView(iv, LinearLayout.LayoutParams(0, act.dp(100), 1f).also {
                                it.setMargins(act.dp(2), act.dp(2), act.dp(2), act.dp(2))
                            })
                        }
                        grid.addView(row)
                    }
                    addView(grid)
                    val save = act.tv("保存 9 张到相册", 13.5f, Color.WHITE, true).apply {
                        gravity = Gravity.CENTER
                        background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                        setPadding(0, act.dp(11), 0, act.dp(11))
                        isClickable = true
                        setOnClickListener {
                            var n = 0
                            for (r in 0 until 3) for (c in 0 until 3) {
                                val piece = Bitmap.createBitmap(square, c * cell, r * cell, cell, cell)
                                if (ImageKit.save(act, piece, "grid_${r * 3 + c + 1}") != null) n++
                            }
                            Ui.toast(act, "已保存 $n 张")
                        }
                    }
                    save.setPadding(0, act.dp(12), 0, 0)
                    addView(save)
                })
                first?.recycle()
            }
        } }
        body.addView(holder)
    }

    private fun collage(act: ToolActivity, body: LinearLayout) {
        var vertical = true
        var picked: List<Bitmap> = emptyList()
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var build: () -> Unit = {}

        body.addView(act.card {
            sectionTitle("拼接方式")
            chips(listOf("纵向（长图）", "横向（宽图）"), 0) { i -> vertical = i == 0; build() }
        })
        pickRow(act, body, multi = true) { uris ->
            picked = uris.mapNotNull { ImageKit.decode(act, it) }
            if (picked.isEmpty()) Ui.toast(act, "没有读到图片") else build()
        }
        body.addView(holder)

        build = {
            if (picked.isNotEmpty()) {
                work(act, "拼接中…", {
                    val gap = 8
                    if (vertical) {
                        val w = picked.minOf { it.width }.coerceAtMost(2000)
                        val items = picked.map { b ->
                            Bitmap.createScaledBitmap(b, w, (b.height.toFloat() * w / b.width).toInt(), true)
                        }
                        val totalH = items.sumOf { it.height } + gap * (items.size - 1)
                        val out = ImageKit.newBitmap(w, totalH)
                        val c = Canvas(out)
                        c.drawColor(Color.WHITE)
                        var y = 0
                        items.forEach { c.drawBitmap(it, 0f, y.toFloat(), null); y += it.height + gap }
                        out
                    } else {
                        val h = picked.minOf { it.height }.coerceAtMost(2000)
                        val items = picked.map { b ->
                            Bitmap.createScaledBitmap(b, (b.width.toFloat() * h / b.height).toInt(), h, true)
                        }
                        val totalW = items.sumOf { it.width } + gap * (items.size - 1)
                        val out = ImageKit.newBitmap(totalW, h)
                        val c = Canvas(out)
                        c.drawColor(Color.WHITE)
                        var x = 0
                        items.forEach { c.drawBitmap(it, x.toFloat(), 0f, null); x += it.width + gap }
                        out
                    }
                }) { bmp -> bmp?.let { showResult(act, holder, it, "collage") } }
            }
        }
    }

    private fun upscale(act: ToolActivity, body: LinearLayout) {
        var factor = 2
        var sharp = true
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("放大倍数")
            chips(listOf("2×", "3×", "4×"), 0) { i -> factor = i + 2 }
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u, 1600) ?: return@let
            work(act, "放大中…", {
                val w = src.width * factor
                val h = src.height * factor
                if (w.toLong() * h > 40_000_000L) return@work null
                var out = Bitmap.createScaledBitmap(src, w, h, true)
                if (sharp) out = Filters.unsharp(out, 0.6f)
                out
            }) { bmp -> bmp?.let { showResult(act, holder, it, "upscale") } }
        } }
        body.addView(holder)
    }

    private fun sharpen(act: ToolActivity, body: LinearLayout) {
        var amount = 0.8f
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("锐化强度")
            chips(listOf("轻微", "标准", "强力"), 1) { i -> amount = listOf(0.4f, 0.8f, 1.4f)[i] }
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u, 2400) ?: return@let
            work(act, "锐化中…", { Filters.unsharp(src, amount) }) { bmp ->
                bmp?.let { showResult(act, holder, it, "sharpen") }
            }
        } }
        body.addView(holder)
    }

    private fun stretchFix(act: ToolActivity, body: LinearLayout) {
        var ratio = "1:1"
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("目标比例")
            chips(listOf("1:1", "4:3", "3:4", "16:9", "9:16"), 0) { i ->
                ratio = listOf("1:1", "4:3", "3:4", "16:9", "9:16")[i]
            }
            addView(act.tv("按比例居中裁剪并缩放，消除被拉变形的画面。", 11.5f, T.ink2))
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u) ?: return@let
            work(act, "修复中…", {
                val (rw, rh) = ratio.split(":").map { it.toInt() }
                val long = 1600
                val w: Int
                val h: Int
                if (rw >= rh) { w = long; h = long * rh / rw } else { h = long; w = long * rw / rh }
                ImageKit.centerCrop(src, w, h)
            }) { bmp -> bmp?.let { showResult(act, holder, it, "fixed") } }
        } }
        body.addView(holder)
    }

    private fun colorize(act: ToolActivity, body: LinearLayout) {
        var style = 0
        val styles = listOf("暖棕（复古）", "冷蓝（夜景）", "青橙（电影）", "紫粉（梦幻）")
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("色调")
            chips(styles, 0) { i -> style = i }
            addView(act.tv("按明暗映射到双色调，纯本地运算，不上传图片。", 11.5f, T.ink2))
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u, 2000) ?: return@let
            work(act, "上色中…", { Filters.duotone(src, style) }) { bmp ->
                bmp?.let { showResult(act, holder, it, "colorized") }
            }
        } }
        body.addView(holder)
    }

    private fun colorPicker(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u, 2000) ?: return@let
            holder.removeAllViews()
            holder.addView(act.card {
                sectionTitle("点击图片取色")
                val swatch = act.tv("—", 16f, T.ink, true).apply {
                    background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                    setPadding(act.dp(14), act.dp(12), act.dp(14), act.dp(12))
                }
                addView(swatch, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = act.dp(10) })
                val iv = ImageView(act).apply {
                    setImageBitmap(src)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    isClickable = true
                }
                addView(iv, LinearLayout.LayoutParams(-1, act.dp(300)))
                iv.setOnTouchListener { v, e ->
                    if (e.action == android.view.MotionEvent.ACTION_DOWN) {
                        val drawable = iv.drawable ?: return@setOnTouchListener false
                        val dw = drawable.intrinsicWidth.toFloat()
                        val dh = drawable.intrinsicHeight.toFloat()
                        val scale = minOf(iv.width / dw, iv.height / dh)
                        val ox = (iv.width - dw * scale) / 2f
                        val oy = (iv.height - dh * scale) / 2f
                        val bx = ((e.x - ox) / scale).toInt()
                        val by = ((e.y - oy) / scale).toInt()
                        if (bx in 0 until src.width && by in 0 until src.height) {
                            val c = src.getPixel(bx, by)
                            swatch.text = "${hex(c)}   R${Color.red(c)} G${Color.green(c)} B${Color.blue(c)}"
                            swatch.background = T.fillBg(c, act.dp(T.R_SMALL).toFloat())
                            swatch.setTextColor(if (Filters.luma(c) > 150) Color.BLACK else Color.WHITE)
                            swatch.setOnClickListener { Ui.copy(act, hex(c)) }
                        }
                    }
                    true
                }
            })
        } }
        body.addView(holder)
    }

    private fun palette(act: ToolActivity, body: LinearLayout) {
        var base = T.primary
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var render: () -> Unit = {}

        body.addView(act.card {
            sectionTitle("主色")
            val e = et("#1890FF", singleLine = true)
            e.setText(hex(T.primary))
            e.doAfterTextChanged { base = colorOf(it?.toString().orEmpty(), T.primary); render() }
            addView(act.tv("示例：#1890FF / #0A0", 11.5f, T.ink2))
        })
        body.addView(holder)

        render = {
            holder.removeAllViews()
            val rows = listOf(
                "主色" to base,
                "互补色" to Filters.shiftHue(base, 180f),
                "类似色 1" to Filters.shiftHue(base, -30f),
                "类似色 2" to Filters.shiftHue(base, 30f),
                "三角色" to Filters.shiftHue(base, 120f),
                "浅色底" to Filters.mix(base, Color.WHITE, 0.86f),
                "深色底" to Filters.mix(base, Color.BLACK, 0.72f),
            )
            holder.addView(act.card {
                sectionTitle("配色方案")
                rows.forEach { (name, c) ->
                    val row = LinearLayout(act).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, act.dp(5), 0, act.dp(5))
                    }
                    val box = act.tv("", 1f, c).apply {
                        background = T.fillBg(c, act.dp(T.R_SMALL).toFloat())
                    }
                    row.addView(box, LinearLayout.LayoutParams(act.dp(44), act.dp(44)))
                    val col = LinearLayout(act).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(act.dp(12), 0, 0, 0)
                    }
                    col.addView(act.tv(name, 13f, T.ink))
                    col.addView(act.tv(hex(c), 12f, T.ink2).apply { setPadding(0, act.dp(2), 0, 0) })
                    row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
                    row.addView(act.tv("复制", 12f, T.primary, true).apply {
                        isClickable = true
                        setOnClickListener { Ui.copy(act, hex(c)) }
                    })
                    addView(row)
                }
            })
        }
        render()
    }

    private fun exifEdit(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val file = ImageKit.copyToCache(act, u, ".jpg")
            holder.removeAllViews()
            if (file == null) { Ui.toast(act, "读取失败"); return@let }
            val exif = runCatching { androidx.exifinterface.media.ExifInterface(file.absolutePath) }.getOrNull()
            val bmp = ImageKit.decode(act, u)
            holder.addView(act.card {
                sectionTitle("图片信息")
                bmp?.let { addView(ImageKit.preview(act, it, 180)) }
                kv("尺寸", bmp?.let { "${it.width} × ${it.height}" } ?: "—")
                kv("文件大小", "${file.length() / 1024} KB")
                if (exif != null) {
                    kv("拍摄时间", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME) ?: "—")
                    kv("设备", listOfNotNull(
                        exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE),
                        exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL),
                    ).joinToString(" ").ifBlank { "—" })
                    kv("光圈", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER) ?: "—")
                    kv("ISO", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "—")
                    kv("曝光", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME) ?: "—")
                    val lat = exif.latLong
                    kv("GPS", if (lat == null) "无" else "%.5f, %.5f".format(lat[0], lat[1]))
                } else {
                    addView(act.tv("这张图没有 EXIF 信息", 12f, T.ink2))
                }
            })
            if (exif != null) {
                holder.addView(act.card {
                    btn("一键清除隐私信息（EXIF）", primary = false) {
                        runCatching {
                            val e2 = androidx.exifinterface.media.ExifInterface(file.absolutePath)
                            listOf(
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
                                androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
                                androidx.exifinterface.media.ExifInterface.TAG_MAKE,
                                androidx.exifinterface.media.ExifInterface.TAG_MODEL,
                                androidx.exifinterface.media.ExifInterface.TAG_MAKER_NOTE,
                            ).forEach { e2.setAttribute(it, null) }
                            e2.saveAttributes()
                            val out = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            if (out != null) {
                                val uri = ImageKit.save(act, out, "clean_${System.currentTimeMillis()}")
                                Ui.toast(act, if (uri != null) "已清除并保存到相册" else "已清除，保存失败")
                            }
                        }.onFailure { Ui.toast(act, "清除失败：${it.message}") }
                    }
                    note("清除后会另存为新图，原图不受影响。")
                })
            }
        } }
        body.addView(holder)
    }

    private fun idPhoto(act: ToolActivity, body: LinearLayout) {
        var sizeIdx = 0
        var bgColor = Color.WHITE
        val sizes = listOf("一寸 25×35" to (295 to 413), "二寸 35×49" to (413 to 579), "小一寸 22×32" to (260 to 378))
        val bgNames = listOf("白底", "蓝底", "红底")
        val bgVals = listOf(Color.WHITE, Color.rgb(0x43, 0x8E, 0xD8), Color.rgb(0xD9, 0x2B, 0x2B))
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("规格")
            chips(sizes.map { it.first }, 0) { i -> sizeIdx = i }
            label("底色")
            chips(bgNames, 0) { i -> bgColor = bgVals[i] }
            note("按证件照比例居中裁剪并补底色。本工具不做人像抠图，请选纯色背景的人像照。")
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u) ?: return@let
            work(act, "生成中…", {
                val (w, h) = sizes[sizeIdx].second
                val ratio = w.toFloat() / h.toFloat()
                val cropW = minOf(src.width, (src.height * ratio).toInt())
                val cropH = (cropW / ratio).toInt()
                val x = (src.width - cropW) / 2
                val y = (src.height - cropH) / 3   // 略偏上，符合证件照构图
                val out = ImageKit.newBitmap(w, h)
                val c = Canvas(out)
                c.drawColor(bgColor)
                c.drawBitmap(src, android.graphics.Rect(x, y, x + cropW, y + cropH), android.graphics.Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
                out
            }) { bmp -> bmp?.let { showResult(act, holder, it, "idphoto") } }
        } }
        body.addView(holder)
    }

    private fun meme(act: ToolActivity, body: LinearLayout) {
        var top = "当你发现"
        var bottom = "工具全都免费"
        var src: Bitmap? = null
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var render: () -> Unit = {}

        body.addView(act.card {
            sectionTitle("文字")
            label("上排")
            val e1 = et("上排文字", singleLine = true)
            e1.setText(top)
            e1.doAfterTextChanged { top = it?.toString().orEmpty(); render() }
            label("下排")
            val e2 = et("下排文字", singleLine = true)
            e2.setText(bottom)
            e2.doAfterTextChanged { bottom = it?.toString().orEmpty(); render() }
        })
        pickRow(act, body) { list ->
            val u = list.firstOrNull() ?: return@pickRow
            src = ImageKit.decode(act, u)
            render()
        }
        body.addView(holder)

        render = {
            val b = src
            if (b == null) {
                if (top.isNotBlank() || bottom.isNotBlank()) Ui.toast(act, "先选一张图片")
            } else {
                work(act, "生成中…", {
                    var out = b.copy(Bitmap.Config.ARGB_8888, true)
                    if (top.isNotBlank()) out = drawTextOn(out, top, 0.085f, Color.WHITE, true, 0)
                    if (bottom.isNotBlank()) out = drawTextOn(out, bottom, 0.085f, Color.WHITE, true, 2)
                    out
                }) { bmp -> bmp?.let { showResult(act, holder, it, "meme") } }
            }
        }
    }

    private fun deviceFrame(act: ToolActivity, body: LinearLayout) {
        var color = 0xFF1B1D22.toInt()
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("外壳颜色")
            chips(listOf("深空黑", "银白", "品牌蓝"), 0) { i ->
                color = listOf(0xFF1B1D22.toInt(), 0xFFD8DCE3.toInt(), T.primary)[i]
            }
            note("在截图四周加圆角边框与刘海，输出接近真机展示图的效果。")
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u) ?: return@let
            work(act, "加壳中…", {
                val marginRatio = 0.06f
                val radiusRatio = 0.10f
                val w = (src.width * (1 + marginRatio * 2)).toInt()
                val h = (src.height * (1 + marginRatio * 2)).toInt()
                val out = ImageKit.newBitmap(w, h)
                val c = Canvas(out)
                c.drawColor(Color.TRANSPARENT)
                val pad = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                val frame = RectF(0f, 0f, w.toFloat(), h.toFloat())
                val r = w * radiusRatio
                c.drawRoundRect(frame, r, r, pad)
                val inner = RectF(
                    w * marginRatio, h * marginRatio,
                    w * (1 - marginRatio), h * (1 - marginRatio),
                )
                val path = android.graphics.Path().apply {
                    addRoundRect(inner, r * 0.78f, r * 0.78f, android.graphics.Path.Direction.CW)
                }
                c.save()
                c.clipPath(path)
                c.drawBitmap(src, null, inner, Paint(Paint.FILTER_BITMAP_FLAG))
                c.restore()
                // 刘海
                val notch = RectF(w * 0.34f, 0f, w * 0.66f, h * 0.045f)
                c.drawRoundRect(notch, notch.height() / 2f, notch.height() / 2f, pad)
                out
            }) { bmp -> bmp?.let { showResult(act, holder, it, "frame") } }
        } }
        body.addView(holder)
    }

    private fun textToImage(act: ToolActivity, body: LinearLayout) {
        var bgIdx = 0
        var sizeIdx = 1
        val bgs = listOf(Color.WHITE, 0xFFF7F8FA.toInt(), 0xFF1F2329.toInt(), T.primary)
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var text = "把想说的话，变成一张图。"
        body.addView(act.card {
            sectionTitle("文字")
            val e = et("输入文字", 6)
            e.setText(text)
            e.doAfterTextChanged { text = it?.toString().orEmpty() }
            label("背景")
            chips(listOf("白", "浅灰", "深色", "品牌色"), 0) { i -> bgIdx = i }
            label("字号")
            chips(listOf("小", "中", "大"), 1) { i -> sizeIdx = i }
            btn("生成图片") {
                if (text.isBlank()) { Ui.toast(act, "先输入文字"); return@btn }
                work(act, "渲染中…", {
                    val width = 1080
                    val pad = 72f
                    val fs = listOf(38f, 50f, 66f)[sizeIdx]
                    val paint = ImageKit.textPaint(fs, if (bgIdx == 2 || bgIdx == 3) Color.WHITE else 0xFF1F2329.toInt())
                    // 先估算高度
                    val tmp = ImageKit.newBitmap(width, 10)
                    val measureC = Canvas(tmp)
                    val h = ImageKit.drawWrapped(measureC, text, paint, pad, pad, width - pad * 2, fs * 0.45f)
                    val bmp = ImageKit.newBitmap(width, (h + pad * 2).toInt())
                    val c = Canvas(bmp)
                    c.drawColor(bgs[bgIdx])
                    var y = pad
                    for (para in text.split("\n")) {
                        if (para.isEmpty()) { y += fs * 1.45f; continue }
                        var line = StringBuilder()
                        for (ch in para) {
                            val test = line.toString() + ch
                            if (paint.measureText(test) > width - pad * 2 && line.isNotEmpty()) {
                                c.drawText(line.toString(), pad, y + fs, paint)
                                y += fs * 1.45f
                                line = StringBuilder().append(ch)
                            } else line.append(ch)
                        }
                        if (line.isNotEmpty()) { c.drawText(line.toString(), pad, y + fs, paint); y += fs * 1.45f }
                    }
                    bmp
                }) { bmp -> bmp?.let { showResult(act, holder, it, "text2img") } }
            }
        })
        body.addView(holder)
    }

    private fun gradientImage(act: ToolActivity, body: LinearLayout) {
        var c1 = 0xFF1890FF.toInt()
        var c2 = 0xFF7C3AED.toInt()
        var dir = 0
        val presets = listOf(
            "晨雾" to (0xFFBFD9FF.toInt() to 0xFFF6E7FF.toInt()),
            "落日" to (0xFFFF7E5F.toInt() to 0xFFFEB47B.toInt()),
            "深海" to (0xFF0F2027.toInt() to 0xFF2C5364.toInt()),
            "薄荷" to (0xFF43C6AC.toInt() to 0xFFF8FFAE.toInt()),
            "夜幕" to (0xFF232526.toInt() to 0xFF414345.toInt()),
        )
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var render: (Int, Int) -> Unit = { _, _ -> }

        body.addView(act.card {
            sectionTitle("渐变预设")
            chips(presets.map { it.first }, 0) { i ->
                c1 = presets[i].second.first
                c2 = presets[i].second.second
                render(1080, 1080)
            }
            label("方向")
            chips(listOf("斜向", "竖向", "横向"), 0) { i -> dir = i; render(1080, 1080) }
            btn("生成壁纸尺寸（1080×1920）") { render(1080, 1920) }
            btn("生成方图（1080×1080）", primary = false) { render(1080, 1080) }
        })
        body.addView(holder)

        render = { w, h ->
            val bmp = ImageKit.newBitmap(w, h)
            val c = Canvas(bmp)
            val (x0, y0, x1, y1) = when (dir) {
                1 -> listOf(0f, 0f, 0f, h.toFloat())
                2 -> listOf(0f, 0f, w.toFloat(), 0f)
                else -> listOf(0f, 0f, w.toFloat(), h.toFloat())
            }
            val paint = Paint().apply {
                shader = LinearGradient(x0, y0, x1, y1, c1, c2, Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            showResult(act, holder, bmp, "gradient")
        }
        render(1080, 1080)
    }

    private fun bead(act: ToolActivity, body: LinearLayout) {
        var cells = 32
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("网格密度")
            chips(listOf("24×24", "32×32", "48×48", "64×64"), 1) { i ->
                cells = listOf(24, 32, 48, 64)[i]
            }
            note("按像素平均色量化到固定色板，并画出网格与色号，可直接照着摆豆。")
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            val src = ImageKit.decode(act, u, 900) ?: return@let
            work(act, "绘制图纸…", { Filters.beadPattern(src, cells) }) { bmp ->
                bmp?.let { showResult(act, holder, it, "bead") }
            }
        } }
        body.addView(holder)
    }

    private fun stego(act: ToolActivity, body: LinearLayout) {
        var mode = 0
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("模式")
            chips(listOf("藏入文字", "取出文字"), 0) { i -> mode = i }
            label("要隐藏的文字")
            val e = et("输入需要隐藏的内容", 3)
            addView(act.tv("原理：把文字字节写进每个像素的最低位，肉眼无差别；载体需为 PNG（JPEG 会破坏数据）。", 11.5f, T.ink2))
            btn("选择载体图片") {
                val txt = e.text?.toString().orEmpty()
                act.pickImage { u ->
                    if (u == null) return@pickImage
                    val src = ImageKit.decode(act, u) ?: return@pickImage
                    if (mode == 0) {
                        if (txt.isBlank()) { Ui.toast(act, "先输入要隐藏的文字"); return@pickImage }
                        work(act, "写入中…", { Filters.hideText(src, txt) }) { bmp ->
                            bmp?.let { showResult(act, holder, it, "stego.png") }
                        }
                    } else {
                        work(act, "读取中…", { Filters.extractText(src)?.let { Filters.textBitmap(it) } }) { bmp ->
                            if (bmp == null) Ui.toast(act, "没有找到隐藏信息") else showResult(act, holder, bmp, "decoded")
                        }
                    }
                }
            }
        })
        body.addView(holder)
    }

    private fun gifTool(act: ToolActivity, body: LinearLayout) {
        var mode = 0
        var delay = 300
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("模式")
            chips(listOf("多图合成 GIF", "GIF 分解为帧"), 0) { i -> mode = i }
            label("每帧间隔（毫秒）")
            val e = et("300", singleLine = true, inputType = android.text.InputType.TYPE_CLASS_NUMBER)
            e.setText("300")
            e.doAfterTextChanged { v -> delay = (v?.toString()?.toIntOrNull() ?: 300).coerceIn(50, 3000) }
            if (mode == 0) {
                btn("选择多张图片") {
                    act.pickImages { uris ->
                        val bitmaps = uris.mapNotNull { ImageKit.decode(act, it, 720) }
                        if (bitmaps.isEmpty()) { Ui.toast(act, "没有读到图片"); return@pickImages }
                        work(act, "合成中…", {
                            val w = bitmaps.minOf { it.width }
                            val h = bitmaps.minOf { it.height }
                            val frames = bitmaps.map { ImageKit.centerCrop(it, w, h) }
                            GifEncoder.encode(frames, delay, File(act.cacheDir, "out.gif"))
                                ?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath) }
                        }) { bmp -> bmp?.let { showResult(act, holder, it, "anim.gif") } }
                    }
                }
            }
        })
        pickRow(act, body) { list -> list.firstOrNull()?.let { u ->
            if (mode == 1) {
                val f = ImageKit.copyToCache(act, u, ".gif") ?: return@let
                work(act, "分解中…", {
                    val movie = android.graphics.Movie.decodeFile(f.absolutePath) ?: return@work null
                    val dur = movie.duration().coerceAtLeast(200)
                    val step = 80
                    val frames = ArrayList<Bitmap>()
                    var t = 0
                    while (t < dur && frames.size < 60) {
                        val bmp = ImageKit.newBitmap(movie.width(), movie.height())
                        val c = Canvas(bmp)
                        c.drawColor(Color.WHITE)
                        movie.setTime(t)
                        movie.draw(c, 0f, 0f)
                        frames.add(bmp)
                        t += step
                    }
                    if (frames.isEmpty()) return@work null
                    GifEncoder.encode(frames, step, File(act.cacheDir, "frames.gif"))
                        ?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath) }
                }) { bmp ->
                    if (bmp == null) Ui.toast(act, "这张图不是有效的 GIF") else showResult(act, holder, bmp, "frames.gif")
                }
            }
        } }
        body.addView(holder)
    }

    private fun board(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val boardView = DrawBoard(act)
        body.addView(act.card(pad = 10) {
            addView(act.tv("手指在下面画，右下角可调笔色与粗细。", 11.5f, T.ink2))
            addView(boardView, LinearLayout.LayoutParams(-1, act.dp(360)).also { it.topMargin = act.dp(8) })
        })
        body.addView(act.card {
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            val colors = listOf(Color.BLACK, T.primary, 0xFFE11D48.toInt(), 0xFF16A34A.toInt(), Color.WHITE)
            colors.forEach { c ->
                val dot = act.tv("", 1f).apply {
                    background = T.fillBg(c, act.dp(16).toFloat()).apply { setStroke(2, T.line) }
                    isClickable = true
                    setOnClickListener { boardView.penColor = c }
                }
                row.addView(dot, LinearLayout.LayoutParams(act.dp(34), act.dp(34)).also { it.rightMargin = act.dp(10) })
            }
            addView(row)
            val row2 = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            listOf("细" to 4f, "中" to 9f, "粗" to 18f).forEach { (n, w) ->
                val b = act.tv(n, 12.5f, T.ink2, true).apply {
                    gravity = Gravity.CENTER
                    background = T.fillBg(T.field, act.dp(T.R_SMALL).toFloat())
                    setPadding(act.dp(14), act.dp(8), act.dp(14), act.dp(8))
                    isClickable = true
                    setOnClickListener { boardView.penWidth = w }
                }
                row2.addView(b, LinearLayout.LayoutParams(0, -2, 1f).also { it.rightMargin = act.dp(8); it.topMargin = act.dp(10) })
            }
            addView(row2)
            val row3 = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            val undo = act.tv("撤销", 13f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener { boardView.undo() }
            }
            val clear = act.tv("清空", 13f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener { boardView.clearAll() }
            }
            val save = act.tv("保存", 13f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener {
                    val bmp = boardView.export()
                    val uri = ImageKit.save(act, bmp, "drawing_${System.currentTimeMillis()}")
                    Ui.toast(act, if (uri != null) "已保存到相册" else "保存失败")
                }
            }
            listOf(undo, clear, save).forEach { row3.addView(it, LinearLayout.LayoutParams(0, -2, 1f).also { lp -> lp.rightMargin = act.dp(8) }) }
            addView(row3)
        })
        body.addView(holder)
    }
}
