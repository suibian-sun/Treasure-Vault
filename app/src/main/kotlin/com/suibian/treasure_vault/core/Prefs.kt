package com.suibian.treasure_vault.core

import android.content.Context
import org.json.JSONArray

/**
 * 极简持久化：一个 SharedPreferences 承载设置、收藏、最近使用与各工具的小数据。
 * 不引入 Room/DataStore —— 本应用的数据量都在 KB 级，直接读写 JSON 更轻。
 */
class Prefs private constructor(ctx: Context) {

    private val sp = ctx.applicationContext.getSharedPreferences("tv", Context.MODE_PRIVATE)

    var themePreset: String
        get() = sp.getString("theme.preset", "minimal") ?: "minimal"
        set(v) = sp.edit().putString("theme.preset", v).apply()

    /** light / dark / system */
    var themeMode: String
        get() = sp.getString("theme.mode", "system") ?: "system"
        set(v) = sp.edit().putString("theme.mode", v).apply()

    /** icon_text / icon / text */
    var navMode: String
        get() = sp.getString("nav.mode", "icon_text") ?: "icon_text"
        set(v) = sp.edit().putString("nav.mode", v).apply()

    var autoCheck: Boolean
        get() = sp.getBoolean("update.auto", true)
        set(v) = sp.edit().putBoolean("update.auto", v).apply()

    var proxy: String
        get() = sp.getString("update.proxy", "https://gh-proxy.com/") ?: "https://gh-proxy.com/"
        set(v) = sp.edit().putString("update.proxy", v).apply()

    var lowMemory: Boolean
        get() = sp.getBoolean("perf.lowMemory", false)
        set(v) = sp.edit().putBoolean("perf.lowMemory", v).apply()

    // ---------------- 收藏 ----------------

    fun favorites(): MutableList<String> {
        val raw = sp.getString("fav", "[]") ?: "[]"
        val list = mutableListOf<String>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(arr.getString(i))
        }
        return list
    }

    fun toggleFavorite(id: String): Boolean {
        val list = favorites()
        val added = if (list.contains(id)) {
            list.remove(id); false
        } else {
            list.add(0, id); true
        }
        sp.edit().putString("fav", JSONArray(list).toString()).apply()
        return added
    }

    fun isFavorite(id: String) = favorites().contains(id)

    // ---------------- 最近使用 ----------------

    fun recents(): MutableList<String> {
        val raw = sp.getString("recent", "[]") ?: "[]"
        val list = mutableListOf<String>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(arr.getString(i))
        }
        return list
    }

    fun markUsed(id: String) {
        val list = recents()
        list.remove(id)
        list.add(0, id)
        while (list.size > 12) list.removeAt(list.size - 1)
        sp.edit().putString("recent", JSONArray(list).toString()).apply()
    }

    fun clearRecents() = sp.edit().putString("recent", "[]").apply()

    // ---------------- 通用键值（工具数据） ----------------

    fun str(key: String, def: String = ""): String = sp.getString(key, def) ?: def
    fun put(key: String, value: String) = sp.edit().putString(key, value).apply()
    fun bool(key: String, def: Boolean = false): Boolean = sp.getBoolean(key, def)
    fun putBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()

    companion object {
        @Volatile
        private var inst: Prefs? = null

        fun get(ctx: Context): Prefs =
            inst ?: synchronized(this) {
                inst ?: Prefs(ctx).also { inst = it }
            }
    }
}
