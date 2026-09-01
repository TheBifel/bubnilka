package com.thebifel.bubnilka.cloud

import android.content.Context
import com.thebifel.bubnilka.core.port.CloudTransformProvider

object CloudProviderFactory {
    fun create(context: Context): CloudTransformProvider {
        val configuration = FirebaseConfiguration.fromBuildConfig()
        val app = configuration.initialize(context) ?: return UnconfiguredCloudTransformProvider()
        return FirebaseGeminiTransformProvider.create(app, configuration.modelName)
    }
}
