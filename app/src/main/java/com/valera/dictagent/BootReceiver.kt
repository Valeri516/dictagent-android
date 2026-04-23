package com.valera.dictagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Config.init(ctx)
        ContextCompat.startForegroundService(ctx, Intent(ctx, VoiceRecorderService::class.java))
        ctx.startService(Intent(ctx, PollService::class.java))
        if (Config.shakeEnabled) ctx.startService(Intent(ctx, ShakeDetectorService::class.java))
        if (Config.voicePhraseEnabled) ctx.startService(Intent(ctx, VoicePhraseService::class.java))
    }
}
