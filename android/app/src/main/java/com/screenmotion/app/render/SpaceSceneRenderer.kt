package com.screenmotion.app.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.motion.MotionState
import com.screenmotion.app.util.ColorUtils
import com.screenmotion.app.util.MathUtils
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Space: parallax starfield, swipe meteors, long-press spaceship.
 */
class SpaceSceneRenderer : SceneRenderer {

    private var w = 0
    private var h = 0
    private val bgPaint = Paint()
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val meteorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val shipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Star(
        var x: Float, var y: Float, var z: Float,
        var size: Float, var twinkle: Float, var phase: Float
    )

    private data class Meteor(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, var length: Float
    )

    private val stars = mutableListOf<Star>()
    private val meteors = mutableListOf<Meteor>()
    private var shipX = 0f
    private var shipY = 0f
    private var shipVisible = 0f
    private var time = 0f
    private val shipPath = Path()
    private val rng = Random(42)

    override fun onSizeChanged(width: Int, height: Int) {
        w = width
        h = height
        shipX = w * 0.5f
        shipY = h * 0.72f
        if (stars.isEmpty()) reset()
    }

    override fun reset() {
        stars.clear()
        meteors.clear()
        val count = 140
        repeat(count) {
            stars += Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                z = rng.nextFloat() * 0.9f + 0.1f,
                size = rng.nextFloat() * 2.8f + 0.6f,
                twinkle = rng.nextFloat() * 2f + 1f,
                phase = rng.nextFloat() * Math.PI.toFloat() * 2f
            )
        }
        shipVisible = 0f
        time = 0f
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
        // Spawn meteor on strong swipe
        if (kotlin.math.abs(motion.swipeVx) + kotlin.math.abs(motion.swipeVy) > 400f) {
            val speed = 900f + rng.nextFloat() * 400f
            val ang = kotlin.math.atan2(motion.swipeVy, motion.swipeVx)
            meteors += Meteor(
                x = if (motion.touchX > 0) motion.touchX else w * rng.nextFloat(),
                y = if (motion.touchY > 0) motion.touchY else h * 0.2f,
                vx = cos(ang) * speed,
                vy = sin(ang) * speed,
                life = 1f,
                length = 40f + rng.nextFloat() * 50f
            )
            motion.clearSwipe()
        }
        // Occasional ambient meteor
        if (rng.nextFloat() < dt * 0.35f) {
            meteors += Meteor(
                x = w * rng.nextFloat(),
                y = -20f,
                vx = (rng.nextFloat() - 0.3f) * 200f,
                vy = 350f + rng.nextFloat() * 250f,
                life = 1f,
                length = 30f + rng.nextFloat() * 40f
            )
        }
        val it = meteors.iterator()
        while (it.hasNext()) {
            val m = it.next()
            m.x += m.vx * dt
            m.y += m.vy * dt
            m.life -= dt * 0.7f
            if (m.life <= 0f || m.y > h + 80f) it.remove()
        }
        val targetShip = if (motion.longPress) 1f else 0f
        shipVisible = MathUtils.smoothDamp(shipVisible, targetShip, dt, 0.18f)
        if (motion.touching && motion.longPress) {
            shipX = MathUtils.smoothDamp(shipX, motion.touchX, dt, 0.1f)
            shipY = MathUtils.smoothDamp(shipY, motion.touchY, dt, 0.1f)
        }
    }

    override fun draw(canvas: Canvas, motion: MotionState, config: ThemeConfig) {
        if (w <= 0 || h <= 0) return
        // Gradient background
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            config.backgroundTop.toInt(), config.backgroundBottom.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Soft nebula blobs
        drawNebula(canvas, motion)

        val px = motion.tiltX * config.parallaxStrength
        val py = motion.tiltY * config.parallaxStrength

        for (s in stars) {
            val depth = s.z
            val ox = px * (1.2f - depth) * 55f
            val oy = py * (1.2f - depth) * 55f
            val sx = s.x * w + ox
            val sy = s.y * h + oy
            val tw = 0.55f + 0.45f * sin(time * s.twinkle + s.phase)
            val alpha = (180 + 75 * tw * depth).toInt().coerceIn(40, 255)
            starPaint.color = ColorUtils.withAlpha(Color.WHITE, alpha)
            val r = s.size * (0.5f + depth)
            canvas.drawCircle(sx, sy, r, starPaint)
            if (depth > 0.7f && tw > 0.85f) {
                starPaint.color = ColorUtils.withAlpha(0xFFAACCFF.toInt(), (alpha * 0.4f).toInt())
                canvas.drawCircle(sx, sy, r * 2.2f, starPaint)
            }
        }

        // Meteors
        for (m in meteors) {
            val a = (m.life * 255).toInt().coerceIn(0, 255)
            meteorPaint.strokeWidth = 3.5f
            meteorPaint.color = ColorUtils.withAlpha(0xFFFFE8A0.toInt(), a)
            val nx = m.vx / (kotlin.math.hypot(m.vx.toDouble(), m.vy.toDouble()).toFloat().coerceAtLeast(1f))
            val ny = m.vy / (kotlin.math.hypot(m.vx.toDouble(), m.vy.toDouble()).toFloat().coerceAtLeast(1f))
            canvas.drawLine(
                m.x, m.y,
                m.x - nx * m.length, m.y - ny * m.length,
                meteorPaint
            )
            meteorPaint.strokeWidth = 7f
            meteorPaint.color = ColorUtils.withAlpha(0xFFFF8800.toInt(), a / 3)
            canvas.drawLine(m.x, m.y, m.x - nx * m.length * 0.5f, m.y - ny * m.length * 0.5f, meteorPaint)
            starPaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), a)
            canvas.drawCircle(m.x, m.y, 4f, starPaint)
        }

        // Spaceship on long-press
        if (shipVisible > 0.02f) {
            drawShip(canvas, shipX, shipY, shipVisible, time)
        }

        // Touch ripple
        if (motion.touching) {
            val pulse = 0.5f + 0.5f * sin(time * 6f)
            glowPaint.shader = RadialGradient(
                motion.touchX, motion.touchY, 80f + pulse * 30f,
                ColorUtils.withAlpha(0xFF6688FF.toInt(), (90 * pulse).toInt()),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(motion.touchX, motion.touchY, 110f, glowPaint)
        }
    }

    private fun drawNebula(canvas: Canvas, motion: MotionState) {
        val cx = w * 0.65f + motion.tiltX * 30f
        val cy = h * 0.35f + motion.tiltY * 20f
        nebulaPaint.shader = RadialGradient(
            cx, cy, w * 0.45f,
            ColorUtils.withAlpha(0xFF4A2080.toInt(), 55),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, w * 0.45f, nebulaPaint)
        val cx2 = w * 0.25f - motion.tiltX * 20f
        val cy2 = h * 0.7f
        nebulaPaint.shader = RadialGradient(
            cx2, cy2, w * 0.35f,
            ColorUtils.withAlpha(0xFF204080.toInt(), 40),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx2, cy2, w * 0.35f, nebulaPaint)
    }

    private fun drawShip(canvas: Canvas, x: Float, y: Float, alphaF: Float, t: Float) {
        val a = (alphaF * 255).toInt().coerceIn(0, 255)
        val bob = sin(t * 3f) * 4f
        canvas.save()
        canvas.translate(x, y + bob)
        canvas.scale(1.15f, 1.15f)
        // Engine glow
        glowPaint.shader = RadialGradient(
            0f, 28f, 36f,
            ColorUtils.withAlpha(0xFF44AAFF.toInt(), (a * 0.7f).toInt()),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 32f + sin(t * 18f) * 3f, 28f, glowPaint)
        // Body
        shipPath.reset()
        shipPath.moveTo(0f, -36f)
        shipPath.lineTo(22f, 18f)
        shipPath.lineTo(8f, 12f)
        shipPath.lineTo(0f, 22f)
        shipPath.lineTo(-8f, 12f)
        shipPath.lineTo(-22f, 18f)
        shipPath.close()
        shipPaint.color = ColorUtils.withAlpha(0xFFE8EEFF.toInt(), a)
        shipPaint.style = Paint.Style.FILL
        canvas.drawPath(shipPath, shipPaint)
        shipPaint.color = ColorUtils.withAlpha(0xFF6AB0FF.toInt(), a)
        canvas.drawCircle(0f, -4f, 6f, shipPaint)
        // Wings accent
        shipPaint.color = ColorUtils.withAlpha(0xFFFF6688.toInt(), a)
        canvas.drawCircle(-16f, 10f, 3f, shipPaint)
        canvas.drawCircle(16f, 10f, 3f, shipPaint)
        canvas.restore()
    }
}
