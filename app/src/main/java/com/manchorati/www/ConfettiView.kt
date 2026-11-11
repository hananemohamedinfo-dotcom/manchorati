package com.manchorati.www

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colors = intArrayOf(
        Color.parseColor("#F59E0B"), // Gold
        Color.parseColor("#38BDF8"), // Cyan
        Color.parseColor("#10B981"), // Emerald
        Color.parseColor("#EC4899"), // Pink
        Color.parseColor("#8B5CF6"), // Purple
        Color.parseColor("#EF4444"), // Red
        Color.parseColor("#E2E8F0")  // Silver
    )

    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var color: Int,
        var rotation: Float,
        var rotationSpeed: Float,
        var alpha: Int = 255
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isPlaying = false
    private var startTime = 0L
    private val duration = 3500L

    fun startConfetti() {
        particles.clear()
        val width = width.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
        val height = height.takeIf { it > 0 } ?: context.resources.displayMetrics.heightPixels

        for (i in 0 until 65) {
            val p = Particle(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * (height * 0.35f) - 50f,
                vx = (Random.nextFloat() - 0.5f) * 6f,
                vy = Random.nextFloat() * 8f + 5f,
                size = Random.nextFloat() * 12f + 10f,
                color = colors[Random.nextInt(colors.size)],
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 12f
            )
            particles.add(p)
        }

        isPlaying = true
        startTime = System.currentTimeMillis()
        visibility = VISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isPlaying || particles.isEmpty()) return

        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > duration) {
            isPlaying = false
            particles.clear()
            visibility = GONE
            return
        }

        val fadeProgress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
        val currentAlpha = ((1f - fadeProgress * fadeProgress) * 255).toInt().coerceIn(0, 255)

        for (p in particles) {
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f // gravity
            p.rotation += p.rotationSpeed
            p.alpha = currentAlpha

            paint.color = p.color
            paint.alpha = p.alpha

            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawRect(-p.size / 2f, -p.size / 3f, p.size / 2f, p.size / 3f, paint)
            canvas.restore()
        }

        postInvalidateOnAnimation()
    }
}
