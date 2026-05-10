package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

/**
 * ConfettiView - draws emoji confetti using Canvas.
 * - Default emoji: "😊"
 * - Tap anywhere to spawn a center burst (center-by-default; option to spawn at touch).
 * - Call spawnBurstAtCenter() to programmatically spawn a burst.
 */
class ConfettiView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), Choreographer.FrameCallback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val random = Random(System.currentTimeMillis())
    private val particles = mutableListOf<Particle>()

    // physics parameters (tweakable)
    private val gravity = 1800f    // px/s^2
    private val drag = 0.995f     // simple per-frame drag multiplier
    private val spawnCount = 30   // particles per burst
    private val maxParticles = 300

    // timing
    private var lastFrameNanos: Long = 0L
    private var running = false

    // default emoji and size
    var emoji: String = "😊"
    var emojiTextSizePx = dpToPx(28f)

    init {
        paint.textSize = emojiTextSizePx
        paint.isAntiAlias = true
        // start frame callbacks
        start()
        // Don't auto-spawn - only spawn when explicitly called
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw each particle
        val saveCount = canvas.save()
        for (p in particles) {
            paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            paint.textSize = p.sizePx
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            // vertical centering adjustment
            val fm = paint.fontMetrics
            val correction = (fm.descent + fm.ascent) / 2f
            canvas.drawText(p.emoji, 0f, -correction, paint)
            canvas.restore()
        }
        canvas.restoreToCount(saveCount)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
        val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNanos = frameTimeNanos

        // update particles
        val itr = particles.listIterator()
        while (itr.hasNext()) {
            val p = itr.next()
            // physics integrate
            p.vy += gravity * dt
            p.vx *= drag
            p.vy *= drag
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.vRot * dt

            // age
            p.age += dt
            val lifeRatio = (p.age / p.lifetime).coerceIn(0f, 1f)
            // alpha fade after 60% life
            p.alpha = if (lifeRatio < 0.6f) 1f else (1f - (lifeRatio - 0.6f) / 0.4f).coerceIn(0f, 1f)

            if (p.age >= p.lifetime) {
                itr.remove()
            }
        }

        // redraw and schedule next frame
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun start() {
        if (!running) {
            running = true
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stop() {
        if (running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    /**
     * Spawn a burst at the center of the view.
     * If you want to spawn at a touch point, call spawnBurst(x, y)
     */
    fun spawnBurstAtCenter() {
        val cx = width / 2f
        val cy = height / 2f
        spawnBurst(cx, cy)
    }

    /**
     * Spawn a burst at given coordinates (px).
     */
    fun spawnBurst(cx: Float, cy: Float) {
        if (width == 0 || height == 0) return
        repeat(spawnCount) {
            if (particles.size >= maxParticles) return
            val angle = random.nextDouble(0.0, 2.0 * PI)
            val speed = random.nextDouble(300.0, 1200.0).toFloat()
            val vx = (cos(angle) * speed).toFloat()
            // bias upward a little so confetti appears to rise first
            val vy = (sin(angle) * speed).toFloat() - random.nextDouble(200.0, 600.0).toFloat()
            val life = random.nextDouble(0.8, 2.0).toFloat()
            val vRot = random.nextDouble(-720.0, 720.0).toFloat() // degrees per second
            val sizePx = emojiTextSizePx * random.nextDouble(0.75, 1.25).toFloat()

            particles.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = vx,
                    vy = vy,
                    vRot = vRot,
                    lifetime = life,
                    emoji = emoji,
                    sizePx = sizePx
                )
            )
        }
    }

    // Don't intercept touch events - let them pass through to video player controls
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Return false to allow touches to pass through to views below (video player)
        return false
    }

    // utility dp -> px
    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    // Particle data class inside the view file
    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var vRot: Float,
        var lifetime: Float,
        var emoji: String,
        var sizePx: Float,
        var age: Float = 0f,
        var rotation: Float = 0f,
        var alpha: Float = 1f
    )
}
