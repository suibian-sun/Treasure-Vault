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
        private const val REQ_PICK = 0x52
        private const val REQ_PICK_MULTI = 0x53
        private const val REQ_CAMERA = 0x54
        private const val REQ_PERMISSION = 0x60
    }

    lateinit var spec: ToolSpec
    lateinit var body: LinearLayout
    private lateinit var star: TextView

    /** 图片选择 / 拍照的临时回调（工具页用完即弃，不持有引用）。 */
    var onPicked: ((android.net.Uri?) -> Unit)? = null
        private set
    private var pickRequest = 0x52
    private var pendingCameraUri: android.net.Uri? = null

    /** 选择一张图片。 */
    fun pickImage(mime: String = "image/*", onResult: (android.net.Uri?) -> Unit) {
        onPicked = onResult
        pickRequest = REQ_PICK
        val i = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = mime
        }
        runCatching { startActivityForResult(i, REQ_PICK) }
            .onFailure { onPicked = null; Ui.toast(this, "无法打开文件选择器") }
    }

    /** 选择多个文件（用于拼接、GIF 合成）。 */
    fun pickImages(onResult: (List<android.net.Uri>) -> Unit) {
        onMultiPicked = onResult
        val i = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        runCatching { startActivityForResult(i, REQ_PICK_MULTI) }
            .onFailure { onMultiPicked = null; Ui.toast(this, "无法打开文件选择器") }
    }

    /** 选择任意文件（FTP 上传、音频等）。 */
    fun pickFile(onResult: (android.net.Uri?) -> Unit) {
        onPicked = onResult
        val i = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        runCatching { startActivityForResult(i, REQ_PICK) }
            .onFailure { onPicked = null; Ui.toast(this, "无法打开文件选择器") }
    }

    private var onMultiPicked: ((List<android.net.Uri>) -> Unit)? = null

    /** 调起系统相机拍照，结果回传为临时文件 Uri。 */
    fun capture(onResult: (android.net.Uri?) -> Unit) {
        onPicked = onResult
        runCatching {
            val dir = java.io.File(cacheDir, "shots").apply { mkdirs() }
            val f = java.io.File(dir, "shot_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", f,
            )
            pendingCameraUri = uri
            val i = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(i, REQ_CAMERA)
        }.onFailure {
            onPicked = null
            Ui.toast(this, "无法调起相机：${it.message}")
        }
    }

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

    private val cleanups = mutableListOf<() -> Unit>()

    /** 注册页面销毁时的清理动作（传感器注销、录音停止等）。 */
    fun onCleanup(block: () -> Unit) {
        cleanups.add(block)
    }

    private var permissionCallback: ((Boolean) -> Unit)? = null

    /** 请求单个运行时权限。 */
    fun askPermission(permission: String, onResult: (Boolean) -> Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }
        permissionCallback = onResult
        androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), REQ_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_PERMISSION) return
        val cb = permissionCallback
        permissionCallback = null
        cb?.invoke(grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    override fun onDestroy() {
        cleanups.forEach { runCatching { it() } }
        cleanups.clear()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_PICK -> {
                val cb = onPicked; onPicked = null
                cb?.invoke(if (resultCode == RESULT_OK) data?.data else null)
            }
            REQ_PICK_MULTI -> {
                val cb = onMultiPicked; onMultiPicked = null
                if (resultCode != RESULT_OK) { cb?.invoke(emptyList()); return }
                val list = mutableListOf<android.net.Uri>()
                val clip = data?.clipData
                if (clip != null) {
                    for (i in 0 until clip.itemCount) list.add(clip.getItemAt(i).uri)
                } else {
                    data?.data?.let { list.add(it) }
                }
                cb?.invoke(list)
            }
            REQ_CAMERA -> {
                val cb = onPicked; onPicked = null
                cb?.invoke(if (resultCode == RESULT_OK) pendingCameraUri else null)
            }
            0x51 -> {
                if (resultCode != RESULT_OK) return
                val uri = data?.data ?: return
                runCatching {
                    val wm = getSystemService(android.app.WallpaperManager::class.java)
                    contentResolver.openInputStream(uri)?.use { wm.setStream(it) }
                    Ui.toast(this, "壁纸已设置")
                }.onFailure { Ui.toast(this, "设置失败：${it.message}") }
            }
        }
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
