package com.valera.dictagent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private lateinit var container: LinearLayout

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
        loadHistory()
    }

    private fun loadHistory() {
        container.removeAllViews()
        val entries = RecordingStorage.getAll(requireContext())
        if (entries.isEmpty()) {
            container.addView(TextView(requireContext()).apply { text = "История пуста" })
            return
        }
        val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        for (e in entries) {
            val tv = TextView(requireContext())
            val icon = when(e.type) {
                "CALL_IN" -> "📞"
                "CALL_OUT" -> "📲"
                "VIBER" -> "💬"
                else -> "🎤"
            }
            val time = sdf.format(Date(e.timestamp))
            val contact = if (!e.contact.isNullOrBlank()) " · ${e.contact}" else ""
            val dur = if (e.durationSec > 0) " · ${e.durationSec}с" else ""
            val status = when(e.status) { "SENT" -> "✓"; "ERROR" -> "✗"; else -> "…" }
            val preview = (e.text ?: "").take(80)
            tv.text = "$icon $time$contact$dur  $status | $preview"
            tv.setPadding(0, 12, 0, 12)
            container.addView(tv)
            val divider = View(requireContext()).apply {
                setBackgroundColor(0xFFDDDDDD.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            }
            container.addView(divider)
        }
    }
}
