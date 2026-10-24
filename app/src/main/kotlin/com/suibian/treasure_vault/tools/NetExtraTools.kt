package com.suibian.treasure_vault.tools

import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.hairline
import com.suibian.treasure_vault.core.kv
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv

/** 查询类工具：全部离线可用，不依赖第三方接口。 */
object NetExtraTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.net, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("express", "快递查询", "识别快递公司，一键跳转官方查询", "📦", "快递", "单号") { a, b -> express(a, b) },
        spec("phone_home", "手机号归属地", "离线识别运营商与号段", "📱", "归属地", "手机号") { a, b -> phoneHome(a, b) },
        spec("postcode", "邮编查询", "按城市查邮政编码", "✉️", "邮编", "邮政") { a, b -> postcode(a, b) },
        spec("address_parse", "地址信息查询", "从一段地址里拆出省市区与联系方式", "📍", "地址", "解析") { a, b -> addressParse(a, b) },
    )

    // ---------------- 快递 ----------------

    private data class Carrier(val name: String, val site: String, val rule: (String) -> Boolean)

    private val carriers = listOf(
        Carrier("顺丰速运", "https://www.sf-express.com/chn/sc/waybill/waybill-detail", { it.startsWith("SF") || (it.length == 12 && it.all(Char::isDigit) && it.startsWith("4")) }),
        Carrier("京东物流", "https://www.jdl.com/orderSearch", { it.startsWith("JD") || it.startsWith("JDV") }),
        Carrier("圆通速递", "https://www.yto.net.cn/", { it.startsWith("YT") || it.startsWith("D") && it.length == 12 }),
        Carrier("中通快递", "https://www.zto.com/", { it.length == 12 && it.all(Char::isDigit) && it.substring(0, 2) in listOf("75", "76", "78") }),
        Carrier("申通快递", "https://www.sto.cn/", { it.length == 12 && it.all(Char::isDigit) && it.substring(0, 2) in listOf("77", "88", "66") }),
        Carrier("韵达快递", "https://www.yundaex.com/", { it.length == 13 && it.all(Char::isDigit) && it.substring(0, 2) in listOf("43", "45", "46") }),
        Carrier("中国邮政", "https://www.ems.com.cn/", { it.startsWith("E") && it.length >= 9 || it.startsWith("10") }),
        Carrier("极兔速递", "https://www.jtexpress.com.cn/", { it.startsWith("JT") }),
        Carrier("德邦快递", "https://www.deppon.com/", { it.startsWith("DP") || it.startsWith("DB") }),
        Carrier("百世快递", "https://www.800best.com/", { it.startsWith("HT") || it.startsWith("BS") }),
    )

    private fun express(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("单号")
            val e = et("粘贴快递单号，支持批量（一行一个）", 4, singleLine = false)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            btn("识别快递公司") { parse(e.text?.toString().orEmpty(), result) }
            addView(result)
        })
        body.note("识别依据是公开的单号编码规则。本工具不抓取物流轨迹，识别后可直接跳到官方查询页。")
    }

    private fun parse(input: String, result: LinearLayout) {
        val act = result.context as ToolActivity
        result.removeAllViews()
        val numbers = input.split(Regex("[\\s,，\\n]+")).filter { it.length >= 6 }
        if (numbers.isEmpty()) {
            result.addView(act.tv("没有识别到有效的单号。", 13f, T.ink2))
            return
        }
        numbers.forEach { no ->
            val up = no.uppercase()
            val hit = carriers.firstOrNull { runCatching { it.rule(up) }.getOrDefault(false) }
            val box = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, act.dp(8), 0, act.dp(8))
            }
            box.addView(act.tv(up, 13.5f, T.ink, true))
            if (hit == null) {
                box.addView(act.tv("未匹配到快递公司，可能是小众快递或单号有误", 12f, T.ink2).apply {
                    setPadding(0, act.dp(3), 0, 0)
                })
            } else {
                box.addView(act.tv(hit.name, 12.5f, T.primary, true).apply { setPadding(0, act.dp(3), 0, 0) })
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                val open = act.tv("去官网查询", 12.5f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                    setPadding(act.dp(14), act.dp(8), act.dp(14), act.dp(8))
                    isClickable = true
                    setOnClickListener {
                        runCatching {
                            act.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(hit.site)))
                        }.onFailure { Ui.toast(act, "没有可用的浏览器") }
                    }
                }
                val copy = act.tv("复制单号", 12.5f, T.ink, true).apply {
                    gravity = Gravity.CENTER
                    background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                    setPadding(act.dp(14), act.dp(8), act.dp(14), act.dp(8))
                    isClickable = true
                    setOnClickListener { Ui.copy(act, up) }
                }
                row.addView(open, LinearLayout.LayoutParams(-2, -2).also { it.topMargin = act.dp(6); it.rightMargin = act.dp(8) })
                row.addView(copy, LinearLayout.LayoutParams(-2, -2).also { it.topMargin = act.dp(6) })
                box.addView(row)
            }
            result.addView(box)
            result.addView(act.hairline())
        }
    }

    // ---------------- 手机号归属地 ----------------

    private val mobilePrefix = mapOf(
        "移动" to listOf("134", "135", "136", "137", "138", "139", "147", "148", "150", "151", "152", "157", "158", "159", "172", "178", "182", "183", "184", "187", "188", "195", "197", "198"),
        "联通" to listOf("130", "131", "132", "145", "146", "155", "156", "166", "167", "171", "175", "176", "185", "186", "196"),
        "电信" to listOf("133", "149", "153", "173", "174", "177", "180", "181", "189", "190", "191", "193", "199"),
        "广电" to listOf("192"),
        "虚拟运营商" to listOf("170", "162", "165", "167", "171"),
    )

    private fun phoneHome(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("手机号")
            val e = et("输入 11 位手机号", singleLine = true, inputType = android.text.InputType.TYPE_CLASS_PHONE)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            e.doAfterTextChanged { query(it?.toString().orEmpty(), result) }
            addView(result)
        })
        body.note("号段与运营商对照为公开规则，可离线使用；精确到地市的归属地需运营商数据，可用下方入口查询。")
    }

    private fun query(raw: String, result: LinearLayout) {
        val act = result.context as ToolActivity
        result.removeAllViews()
        val phone = raw.filter { it.isDigit() }
        if (phone.isEmpty()) {
            result.addView(act.tv("—", 13f, T.ink3))
            return
        }
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val valid = phone.length == 11 && phone.startsWith("1")
        box.kv("号码", phone)
        box.kv("位数", "${phone.length} 位" + if (valid) "（格式正确）" else "（应为 11 位）")
        if (phone.length >= 3) {
            val prefix = phone.substring(0, 3)
            val operator = mobilePrefix.entries.firstOrNull { prefix in it.value }?.key ?: "未知"
            box.kv("运营商", operator)
            box.kv("号段", "$prefix ****")
        }
        result.addView(box)
        if (valid) {
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
            val copy = act.tv("复制号码", 12.5f, T.ink, true).apply {
                gravity = Gravity.CENTER
                background = T.cardBg(act.dp(T.R_SMALL).toFloat(), T.field)
                setPadding(act.dp(14), act.dp(8), act.dp(14), act.dp(8))
                isClickable = true
                setOnClickListener { Ui.copy(act, phone) }
            }
            val web = act.tv("查详细归属地", 12.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                setPadding(act.dp(14), act.dp(8), act.dp(14), act.dp(8))
                isClickable = true
                setOnClickListener {
                    runCatching {
                        act.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.ip138.com/mobile.asp?mobile=$phone"),
                            ),
                        )
                    }.onFailure { Ui.toast(act, "没有可用的浏览器") }
                }
            }
            row.addView(copy, LinearLayout.LayoutParams(-2, -2).also { it.rightMargin = act.dp(8); it.topMargin = act.dp(8) })
            row.addView(web, LinearLayout.LayoutParams(-2, -2).also { it.topMargin = act.dp(8) })
            result.addView(row)
        }
    }

    // ---------------- 邮编 ----------------

    private val postcodes = """
北京|100000, 上海|200000, 天津|300000, 重庆|400000, 广州|510000, 深圳|518000
杭州|310000, 南京|210000, 武汉|430000, 成都|610000, 西安|710000, 沈阳|110000
哈尔滨|150000, 长春|130000, 石家庄|050000, 太原|030000, 呼和浩特|010000, 济南|250000
郑州|450000, 合肥|230000, 南昌|330000, 福州|350000, 长沙|410000, 南宁|530000
海口|570000, 贵阳|550000, 昆明|650000, 拉萨|850000, 兰州|730000, 西宁|810000
银川|750000, 乌鲁木齐|830000, 苏州|215000, 无锡|214000, 常州|213000, 南通|226000
徐州|221000, 扬州|225000, 温州|325000, 宁波|315000, 绍兴|312000, 嘉兴|314000
金华|321000, 台州|318000, 东莞|523000, 佛山|528000, 珠海|519000, 中山|528400
惠州|516000, 汕头|515000, 湛江|524000, 厦门|361000, 泉州|362000, 漳州|363000
烟台|264000, 青岛|266000, 潍坊|261000, 淄博|255000, 临沂|276000, 洛阳|471000
开封|475000, 新乡|453000, 唐山|063000, 保定|071000, 廊坊|065000, 秦皇岛|066000
大同|037000, 临汾|041000, 包头|014000, 鄂尔多斯|017000, 鞍山|114000, 大连|116000
吉林|132000, 齐齐哈尔|161000, 大庆|163000, 无锡|214000, 镇江|212000, 泰州|225300
盐城|224000, 淮安|223000, 连云港|222000, 宿迁|223800, 湖州|313000, 衢州|324000
丽水|323000, 舟山|316000, 芜湖|241000, 蚌埠|233000, 安庆|246000, 九江|332000
赣州|341000, 宜昌|443000, 襄阳|441000, 荆州|434000, 株洲|412000, 湘潭|411100
衡阳|421000, 岳阳|414000, 常德|415000, 柳州|545000, 桂林|541000, 北海|536000
三亚|572000, 绵阳|621000, 德阳|618000, 宜宾|644000, 遵义|563000, 曲靖|655000
大理|671000, 宝鸡|721000, 咸阳|712000, 天水|741000, 克拉玛依|834000, 喀什|844000
""".trim().split(",").mapNotNull { s ->
        val p = s.trim().split("|")
        if (p.size == 2) p[0] to p[1] else null
    }

    private fun postcode(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("按城市查询")
            val e = et("输入城市名，如：杭州", singleLine = true)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            btn("查询") {
                val q = e.text?.toString()?.trim().orEmpty()
                result.removeAllViews()
                if (q.isEmpty()) { Ui.toast(act, "先输入城市"); return@btn }
                val hits = postcodes.filter { it.first.contains(q) || q.contains(it.first) }
                if (hits.isEmpty()) {
                    result.addView(act.tv("内置表未收录「$q」。可尝试省会或地级市名。", 13f, T.ink2))
                } else {
                    hits.forEach { (city, code) ->
                        val row = LinearLayout(act).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(0, act.dp(10), 0, act.dp(10))
                        }
                        row.addView(act.tv(city, 14f, T.ink), LinearLayout.LayoutParams(0, -2, 1f))
                        row.addView(act.tv(code, 15f, T.primary, true))
                        row.isClickable = true
                        row.setOnClickListener { Ui.copy(act, code) }
                        result.addView(row)
                        result.addView(act.hairline())
                    }
                    result.addView(act.tv("点击任意一行可复制邮编", 11f, T.ink3).apply { setPadding(0, act.dp(8), 0, 0) })
                }
            }
            addView(result)
        })
        body.note("内置 ${postcodes.size} 个主要城市的邮政编码；具体街道的六位邮编请以邮政官网为准。")
    }

    // ---------------- 地址解析 ----------------

    private val provinces = listOf(
        "北京市", "天津市", "上海市", "重庆市", "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省",
        "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省", "河南省", "湖北省", "湖南省",
        "广东省", "海南省", "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省",
        "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
        "香港特别行政区", "澳门特别行政区",
    )

    private fun addressParse(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("地址")
            val e = et("粘贴一段地址，如：浙江省杭州市西湖区文三路 90 号 13800138000", 4)
            val result = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            btn("解析") {
                val s = e.text?.toString()?.trim().orEmpty()
                result.removeAllViews()
                if (s.isEmpty()) { Ui.toast(act, "先输入地址"); return@btn }
                val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
                val prov = provinces.firstOrNull { s.contains(it) }
                    ?: provinces.firstOrNull { s.contains(it.removeSuffix("省").removeSuffix("市").removeSuffix("自治区")) }
                box.kv("省份", prov ?: "未识别")
                val city = Regex("([\\u4e00-\\u9fa5]{2,8}?(市|自治州|地区|盟))").find(s)?.value
                box.kv("城市", city ?: "未识别")
                val district = Regex("([\\u4e00-\\u9fa5]{2,8}?(区|县|旗|市))").findAll(s)
                    .map { it.value }.firstOrNull { it != city && (prov == null || !it.contains(prov.removeSuffix("省"))) }
                box.kv("区县", district ?: "未识别")
                val phone = Regex("1[3-9]\\d{9}").find(s)?.value
                box.kv("手机号", phone ?: "未提供")
                val tel = Regex("0\\d{2,3}-?\\d{7,8}").find(s)?.value
                box.kv("固话", tel ?: "未提供")
                val zip = Regex("(?<!\\d)\\d{6}(?!\\d)").find(s)?.value
                box.kv("邮编", zip ?: "未提供")
                val detail = s
                    .replace(prov ?: "\u0000", "")
                    .replace(city ?: "\u0000", "")
                    .replace(district ?: "\u0000", "")
                    .replace(phone ?: "\u0000", "")
                    .replace(tel ?: "\u0000", "")
                    .replace(zip ?: "\u0000", "")
                    .trim(' ', ',', '，')
                box.kv("详细地址", detail.ifBlank { "—" })
                result.addView(box)
                result.addView(act.tv("解析基于省市词表与正则，遇到生僻地名可能需要手工校正。", 11f, T.ink3).apply {
                    setPadding(0, act.dp(6), 0, 0)
                })
            }
            addView(result)
        })
    }
}
