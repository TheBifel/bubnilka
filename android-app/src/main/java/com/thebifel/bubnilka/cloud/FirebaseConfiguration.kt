package com.thebifel.bubnilka.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.thebifel.bubnilka.BuildConfig
import com.thebifel.bubnilka.runtime.BubnilkaLog

data class FirebaseConfiguration(
    val apiKey: String,
    val applicationId: String,
    val projectId: String,
    val storageBucket: String?,
    val modelName: String,
) {
    val isComplete: Boolean
        get() = apiKey.isNotBlank() &&
            applicationId.isNotBlank() &&
            projectId.isNotBlank() &&
            modelName in ALLOWED_FREE_MODELS

    fun initialize(context: Context): FirebaseApp? {
        if (!isComplete) {
            BubnilkaLog.state("firebase_not_configured")
            return null
        }

        val existing = FirebaseApp.getApps(context).firstOrNull {
            it.name == FirebaseApp.DEFAULT_APP_NAME
        }
        val app = existing ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(applicationId)
                .setProjectId(projectId)
                .apply {
                    storageBucket?.takeIf(String::isNotBlank)?.let(::setStorageBucket)
                }
                .build(),
        )

        AppCheckInstaller.install(app)
        BubnilkaLog.state("firebase_initialized")
        return app
    }

    companion object {
        val ALLOWED_FREE_MODELS = setOf(
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
        )

        fun fromBuildConfig(): FirebaseConfiguration = FirebaseConfiguration(
            apiKey = BuildConfig.FIREBASE_API_KEY,
            applicationId = BuildConfig.FIREBASE_APP_ID,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET.ifBlank { null },
            modelName = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-3.5-flash-lite" },
        )
    }
}
