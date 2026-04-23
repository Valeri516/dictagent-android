package com.valera.dictagent

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import kotlin.math.sqrt

class ShakeDetectorService : Service(), SensorEventListener {
    private lateinit var sm: SensorManager
    private var lastShake = 0L
    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
    private var initialized = false

    private val threshold get() = when (Config.shakeSensitivity) {
        "LOW" -> 18f; "HIGH" -> 8f; else -> 12f
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Config.init(this)
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
        if (!initialized) { lastX = x; lastY = y; lastZ = z; initialized = true; return }
        val delta = sqrt((x-lastX)*(x-lastX) + (y-lastY)*(y-lastY) + (z-lastZ)*(z-lastZ))
        lastX = x; lastY = y; lastZ = z
        if (delta > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShake > 1000) {
                lastShake = now
                val intent = Intent(this, VoiceRecorderService::class.java)
                intent.action = "TOGGLE"
                startService(intent)
            }
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onDestroy() {
        sm.unregisterListener(this)
        super.onDestroy()
    }
}
