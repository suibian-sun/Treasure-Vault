package com.suibian.treasure_vault.tools

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 内置词库 + 在线兜底（免 Key）。 */
object DictData {

    // 成语：词条|释义
    private val idiomRows = """
一鸣惊人|比喻平时没有突出表现，一下子做出惊人的成绩。
一心一意|形容做事专心，一门心思只做一件事。
一帆风顺|比喻非常顺利，毫无挫折。
一箭双雕|比喻做一件事达到两个目的。
一见如故|初次见面就像老朋友一样合得来。
一丝不苟|形容做事认真细致，一点儿不马虎。
一望无际|一眼望不到边，形容非常辽阔。
一落千丈|形容地位、名声、经济状况等急剧下降。
一鼓作气|趁劲头大的时候一口气把事情做完。
一败涂地|形容失败到不可收拾的地步。
画蛇添足|比喻做了多余的事，反而把事情弄坏。
守株待兔|比喻死守经验，不知变通，妄想不劳而获。
亡羊补牢|比喻出了问题以后想办法补救，防止继续受损失。
井底之蛙|比喻见识狭窄的人。
对牛弹琴|比喻对不懂道理的人讲道理，白费口舌。
自相矛盾|比喻自己说话做事前后抵触。
刻舟求剑|比喻拘泥成法，不知随着情势的变化而变通。
掩耳盗铃|比喻自己欺骗自己，明明掩盖不住的事情偏要设法掩盖。
滥竽充数|比喻没有真才实学的人混在行家里面充数。
拔苗助长|比喻违反事物发展规律，急于求成，反而坏事。
杯弓蛇影|比喻疑神疑鬼，自相惊扰。
草木皆兵|形容人在惊慌时疑神疑鬼。
胸有成竹|比喻做事之前已有通盘考虑和把握。
水滴石穿|比喻只要有恒心，不断努力，事情就一定能成功。
锲而不舍|比喻有恒心、有毅力，坚持不懈。
坚持不懈|坚持到底，一点也不松懈。
持之以恒|长久地坚持下去。
孜孜不倦|勤奋努力，不知疲倦。
废寝忘食|顾不得睡觉，忘记吃饭，形容非常专心努力。
全神贯注|全部精神集中在一点上，形容注意力高度集中。
专心致志|一心一意，集中精神。
目不转睛|眼珠子一动不动地盯着看，形容注意力集中。
聚精会神|集中精神，集中注意力。
恍然大悟|一下子完全明白过来。
豁然开朗|形容由狭窄昏暗变得宽敞明亮，也比喻突然领悟。
茅塞顿开|形容思想忽然开窍，立刻明白了某个道理。
融会贯通|把各方面的知识和道理融合贯穿，得到全面透彻的理解。
举一反三|从一件事情类推而知道其他许多事情。
触类旁通|掌握了某一事物的规律，就能推知同类事物。
温故知新|温习学过的知识，得到新的理解和体会。
不耻下问|不以向地位比自己低、知识比自己少的人请教为可耻。
学而不厌|学习总感到不满足，形容好学。
博览群书|广泛地阅读各种书籍。
学富五车|形容读书多，学问大。
才高八斗|形容人文才极高。
出口成章|说出话来就成文章，形容文思敏捷、口才好。
妙笔生花|比喻写作能力大有进步，或文章写得很好。
栩栩如生|形容艺术形象非常生动逼真，像活的一样。
惟妙惟肖|形容描写或模仿得非常逼真。
绘声绘色|形容叙述、描写生动逼真。
淋漓尽致|形容文章或说话表达得非常充分、透彻。
入木三分|形容书法笔力强劲，也比喻分析问题很深刻。
力透纸背|形容书法遒劲有力，也形容诗文立意深刻。
别具一格|另有一种独特的风格。
独树一帜|比喻独特新奇，自成一家。
别出心裁|另有一种构思或设计，指想出的办法与众不同。
匠心独运|独创性地运用巧妙的心思。
巧夺天工|形容技艺十分巧妙。
鬼斧神工|形容建筑、雕塑等技艺精巧，非人力所能达到。
炉火纯青|比喻功夫达到了纯熟完美的境界。
登峰造极|比喻学问、技艺等已达到最高的境界。
精益求精|已经很好了，还要求更好。
尽善尽美|形容事物完美到没有一点缺点。
十全十美|各方面都非常完美，毫无缺陷。
完美无缺|完善美好，没有缺点。
无与伦比|没有能比得上的。
举世闻名|全世界都知道，形容非常著名。
名扬四海|名声传遍天下。
家喻户晓|每家每户都知道。
妇孺皆知|妇女和小孩都知道，指某件事物众所周知。
人山人海|形容人聚集得非常多。
川流不息|形容行人、车马等像水流一样连续不断。
络绎不绝|形容行人车马来来往往，接连不断。
熙熙攘攘|形容人来人往，非常热闹。
门庭若市|门前和院子里人很多，像市场一样，形容来的人很多。
车水马龙|形容车马往来不绝的热闹情景。
热火朝天|形容群众性的活动情绪热烈，气氛高涨。
如火如荼|形容气势旺盛、气氛热烈。
生机勃勃|形容自然界充满生命力，或社会生活活跃。
欣欣向荣|形容草木长得茂盛，比喻事业蓬勃发展。
蒸蒸日上|形容事业一天天向上发展。
日新月异|每天每月都有新的变化，形容进步、发展很快。
与日俱增|随着时间的推移而不断增长。
突飞猛进|形容事业、学问等进展非常迅速。
一日千里|形容进展极快。
翻天覆地|形容变化巨大而彻底。
沧海桑田|比喻世事变化很大。
物是人非|东西还是原来的，人已不是原来的人，形容世事变化。
恍如隔世|仿佛隔了一世，多表示由于人事变化很大而感慨。
光阴似箭|形容时间过得极快。
日月如梭|形容时间过得很快。
白驹过隙|比喻时间过得很快，就像小白马在缝隙前一闪而过。
转瞬即逝|形容转眼之间就消失了。
稍纵即逝|形容时间或机会很容易过去。
千钧一发|比喻情况万分危急。
迫在眉睫|形容事情已到眼前，情势十分紧迫。
刻不容缓|形势紧迫，一刻也不允许拖延。
燃眉之急|像火烧眉毛那样紧急，比喻非常紧迫的情况。
化险为夷|使危险的情况变成平安。
转危为安|由危险转为平安。
有惊无险|虽然受到惊吓，但最终平安无事。
安然无恙|平安无事，没有受到损害。
万事如意|一切事情都符合心意。
心想事成|心里想的事情都能做成。
如愿以偿|像所希望的那样得到满足。
称心如意|完全符合心意。
心花怒放|形容内心高兴极了。
喜出望外|遇到出乎意料的喜事而特别高兴。
欣喜若狂|形容高兴到了极点。
兴高采烈|形容兴致高、情绪热烈。
眉开眼笑|形容高兴愉快的样子。
笑逐颜开|形容笑容满面，十分高兴。
愁眉苦脸|形容愁苦的神色。
忧心忡忡|形容心事重重，非常忧愁。
郁郁寡欢|形容心里苦闷，少有欢乐。
闷闷不乐|形容心事放不下，心里不快活。
怒发冲冠|形容极端愤怒。
火冒三丈|形容愤怒到极点。
勃然大怒|突然变脸大发脾气。
目瞪口呆|形容因吃惊或害怕而发愣的样子。
大惊失色|非常害怕，脸色都变了。
惊慌失措|由于惊慌，一下子不知怎么办才好。
泰然自若|形容在紧急情况下沉着镇定，不慌不乱。
从容不迫|非常镇静，不慌不忙。
若无其事|像没有那么回事似的，形容不动声色或漠不关心。
心平气和|心情平静，态度温和。
平心静气|心情平和，态度冷静。
""".trim()

    // 近义词|反义词
    private val synonymRows = """
高兴|快乐、愉快、欢喜|悲伤、难过、忧愁
美丽|漂亮、秀丽、俊俏|丑陋、难看
安静|宁静、寂静、清静|喧闹、嘈杂、吵闹
勤奋|刻苦、努力、勤勉|懒惰、懈怠
危险|凶险、险恶|安全、平安
聪明|机灵、聪慧、伶俐|愚笨、愚蠢
勇敢|英勇、大胆、无畏|怯懦、胆小
宽阔|宽广、辽阔|狭窄、狭小
明亮|光亮、透亮|昏暗、阴暗
寒冷|冰冷、严寒|炎热、酷热
干净|洁净、清洁|肮脏、污秽
诚实|老实、诚恳|虚伪、狡猾
团结|齐心、和睦|分裂、内讧
迅速|快速、快捷|缓慢、迟缓
珍贵|宝贵、名贵|廉价、低贱
坚强|刚强、顽强|软弱、脆弱
富裕|富足、宽裕|贫穷、贫困
整齐|齐整、井井有条|杂乱、凌乱
温和|温柔、和善|严厉、粗暴
热闹|繁华、喧闹|冷清、萧条
简单|容易、简易|复杂、繁琐
节约|节省、俭省|浪费、奢侈
喜欢|喜爱、爱好|讨厌、厌恶
开始|起初、开端|结束、终结
前进|迈进、前行|后退、退缩
保护|爱护、守护|破坏、损害
鼓励|激励、鼓舞|打击、压制
模糊|含糊、朦胧|清晰、清楚
新鲜|鲜嫩、新颖|陈旧、腐烂
紧张|紧迫、紧绷|松弛、轻松
熟悉|熟识、了解|陌生、生疏
幸福|美满、愉快|痛苦、悲惨
骄傲|自豪、自满|谦虚、虚心
惊慌|慌张、慌乱|镇定、从容
明亮|光明、辉煌|黑暗、昏暗
沉重|繁重、沉甸甸|轻松、轻快
粗糙|毛糙、粗劣|光滑、精细
果断|坚决、干脆|犹豫、迟疑
慷慨|大方、豪爽|吝啬、小气
虚伪|虚假、伪善|真诚、坦率
细腻|细致、精细|粗疏、粗糙
敏捷|敏锐、迅速|迟钝、笨拙
谦虚|谦逊、虚心|骄傲、自大
光荣|荣耀、光彩|耻辱、羞耻
支持|拥护、赞同|反对、阻挠
坚固|牢固、坚实|脆弱、松软
深刻|深入、透彻|肤浅、浅薄
遵守|服从、遵循|违反、违背
珍惜|爱惜、珍视|浪费、糟蹋
崎岖|坎坷、险峻|平坦、平缓
辽阔|广阔、宽广|狭窄、狭小
茂盛|繁茂、旺盛|枯萎、凋零
清晰|清楚、明白|模糊、含混
认真|仔细、负责|马虎、草率
和蔼|温和、慈祥|凶狠、严厉
富裕|宽裕、富庶|贫困、拮据
沮丧|失落、消沉|振奋、昂扬
""".trim()

    // 常用词释义
    private val wordRows = """
宝藏|储藏的珍宝或财富，多指矿产；比喻珍奇的东西。
理想|对未来事物的想象或希望，有根据、合理的。
责任|分内应做的事；没有做好分内的事而应承担的过失。
自由|在法律规定的范围内，随自己意志活动的权利。
幸福|使人心情舒畅的境遇和生活。
时间|物质运动、变化的持续性、顺序性的表现。
习惯|在长时期里逐渐养成的、一时不容易改变的行为。
知识|人们在实践中获得的认识和经验。
经验|由实践得来的知识或技能。
能力|能胜任某项工作或任务的主观条件。
方法|关于解决思想、说话、行动等问题的门路、程序。
态度|对于事情的看法和采取的行动。
目标|想要达到的境地或标准。
计划|工作或行动以前预先拟定的具体内容和步骤。
效率|单位时间内完成的工作量。
质量|产品或工作的优劣程度。
价值|体现在商品里的社会必要劳动；事物的用途或积极作用。
成长|向成熟的阶段发展；生长。
坚持|坚决保持、维护或进行。
选择|挑选；做出的决定。
沟通|使两方能通连；交换意见。
信任|相信而敢于托付。
尊重|尊敬、重视。
理解|懂、了解。
包容|宽容、容纳。
表达|表示思想、感情。
倾听|细心地听取。
合作|互相配合做某事或共同完成某项任务。
竞争|为了自己方面的利益而跟人争胜。
创新|抛开旧的，创造新的。
进取|努力向前，立志有所作为。
平衡|对立的各方面在数量或质量上相等或相抵。
节奏|音乐中交替出现的有规律的强弱、长短的现象；比喻均匀的、有规律的工作进程。
专注|专心注意。
耐心|心里不急躁，不厌烦。
情绪|人从事某种活动时产生的兴奋心理状态。
压力|物体所承受的与表面垂直的作用力；使人感到紧张的社会环境因素。
睡眠|一种周期性的生理状态，意识暂时丧失。
运动|物体的位置不断变化的现象；体育活动。
营养|生物体从外界吸取养分以维持生命活动的过程。
健康|人体各器官系统发育良好、功能正常的状态。
环境|周围的地方；周围的情况和条件。
自然|自然界；自由发展，不经人力干预。
城市|人口集中、工商业发达、居民以非农业人口为主的地区。
乡村|主要从事农业、人口分布较城镇分散的地方。
文化|人类在社会历史发展过程中所创造的物质财富和精神财富的总和。
艺术|用形象来反映现实但比现实有典型性的社会意识形态。
科学|反映自然、社会、思维等的客观规律的分科的知识体系。
技术|人类在利用自然和改造自然的过程中积累起来并在生产劳动中体现出来的经验和知识。
数据|进行各种统计、计算、科学研究或技术设计等所依据的数值。
算法|解题方案的准确而完整的描述，是一系列解决问题的清晰指令。
程序|事情进行的先后次序；用计算机语言编写的指令序列。
""".trim()

    private val idioms: List<Pair<String, String>> by lazy {
        idiomRows.split("\n").mapNotNull { line ->
            val i = line.indexOf('|')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }
    }

    private val synonyms: List<Triple<String, String, String>> by lazy {
        synonymRows.split("\n").mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 3) null else Triple(p[0], p[1], p[2])
        }
    }

    private val words: Map<String, String> by lazy {
        wordRows.split("\n").mapNotNull { line ->
            val i = line.indexOf('|')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
    }

    val idiomCount: Int get() = idioms.size
    val synonymCount: Int get() = synonyms.size

    fun lookup(word: String): String? {
        words[word]?.let { return it }
        idioms.firstOrNull { it.first == word }?.let { return it.second }
        // 单字：从词条释义里找包含该字的，给一个近似提示
        return null
    }

    fun searchIdiom(q: String): List<Pair<String, String>> {
        if (q.isBlank()) return idioms.shuffled().take(20)
        return idioms.filter { it.first.contains(q) || it.second.contains(q) }
            .sortedBy { if (it.first.startsWith(q)) 0 else 1 }
    }

    fun synonym(q: String): Pair<String, String>? {
        val hit = synonyms.firstOrNull { it.first == q }
            ?: synonyms.firstOrNull { it.first.contains(q) || q.contains(it.first) }
            ?: return null
        return hit.second to hit.third
    }

    /** 有道公开词条接口（免 Key）。 */
    fun online(q: String): String? = runCatching {
        val url = "https://dict.youdao.com/jsonapi?q=" + URLEncoder.encode(q, "UTF-8")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) TreasureVault")
        if (c.responseCode != 200) return null
        val body = c.inputStream.bufferedReader().readText()
        val json = JSONObject(body)
        val sb = StringBuilder()
        // 英汉释义
        json.optJSONObject("ec")?.optJSONArray("trs")?.let { trs ->
            for (i in 0 until trs.length()) {
                val tr = trs.optJSONObject(i)?.optJSONArray("tr") ?: continue
                for (j in 0 until tr.length()) {
                    val lines = tr.optJSONObject(j)?.optJSONObject("l")?.optJSONArray("i") ?: continue
                    for (k in 0 until lines.length()) sb.append(lines.optString(k)).append("\n")
                }
            }
        }
        // 网络释义
        if (sb.isBlank()) {
            val wt = json.optJSONObject("web_trans")?.optJSONArray("web-translation")
            val first = wt?.optJSONObject(0)
            val trans = first?.optJSONArray("trans")
            val value = trans?.optJSONObject(0)?.optString("value").orEmpty()
            if (value.isNotBlank()) sb.append("英文对应：$value\n\n")
            val line = trans?.optJSONObject(0)?.optJSONObject("summary")
                ?.optJSONArray("line")?.optString(0).orEmpty()
            if (line.isNotBlank()) {
                sb.append(line.replace(Regex("<[^>]+>"), "").take(300))
            }
        }
        sb.toString().trim().ifBlank { null }
    }.getOrNull()
}

/** 藏头诗 / 对联生成（本地词库拼句）。 */
object Poem {

    private val images4 = listOf(
        "风起云涌", "月落乌啼", "山高水长", "云淡风轻", "花开花落", "雨过天晴",
        "春江水暖", "秋水长天", "松风竹影", "柳暗花明", "烟波浩渺", "星河灿烂",
        "孤帆远影", "流水潺潺", "落霞孤鹜", "长亭古道", "小桥人家", "竹露荷风",
        "雪落无声", "夜静山空", "清风明月", "白云苍狗", "远山如黛", "古道西风",
        "疏影横斜", "暗香浮动", "江天一色", "渔舟唱晚", "晨钟暮鼓", "苔痕上阶",
        "半窗疏影", "一径落花", "杏花春雨", "杨柳依依", "蒹葭苍苍", "白露为霜",
        "千山暮雪", "万里层云", "海阔天空", "石上清泉", "月照松间", "鸟鸣山幽",
    )

    private val tail3 = listOf(
        "入梦来", "满庭芳", "共长天", "落谁家", "寄流年", "度春秋",
        "照归人", "映画屏", "染青衫", "听松风", "问流年", "随流水",
        "过小桥", "伴月明", "上层楼", "归远山", "满衣襟", "落明月",
    )

    private val head3 = listOf(
        "清风", "明月", "远山", "近水", "孤舟", "归雁", "落花", "疏雨",
        "寒江", "暖阳", "野渡", "长亭", "古城", "深巷", "旧梦", "春衫",
    )

    private val couplets = listOf(
        "海内存知己" to "天涯若比邻",
        "春风送暖入屠苏" to "爆竹声中一岁除",
        "天增岁月人增寿" to "春满乾坤福满门",
        "一年四季行好运" to "八方财宝进家门",
        "和顺一门有百福" to "平安二字值千金",
        "喜居宝地千年旺" to "福照家门万事兴",
        "五湖四海皆春色" to "万水千山尽得辉",
        "门迎百福福星照" to "户纳千祥祥云腾",
        "江山如画" to "岁月如歌",
        "松竹梅岁寒三友" to "桃李杏春风一家",
        "书山有路勤为径" to "学海无涯苦作舟",
        "宝剑锋从磨砺出" to "梅花香自苦寒来",
        "有志者事竟成" to "苦心人天不负",
        "海阔凭鱼跃" to "天高任鸟飞",
        "长风破浪会有时" to "直挂云帆济沧海",
        "会当凌绝顶" to "一览众山小",
        "欲穷千里目" to "更上一层楼",
        "野火烧不尽" to "春风吹又生",
        "春蚕到死丝方尽" to "蜡炬成灰泪始干",
        "落红不是无情物" to "化作春泥更护花",
        "山重水复疑无路" to "柳暗花明又一村",
        "沉舟侧畔千帆过" to "病树前头万木春",
        "千磨万击还坚劲" to "任尔东西南北风",
        "粉骨碎身浑不怕" to "要留清白在人间",
        "不要人夸颜色好" to "只留清气满乾坤",
        "疏影横斜水清浅" to "暗香浮动月黄昏",
        "接天莲叶无穷碧" to "映日荷花别样红",
        "小荷才露尖尖角" to "早有蜻蜓立上头",
        "停车坐爱枫林晚" to "霜叶红于二月花",
        "忽如一夜春风来" to "千树万树梨花开",
        "孤帆远影碧空尽" to "唯见长江天际流",
        "桃花潭水深千尺" to "不及汪伦送我情",
        "莫愁前路无知己" to "天下谁人不识君",
        "劝君更尽一杯酒" to "西出阳关无故人",
        "明月松间照" to "清泉石上流",
        "大漠孤烟直" to "长河落日圆",
        "两个黄鹂鸣翠柳" to "一行白鹭上青天",
        "春潮带雨晚来急" to "野渡无人舟自横",
        "随风潜入夜" to "润物细无声",
        "日出江花红胜火" to "春来江水绿如蓝",
    )

    fun acrostic(text: String, lineLen: Int): String {
        val sb = StringBuilder()
        text.forEachIndexed { idx, ch ->
            val body = if (lineLen == 5) {
                images4.random()
            } else {
                head3.random() + tail3.random()
            }
            sb.append(ch).append(body.take(if (lineLen == 5) 4 else 6)).append("\n")
        }
        return sb.toString().trim()
    }

    fun randomCouplet(): String {
        val (up, down) = couplets.random()
        return "上联：$up\n下联：$down"
    }

    fun replyCouplet(up: String): String {
        // 优先在名联库里找
        couplets.firstOrNull { it.first == up }?.let { return "上联：$up\n下联：${it.second}" }
        val n = up.length
        val pool = when {
            n <= 4 -> head3
            n <= 6 -> head3.map { it + tail3.random().take(2) }
            else -> head3.map { it + tail3.random() }
        }
        val down = pool.random().take(n)
        val padded = down.padEnd(n, '春').take(n)
        return "上联：$up\n下联：$padded\n\n（自拟下联按字数拼句，仅供娱乐；可在上方输入名联查原配）"
    }
}

/** 抽取式摘要：TextRank 简化实现。 */
object TextRank {

    private val splitRegex = Regex("[。！？!?；;\\n]+")

    fun summarize(text: String, topN: Int): String {
        val sentences = text.split(splitRegex)
            .map { it.trim() }
            .filter { it.length >= 8 }
        if (sentences.size <= topN) return sentences.joinToString("。") + "。"
        val tokens = sentences.map { tokenize(it) }
        val n = sentences.size
        val sim = Array(n) { DoubleArray(n) }
        for (i in 0 until n) for (j in i + 1 until n) {
            val a = tokens[i]
            val b = tokens[j]
            if (a.isEmpty() || b.isEmpty()) continue
            val common = a.keys.intersect(b.keys).size
            val denom = kotlin.math.ln((a.size + 1).toDouble()) + kotlin.math.ln((b.size + 1).toDouble())
            val s = if (denom <= 0.0) 0.0 else common / denom
            sim[i][j] = s
            sim[j][i] = s
        }
        var score = DoubleArray(n) { 1.0 }
        repeat(20) {
            val next = DoubleArray(n)
            for (i in 0 until n) {
                var sum = 0.0
                for (j in 0 until n) {
                    if (i == j) continue
                    val outSum = (0 until n).sumOf { if (it == j) 0.0 else sim[j][it] }
                    if (outSum > 0) sum += sim[j][i] / outSum * score[j]
                }
                next[i] = 0.15 + 0.85 * sum
            }
            score = next
        }
        val picked = score.indices.sortedByDescending { score[it] }.take(topN).sorted()
        val sb = StringBuilder()
        for (k in picked.indices) {
            if (k > 0) sb.append("\n")
            sb.append("• ").append(sentences[picked[k]])
        }
        return sb.toString()
    }

    private fun tokenize(s: String): Map<String, Int> {
        val map = HashMap<String, Int>()
        val clean = s.replace(Regex("[\\p{P}\\s]+"), "")
        for (i in 0 until clean.length - 1) {
            val g = clean.substring(i, i + 2)
            map[g] = (map[g] ?: 0) + 1
        }
        return map
    }
}
