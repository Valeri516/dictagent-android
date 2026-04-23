package com.valera.dictagent

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Сервер
        val etServer = view.findViewById<EditText>(R.id.etServer)
        val etDeviceId = view.findViewById<EditText>(R.id.etDeviceId)
        // Гарнитура
        val switchHeadset = view.findViewById<Switch>(R.id.switchHeadset)
        val spinnerHeadset = view.findViewById<Spinner>(R.id.spinnerHeadset)
        // Встряхивание
        val switchShake = view.findViewById<Switch>(R.id.switchShake)
        val spinnerShake = view.findViewById<Spinner>(R.id.spinnerShake)
        // Экран
        val switchScreen = view.findViewById<Switch>(R.id.switchScreen)
        // Голосовая фраза
        val switchVoice = view.findViewById<Switch>(R.id.switchVoice)
        val etVoiceStart = view.findViewById<EditText>(R.id.etVoiceStart)
        val etVoiceStop = view.findViewById<EditText>(R.id.etVoiceStop)
        val spinnerVoiceSched = view.findViewById<Spinner>(R.id.spinnerVoiceSched)
        val etVoiceFrom = view.findViewById<EditText>(R.id.etVoiceFrom)
        val etVoiceTo = view.findViewById<EditText>(R.id.etVoiceTo)
        // Захват
        val switchCalls = view.findViewById<Switch>(R.id.switchCalls)
        val switchViber = view.findViewById<Switch>(R.id.switchViber)
        val switchContacts = view.findViewById<Switch>(R.id.switchContacts)
        // Ответ
        val switchTts = view.findViewById<Switch>(R.id.switchTts)
        val switchSilent = view.findViewById<Switch>(R.id.switchSilent)
        val switchSuppressTg = view.findViewById<Switch>(R.id.switchSuppressTg)
        // ИИ модель
        val spinnerModel = view.findViewById<Spinner>(R.id.spinnerModel)
        // Кнопка сохранить
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        // Индикатор сервера
        val tvServerStatus = view.findViewById<TextView>(R.id.tvServerStatus)

        // Заполняем из Config
        etServer.setText(Config.serverUrl)
        etDeviceId.setText(Config.deviceId)

        val triggerModes = arrayOf("Одно нажатие", "Двойное нажатие", "Долгое нажатие")
        spinnerHeadset.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, triggerModes)
        spinnerHeadset.setSelection(when(Config.headsetTrigger) { "DOUBLE" -> 1; "LONG" -> 2; else -> 0 })
        switchHeadset.isChecked = Config.headsetEnabled

        val sensItems = arrayOf("Слабая", "Средняя", "Сильная")
        spinnerShake.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sensItems)
        spinnerShake.setSelection(when(Config.shakeSensitivity) { "LOW" -> 0; "HIGH" -> 2; else -> 1 })
        switchShake.isChecked = Config.shakeEnabled

        switchScreen.isChecked = Config.screenWakeEnabled

        switchVoice.isChecked = Config.voicePhraseEnabled
        etVoiceStart.setText(Config.voiceStartPhrase)
        etVoiceStop.setText(Config.voiceStopPhrase)
        val schedItems = arrayOf("Всегда", "По расписанию", "Когда экран включён")
        spinnerVoiceSched.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, schedItems)
        spinnerVoiceSched.setSelection(when(Config.voicePhraseSchedule) { "SCHEDULE" -> 1; "SCREEN_ON" -> 2; else -> 0 })
        etVoiceFrom.setText(Config.voiceScheduleFrom.toString())
        etVoiceTo.setText(Config.voiceScheduleTo.toString())

        switchCalls.isChecked = Config.recordCalls
        switchViber.isChecked = Config.captureViber
        switchContacts.isChecked = Config.useContactNames

        switchTts.isChecked = Config.ttsEnabled
        switchSilent.isChecked = Config.silentSkipTts
        switchSuppressTg.isChecked = Config.suppressTelegramWhenHeadphones

        val models = arrayOf("auto", "gemini-2.0-flash", "deepseek/deepseek-chat", "gemini-2.5-flash")
        spinnerModel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, models)
        val modelIdx = models.indexOf(Config.aiModel).takeIf { it >= 0 } ?: 0
        spinnerModel.setSelection(modelIdx)

        // Проверка сервера
        tvServerStatus.text = "Сервер: проверяю..."
        Thread {
            val ok = try {
                val url = java.net.URL("${Config.serverUrl}/health")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                val code = conn.responseCode; conn.disconnect(); code == 200
            } catch (_: Exception) { false }
            requireActivity().runOnUiThread {
                tvServerStatus.text = if (ok) "Сервер: доступен" else "Сервер: недоступен"
                tvServerStatus.setTextColor(if (ok) 0xFF2E7D32.toInt() else 0xFFC62828.toInt())
            }
        }.start()

        btnSave.setOnClickListener {
            Config.serverUrl = etServer.text.toString().trim().trimEnd('/')
            Config.deviceId = etDeviceId.text.toString().trim()
            Config.headsetEnabled = switchHeadset.isChecked
            Config.headsetTrigger = when(spinnerHeadset.selectedItemPosition) { 1 -> "DOUBLE"; 2 -> "LONG"; else -> "SINGLE" }
            Config.shakeEnabled = switchShake.isChecked
            Config.shakeSensitivity = when(spinnerShake.selectedItemPosition) { 0 -> "LOW"; 2 -> "HIGH"; else -> "MEDIUM" }
            Config.screenWakeEnabled = switchScreen.isChecked
            Config.voicePhraseEnabled = switchVoice.isChecked
            Config.voiceStartPhrase = etVoiceStart.text.toString()
            Config.voiceStopPhrase = etVoiceStop.text.toString()
            Config.voicePhraseSchedule = when(spinnerVoiceSched.selectedItemPosition) { 1 -> "SCHEDULE"; 2 -> "SCREEN_ON"; else -> "ALWAYS" }
            Config.voiceScheduleFrom = etVoiceFrom.text.toString().toIntOrNull() ?: 8
            Config.voiceScheduleTo = etVoiceTo.text.toString().toIntOrNull() ?: 22
            Config.recordCalls = switchCalls.isChecked
            Config.captureViber = switchViber.isChecked
            Config.useContactNames = switchContacts.isChecked
            Config.ttsEnabled = switchTts.isChecked
            Config.silentSkipTts = switchSilent.isChecked
            Config.suppressTelegramWhenHeadphones = switchSuppressTg.isChecked
            Config.aiModel = models[spinnerModel.selectedItemPosition]
            Config.save(requireContext())

            // Перезапуск сервисов
            restartServices()
            Toast.makeText(requireContext(), "Сохранено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restartServices() {
        val ctx = requireContext()
        ctx.stopService(Intent(ctx, ShakeDetectorService::class.java))
        ctx.stopService(Intent(ctx, VoicePhraseService::class.java))
        if (Config.shakeEnabled) ctx.startService(Intent(ctx, ShakeDetectorService::class.java))
        if (Config.voicePhraseEnabled) ctx.startService(Intent(ctx, VoicePhraseService::class.java))
    }
}
