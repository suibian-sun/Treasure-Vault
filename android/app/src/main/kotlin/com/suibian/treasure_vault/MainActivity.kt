import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileOutputStream

/**
 * 宿主 Activity —— 只承担 Flutter 无法用标准库完成的最小原生能力：
 *
 *  1. appDirs       应用私有目录（设置持久化用它，避免引入 path_provider）
 *  2. deviceInfo    设备与 ABI 信息（用于挑选正确的 APK 变体）
 *  3. installApk    调用系统安装器（应用内更新的最后一步）
 *  4. pickFile      SAF 文件选择（避免引入 file_picker）
 *  5. imageInfo     只读图片头，不解码像素
 *  6. compressImage 原生采样 + JPEG 压缩（内存峰值远低于 Dart 解码整图）
 *
 * 选型原则：这些能力若用插件实现，会引入额外 .so 与插件注册开销；
 * 自己写不到 200 行 Kotlin，换来更小的包体与更低的常驻内存。
 */
class MainActivity : FlutterActivity() {

    private var pendingPick: MethodChannel.Result? = null

    companion object {
        private const val REQ_PICK = 1001
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "treasure_vault/native")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "appDirs" -> result.success(
                        mapOf(
                            "files" to (filesDir?.absolutePath ?: cacheDir.absolutePath),
                            "cache" to cacheDir.absolutePath,
                        )
                    )

                    "deviceInfo" -> result.success(deviceInfo())

                    "installApk" -> {
                        val path = call.argument<String>("path")
                        if (path.isNullOrEmpty()) {
                            result.error("BAD_PATH", "安装包路径为空", null)
                        } else {
                            try {
                                result.success(installApk(path))
                            } catch (e: Exception) {
                                result.error("INSTALL_FAILED", e.message, null)
                            }
                        }
                    }

                    "pickFile" -> {
                        if (pendingPick != null) {
                            result.error("BUSY", "已有文件选择进行中", null)
                            return@setMethodCallHandler
                        }
                        pendingPick = result
                        val mime = call.argument<String>("mime") ?: "*/*"
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = mime
                            }
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, REQ_PICK)
                        } catch (e: Exception) {
                            pendingPick = null
                            result.error("PICK_FAILED", e.message, null)
                        }
                    }

                    "imageInfo" -> {
                        val path = call.argument<String>("path")
                        val file = if (path == null) null else safeFile(path)
                        if (file == null) {
                            result.error("BAD_PATH", "文件不可读", null)
                        } else {
                            result.success(imageInfo(file))
                        }
                    }

                    "compressImage" -> {
                        val path = call.argument<String>("path")
                        val maxDim = call.argument<Int>("maxDim") ?: 1280
                        val quality = call.argument<Int>("quality") ?: 80
                        val file = if (path == null) null else safeFile(path)
                        if (file == null) {
                            result.error("BAD_PATH", "文件不可读", null)
                        } else {
                            try {
                                result.success(compressImage(file, maxDim, quality))
                            } catch (e: Exception) {
                                result.error("COMPRESS_FAILED", e.message, null)
                            }
                        }
                    }

                    else -> result.notImplemented()
                }
            }
    }

    /**
     * 兼容式文件选择回调。
     * 用 startActivityForResult 而不是 ActivityResultContracts：
     * 后者需要 androidx.activity 的额外编译期依赖，这里保持零新增依赖。
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK) return
        val result = pendingPick
        pendingPick = null
        if (result == null) return
        val uri: Uri? = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) {
            result.success(null)
            return
        }
        try {
            result.success(copyToCache(uri))
        } catch (e: Exception) {
            result.error("PICK_FAILED", e.message ?: "读取文件失败", null)
        }
    }

    override fun onDestroy() {
        pendingPick = null
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // 目录与安全
    // ------------------------------------------------------------------

    /** 允许访问的根目录：应用私有 files / cache / 外部私有目录。 */
    private fun allowedRoots(): List<String> = listOfNotNull(
        filesDir?.canonicalPath,
        cacheDir?.canonicalPath,
        getExternalFilesDir(null)?.canonicalPath,
        noBackupFilesDir?.canonicalPath,
    )

    /** 规范化路径并做白名单校验，防止任意路径读取。 */
    private fun safeFile(path: String): File? {
        val f = File(path)
        if (!f.exists() || !f.isFile) return null
        val canonical = try {
            f.canonicalPath
        } catch (_: Exception) {
            return null
        }
        val ok = allowedRoots().any { canonical == it || canonical.startsWith("$it${File.separator}") }
        return if (ok) f else null
    }

    private fun pickDir(): File {
        val dir = File(cacheDir, "picked")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 把 SAF 选中的文件复制到应用缓存，返回可被后续工具读取的路径。 */
    private fun copyToCache(uri: Uri): String {
        var name = "picked_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                val raw = cursor.getString(idx)
                if (!raw.isNullOrEmpty()) name = sanitize(raw)
            }
        }
        val target = File(pickDir(), "${System.currentTimeMillis()}_$name")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output, 64 * 1024) }
        } ?: throw IllegalStateException("无法打开所选文件")
        return target.absolutePath
    }

    private fun sanitize(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = base.replace(Regex("[^\\w.\\-\\u4e00-\\u9fa5]"), "_")
        return if (cleaned.isEmpty()) "file" else cleaned.take(80)
    }

    // ------------------------------------------------------------------
    // 图片
    // ------------------------------------------------------------------

    /** 只读图片头，不解码像素。 */
    private fun imageInfo(file: File): Map<String, Any> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return mapOf(
            "width" to opts.outWidth,
            "height" to opts.outHeight,
            "bytes" to file.length(),
            "mime" to (opts.outMimeType ?: "unknown"),
            "path" to file.absolutePath,
        )
    }

    /**
     * 压缩图片：先按 inSampleSize 采样解码，再按长边缩放，最后 JPEG 输出。
     * 整个过程不会把原图整幅读入内存，因此大图也不会 OOM。
     */
    private fun compressImage(file: File, maxDim: Int, quality: Int): Map<String, Any> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) throw IllegalArgumentException("不是有效的图片")

        var sample = 1
        val longEdge = maxOf(srcW, srcH)
        while (longEdge / (sample * 2) >= maxDim) {
            sample *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            ?: throw IllegalStateException("图片解码失败")

        val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height))
        val target = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }

        val out = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { fos ->
            target.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 100), fos)
        }
        val result = mapOf(
            "path" to out.absolutePath,
            "bytes" to out.length(),
            "width" to target.width,
            "height" to target.height,
            "originalBytes" to file.length(),
            "originalWidth" to srcW,
            "originalHeight" to srcH,
        )
        if (target !== decoded) target.recycle()
        decoded.recycle()
        return result
    }

    // ------------------------------------------------------------------
    // 安装
    // ------------------------------------------------------------------

    /** 通过 FileProvider + 系统安装器安装 APK（Android 7+ 禁止 file:// 直传）。 */
    private fun installApk(path: String): Boolean {
        val file = safeFile(path) ?: return false
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        return true
    }

    // ------------------------------------------------------------------
    // 设备信息
    // ------------------------------------------------------------------

    private fun deviceInfo(): Map<String, String> {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        return mapOf(
            "brand" to Build.BRAND.replaceFirstChar { it.uppercase() },
            "model" to Build.MODEL,
            "sdk" to "API ${Build.VERSION.SDK_INT}（Android ${Build.VERSION.RELEASE}）",
            "abi" to abi,
            "device" to Build.DEVICE,
            "dataDir" to (filesDir?.absolutePath ?: ""),
        )
    }
}
