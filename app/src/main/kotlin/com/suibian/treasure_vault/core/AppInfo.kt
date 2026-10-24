package com.suibian.treasure_vault.core

/** 应用级常量：更新源、加速源、内置更新日志。 */
object AppInfo {

    const val RELEASE_REPO = "suibian-sun/Treasure-Vault"
    const val RELEASE_API = "https://api.github.com/repos/$RELEASE_REPO/releases/latest"
    const val RELEASES_API = "https://api.github.com/repos/$RELEASE_REPO/releases?per_page=20"

    val PROXIES = listOf(
        "https://gh-proxy.com/" to "gh-proxy.com（默认）",
        "https://ghfast.top/" to "ghfast.top",
        "https://gh-proxy.org/" to "gh-proxy.org",
        "https://hk.gh-proxy.com/" to "hk.gh-proxy.com",
        "" to "不使用（直连 GitHub）",
    )

    class Note(val version: String, val date: String, val items: List<String>)

    /** 离线兜底日志（在线时以 Release 说明为准）。 */
    val NOTES = listOf(
        Note(
            "2.0.0", "2026-10-24",
            listOf(
                "【重构】改用 Kotlin 原生 View 重写，去掉 Flutter 引擎，安装包与常驻内存大幅下降。",
                "【新增】工具扩充到 140+：学习专注、记录管理、传感工具、系统工具、网络查询全面补齐。",
                "【动画】掷骰子 / 抛硬币 / 随机抽签加入真实动效，页面与工具切换增加过渡动画。",
                "【界面】新增「主题设置」页：6 套配色、明暗模式、底部导航三种显示样式。",
                "【优化】启动只构建首页视图；工具页按需创建，返回即释放。",
            ),
        ),
        Note(
            "1.0.1", "2026-10-24",
            listOf("修复启动闪退：入口 Activity 未打入安装包。"),
        ),
    )
}
