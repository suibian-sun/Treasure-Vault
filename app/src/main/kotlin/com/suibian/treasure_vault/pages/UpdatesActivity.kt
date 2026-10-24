package com.suibian.treasure_vault.pages

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.suibian.treasure_vault.core.AppInfo
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.pageBody
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 更新记录：在线读取 GitHub Release，失败回退内置日志。 */
class UpdatesActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        T.sync(this)
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(T.canvas)
        }
        root.addView(header())
        container = pageBody()
        container.addView(tv("正在获取更新记录…", 12.5f, T.ink2))
        root.addView(
            ScrollView(this).apply { isFillViewport = true; addView(container) },
            LinearLayout.LayoutParams(-1, 0, 1f),
        )
        setContentView(root)
        load()
    }

    private fun header(): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(T.surface)
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        bar.addView(tv("‹", 30f, T.ink).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), 0, dp(6), 0)
            isClickable = true
            setOnClickListener { finish() }
        })
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv("更新记录", 16f, T.ink, true))
        col.addView(tv("当前 v${com.suibian.treasure_vault.BuildConfig.VERSION_NAME}", 11.5f, T.ink2)
            .apply { setPadding(0, dp(2), 0, 0) })
        bar.addView(col, LinearLayout.LayoutParams(0, -2, 1f).also { it.leftMargin = dp(4) })
        wrap.addView(bar)
        wrap.addView(hairline())
        return wrap
    }

    private fun load() {
        Thread {
            val list = fetch()
            runOnUiThread {
                container.removeAllViews()
                if (list.isEmpty()) {
                    container.addView(tv("暂时无法获取在线记录，以下为内置日志：", 11.5f, T.ink2)
                        .apply { setPadding(0, 0, 0, dp(8)) })
                    AppInfo.NOTES.forEach { addNote(it) }
                } else {
                    list.forEach { addNote(it) }
                }
                container.addView(tv("更新源：${AppInfo.RELEASE_REPO}", 10.5f, T.ink3)
                    .apply { gravity = Gravity.CENTER })
            }
        }.start()
    }

    private fun fetch(): List<AppInfo.Note> {
        val text = runCatching {
            val conn = URL(AppInfo.RELEASES_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "TreasureVault")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return emptyList()
            conn.inputStream.bufferedReader().readText()
        }.getOrNull() ?: return emptyList()

        return runCatching {
            val arr = JSONArray(text)
            val out = mutableListOf<AppInfo.Note>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tag = o.optString("tag_name").removePrefix("v")
                if (tag.isBlank()) continue
                val date = o.optString("published_at").take(10)
                val items = o.optString("body").split('\n')
                    .map { it.trim().removePrefix("-").removePrefix("*").replace("**", "").trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("![") }
                    .take(12)
                out.add(AppInfo.Note(tag, date, items))
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun addNote(note: AppInfo.Note) {
        container.addView(card {
            sectionTitle("v${note.version}", tv(note.date, 11f, T.ink2))
            val items = note.items.ifEmpty { listOf("本次更新包含体验优化与问题修复。") }
            items.forEach { line ->
                val row = LinearLayout(this@UpdatesActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(3), 0, dp(3))
                }
                row.addView(tv("·", 14f, T.primary))
                row.addView(tv(line, 12.5f, T.ink).apply { setPadding(dp(8), 0, 0, 0) },
                    LinearLayout.LayoutParams(0, -2, 1f))
                addView(row)
            }
        })
    }
}
