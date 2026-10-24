package com.suibian.treasure_vault.tools

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/** 存储清理与局域网传输。 */
object CleanTools {

    private val pool = Executors.newFixedThreadPool(2)

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.system, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("big_files", "大文件清理", "找出占用空间最多的文件", "🐘", "大文件", "清理") { a, b -> scan(a, b, Mode.BIG) },
        spec("empty_files", "空文件清理", "0 字节的残留文件", "📄", "空文件", "清理") { a, b -> scan(a, b, Mode.EMPTY_FILE) },
        spec("empty_dirs", "空文件夹清理", "没有内容的多余目录", "📁", "空目录", "清理") { a, b -> scan(a, b, Mode.EMPTY_DIR) },
        spec("apk_cleaner", "安装包清理", "扫描残留的 .apk 安装包", "📦", "apk", "安装包") { a, b -> scan(a, b, Mode.APK) },
        spec("ftp", "FTP 局域网传输", "手机开 FTP，电脑浏览器取文件", "🔗", "ftp", "局域网") { a, b -> ftp(a, b) },
    )

    private enum class Mode { BIG, EMPTY_FILE, EMPTY_DIR, APK }

    // ---------------- 权限 ----------------

    private fun storageReady(act: ToolActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                act, android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStorage(act: ToolActivity, onResult: (Boolean) -> Unit) {
        if (storageReady(act)) { onResult(true); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Ui.toast(act, "请在设置里允许「所有文件访问」后返回")
            runCatching {
                act.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${act.packageName}")),
                )
            }.onFailure {
                runCatching { act.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            }
            onResult(false)
        } else {
            act.askPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) { onResult(it) }
        }
    }

    // ---------------- 扫描类工具 ----------------

    private fun scan(act: ToolActivity, body: LinearLayout, mode: Mode) {
        var minMb = 50
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val summary = act.tv("尚未扫描", 12.5f, T.ink2)

        body.addView(act.card {
            sectionTitle(
                when (mode) {
                    Mode.BIG -> "大文件扫描"
                    Mode.EMPTY_FILE -> "空文件扫描"
                    Mode.EMPTY_DIR -> "空文件夹扫描"
                    else -> "安装包扫描"
                },
            )
            if (mode == Mode.BIG) {
                chips(listOf("> 50MB", "> 100MB", "> 500MB"), 0) { i -> minMb = listOf(50, 100, 500)[i] }
            }
            addView(summary)
            btn("开始扫描") {
                requestStorage(act) { ok ->
                    if (!ok) {
                        summary.text = "未获得存储访问权限，请在系统设置中开启后重试"
                        summary.setTextColor(T.danger)
                        return@requestStorage
                    }
                    holder.removeAllViews()
                    summary.text = "扫描中…"
                    summary.setTextColor(T.ink2)
                    pool.execute {
                        val found = scanFiles(mode, minMb.toLong() * 1024 * 1024)
                        act.runOnUiThread { render(act, holder, summary, found, mode) }
                    }
                }
            }
            if (mode == Mode.EMPTY_DIR) {
                note("空目录删除后不可恢复，但通常不影响任何应用（缓存目录会自动重建）。")
            }
        })
        body.addView(holder)
    }

    private data class Hit(val file: File, val size: Long)

    private fun scanFiles(mode: Mode, threshold: Long): List<Hit> {
        val root = Environment.getExternalStorageDirectory() ?: return emptyList()
        val out = ArrayList<Hit>()
        var visited = 0
        fun walk(dir: File, depth: Int) {
            if (depth > 8 || visited > 60_000) return
            val children = runCatching { dir.listFiles() }.getOrNull() ?: return
            if (children.isEmpty() && depth > 1 && mode == Mode.EMPTY_DIR && dir.absolutePath != root.absolutePath) {
                out.add(Hit(dir, 0))
                return
            }
            for (f in children) {
                visited++
                val name = f.name
                if (f.isDirectory) {
                    if (name == "Android" || name.startsWith(".")) continue
                    walk(f, depth + 1)
                } else {
                    val size = f.length()
                    when (mode) {
                        Mode.BIG -> if (size >= threshold) out.add(Hit(f, size))
                        Mode.EMPTY_FILE -> if (size == 0L) out.add(Hit(f, 0))
                        Mode.APK -> if (name.endsWith(".apk", true)) out.add(Hit(f, size))
                        Mode.EMPTY_DIR -> {}
                    }
                }
            }
        }
        walk(root, 0)
        return if (mode == Mode.BIG) out.sortedByDescending { it.size } else out.sortedBy { it.file.absolutePath }
    }

    private fun human(bytes: Long): String = when {
        bytes >= 1L shl 30 -> "%.1f GB".format(bytes / 1073741824.0)
        bytes >= 1L shl 20 -> "%.1f MB".format(bytes / 1048576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun render(act: ToolActivity, holder: LinearLayout, summary: android.widget.TextView,
                       hits: List<Hit>, mode: Mode) {
        holder.removeAllViews()
        if (hits.isEmpty()) {
            summary.text = "没有找到符合条件的项目"
            summary.setTextColor(T.ok)
            return
        }
        val total = hits.sumOf { it.size }
        summary.text = "找到 ${hits.size} 项，共 ${human(total)}"
        summary.setTextColor(T.ink)

        val listBox = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var shown = 0
        val page = 40
        fun addPage() {
            val end = minOf(shown + page, hits.size)
            for (i in shown until end) {
                val h = hits[i]
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, act.dp(9), 0, act.dp(9))
                }
                val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                col.addView(act.tv(h.file.name, 13f, T.ink))
                col.addView(act.tv(h.file.parent ?: "", 10.5f, T.ink3).apply {
                    setPadding(0, act.dp(2), 0, 0)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                })
                row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
                if (h.size > 0) {
                    row.addView(act.tv(human(h.size), 12f, T.ink2).apply { setPadding(act.dp(8), 0, act.dp(8), 0) })
                }
                val del = act.tv("删除", 12f, T.danger, true)
                del.isClickable = true
                del.setOnClickListener {
                    val ok = runCatching { h.file.delete() }.getOrDefault(false)
                    Ui.toast(act, if (ok) "已删除" else "删除失败（可能受系统保护）")
                    if (ok) { row.alpha = 0.4f; del.isEnabled = false; del.text = "已删除" }
                }
                row.addView(del)
                listBox.addView(row)
                listBox.addView(act.hairline())
            }
            shown = end
        }
        addPage()
        if (hits.size > shown) {
            val more = act.tv("显示更多（剩余 ${hits.size - shown}）", 13f, T.primary, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, act.dp(12), 0, act.dp(6))
                isClickable = true
                setOnClickListener { addPage(); if (shown >= hits.size) { text = "已全部显示"; isClickable = false } }
            }
            holder.addView(act.card {
                sectionTitle("扫描结果")
                addView(listBox)
                addView(more)
            })
        } else {
            holder.addView(act.card {
                sectionTitle("扫描结果")
                addView(listBox)
            })
        }
    }

    // ---------------- FTP ----------------

    private fun ftp(act: ToolActivity, body: LinearLayout) {
        var server: FtpServer? = null
        val rootDir = File(act.getExternalFilesDir(null), "ftp").apply { mkdirs() }
        val status = act.tv("未启动", 13f, T.ink2, true)
        val log = act.tv("", 11f, T.ink3).apply {
            setTextIsSelectable(true)
            setPadding(0, act.dp(8), 0, 0)
            maxLines = 12
        }
        val startBtn = act.tv("启动 FTP 服务", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.primary, act.dp(T.R_MEDIUM).toFloat())
            setPadding(0, act.dp(13), 0, act.dp(13))
            isClickable = true
        }
        val addr = act.tv("—", 15f, T.primary, true).apply {
            setTextIsSelectable(true)
            setPadding(0, act.dp(6), 0, act.dp(4))
        }

        body.addView(act.card {
            sectionTitle("FTP 服务器")
            addView(act.tv("共享目录：${rootDir.absolutePath}", 11f, T.ink3).apply {
                setTextIsSelectable(true)
                setPadding(0, 0, 0, act.dp(10))
            })
            addView(addr)
            addView(status)
            addView(startBtn, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
            addView(log)
        })
        body.note("启动后在同一 Wi-Fi 下，用电脑浏览器打开 ftp://地址:2121 即可浏览、下载或上传文件。账号密码随便填（匿名模式）。")

        startBtn.setOnClickListener {
            val cur = server
            if (cur != null) {
                cur.stop()
                server = null
                startBtn.text = "启动 FTP 服务"
                status.text = "已停止"
                status.setTextColor(T.ink2)
                addr.text = "—"
                return@setOnClickListener
            }
            val ip = localIp()
            if (ip == null) {
                Ui.toast(act, "没有连上局域网，请先连接 Wi-Fi")
                return@setOnClickListener
            }
            val s = FtpServer(rootDir, 2121) { msg ->
                act.runOnUiThread {
                    log.text = "${msg}\n${log.text}".take(1200)
                }
            }
            val ok = s.start()
            if (!ok) {
                Ui.toast(act, "端口被占用，无法启动")
                return@setOnClickListener
            }
            server = s
            startBtn.text = "停止服务"
            status.text = "运行中"
            status.setTextColor(T.ok)
            addr.text = "ftp://$ip:2121"
            act.onCleanup { s.stop() }
        }
    }

    private fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.hostAddress?.startsWith("192.168.") == true || it.hostAddress?.startsWith("10.") == true || it.hostAddress?.startsWith("172.") == true }
            ?.hostAddress
    }.getOrNull()

    /** 极简 FTP 服务（PASV + 匿名登录 + LIST/RETR/STOR/CWD）。 */
    private class FtpServer(
        private val root: File,
        private val port: Int,
        private val log: (String) -> Unit,
    ) {
        @Volatile private var running = false
        private var serverSocket: ServerSocket? = null

        fun start(): Boolean = runCatching {
            val ss = ServerSocket(port)
            serverSocket = ss
            running = true
            Thread {
                while (running) {
                    val socket = runCatching { ss.accept() }.getOrNull() ?: break
                    Thread { handle(socket) }.start()
                }
            }.start()
            log("服务已启动，端口 $port")
            true
        }.getOrDefault(false)

        fun stop() {
            running = false
            runCatching { serverSocket?.close() }
            serverSocket = null
            log("服务已停止")
        }

        private fun handle(socket: Socket) {
            runCatching {
                socket.soTimeout = 30000
                val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                val output = socket.getOutputStream()
                fun send(line: String) {
                    output.write((line + "\r\n").toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                var cwd = ""
                var pasv: ServerSocket? = null
                send("220 TreasureVault FTP ready")
                while (running) {
                    val line = input.readLine() ?: break
                    val cmd = line.substringBefore(' ').uppercase()
                    val arg = line.substringAfter(' ', "").trim()
                    when (cmd) {
                        "USER" -> send("331 anonymous ok, send any password")
                        "PASS" -> send("230 logged in")
                        "SYST" -> send("215 UNIX Type: L8")
                        "FEAT" -> { send("211-Features:"); send(" UTF8"); send("211 End") }
                        "PWD", "XPWD" -> send("257 \"/$cwd\"")
                        "CWD", "XCWD" -> {
                            val target = resolve(cwd, arg)
                            if (target != null && target.isDirectory) {
                                cwd = target.relativeTo(root).path.replace('\\', '/')
                                send("250 ok")
                            } else send("550 no such directory")
                        }
                        "TYPE" -> send("200 type set")
                        "SIZE" -> {
                            val f = resolve(cwd, arg)
                            if (f != null && f.isFile) send("213 ${f.length()}") else send("550 not a file")
                        }
                        "PASV" -> {
                            runCatching { pasv?.close() }
                            val ps = ServerSocket(0)
                            pasv = ps
                            val p = ps.localPort
                            val h = socket.localAddress.hostAddress?.replace('.', ',') ?: "127,0,0,1"
                            send("227 Entering Passive Mode ($h,${p / 256},${p % 256})")
                        }
                        "LIST", "NLST" -> {
                            val data = runCatching { pasv?.accept() }.getOrNull()
                            if (data == null) { send("425 use PASV first"); continue }
                            send("150 opening data connection")
                            val out = data.getOutputStream()
                            val dir = resolve(cwd, if (arg.isEmpty()) "." else arg) ?: root
                            dir.listFiles()?.sortedByDescending { it.isDirectory }?.forEach { f ->
                                val name = f.name
                                val perms = if (f.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                                val size = if (f.isDirectory) 0L else f.length()
                                val t = java.text.SimpleDateFormat("MMM dd HH:mm", java.util.Locale.US).format(java.util.Date(f.lastModified()))
                                out.write("$perms 1 ftp ftp $size $t $name\r\n".toByteArray(Charsets.UTF_8))
                            }
                            out.flush(); out.close(); data.close()
                            pasv = null
                            send("226 done")
                            log("LIST $cwd")
                        }
                        "RETR" -> {
                            val data = runCatching { pasv?.accept() }.getOrNull()
                            val f = resolve(cwd, arg)
                            if (data == null || f == null || !f.isFile) { send("550 not found"); continue }
                            send("150 sending file")
                            f.inputStream().use { it.copyTo(data.getOutputStream()) }
                            data.close(); pasv = null
                            send("226 done")
                            log("下载 ${f.name}")
                        }
                        "STOR" -> {
                            val data = runCatching { pasv?.accept() }.getOrNull()
                            val f = resolve(cwd, arg, create = true)
                            if (data == null || f == null) { send("550 cannot write"); continue }
                            send("150 receiving")
                            f.outputStream().use { data.getInputStream().copyTo(it) }
                            data.close(); pasv = null
                            send("226 done")
                            log("上传 ${f.name}")
                        }
                        "DELE" -> {
                            val f = resolve(cwd, arg)
                            send(if (f != null && f.isFile && f.delete()) "250 deleted" else "550 failed")
                        }
                        "MKD" -> {
                            val f = resolve(cwd, arg, create = true)
                            send(if (f != null && (f.exists() || f.mkdirs())) "257 created" else "550 failed")
                        }
                        "NOOP" -> send("200 ok")
                        "QUIT" -> { send("221 bye"); break }
                        else -> send("502 not implemented: $cmd")
                    }
                }
                runCatching { pasv?.close() }
                runCatching { socket.close() }
            }.onFailure { log("连接异常：${it.message}") }
        }

        /** 防止 ../ 逃出根目录。 */
        private fun resolve(cwd: String, name: String, create: Boolean = false): File? = runCatching {
            val base = File(root, cwd)
            val target = if (name.startsWith("/")) File(root, name.trimStart('/')) else File(base, name)
            val canonical = target.canonicalFile
            if (!canonical.path.startsWith(root.canonicalFile.path)) return null
            if (create) canonical.parentFile?.mkdirs()
            canonical
        }.getOrNull()
    }
}
