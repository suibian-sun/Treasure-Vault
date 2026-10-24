package com.suibian.treasure_vault.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.suibian.treasure_vault.core.T
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** 纯 Kotlin 图像算法（无第三方库）。 */
object Filters {

    fun luma(c: Int): Int = ((Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000)

    fun mix(a: Int, b: Int, ratio: Float): Int = Color.rgb(
        (Color.red(a) * (1 - ratio) + Color.red(b) * ratio).toInt(),
        (Color.green(a) * (1 - ratio) + Color.green(b) * ratio).toInt(),
        (Color.blue(a) * (1 - ratio) + Color.blue(b) * ratio).toInt(),
    )

    fun shiftHue(color: Int, deg: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + deg + 360f) % 360f
        return Color.HSVToColor(hsv)
    }

    /** USM 锐化：src + amount × (src − 均值模糊)。 */
    fun unsharp(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        val input = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        // 3×3 均值模糊的近似：先横向再纵向的一维均值，减少一次全图遍历
        val tmp = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val x0 = if (x > 0) x - 1 else 0
                val x1 = if (x < w - 1) x + 1 else w - 1
                val c0 = input[row + x0]
                val c1 = input[row + x]
                val c2 = input[row + x1]
                tmp[row + x] = Color.rgb(
                    (Color.red(c0) + Color.red(c1) + Color.red(c2)) / 3,
                    (Color.green(c0) + Color.green(c1) + Color.green(c2)) / 3,
                    (Color.blue(c0) + Color.blue(c1) + Color.blue(c2)) / 3,
                )
            }
        }
        for (y in 0 until h) {
            val y0 = if (y > 0) (y - 1) * w else 0
            val y1 = y * w
            val y2 = if (y < h - 1) (y + 1) * w else (h - 1) * w
            for (x in 0 until w) {
                val a = tmp[y0 + x]
                val b = tmp[y1 + x]
                val c = tmp[y2 + x]
                val blurR = (Color.red(a) + Color.red(b) + Color.red(c)) / 3
                val blurG = (Color.green(a) + Color.green(b) + Color.green(c)) / 3
                val blurB = (Color.blue(a) + Color.blue(b) + Color.blue(c)) / 3
                val p = input[y1 + x]
                val r = (Color.red(p) + amount * (Color.red(p) - blurR)).toInt().coerceIn(0, 255)
                val g = (Color.green(p) + amount * (Color.green(p) - blurG)).toInt().coerceIn(0, 255)
                val bl = (Color.blue(p) + amount * (Color.blue(p) - blurB)).toInt().coerceIn(0, 255)
                out[y1 + x] = Color.argb(Color.alpha(p), r, g, bl)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /** 双色调上色：亮度映射到「暗色 → 亮色」渐变。 */
    fun duotone(src: Bitmap, style: Int): Bitmap {
        val (dark, light) = when (style) {
            0 -> 0xFF3B2A1A.toInt() to 0xFFFFE8C8.toInt()
            1 -> 0xFF0B1E3A.toInt() to 0xFFBEE3FF.toInt()
            2 -> 0xFF06202B.toInt() to 0xFFFFD3A3.toInt()
            else -> 0xFF2A1040.toInt() to 0xFFFFC6EE.toInt()
        }
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val p = px[i]
            val l = luma(p) / 255f
            px[i] = mix(dark, light, l)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    // ---------------- 拼豆图纸 ----------------

    private val beadPalette = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFF4E3C8.toInt(), 0xFFE0C08D.toInt(), 0xFFC98A4B.toInt(),
        0xFF8B5A2B.toInt(), 0xFF5A3A1B.toInt(), 0xFF000000.toInt(), 0xFF6B7280.toInt(),
        0xFFD1D5DB.toInt(), 0xFFEF4444.toInt(), 0xFFB91C1C.toInt(), 0xFFF97316.toInt(),
        0xFFFBBF24.toInt(), 0xFFFDE68A.toInt(), 0xFF84CC16.toInt(), 0xFF22C55E.toInt(),
        0xFF15803D.toInt(), 0xFF06B6D4.toInt(), 0xFF3B82F6.toInt(), 0xFF1E3A8A.toInt(),
        0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(), 0xFFF9A8D4.toInt(), 0xFF7C2D12.toInt(),
    )

    fun beadPattern(src: Bitmap, cells: Int): Bitmap {
        val small = Bitmap.createScaledBitmap(src, cells, cells, true)
        val cellPx = if (cells <= 32) 30 else if (cells <= 48) 22 else 18
        val gap = 2
        val step = cellPx + gap
        val pad = 16
        val size = pad * 2 + cells * step
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (y in 0 until cells) for (x in 0 until cells) {
            val px = small.getPixel(x, y)
            var best = beadPalette[0]
            var bestD = Int.MAX_VALUE
            for (b in beadPalette) {
                val dr = Color.red(px) - Color.red(b)
                val dg = Color.green(px) - Color.green(b)
                val db = Color.blue(px) - Color.blue(b)
                val d = dr * dr + dg * dg + db * db
                if (d < bestD) { bestD = d; best = b }
            }
            p.color = best
            p.style = Paint.Style.FILL
            val left = (pad + x * step).toFloat()
            val top = (pad + y * step).toFloat()
            c.drawRoundRect(left, top, left + cellPx, top + cellPx, 3f, 3f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            p.color = 0xFFE5E7EB.toInt()
            c.drawRoundRect(left, top, left + cellPx, top + cellPx, 3f, 3f, p)
        }
        // 每 5 格加粗辅助线
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = 0xFF9CA3AF.toInt()
        for (i in 0..cells step 5) {
            val v = (pad + i * step - gap / 2f)
            c.drawLine(v, pad.toFloat(), v, (pad + cells * step).toFloat(), p)
            c.drawLine(pad.toFloat(), v, (pad + cells * step).toFloat(), v, p)
        }
        small.recycle()
        return out
    }

    // ---------------- LSB 隐写 ----------------

    private const val MAGIC = 0x54565331 // "TVS1"

    fun hideText(src: Bitmap, text: String): Bitmap {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArrayOutputStream().apply {
            write(byteArrayOf(
                (MAGIC ushr 24).toByte(), (MAGIC ushr 16).toByte(),
                (MAGIC ushr 8).toByte(), MAGIC.toByte(),
            ))
            write(byteArrayOf(
                (bytes.size ushr 24).toByte(), (bytes.size ushr 16).toByte(),
                (bytes.size ushr 8).toByte(), bytes.size.toByte(),
            ))
            write(bytes)
        }.toByteArray()
        val capacity = src.width * src.height / 8
        if (payload.size > capacity) throw IllegalStateException("图片太小，最多能藏 $capacity 字节")
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val px = IntArray(w * h)
        out.getPixels(px, 0, w, 0, 0, w, h)
        var bitIndex = 0
        val total = payload.size * 8
        outer@ for (i in px.indices) {
            if (bitIndex >= total) break@outer
            var c = px[i]
            var b = Color.blue(c)
            val byte = payload[bitIndex / 8].toInt()
            val bit = (byte ushr (7 - bitIndex % 8)) and 1
            b = (b and 0xFE) or bit
            px[i] = Color.argb(Color.alpha(c), Color.red(c), Color.green(c), b)
            bitIndex++
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    fun extractText(src: Bitmap): String? = runCatching {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        fun byteAt(idx: Int): Int {
            var v = 0
            for (k in 0 until 8) {
                val pos = idx * 8 + k
                if (pos >= px.size) return -1
                v = (v shl 1) or (Color.blue(px[pos]) and 1)
            }
            return v
        }
        val magic = (byteAt(0) shl 24) or (byteAt(1) shl 16) or (byteAt(2) shl 8) or byteAt(3)
        if (magic != MAGIC) return null
        val len = (byteAt(4) shl 24) or (byteAt(5) shl 16) or (byteAt(6) shl 8) or byteAt(7)
        if (len <= 0 || len > px.size / 8) return null
        val bytes = ByteArray(len) { byteAt(8 + it).toByte() }
        String(bytes, Charsets.UTF_8)
    }.getOrNull()

    /** 把一段文本渲染成图片（用于展示隐写结果）。 */
    fun textBitmap(text: String, width: Int = 1000): Bitmap {
        val pad = 32f
        val paint = ImageKit.textPaint(34f, 0xFF1F2329.toInt())
        var height = 0f
        for (para in text.split("\n")) {
            height += paint.textSize * 1.5f * maxOf(1, (paint.measureText(para) / (width - pad * 2)).toInt() + 1)
        }
        val bmp = ImageKit.newBitmap(width, (height + pad * 2).toInt())
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        ImageKit.drawWrapped(c, text, paint, pad, pad, width - pad * 2, paint.textSize * 0.5f)
        return bmp
    }
}

/**
 * 极简 GIF89a 编码器（LZW + 216 色固定调色板）。
 *
 * 不依赖任何三方库：Android 自带的 Movie 只能解码，编码需要自己实现。
 */
object GifEncoder {

    fun encode(frames: List<Bitmap>, delayMs: Int, outFile: File): File? = runCatching {
        if (frames.isEmpty()) return null
        val w = frames[0].width
        val h = frames[0].height
        FileOutputStream(outFile).use { fos ->
            val out = fos.buffered()
            out.write("GIF89a".toByteArray(Charsets.US_ASCII))
            // 逻辑屏幕描述
            writeShort(out, w)
            writeShort(out, h)
            out.write(0xF7) // GCT 存在 + 256 色
            out.write(0)    // 背景色索引
            out.write(0)    // 像素宽高比
            writePalette(out)
            // 循环扩展
            out.write(0x21); out.write(0xFF); out.write(0x0B)
            out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
            out.write(0x03); out.write(0x01); writeShort(out, 0); out.write(0x00)
            val delayCs = (delayMs / 10).coerceIn(2, 1000)
            for (f in frames) {
                // 图形控制扩展
                out.write(0x21); out.write(0xF9); out.write(0x04)
                out.write(0x04); writeShort(out, delayCs); out.write(0x00); out.write(0x00)
                // 图像描述
                out.write(0x2C); writeShort(out, 0); writeShort(out, 0)
                writeShort(out, w); writeShort(out, h); out.write(0x00)
                out.write(0x08) // LZW 最小码长
                val pixels = quantize(f, w, h)
                writeLzw(out, pixels)
                out.write(0x00)
            }
            out.write(0x3B)
            out.flush()
        }
        outFile
    }.getOrNull()

    private fun writeShort(out: OutputStream, v: Int) {
        out.write(v and 0xFF); out.write((v ushr 8) and 0xFF)
    }

    private fun writePalette(out: OutputStream) {
        for (r in 0 until 6) for (g in 0 until 6) for (b in 0 until 6) {
            out.write(r * 51); out.write(g * 51); out.write(b * 51)
        }
        for (i in 216 until 256) {
            val v = ((i - 216) * 255 / 39)
            out.write(v); out.write(v); out.write(v)
        }
    }

    private fun quantize(bmp: Bitmap, w: Int, h: Int): ByteArray {
        val scaled = if (bmp.width == w && bmp.height == h) bmp
        else Bitmap.createScaledBitmap(bmp, w, h, true)
        val px = IntArray(w * h)
        scaled.getPixels(px, 0, w, 0, 0, w, h)
        val out = ByteArray(w * h)
        for (i in px.indices) {
            val c = px[i]
            val r = Color.red(c) * 5 / 255
            val g = Color.green(c) * 5 / 255
            val b = Color.blue(c) * 5 / 255
            out[i] = (r * 36 + g * 6 + b).toByte()
        }
        if (scaled !== bmp) scaled.recycle()
        return out
    }

    private fun writeLzw(out: OutputStream, pixels: ByteArray) {
        val clear = 256
        val end = 257
        var size = 9
        var next = 258
        val dict = HashMap<Int, Int>(4096)
        val writer = BlockWriter(out)
        writer.writeCode(clear, size)
        var prefix = pixels[0].toInt() and 0xFF
        for (i in 1 until pixels.size) {
            val ch = pixels[i].toInt() and 0xFF
            val key = (prefix shl 8) or ch
            val code = dict[key]
            if (code != null) {
                prefix = code
            } else {
                writer.writeCode(prefix, size)
                if (next < 4096) {
                    dict[key] = next
                    next++
                    if (next > (1 shl size) && size < 12) size++
                } else {
                    writer.writeCode(clear, size)
                    dict.clear()
                    next = end + 1
                    size = 9
                }
                prefix = ch
            }
        }
        writer.writeCode(prefix, size)
        writer.writeCode(end, size)
        writer.finish()
    }

    /** GIF 数据子块（每块最多 255 字节）。 */
    private class BlockWriter(private val out: OutputStream) {
        private val block = ByteArray(255)
        private var len = 0
        private var cur = 0
        private var bits = 0

        fun writeCode(code: Int, size: Int) {
            cur = cur or (code shl bits)
            bits += size
            while (bits >= 8) {
                push((cur and 0xFF).toByte())
                cur = cur ushr 8
                bits -= 8
            }
        }

        private fun push(b: Byte) {
            block[len++] = b
            if (len == 255) flushBlock()
        }

        fun flushBlock() {
            if (len > 0) {
                out.write(len)
                out.write(block, 0, len)
                len = 0
            }
        }

        fun finish() {
            if (bits > 0) {
                push((cur and 0xFF).toByte())
                cur = 0
                bits = 0
            }
            flushBlock()
        }
    }
}

/** 极简画板：路径 + 颜色 + 粗细，支持撤销 / 清空 / 导出。 */
class DrawBoard(context: android.content.Context) : View(context) {

    private class Stroke(val path: Path, val color: Int, val width: Float)

    private val strokes = ArrayList<Stroke>()
    private var current: Stroke? = null
    var penColor: Int = Color.BLACK
    var penWidth: Float = 9f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val bg = Paint().apply { color = Color.WHITE }

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        for (s in strokes) {
            paint.color = s.color
            paint.strokeWidth = s.width
            canvas.drawPath(s.path, paint)
        }
        current?.let {
            paint.color = it.color
            paint.strokeWidth = it.width
            canvas.drawPath(it.path, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val s = Stroke(Path(), penColor, penWidth)
                s.path.moveTo(event.x, event.y)
                current = s
                strokes.add(s)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                current?.let {
                    it.path.lineTo(event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current?.let {
                    it.path.lineTo(event.x, event.y)
                    invalidate()
                }
                current = null
            }
        }
        return true
    }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1)
        invalidate()
    }

    fun clearAll() {
        strokes.clear()
        invalidate()
    }

    fun export(): Bitmap {
        val w = if (width > 0) width else 1080
        val h = if (height > 0) height else 720
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp))
        return bmp
    }
}
