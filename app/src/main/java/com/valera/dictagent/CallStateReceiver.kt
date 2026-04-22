package com.valera.dictagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!Config.recordCalls) return
        val intentAction = intent.action ?: return
        when (intentAction) {
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
                AppState.pendingCallNumber = number
                AppState.pendingCallType = "CALL_OUT"
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: AppState.pendingCallNumber
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        AppState.pendingCallNumber = number
                        AppState.pendingCallType = "CALL_IN"
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        val svcIntent = Intent(ctx, CallRecordingService::class.java).apply {
                            this.action = "START"
                            putExtra("type", AppState.pendingCallType ?: "CALL_IN")
                            putExtra("contact", number)
                        }
                        ContextCompat.startForegroundService(ctx, svcIntent)
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        ctx.startService(Intent(ctx, CallRecordingService::class.java).apply { this.action = "STOP" })
                        AppState.pendingCallNumber = null
                        AppState.pendingCallType = null
                    }
                }
            }
        }
    }
}
