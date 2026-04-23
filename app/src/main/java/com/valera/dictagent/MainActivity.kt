package com.valera.dictagent

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val PERMS = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            add(Manifest.permission.BLUETOOTH_CONNECT)
    }.toTypedArray()

    private val headphonesReceiver = HeadphonesReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Config.init(this)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_record -> RecordFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment).commit()
            true
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RecordFragment()).commit()
        }

        // Отслеживание наушников
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(headphonesReceiver, filter)

        // Проверяем текущее состояние наушников
        val am = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        AppState.headphonesConnected = am.isWiredHeadsetOn || am.isBluetoothA2dpOn

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMS, 1)
        } else {
            startAllServices()
        }
    }

    private fun hasPermissions() = PERMS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startAllServices() {
        ContextCompat.startForegroundService(this, Intent(this, VoiceRecorderService::class.java))
        startService(Intent(this, PollService::class.java))
        if (Config.shakeEnabled) startService(Intent(this, ShakeDetectorService::class.java))
        if (Config.voicePhraseEnabled) startService(Intent(this, VoicePhraseService::class.java))
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED })
            startAllServices()
    }

    override fun onDestroy() {
        unregisterReceiver(headphonesReceiver)
        super.onDestroy()
    }
}
