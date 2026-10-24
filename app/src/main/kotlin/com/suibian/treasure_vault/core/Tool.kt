package com.suibian.treasure_vault.core

import android.widget.LinearLayout

/** 工具分类。 */
class Category(
    val id: String,
    val name: String,
    val desc: String,
    val emoji: String,
)

/**
 * 单个工具的元数据 + 构建闭包。
 *
 * 只有点开工具时才会调用 [build]，因此注册 140+ 个工具不会带来任何启动开销。
 */
class ToolSpec(
    val id: String,
    val name: String,
    val desc: String,
    val category: Category,
    val emoji: String,
    val keywords: List<String> = emptyList(),
    val scroll: Boolean = true,
    val build: (ToolActivity, LinearLayout) -> Unit,
) {
    fun matches(q: String): Boolean {
        if (q.isBlank()) return true
        val s = q.trim().lowercase()
        return name.lowercase().contains(s) || desc.lowercase().contains(s) ||
            category.name.lowercase().contains(s) || emoji == q.trim() ||
            keywords.any { it.lowercase().contains(s) }
    }
}

object Cats {
    val text = Category("text", "文本处理", "清洗、统计、改写文本", "✍️")
    val codec = Category("codec", "编码转换", "Base64 / URL / JSON", "🔁")
    val crypto = Category("crypto", "加密安全", "哈希、密码、UUID", "🛡️")
    val calc = Category("calc", "计算换算", "算式、单位、金融", "🧮")
    val date = Category("date", "日期时间", "时间戳、倒数、计时", "📅")
    val dev = Category("dev", "开发工具", "正则、颜色、JWT", "⌨️")
    val image = Category("image", "图片工具", "编辑、识别、生成", "🖼️")
    val media = Category("media", "影音工具", "白噪音、转码、弹幕", "🎬")
    val life = Category("life", "生活娱乐", "骰子、抽签、决定", "🎲")
    val study = Category("study", "学习专注", "数独、方格、测试", "📚")
    val data = Category("data", "记录管理", "记账、笔记、纪念日", "📒")
    val sensor = Category("sensor", "传感工具", "水平仪、指南针、噪音", "📡")
    val system = Category("system", "系统工具", "应用、壁纸、清理", "⚙️")
    val net = Category("net", "网络查询", "翻译、汇率、IP", "🌐")

    val all = listOf(
        text, codec, crypto, calc, date, dev, image, media,
        life, study, data, sensor, system, net,
    )
}
