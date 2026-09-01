package com.thebifel.bubnilka.runtime

import android.util.Log

/** Structured diagnostics that intentionally accept categories, never dictated content. */
object BubnilkaLog {
    private const val TAG = "Bubnilka"

    fun state(category: String) {
        Log.d(TAG, "state=$category")
    }

    fun failure(category: String, throwableType: String? = null) {
        Log.w(TAG, "failure=$category type=${throwableType ?: "none"}")
    }
}
