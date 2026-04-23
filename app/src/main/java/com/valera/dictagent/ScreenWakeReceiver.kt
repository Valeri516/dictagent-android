package com.valera.dictagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenWakeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!Config.screenWakeEnabled) return
        if (intent.action != Intent.ACTION_SCREEN_ON) return
        if (AppState.isRecording) return
        val svcIntent = Intent(ctx, VoiceRecorderService::class.java)
        svcIntent.action = "TOGGLE"
        ctx.startService(svcIntent)
    }
}
