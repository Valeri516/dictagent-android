package com.valera.dictagent

import android.content.Context

object Config {
    private const val P = "da_prefs"
    private var ctx: Context? = null
    private val p get() = ctx!!.getSharedPreferences(P, Context.MODE_PRIVATE)

    fun init(c: Context) { ctx = c.applicationContext }
    fun save(c: Context) { ctx = c.applicationContext }

    // Сервер
    var serverUrl: String get() = p.getString("server_url", "http://144.91.98.6:8340")!! ; set(v) = p.edit().putString("server_url", v).apply()
    var deviceId:  String get() = p.getString("device_id", "android")!! ; set(v) = p.edit().putString("device_id", v).apply()

    // Триггеры — гарнитура
    var headsetEnabled: Boolean get() = p.getBoolean("hs_en", true) ; set(v) = p.edit().putBoolean("hs_en", v).apply()
    var headsetTrigger: String  get() = p.getString("ht", "SINGLE")!! ; set(v) = p.edit().putString("ht", v).apply()

    // Триггеры — встряхивание
    var shakeEnabled: Boolean get() = p.getBoolean("shake_en", false) ; set(v) = p.edit().putBoolean("shake_en", v).apply()
    var shakeSensitivity: String get() = p.getString("shake_sens", "MEDIUM")!! ; set(v) = p.edit().putString("shake_sens", v).apply()

    // Триггеры — включение экрана
    var screenWakeEnabled: Boolean get() = p.getBoolean("screen_en", false) ; set(v) = p.edit().putBoolean("screen_en", v).apply()

    // Триггеры — голосовая фраза
    var voicePhraseEnabled:   Boolean get() = p.getBoolean("vp_en", false) ; set(v) = p.edit().putBoolean("vp_en", v).apply()
    var voiceStartPhrase:     String  get() = p.getString("vp_start", "начать запись")!! ; set(v) = p.edit().putString("vp_start", v).apply()
    var voiceStopPhrase:      String  get() = p.getString("vp_stop", "стоп")!! ; set(v) = p.edit().putString("vp_stop", v).apply()
    var voicePhraseSchedule:  String  get() = p.getString("vp_sched", "ALWAYS")!! ; set(v) = p.edit().putString("vp_sched", v).apply()
    var voiceScheduleFrom:    Int     get() = p.getInt("vp_from", 8) ; set(v) = p.edit().putInt("vp_from", v).apply()
    var voiceScheduleTo:      Int     get() = p.getInt("vp_to", 22) ; set(v) = p.edit().putInt("vp_to", v).apply()

    // Захват
    var recordCalls:   Boolean get() = p.getBoolean("rc", true)  ; set(v) = p.edit().putBoolean("rc", v).apply()
    var captureViber:  Boolean get() = p.getBoolean("cv", true)  ; set(v) = p.edit().putBoolean("cv", v).apply()
    var useContactNames: Boolean get() = p.getBoolean("contacts", true) ; set(v) = p.edit().putBoolean("contacts", v).apply()

    // Ответ
    var ttsEnabled:      Boolean get() = p.getBoolean("tts_en", true)  ; set(v) = p.edit().putBoolean("tts_en", v).apply()
    var ttsMaxSeconds:   Int     get() = p.getInt("tts_max", 30) ; set(v) = p.edit().putInt("tts_max", v).apply()
    var silentSkipTts:   Boolean get() = p.getBoolean("silent_skip", true) ; set(v) = p.edit().putBoolean("silent_skip", v).apply()
    var suppressTelegramWhenHeadphones: Boolean get() = p.getBoolean("supp_tg", true) ; set(v) = p.edit().putBoolean("supp_tg", v).apply()

    // ИИ модель
    var aiModel: String get() = p.getString("ai_model", "auto")!! ; set(v) = p.edit().putString("ai_model", v).apply()

    // Хранение
    var deleteAudioAfterSend: Boolean get() = p.getBoolean("del_audio", true) ; set(v) = p.edit().putBoolean("del_audio", v).apply()

    val ok get() = serverUrl.isNotBlank()
}
