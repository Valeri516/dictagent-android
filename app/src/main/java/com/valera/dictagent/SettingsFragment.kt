package com.valera.dictagent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etToken = view.findViewById<EditText>(R.id.etToken)
        val etChat = view.findViewById<EditText>(R.id.etChat)
        val spinnerTrigger = view.findViewById<Spinner>(R.id.spinnerTrigger)
        val switchCalls = view.findViewById<Switch>(R.id.switchCalls)
        val switchViber = view.findViewById<Switch>(R.id.switchViber)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        etToken.setText(Config.botToken)
        etChat.setText(Config.chatId)
        switchCalls.isChecked = Config.recordCalls
        switchViber.isChecked = Config.captureViber

        val triggers = arrayOf("Одно нажатие", "Двойное нажатие", "Долгое нажатие")
        spinnerTrigger.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, triggers)
        spinnerTrigger.setSelection(when(Config.headsetTrigger) { "DOUBLE" -> 1; "LONG" -> 2; else -> 0 })

        btnSave.setOnClickListener {
            Config.botToken = etToken.text.toString().trim()
            Config.chatId = etChat.text.toString().trim()
            Config.headsetTrigger = when(spinnerTrigger.selectedItemPosition) { 1 -> "DOUBLE"; 2 -> "LONG"; else -> "SINGLE" }
            Config.recordCalls = switchCalls.isChecked
            Config.captureViber = switchViber.isChecked
            Config.save(requireContext())
            Toast.makeText(requireContext(), "Сохранено", Toast.LENGTH_SHORT).show()
        }
    }
}
