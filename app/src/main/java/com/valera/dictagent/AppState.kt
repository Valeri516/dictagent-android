package com.valera.dictagent

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Intent

object AppState {
    var isRecording = false
    var pendingCallNumber: String? = null
    var pendingCallType: String? = null

    fun notifyUpdate(ctx: Context) {
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(Intent("com.valera.dictagent.UPDATE"))
    }
}
