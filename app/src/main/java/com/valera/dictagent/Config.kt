package com.valera.dictagent

import android.content.Context

object Config {
    private const val P = "da_prefs"
    private var ctx: Context? = null
    private val p get() = ctx!!.getSharedPreferences(P, Context.MODE_PRIVATE)

    fun init(c: Context) { ctx = c.applicationContext }

    var botToken: String get() = p.getString("tok","")!! ; set(v) { p.edit().putString("tok",v).apply() }
    var chatId:   String get() = p.getString("cid","")!! ; set(v) { p.edit().putString("cid",v).apply() }
    var headsetTrigger: String get() = p.getString("ht","SINGLE")!! ; set(v) { p.edit().putString("ht",v).apply() }
    var recordCalls:  Boolean get() = p.getBoolean("rc",true)  ; set(v) { p.edit().putBoolean("rc",v).apply() }
    var captureViber: Boolean get() = p.getBoolean("cv",true)  ; set(v) { p.edit().putBoolean("cv",v).apply() }
    fun save(c: Context) { ctx = c.applicationContext }
    val ok get() = botToken.isNotBlank() && chatId.isNotBlank()
}