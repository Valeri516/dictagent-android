package com.valera.dictagent

data class RecordingEntry(
    val id: Long = System.currentTimeMillis(),
    val type: String,       // MANUAL, CALL_IN, CALL_OUT, VIBER
    val timestamp: Long,
    val durationSec: Int = 0,
    val filePath: String? = null,
    val text: String? = null,
    val contact: String? = null,
    var status: String = "PENDING" // PENDING, SENT, FAILED
)