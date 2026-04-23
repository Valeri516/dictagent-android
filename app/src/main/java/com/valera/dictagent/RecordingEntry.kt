package com.valera.dictagent

data class RecordingEntry(
    val id: Long = System.currentTimeMillis(),
    val type: String,       // MANUAL, CALL_IN, CALL_OUT, VIBER
    val timestamp: Long,
    val durationSec: Int = 0,
    var filePath: String? = null,
    var text: String? = null,
    var contact: String? = null,
    var status: String = "PENDING" // PENDING, SENT, FAILED
)