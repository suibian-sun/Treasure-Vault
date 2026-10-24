package com.suibian.treasure_vault.core

import com.suibian.treasure_vault.tools.CalcTools
import com.suibian.treasure_vault.tools.CleanTools
import com.suibian.treasure_vault.tools.CodecTools
import com.suibian.treasure_vault.tools.CryptoTools
import com.suibian.treasure_vault.tools.DataTools
import com.suibian.treasure_vault.tools.DateTools
import com.suibian.treasure_vault.tools.DevTools
import com.suibian.treasure_vault.tools.ImageTools
import com.suibian.treasure_vault.tools.LifeTools
import com.suibian.treasure_vault.tools.NetSystemTools
import com.suibian.treasure_vault.tools.MediaTools
import com.suibian.treasure_vault.tools.NetExtraTools
import com.suibian.treasure_vault.tools.OcrTools
import com.suibian.treasure_vault.tools.StudyTools
import com.suibian.treasure_vault.tools.SensorTools
import com.suibian.treasure_vault.tools.TextExtraTools
import com.suibian.treasure_vault.tools.TextTools

/**
 * 工具注册表。
 *
 * 工具只登记「元数据 + 构建闭包」，点开时才实例化页面；
 * 因此 140+ 个工具的注册成本接近于 0。
 */
object Registry {

    val categories: List<Category> = Cats.all

    val tools: List<ToolSpec> by lazy {
        buildList {
            addAll(TextTools.all)
            addAll(TextExtraTools.all)
            addAll(CodecTools.all)
            addAll(CryptoTools.all)
            addAll(DevTools.all)
            addAll(CalcTools.all)
            addAll(DateTools.all)
            addAll(LifeTools.all)
            addAll(DataTools.all)
            addAll(StudyTools.all)
            addAll(ImageTools.all)
            addAll(OcrTools.all)
            addAll(NetExtraTools.all)
            addAll(SensorTools.all)
            addAll(MediaTools.all)
            addAll(CleanTools.all)
            addAll(NetSystemTools.all)
        }
    }

    private val byId: Map<String, ToolSpec> by lazy { tools.associateBy { it.id } }

    fun byId(id: String): ToolSpec? = byId[id]

    fun of(c: Category): List<ToolSpec> = tools.filter { it.category.id == c.id }

    fun search(q: String): List<ToolSpec> =
        if (q.isBlank()) tools else tools.filter { it.matches(q) }
}
