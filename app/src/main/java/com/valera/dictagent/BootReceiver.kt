package com.valera.dictagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Config.init(ctx)
            if (Config.ok) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, VoiceRecorderService::class.java))
            }
        }
    }
}
