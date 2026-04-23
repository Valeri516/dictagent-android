package com.valera.dictagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private lateinit var container: LinearLayout
    private val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) { loadHistory() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        this.container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        scroll.addView(this.container)
        return scroll
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(updateReceiver, IntentFilter("com.valera.dictagent.UPDATE"))
        loadHistory()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateReceiver)
        super.onPause()
    }

    private fun loadHistory() {
        container.removeAllViews()
        val entries = RecordingStorage.getAll(requireContext())
        if (entries.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "История пуста"; textSize = 16f; setPadding(0, 32, 0, 0)
            })
            return
        }
        for (e in entries) {
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(0xFFF5F5F5.toInt())
            }
            val header = TextView(requireContext()).apply {
                val icon = when(e.type) { "CALL_IN" -> "Вх. звонок"; "CALL_OUT" -> "Исх. звонок"; "VIBER" -> "Viber"; else -> "Запись" }
                val contact = if (!e.contact.isNullOrBlank()) " — ${e.contact}" else ""
                val dur = if (e.durationSec > 0) " (${e.durationSec}с)" else ""
                val status = when(e.status) { "SENT" -> "✓"; "ERROR" -> "✗"; "QUEUED" -> "⏳"; else -> "…" }
                text = "$icon$contact$dur  ${sdf.format(Date(e.timestamp))}  $status"
                textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
            }
            card.addView(header)
            if (!e.text.isNullOrBlank()) {
                card.addView(TextView(requireContext()).apply {
                    text = e.text; textSize = 13f; setPadding(0, 8, 0, 0)
                })
            }
            container.addView(card)
            container.addView(View(requireContext()).apply {
                setBackgroundColor(0xFFDDDDDD.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply { setMargins(0, 8, 0, 8) }
            })
        }
    }
}
