package com.suibian.treasure_vault.pages

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import com.suibian.treasure_vault.BuildConfig
import com.suibian.treasure_vault.core.AppInfo
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.tv
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新：检查 GitHub Release → 可关闭弹窗 → 下载（显示进度）→ 拉起系统安装器。
 * 只用免 Key 的公开接口；支持加速源，失败自动回退直连。
 */
object UpdateHelper {

    private val ui = Handler(Looper.getMainLooper())

    private class Release(
        val version: String,
        val body: String,
        val assets: Map<String, String>,
    )

    fun proxyLabel(proxy: String): String =
        AppInfo.PROXIES.firstOrNull { it.first == proxy }?.second ?: proxy.ifBlank { "直连" }

    fun check(act: Activity, silent: Boolean) {
        val prefs = Prefs.get(act)
        if (!silent) Ui.toast(act, "正在检查更新…")
        Thread {
            val rel = fetchLatest(prefs.proxy)
            ui.post {
                if (act.isFinishing) return@post
                if (rel == null) {
                    if (!silent) Ui.toast(act, "检查失败，请检查网络或切换加速源")
                    return@post
                }
                if (newer(rel.version, BuildConfig.VERSION_NAME)) {
                    showDialog(act, rel, prefs.proxy)
                } else if (!silent) {
                    Ui.toast(act, "已是最新版本 v${BuildConfig.VERSION_NAME}")
                }
            }
        }.start()
    }

    private fun fetchLatest(proxy: String): Release? {
        val urls = buildList {
            if (proxy.isNotBlank()) add(proxy + AppInfo.RELEASE_API)
            add(AppInfo.RELEASE_API)
        }
        for (u in urls) {
            val text = httpGet(u) ?: continue
            runCatching {
                val o = JSONObject(text)
                val tag = o.optString("tag_name").removePrefix("v")
                if (tag.isBlank()) return@runCatching null
                val assets = mutableMapOf<String, String>()
                val arr = o.optJSONArray("assets")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val a = arr.getJSONObject(i)
                        val name = a.optString("name")
                        val url = a.optString("browser_download_url")
                        if (name.isNotBlank() && url.startsWith("https://")) assets[name] = url
                    }
                }
                Release(tag, o.optString("body"), assets)
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun httpGet(url: String): String? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 12000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "TreasureVault")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        if (conn.responseCode != 200) return null
        conn.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun newer(remote: String, local: String): Boolean {
        val a = remote.split(".", "-").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val b = local.split(".", "-").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun showDialog(act: Activity, rel: Release, proxy: String) {
        val abi = if (android.os.Build.SUPPORTED_ABIS.isNotEmpty()) android.os.Build.SUPPORTED_ABIS[0] else ""
        val apkUrl = rel.assets.entries.firstOrNull { it.key.contains(abi) }?.value
            ?: rel.assets.entries.firstOrNull { it.key.contains("arm64") }?.value
            ?: rel.assets.values.firstOrNull()

        val msg = rel.body.ifBlank { "本次更新包含体验优化与问题修复。" }
            .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
            .replace(Regex("^#{1,6}\\s*"), "")
            .trim()

        AlertDialog.Builder(act)
            .setTitle("发现新版本 v${rel.version}")
            .setMessage("$msg\n\n当前版本 v${BuildConfig.VERSION_NAME}")
            .setNegativeButton("关闭", null)
            .setPositiveButton("更新") { _, _ ->
                if (apkUrl == null) {
                    Ui.toast(act, "该版本未提供安装包")
                } else {
                    download(act, apkUrl, rel.version, proxy)
                }
            }
            .show()
    }

    private fun download(act: Activity, url: String, version: String, proxy: String) {
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(20), act.dp(16), act.dp(20), act.dp(8))
        }
        val tip = act.tv("0%", 12.5f, T.ink2)
        val bar = ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
        }
        box.addView(tip)
        box.addView(bar, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })

        val dialog = AlertDialog.Builder(act)
            .setTitle("正在下载 v$version")
            .setView(box)
            .setNegativeButton("取消", null)
            .setCancelable(true)
            .create()
        dialog.show()

        val target = File(act.getExternalFilesDir(null) ?: act.filesDir, "TreasureVault-$version.apk")
        if (target.exists()) target.delete()

        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        dialog.setOnDismissListener { cancelled.set(true) }

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val full = if (proxy.isNotBlank() && url.startsWith("https://github.com/")) proxy + url else url
                conn = (URL(full).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "TreasureVault")
                }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    target.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var last = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0 || cancelled.get()) break
                            out.write(buf, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            if (now - last > 200) {
                                last = now
                                val pct = if (total > 0) (done * 100 / total).toInt() else 0
                                ui.post { tip.text = "$pct%  ${done / 1024 / 1024}MB / ${if (total > 0) total / 1024 / 1024 else 0}MB"; bar.progress = pct }
                            }
                        }
                    }
                }
                if (cancelled.get()) {
                    target.delete()
                    return@Thread
                }
                ui.post {
                    dialog.dismiss()
                    install(act, target)
                }
            } catch (e: Exception) {
                ui.post {
                    dialog.dismiss()
                    Ui.toast(act, "下载失败：${e.message}")
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun install(act: Activity, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(act, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            val i = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            act.startActivity(i)
        }.onFailure { Ui.toast(act, "无法拉起安装器：${it.message}") }
    }

    fun pickProxy(act: Activity) {
        val prefs = Prefs.get(act)
        val labels = AppInfo.PROXIES.map { it.second }.toTypedArray()
        val checked = AppInfo.PROXIES.indexOfFirst { it.first == prefs.proxy }.coerceAtLeast(0)
        AlertDialog.Builder(act)
            .setTitle("下载加速源")
            .setSingleChoiceItems(labels, checked) { d, which ->
                prefs.proxy = AppInfo.PROXIES[which].first
                d.dismiss()
                Ui.toast(act, "已切换为 ${AppInfo.PROXIES[which].second}")
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
