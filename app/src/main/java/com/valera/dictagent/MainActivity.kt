package com.valera.dictagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERMS = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            add(Manifest.permission.BLUETOOTH_CONNECT)
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.tvStatus)
        val btnToggle = findViewById<Button>(R.id.btnToggle)

        btnToggle.setOnClickListener {
            startRecorderService()
            // Кнопка на экране тоже работает как триггер
            val intent = Intent(this, VoiceRecorderService::class.java)
            intent.action = "TOGGLE"
            startService(intent)
        }

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMS, 1)
        } else {
            startRecorderService()
            status.text = "Сервис запущен.\nНажми кнопку гарнитуры для записи."
        }
    }

    private fun hasPermissions() = PERMS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecorderService() {
        ContextCompat.startForegroundService(this, Intent(this, VoiceRecorderService::class.java))
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED })
            startRecorderService()
    }
}
