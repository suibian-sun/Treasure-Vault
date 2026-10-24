package com.suibian.treasure_vault.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
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
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.out
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors

/** 识别类工具：OCR、表格提取、二维码、以图搜图、拍照计数。 */
object OcrTools {

    private val pool = Executors.newSingleThreadExecutor { r -> Thread(r, "ocr-worker").apply { priority = Thread.MIN_PRIORITY } }

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.image, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("ocr", "OCR 图片文字识别", "端侧识别中英文，可复制导出", "🔎", "ocr", "文字识别") { a, b -> ocr(a, b) },
        spec("table_ocr", "表格识别提取", "把表格图片转成 CSV 文本", "📊", "表格", "csv") { a, b -> tableOcr(a, b) },
        spec("qrcode", "二维码工具", "生成二维码 / 识别二维码", "▩", "二维码", "扫码") { a, b -> qr(a, b) },
        spec("image_search", "以图搜图", "一键打开各大识图引擎", "🧭", "搜图", "识图") { a, b -> imageSearch(a, b) },
        spec("photo_count", "拍照计数", "拍照后在图上打点计数", "🔢", "计数", "点数") { a, b -> photoCount(a, b) },
    )

    private fun pickRow(act: ToolActivity, body: LinearLayout, onPicked: (List<android.net.Uri>) -> Unit) {
        body.addView(act.card(pad = 12) {
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            val pick = act.tv("从相册选择", 13.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener { act.pickImage { u -> onPicked(if (u == null) emptyList() else listOf(u)) } }
            }
            row.addView(pick, LinearLayout.LayoutParams(0, -2, 1f))
            val cam = act.tv("拍照", 13.5f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(0, act.dp(11), 0, act.dp(11))
                isClickable = true
                setOnClickListener { act.capture { u -> onPicked(if (u == null) emptyList() else listOf(u)) } }
            }
            row.addView(cam, LinearLayout.LayoutParams(0, -2, 1f).also { it.leftMargin = act.dp(10) })
            addView(row)
        })
    }

    /** 识别文字，返回按行合并的纯文本 + 每个文本块的位置。 */
    private fun recognize(bmp: Bitmap): Pair<String, List<Triple<String, Int, Int>>> {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val image = InputImage.fromBitmap(bmp, 0)
        val result = Tasks.await(recognizer.process(image))
        val blocks = ArrayList<Triple<String, Int, Int>>()
        for (block in result.textBlocks) {
            val box = block.boundingBox ?: continue
            blocks.add(Triple(block.text, box.top, box.left))
        }
        recognizer.close()
        return result.text to blocks
    }

    private fun ocr(act: ToolActivity, body: LinearLayout) {
        body.note("识别在手机本地完成，图片不会上传。首次使用会下载约 20MB 的中文识别模型。")
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        pickRow(act, body) { list ->
            val u = list.firstOrNull() ?: return@pickRow
            val bmp = ImageKit.decode(act, u, 2000) ?: return@pickRow
            holder.removeAllViews()
            holder.addView(act.card {
                sectionTitle("识别结果")
                val loading = act.tv("识别中，请稍候…", 13f, T.ink2)
                addView(loading)
            })
            pool.execute {
                val r = runCatching { recognize(bmp) }
                act.runOnUiThread {
                    holder.removeAllViews()
                    r.onSuccess { (text, _) ->
                        holder.addView(act.card {
                            sectionTitle("识别结果")
                            if (text.isBlank()) {
                                addView(act.tv("没有识别到文字，换一张更清晰的图片试试。", 13f, T.ink2))
                            } else {
                                out(text)
                                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                                val copy = act.tv("复制全文", 13f, Color.WHITE, true).apply {
                                    gravity = Gravity.CENTER
                                    background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                                    setPadding(0, act.dp(11), 0, act.dp(11))
                                    isClickable = true
                                    setOnClickListener { Ui.copy(act, text) }
                                }
                                val save = act.tv("保存为文本", 13f, T.ink, true).apply {
                                    gravity = Gravity.CENTER
                                    background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                                    setPadding(0, act.dp(11), 0, act.dp(11))
                                    isClickable = true
                                    setOnClickListener {
                                        val f = java.io.File(act.getExternalFilesDir(null), "ocr_${System.currentTimeMillis()}.txt")
                                        runCatching { f.writeText(text) }
                                            .onSuccess { Ui.toast(act, "已保存：${f.absolutePath}") }
                                            .onFailure { Ui.toast(act, "保存失败") }
                                    }
                                }
                                row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
                                row.addView(save, LinearLayout.LayoutParams(0, -2, 1f).also { it.leftMargin = act.dp(10) })
                                addView(row)
                                addView(act.tv("共 ${text.length} 个字符", 11f, T.ink3).apply { setPadding(0, act.dp(8), 0, 0) })
                            }
                        })
                    }.onFailure {
                        holder.addView(act.card {
                            addView(act.tv("识别失败：${it.message}\n\n若首次使用，请确认网络可用（需要下载识别模型）。", 12.5f, T.danger))
                        })
                    }
                }
            }
        }
        body.addView(holder)
    }

    private fun tableOcr(act: ToolActivity, body: LinearLayout) {
        body.note("按文字块的位置聚类成行列，输出 CSV。适合规则表格的截图。")
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        pickRow(act, body) { list ->
            val u = list.firstOrNull() ?: return@pickRow
            val bmp = ImageKit.decode(act, u, 2200) ?: return@pickRow
            holder.removeAllViews()
            holder.addView(act.card { addView(act.tv("识别中，请稍候…", 13f, T.ink2)) })
            pool.execute {
                val r = runCatching {
                    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    val res = Tasks.await(recognizer.process(InputImage.fromBitmap(bmp, 0)))
                    recognizer.close()
                    // 收集所有行
                    data class Cell(val text: String, val x: Int, val y: Int, val h: Int)
                    val cells = ArrayList<Cell>()
                    for (block in res.textBlocks) for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        cells.add(Cell(line.text, box.left, box.top, box.height()))
                    }
                    cells.sortBy { it.y }
                    // 按 y 中心聚类成行
                    val rows = ArrayList<MutableList<Cell>>()
                    for (c in cells) {
                        val cy = c.y + c.h / 2
                        val target = rows.firstOrNull { row ->
                            val ref = row[0]
                            kotlin.math.abs((ref.y + ref.h / 2) - cy) < maxOf(ref.h, c.h) * 0.6
                        }
                        if (target == null) rows.add(mutableListOf(c)) else target.add(c)
                    }
                    rows.sortBy { it[0].y }
                    val sb = StringBuilder()
                    for (row in rows) {
                        row.sortBy { it.x }
                        sb.append(row.joinToString(",") { csvCell(it.text) }).append("\n")
                    }
                    sb.toString().trim()
                }
                act.runOnUiThread {
                    holder.removeAllViews()
                    r.onSuccess { csv ->
                        holder.addView(act.card {
                            sectionTitle("CSV 结果")
                            if (csv.isBlank()) {
                                addView(act.tv("没有识别到表格内容。", 13f, T.ink2))
                            } else {
                                out(csv)
                                btn("复制 CSV") { Ui.copy(act, csv) }
                                btn("保存为 .csv", primary = false) {
                                    val f = java.io.File(act.getExternalFilesDir(null), "table_${System.currentTimeMillis()}.csv")
                                    runCatching { f.writeText("\uFEFF$csv") }
                                        .onSuccess { Ui.toast(act, "已保存：${f.absolutePath}") }
                                        .onFailure { Ui.toast(act, "保存失败") }
                                }
                            }
                        })
                    }.onFailure {
                        holder.addView(act.card { addView(act.tv("识别失败：${it.message}", 12.5f, T.danger)) })
                    }
                }
            }
        }
        body.addView(holder)
    }

    private fun csvCell(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"${s.replace("\"", "\"\"")}\"" else s

    private fun qr(act: ToolActivity, body: LinearLayout) {
        var mode = 0
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var generate: (String) -> Unit = {}
        body.addView(act.card {
            sectionTitle("功能")
            chips(listOf("生成二维码", "识别二维码"), 0) { i -> mode = i }
        })
        pickRow(act, body) { list ->
            if (mode != 1) { Ui.toast(act, "识别模式才需要选图"); return@pickRow }
            val u = list.firstOrNull() ?: return@pickRow
            val bmp = ImageKit.decode(act, u, 2000) ?: return@pickRow
            holder.removeAllViews()
            holder.addView(act.card { addView(act.tv("识别中…", 13f, T.ink2)) })
            pool.execute {
                val r = runCatching {
                    val scanner = BarcodeScanning.getClient()
                    val res = Tasks.await(scanner.process(InputImage.fromBitmap(bmp, 0)))
                    scanner.close()
                    res
                }
                act.runOnUiThread {
                    holder.removeAllViews()
                    r.onSuccess { codes ->
                        holder.addView(act.card {
                            sectionTitle("识别结果")
                            if (codes.isEmpty()) {
                                addView(act.tv("没有找到二维码或条形码。", 13f, T.ink2))
                            } else {
                                codes.forEach { c ->
                                    val v = c.rawValue ?: c.displayValue ?: ""
                                    addView(act.tv(typeName(c.format), 11.5f, T.ink2, true))
                                    out(v)
                                    if (v.startsWith("http")) {
                                        btn("在浏览器打开") {
                                            runCatching {
                                                act.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(v)))
                                            }.onFailure { Ui.toast(act, "无法打开链接") }
                                        }
                                    }
                                }
                            }
                        })
                    }.onFailure {
                        holder.addView(act.card { addView(act.tv("识别失败：${it.message}", 12.5f, T.danger)) })
                    }
                }
            }
        }
        body.addView(act.card {
            sectionTitle("生成二维码")
            val e = et("输入网址或任意文字", 3)
            e.setText("https://github.com/suibian-sun")
            note("支持网址、文本、Wi-Fi 配置等任意内容；内容越长码点越密。")
            btn("生成") { generate(e.text?.toString().orEmpty()) }
        })
        body.addView(holder)

        generate = { content ->
            if (content.isBlank()) Ui.toast(act, "先输入内容") else {
            holder.removeAllViews()
            val bmp = runCatching {
                val hints = HashMap<EncodeHintType, Any>().apply {
                    put(EncodeHintType.MARGIN, 1)
                    put(EncodeHintType.CHARACTER_SET, "UTF-8")
                }
                val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 720, 720, hints)
                val b = ImageKit.newBitmap(matrix.width, matrix.height)
                val px = IntArray(matrix.width * matrix.height)
                for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                    px[y * matrix.width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
                b.setPixels(px, 0, matrix.width, 0, 0, matrix.width, matrix.height)
                b
            }.getOrNull()
            holder.addView(act.card {
                sectionTitle("二维码")
                if (bmp == null) {
                    addView(act.tv("生成失败：内容可能过长", 12.5f, T.danger))
                } else {
                    val iv = ImageView(act).apply {
                        setImageBitmap(bmp)
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    addView(iv, LinearLayout.LayoutParams(-1, act.dp(260)))
                    btn("保存到相册") {
                        val uri = ImageKit.save(act, bmp, "qrcode_${System.currentTimeMillis()}.png")
                        Ui.toast(act, if (uri != null) "已保存" else "保存失败")
                    }
                    btn("分享", primary = false) {
                        ImageKit.shareable(act, bmp)?.let { ImageKit.share(act, it) }
                    }
                }
            })
            }
        }
    }

    private fun typeName(f: Int): String = when (f) {
        Barcode.FORMAT_QR_CODE -> "二维码"
        Barcode.FORMAT_EAN_13 -> "EAN-13 条码"
        Barcode.FORMAT_EAN_8 -> "EAN-8 条码"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_PDF417 -> "PDF417"
        else -> "条码类型 $f"
    }

    private fun imageSearch(act: ToolActivity, body: LinearLayout) {
        body.note("识图引擎需要图片上传后才能搜索：先保存图片，再用下面任一入口粘贴/上传。")
        var saved: android.net.Uri? = null
        pickRow(act, body) { list ->
            val u = list.firstOrNull() ?: return@pickRow
            val bmp = ImageKit.decode(act, u) ?: return@pickRow
            saved = ImageKit.save(act, bmp, "search_${System.currentTimeMillis()}")
            Ui.toast(act, if (saved != null) "图片已保存到相册" else "保存失败")
        }
        body.addView(act.card {
            sectionTitle("识图入口")
            val engines = listOf(
                "Google Lens" to "https://lens.google.com/",
                "百度识图" to "https://graph.baidu.com/pcpage/index?tpl_from=pc",
                "Yandex 识图" to "https://yandex.com/images/",
                "TinEye" to "https://tineye.com/",
                "搜狗识图" to "https://pic.sogou.com/",
            )
            engines.forEach { (name, url) ->
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, act.dp(12), 0, act.dp(12))
                    isClickable = true
                    setOnClickListener {
                        runCatching { act.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                            .onFailure { Ui.toast(act, "没有可用的浏览器") }
                    }
                }
                row.addView(act.tv(name, 14f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(act.tv("›", 18f, T.ink3))
                addView(row)
            }
        })
    }

    private fun photoCount(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.note("点一下图片添加一个计数标记，再点一次可移除。适合数零件、人数、货物。")
        pickRow(act, body) { list ->
            val u = list.firstOrNull() ?: return@pickRow
            val bmp = ImageKit.decode(act, u, 2400) ?: return@pickRow
            holder.removeAllViews()
            val view = CountView(act, bmp)
            val label = act.tv("已标记 0 个", 15f, T.ink, true).apply {
                setPadding(0, act.dp(6), 0, act.dp(8))
            }
            view.onChange = { n -> label.text = "已标记 $n 个" }
            holder.addView(act.card {
                sectionTitle("点击图片计数")
                addView(label)
                addView(view, LinearLayout.LayoutParams(-1, act.dp(360)))
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                val undo = act.tv("撤销", 13f, T.ink, true).apply {
                    gravity = Gravity.CENTER
                    background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                    setPadding(0, act.dp(11), 0, act.dp(11))
                    isClickable = true
                    setOnClickListener { view.undo() }
                }
                val clear = act.tv("清空", 13f, T.ink, true).apply {
                    gravity = Gravity.CENTER
                    background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                    setPadding(0, act.dp(11), 0, act.dp(11))
                    isClickable = true
                    setOnClickListener { view.clearAll() }
                }
                val save = act.tv("保存标注图", 13f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                    setPadding(0, act.dp(11), 0, act.dp(11))
                    isClickable = true
                    setOnClickListener {
                        val out = view.export()
                        Ui.toast(act, if (ImageKit.save(act, out, "count") != null) "已保存" else "保存失败")
                    }
                }
                listOf(undo, clear, save).forEach {
                    row.addView(it, LinearLayout.LayoutParams(0, -2, 1f).also { lp -> lp.rightMargin = act.dp(8) })
                }
                addView(row)
            })
        }
        body.addView(holder)
    }

    /** 图片 + 打点标注。 */
    private class CountView(context: android.content.Context, private val bmp: Bitmap) : View(context) {
        private val marks = ArrayList<Pair<Float, Float>>()   // 归一化坐标
        var onChange: ((Int) -> Unit)? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            setBackgroundColor(Color.BLACK)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val scale = minOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val ox = (width - dw) / 2
            val oy = (height - dh) / 2
            canvas.drawBitmap(bmp, null, android.graphics.RectF(ox, oy, ox + dw, oy + dh), Paint(Paint.FILTER_BITMAP_FLAG))
            paint.style = Paint.Style.FILL
            marks.forEachIndexed { i, (nx, ny) ->
                val x = ox + nx * dw
                val y = oy + ny * dh
                paint.color = 0xCCFF3B30.toInt()
                canvas.drawCircle(x, y, 14f, paint)
                paint.color = Color.WHITE
                paint.textSize = 20f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("${i + 1}", x, y + 7f, paint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_DOWN) return true
            val scale = minOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val ox = (width - dw) / 2
            val oy = (height - dh) / 2
            val nx = (event.x - ox) / dw
            val ny = (event.y - oy) / dh
            if (nx !in 0f..1f || ny !in 0f..1f) return true
            // 点在已有标记附近则移除
            val hit = marks.indexOfFirst { (mx, my) ->
                val dx = (mx - nx) * dw
                val dy = (my - ny) * dh
                dx * dx + dy * dy < 40f * 40f
            }
            if (hit >= 0) marks.removeAt(hit) else marks.add(nx to ny)
            onChange?.invoke(marks.size)
            invalidate()
            return true
        }

        fun undo() {
            if (marks.isNotEmpty()) marks.removeAt(marks.size - 1)
            onChange?.invoke(marks.size)
            invalidate()
        }

        fun clearAll() {
            marks.clear()
            onChange?.invoke(0)
            invalidate()
        }

        fun export(): Bitmap {
            val out = bmp.copy(Bitmap.Config.ARGB_8888, true)
            val c = Canvas(out)
            paint.style = Paint.Style.FILL
            marks.forEachIndexed { i, (nx, ny) ->
                val x = nx * out.width
                val y = ny * out.height
                paint.color = 0xCCFF3B30.toInt()
                c.drawCircle(x, y, out.width * 0.018f, paint)
                paint.color = Color.WHITE
                paint.textSize = out.width * 0.026f
                paint.textAlign = Paint.Align.CENTER
                c.drawText("${i + 1}", x, y + paint.textSize * 0.35f, paint)
            }
            return out
        }
    }
}
