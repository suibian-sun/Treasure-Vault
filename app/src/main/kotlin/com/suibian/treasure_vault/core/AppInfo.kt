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
            "2.1.0", "2026-10-24",
            listOf(
                "【新增 54 个工具】工具总数达到 138 个，覆盖图片、识别、传感、影音、清理等此前缺失的类别。",
                "【图片】水印、九宫格、拼接、放大、锐化、拉伸修复、黑白上色、取色器、配色助手、证件照、表情包、带壳截图、文字转图、渐变图、拼豆图纸、隐藏图、GIF 合成与分解、画板。",
                "【识别】OCR 文字识别、表格转 CSV、二维码生成与识别、以图搜图入口、拍照打点计数。",
                "【文本】文本编辑器、拆分选词、中文转拼音、汉语词典、成语查询、近反义词、藏头诗、对联、TextRank 摘要。",
                "【传感】屏幕直尺（可校准）、气泡水平仪、指南针、噪音计、金属探测仪。",
                "【影音】白噪音（白/粉/棕/雨声）、文字转语音、视频提取音轨、氢壁纸、系统壁纸导出。",
                "【系统】大文件 / 空文件 / 空文件夹 / 安装包清理，以及手机端 FTP 局域网传输。",
                "【查询】快递单号识别、手机号运营商、城市邮编、地址结构化解析（全部离线可用）。",
                "【优化】所有图片处理在后台线程执行，大图自动采样，避免卡顿与内存溢出。",
            ),
        ),
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
