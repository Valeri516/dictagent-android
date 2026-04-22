package com.valera.dictagent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class RecordFragment : Fragment() {
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_record, container, false)
        tvStatus = v.findViewById(R.id.tvStatus)
        btnToggle = v.findViewById(R.id.btnToggle)
        btnToggle.setOnClickListener {
            val intent = Intent(requireContext(), VoiceRecorderService::class.java)
            intent.action = "TOGGLE"
            requireContext().startService(intent)
        }
        updateUI()
        return v
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val recording = AppState.isRecording
        if (recording) {
            tvStatus.text = "Запись... Нажми кнопку гарнитуры чтобы остановить"
            btnToggle.text = "■ Стоп"
        } else {
            tvStatus.text = "Готов к записи. Нажми кнопку гарнитуры или кнопку ниже."
            btnToggle.text = "● Запись"
        }
    }
}
