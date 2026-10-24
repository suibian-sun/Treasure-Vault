package com.suibian.treasure_vault

import android.app.Application
import com.suibian.treasure_vault.core.Prefs

/**
 * 应用入口。
 *
 * 这里刻意只做一件事：记录上一次未捕获异常，便于用户把原因反馈回来。
 * 任何耗时或可能失败的初始化（OCR 模型、传感器、更新检查）都不放在启动路径，
 * 全部推迟到真正用到的时候。
 */
class VaultApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = error.stackTrace.take(14).joinToString("\n") { "  at $it" }
                Prefs.get(this).lastCrash =
                    "${error.javaClass.name}: ${error.message}\n$trace\n（线程：${thread.name}）"
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
