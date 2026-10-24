package com.suibian.treasure_vault.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.suibian.treasure_vault.MainActivity
import com.suibian.treasure_vault.R

/** 页面跳转。 */
object Router {

    fun open(ctx: Context, spec: ToolSpec) {
        val i = Intent(ctx, ToolActivity::class.java).putExtra(ToolActivity.EXTRA_ID, spec.id)
        ctx.startActivity(i)
        if (ctx is Activity && !Prefs.get(ctx).lowMemory) {
            ctx.overridePendingTransition(R.anim.slide_in, R.anim.fade_out)
        }
    }

    fun openActivity(ctx: Context, cls: Class<out Activity>) {
        ctx.startActivity(Intent(ctx, cls))
        if (ctx is Activity && !Prefs.get(ctx).lowMemory) {
            ctx.overridePendingTransition(R.anim.slide_in, R.anim.fade_out)
        }
    }

    fun restart(ctx: Activity) {
        val i = Intent(ctx, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        ctx.finish()
        if (!Prefs.get(ctx).lowMemory) {
            ctx.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
