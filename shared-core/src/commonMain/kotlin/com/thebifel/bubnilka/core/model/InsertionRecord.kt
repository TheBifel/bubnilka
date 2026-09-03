package com.thebifel.bubnilka.core.model

data class InsertionRecord(
    val sessionId: Long,
    val rawTranscript: String,
    val insertedText: String,
    val styleId: String,
    val existingFieldText: String? = null,
)
