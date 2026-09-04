package com.thebifel.bubnilka.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.thebifel.bubnilka.R
import kotlin.math.sin

/** Lightweight microphone visualization driven by SpeechRecognizer RMS callbacks. */
class VoiceRippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private var rippleColor = context.getColor(R.color.bubnilka_primary)
    private var listening = false
    private var targetLevel = 0f
    private var displayedLevel = 0f
    private var animationStartedAt = 0L

    fun setListening(value: Boolean) {
        if (listening == value) return
        listening = value
        if (value) {
            animationStartedAt = SystemClock.uptimeMillis()
            postInvalidateOnAnimation()
        } else {
            targetLevel = 0f
            displayedLevel = 0f
            invalidate()
        }
    }

    fun setAudioLevel(level: Float) {
        targetLevel = if (level.isFinite()) level.coerceIn(0f, 1f) else 0f
        if (listening) postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!listening) return

        val response = if (targetLevel > displayedLevel) 0.34f else 0.12f
        displayedLevel += (targetLevel - displayedLevel) * response
        targetLevel *= 0.94f

        val centerX = width / 2f
        val centerY = height / 2f
        val elapsed = (SystemClock.uptimeMillis() - animationStartedAt).coerceAtLeast(0L)
        val cycle = (elapsed % RIPPLE_DURATION_MS).toFloat() / RIPPLE_DURATION_MS
        val idleBreath = ((sin(elapsed / 360.0) + 1.0) * 0.5).toFloat()
        val energy = (MINIMUM_BREATH + displayedLevel * LEVEL_WEIGHT).coerceIn(0f, 1f)

        haloPaint.color = rippleColor
        haloPaint.alpha = (14f + 24f * energy + 8f * idleBreath).toInt()
        canvas.drawCircle(centerX, centerY, dp(34f + 3f * energy), haloPaint)

        ringPaint.color = rippleColor
        repeat(RING_COUNT) { index ->
            val progress = (cycle + index.toFloat() / RING_COUNT) % 1f
            val radius = dp(33f + progress * (8f + 10f * energy))
            ringPaint.alpha = ((1f - progress) * (30f + 105f * energy)).toInt().coerceIn(0, 255)
            ringPaint.strokeWidth = dp(1.2f + 1.2f * energy)
            canvas.drawCircle(centerX, centerY, radius, ringPaint)
        }

        postInvalidateOnAnimation()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        rippleColor = context.getColor(R.color.bubnilka_primary)
    }

    override fun onDetachedFromWindow() {
        listening = false
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float): Float = value * density

    private companion object {
        const val RIPPLE_DURATION_MS = 1_250L
        const val RING_COUNT = 3
        const val MINIMUM_BREATH = 0.12f
        const val LEVEL_WEIGHT = 0.88f
    }
}
