package com.suibian.treasure_vault.pages

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import com.suibian.treasure_vault.BuildConfig
import com.suibian.treasure_vault.MainActivity
import com.suibian.treasure_vault.core.AppInfo
import com.suibian.treasure_vault.core.Prefs
import com.suibian.treasure_vault.core.Registry
import com.suibian.treasure_vault.core.Router
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.pageBody
import com.suibian.treasure_vault.core.settingsCard
import com.suibian.treasure_vault.core.settingRow
import com.suibian.treasure_vault.core.tv

/** 我的：应用设置、主题入口、更新、关于。 */
class MinePage(private val act: MainActivity) {

    fun view(): View {
        val prefs = Prefs.get(act)
        val col = act.pageBody()

        val head = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(4), act.dp(6), act.dp(4), act.dp(14))
        }
        head.addView(act.tv("我的", 24f, T.ink, true))
        head.addView(act.tv("应用设置与更新", 11.5f, T.ink2).apply { setPadding(0, act.dp(4), 0, 0) })
        col.addView(head)

        // 应用卡
        col.addView(act.card {
            val row = LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(act.tv("📦", 24f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_MEDIUM).toFloat())
                layoutParams = LinearLayout.LayoutParams(act.dp(50), act.dp(50))
            })
            val texts = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(act.dp(13), 0, act.dp(8), 0)
            }
            texts.addView(act.tv("宝藏盒", 16f, T.ink, true))
            texts.addView(
                act.tv("v${BuildConfig.VERSION_NAME} · ${Registry.tools.size} 个工具", 11.5f, T.ink2)
                    .apply { setPadding(0, act.dp(3), 0, 0) },
            )
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
            val check = act.tv("检查更新", 12f, T.primary, true).apply {
                background = T.cardBg(act.dp(T.R_MEDIUM).toFloat(), T.surface)
                setPadding(act.dp(12), act.dp(7), act.dp(12), act.dp(7))
                isClickable = true
                setOnClickListener { UpdateHelper.check(act, silent = false) }
            }
            row.addView(check)
            addView(row)
        })

        // 外观（THEME 入口）
        col.addView(act.settingsCard("外观") {
            settingRow("🎨", "主题设置", "配色 · 明暗 · 导航栏样式", onClick = {
                Router.openActivity(act, ThemeActivity::class.java)
            })
            settingRow("🌙", "深色模式", null, trailing = Switch(act).apply {
                isChecked = prefs.themeMode == "dark"
                setOnCheckedChangeListener { _, v ->
                    prefs.themeMode = if (v) "dark" else "light"
                    act.recreate()
                }
            })
        })

        // 更新
        col.addView(act.settingsCard("更新") {
            settingRow("🔔", "自动检查更新", "启动时静默检查", trailing = Switch(act).apply {
                isChecked = prefs.autoCheck
                setOnCheckedChangeListener { _, v -> prefs.autoCheck = v }
            })
            settingRow("🧾", "更新记录", "查看每个版本的改动", onClick = {
                Router.openActivity(act, UpdatesActivity::class.java)
            })
            settingRow("🚀", "下载加速源", UpdateHelper.proxyLabel(prefs.proxy), onClick = {
                UpdateHelper.pickProxy(act)
            })
        })

        // 性能
        col.addView(act.settingsCard("性能") {
            settingRow("🪶", "低内存模式", "关闭页面动画，进一步降低开销", trailing = Switch(act).apply {
                isChecked = prefs.lowMemory
                setOnCheckedChangeListener { _, v ->
                    prefs.lowMemory = v
                    Ui.toast(act, if (v) "已开启低内存模式" else "已关闭低内存模式")
                }
            })
            settingRow("🧹", "清除最近使用", "已记录 ${prefs.recents().size} 个工具", onClick = {
                prefs.clearRecents()
                Ui.toast(act, "已清除")
                act.show(2, false)
            })
        })

        // 关于（按要求去掉了「界面设计」一栏）
        col.addView(act.settingsCard("关于") {
            settingRow("ℹ️", "版本信息", "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = { showAbout() })
            settingRow("🌐", "更新源", AppInfo.RELEASE_REPO, onClick = {
                Ui.copy(act, AppInfo.RELEASE_REPO)
            })
            settingRow("📄", "开源许可", "查看第三方组件许可", onClick = {
                startBrowser("https://github.com/${AppInfo.RELEASE_REPO}")
            })
        })

        col.addView(
            act.tv("原生 Android · 零第三方 SDK 常驻 · 数据留在本机", 10.5f, T.ink3)
                .apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(6) },
        )

        return ScrollView(act).apply {
            isFillViewport = true
            addView(col)
        }
    }

    private fun showAbout() {
        AlertDialog.Builder(act)
            .setTitle("宝藏盒")
            .setMessage(
                "版本 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                    "工具 ${Registry.tools.size} 个 · 分类 ${Registry.categories.size} 个\n" +
                    "包名 ${BuildConfig.APPLICATION_ID}\n" +
                    "更新源 ${AppInfo.RELEASE_REPO}\n\n" +
                    "全部能力在本机运行，设置与数据只保存在设备上。",
            )
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun startBrowser(url: String) {
        runCatching { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
