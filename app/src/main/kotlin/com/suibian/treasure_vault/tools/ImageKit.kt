package com.suibian.treasure_vault.tools

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.LinearLayout
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.dp
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 图片工具公共能力：解码（带采样上限）、保存到相册、分享、预览。
 *
 * 全部使用系统 API，不引入任何图片库；解码时按目标边长采样，
 * 避免大图直接把内存打满（这是同类工具最常见的崩溃点）。
 */
object ImageKit {

    /** 解码上限：超出则等比缩小，保证单张位图不超过约 40MB。 */
    private const val MAX_EDGE = 4096

    fun openStream(ctx: Context, uri: Uri): InputStream? =
        runCatching { ctx.contentResolver.openInputStream(uri) }.getOrNull()

    /** 读取图片实际尺寸（只读头部）。 */
    fun bounds(ctx: Context, uri: Uri): Pair<Int, Int>? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(ctx, uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0) null else o.outWidth to o.outHeight
    }.getOrNull()

    fun decode(ctx: Context, uri: Uri, maxEdge: Int = MAX_EDGE): Bitmap? = runCatching {
        val b = bounds(ctx, uri) ?: return null
        var sample = 1
        var w = b.first
        var h = b.second
        while (maxOf(w, h) / sample > maxEdge) sample *= 2
        val o = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        openStream(ctx, uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    }.getOrNull()

    /**
     * 保存图片。
     * Android 10+ 走 MediaStore（相册可见，无需权限）；更低版本写应用外部私有目录。
     * 返回可分享的 Uri。
     */
    fun save(ctx: Context, bmp: Bitmap, name: String = "tv_${System.currentTimeMillis()}", quality: Int = 92): Uri? {
        val fileName = if (name.endsWith(".png")) name else "$name.jpg"
        val mime = if (fileName.endsWith(".png")) "image/png" else "image/jpeg"
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TreasureVault")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = ctx.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                resolver.openOutputStream(uri)?.use { out ->
                    bmp.compress(if (mime == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "TreasureVault")
                dir.mkdirs()
                val f = File(dir, fileName)
                FileOutputStream(f).use { out ->
                    bmp.compress(if (mime == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality, out)
                }
                Uri.fromFile(f)
            }
        }.getOrNull()
    }

    fun saveBitmapToFile(ctx: Context, bmp: Bitmap, dir: File, fileName: String, png: Boolean = false): File? =
        runCatching {
            dir.mkdirs()
            val f = File(dir, fileName)
            FileOutputStream(f).use {
                bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, it)
            }
            f
        }.getOrNull()

    /** 复制 Uri 到缓存文件（FTP 上传、音频处理需要真实路径）。 */
    fun copyToCache(ctx: Context, uri: Uri, suffix: String = ""): File? = runCatching {
        val dir = File(ctx.cacheDir, "picked").apply { mkdirs() }
        val f = File(dir, "f_${System.currentTimeMillis()}$suffix")
        openStream(ctx, uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
        f
    }.getOrNull()

    /** 缩略预览，长边不超过 [maxWidth] dp。 */
    fun preview(ctx: Context, bmp: Bitmap, maxHeightDp: Int = 220): ImageView =
        ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(bmp)
            background = T.fieldBg(ctx.dp(T.R_MEDIUM).toFloat())
            adjustViewBounds = true
            val w = ctx.resources.displayMetrics.widthPixels - ctx.dp(64)
            val ratio = bmp.height.toFloat() / bmp.width.toFloat()
            layoutParams = LinearLayout.LayoutParams(w, (w * ratio).toInt().coerceAtMost(ctx.dp(maxHeightDp))).also {
                it.bottomMargin = ctx.dp(10)
            }
        }

    fun share(ctx: Context, uri: Uri) {
        runCatching {
            val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = ctx.contentResolver.getType(uri) ?: "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(android.content.Intent.createChooser(i, "分享图片"))
        }.onFailure { Ui.toast(ctx, "分享失败") }
    }

    /** 复制一份到可对外授权的缓存（分享低版本 File Uri 时需要）。 */
    fun shareable(ctx: Context, bmp: Bitmap): Uri? {
        val f = saveBitmapToFile(ctx, bmp, File(ctx.cacheDir, "share"), "share_${System.currentTimeMillis()}.jpg") ?: return null
        return runCatching {
            androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        }.getOrNull()
    }

    // ---------------- Canvas 辅助 ----------------

    fun newBitmap(w: Int, h: Int): Bitmap = Bitmap.createBitmap(
        w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888,
    )

    fun textPaint(size: Float, color: Int = Color.WHITE, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    /** 在画布上绘制自动换行的文本，返回占用高度。 */
    fun drawWrapped(
        canvas: Canvas,
        text: String,
        paint: Paint,
        left: Float,
        top: Float,
        maxWidth: Float,
        lineGap: Float = 0f,
    ): Float {
        var y = top + paint.textSize
        for (para in text.split("\n")) {
            if (para.isEmpty()) {
                y += paint.textSize + lineGap
                continue
            }
            var line = StringBuilder()
            for (ch in para) {
                val test = line.toString() + ch
                if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                    canvas.drawText(line.toString(), left, y, paint)
                    y += paint.textSize + lineGap
                    line = StringBuilder().append(ch)
                } else {
                    line.append(ch)
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line.toString(), left, y, paint)
                y += paint.textSize + lineGap
            }
        }
        return y - top
    }

    /** 居中裁剪到指定宽高比。 */
    fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val targetRatio = w.toFloat() / h.toFloat()
        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val rect: Rect
        if (srcRatio > targetRatio) {
            val nw = (src.height * targetRatio).toInt()
            val x = (src.width - nw) / 2
            rect = Rect(x, 0, x + nw, src.height)
        } else {
            val nh = (src.width / targetRatio).toInt()
            val y = (src.height - nh) / 2
            rect = Rect(0, y, src.width, y + nh)
        }
        canvas.drawBitmap(src, rect, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** 等比缩放到指定长边。 */
    fun scaleTo(src: Bitmap, maxEdge: Int): Bitmap {
        val m = maxOf(src.width, src.height)
        if (m <= maxEdge) return src
        val r = maxEdge.toFloat() / m
        return Bitmap.createScaledBitmap(src, (src.width * r).toInt(), (src.height * r).toInt(), true)
    }
}
