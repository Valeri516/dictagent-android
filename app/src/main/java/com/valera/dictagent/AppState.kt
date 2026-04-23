package com.valera.dictagent

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object AppState {
    var isRecording = false
    var pendingCallNumber: String? = null
    var pendingCallType: String? = null
    var headphonesConnected = false
    var lastTranscript: String? = null

    fun notifyUpdate(ctx: Context) {
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(Intent("com.valera.dictagent.UPDATE"))
    }
}
