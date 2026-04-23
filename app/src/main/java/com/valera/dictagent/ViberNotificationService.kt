package com.valera.dictagent

import android.provider.ContactsContract
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
        val contactName = if (Config.useContactNames) resolveContactName(title) else title
        val entry = RecordingEntry(
            type = "VIBER",
            timestamp = sbn.postTime,
            text = text,
            contact = contactName,
            status = "SENDING"
        )
        RecordingStorage.save(applicationContext, entry)
        AppState.notifyUpdate(applicationContext)
        executor.submit { ServerSender.sendViberText(applicationContext, entry) }
    }

    private fun resolveContactName(name: String): String {
        if (name.isBlank()) return name
        return try {
            val uri = ContactsContract.Contacts.CONTENT_URI
            contentResolver.query(uri,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                "${ContactsContract.Contacts.DISPLAY_NAME} = ?", arrayOf(name), null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else name
            } ?: name
        } catch (_: Exception) { name }
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
