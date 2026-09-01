package com.thebifel.bubnilka.core.port

import com.thebifel.bubnilka.core.model.TransformationRequest
import com.thebifel.bubnilka.core.model.TransformationResult
import com.thebifel.bubnilka.core.model.KeyboardFailure

interface CloudTransformProvider {
    suspend fun transform(request: TransformationRequest): TransformationResult
}

class CloudTransformException(
    val category: KeyboardFailure,
    cause: Throwable? = null,
) : Exception(category.name, cause)
