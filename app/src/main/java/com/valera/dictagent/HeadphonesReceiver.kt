package com.valera.dictagent

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class HeadphonesReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", 0)
                AppState.headphonesConnected = state == 1
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> AppState.headphonesConnected = true
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                AppState.headphonesConnected = am.isBluetoothA2dpOn || am.isWiredHeadsetOn
            }
        }
    }
}
