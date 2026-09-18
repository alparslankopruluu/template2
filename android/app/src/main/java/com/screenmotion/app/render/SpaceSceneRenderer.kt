package com.screenmotion.app.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.screenmotion.app.audio.SfxKind
import com.screenmotion.app.audio.SfxPlayer
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.motion.MotionState
import com.screenmotion.app.util.ColorUtils
import com.screenmotion.app.util.MathUtils
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Space: 3-depth parallax stars, friendlier spaceship mascot, planet accents,
 * swipe meteors, idle bobbing when no input.
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
    private val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Star(
        var x: Float, var y: Float, var z: Float,
        var size: Float, var twinkle: Float, var phase: Float
    )

    private data class Meteor(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, var length: Float
    )

    private data class Planet(
        var x: Float, var y: Float, var r: Float, var color: Int, var ring: Boolean, var depth: Float
    )

    private val stars = mutableListOf<Star>()
    private val meteors = mutableListOf<Meteor>()
    private val planets = mutableListOf<Planet>()
    private var shipX = 0f
    private var shipY = 0f
    private var shipVisible = 0f
    private var idleShip = 0f
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
        planets.clear()
        repeat(180) {
            stars += Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                z = rng.nextFloat() * 0.9f + 0.1f,
                size = rng.nextFloat() * 2.8f + 0.5f,
                twinkle = rng.nextFloat() * 2.2f + 0.8f,
                phase = rng.nextFloat() * Math.PI.toFloat() * 2f
            )
        }
        planets += Planet(0.22f, 0.28f, 38f, 0xFF6B8CFF.toInt(), true, 0.4f)
        planets += Planet(0.78f, 0.55f, 22f, 0xFFC8A0A8.toInt(), false, 0.7f)
        planets += Planet(0.55f, 0.18f, 14f, 0xFFFFD580.toInt(), false, 0.25f)
        shipVisible = 0f
        idleShip = 0.55f
        time = 0f
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
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
            SfxPlayer.play(SfxKind.METEOR)
            motion.clearSwipe()
        }
        if (rng.nextFloat() < dt * 0.4f) {
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

        val targetShip = when {
            motion.longPress -> 1f
            !motion.touching -> 0.55f // idle friendly ship
            else -> 0.15f
        }
        shipVisible = MathUtils.smoothDamp(shipVisible, targetShip, dt, 0.18f)
        idleShip = MathUtils.smoothDamp(idleShip, if (motion.longPress) 1f else 0.55f, dt, 0.2f)

        if (motion.touching && motion.longPress) {
            shipX = MathUtils.smoothDamp(shipX, motion.touchX, dt, 0.1f)
            shipY = MathUtils.smoothDamp(shipY, motion.touchY, dt, 0.1f)
        } else if (!motion.touching) {
            val idleX = w * (0.5f + sin(time * 0.35f) * 0.12f + motion.tiltX * 0.08f)
            val idleY = h * (0.68f + cos(time * 0.28f) * 0.04f)
            shipX = MathUtils.smoothDamp(shipX, idleX, dt, 0.35f)
            shipY = MathUtils.smoothDamp(shipY, idleY, dt, 0.35f)
        }
    }

    override fun draw(canvas: Canvas, motion: MotionState, config: ThemeConfig) {
        if (w <= 0 || h <= 0) return
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            config.backgroundTop.toInt(), config.backgroundBottom.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        drawNebula(canvas, motion)

        val px = motion.tiltX * config.parallaxStrength
        val py = motion.tiltY * config.parallaxStrength

        // Far planets (depth layer 1)
        for (p in planets) {
            val ox = px * (1.2f - p.depth) * 40f
            val oy = py * (1.2f - p.depth) * 40f
            drawPlanet(canvas, p.x * w + ox, p.y * h + oy, p.r, p.color, p.ring)
        }

        // Stars — three depth bands via z
        for (s in stars) {
            val depth = s.z
            val ox = px * (1.25f - depth) * 60f
            val oy = py * (1.25f - depth) * 60f
            val sx = s.x * w + ox
            val sy = s.y * h + oy
            val tw = 0.55f + 0.45f * sin(time * s.twinkle + s.phase)
            val alpha = (170 + 85 * tw * depth).toInt().coerceIn(35, 255)
            starPaint.color = ColorUtils.withAlpha(Color.WHITE, alpha)
            val r = s.size * (0.45f + depth)
            canvas.drawCircle(sx, sy, r, starPaint)
            if (depth > 0.65f && tw > 0.8f) {
                starPaint.color = ColorUtils.withAlpha(0xFFAACCFF.toInt(), (alpha * 0.45f).toInt())
                canvas.drawCircle(sx, sy, r * 2.4f, starPaint)
            }
        }

        for (m in meteors) {
            val a = (m.life * 255).toInt().coerceIn(0, 255)
            meteorPaint.strokeWidth = 3.5f
            meteorPaint.color = ColorUtils.withAlpha(0xFFFFE8A0.toInt(), a)
            val len = kotlin.math.hypot(m.vx.toDouble(), m.vy.toDouble()).toFloat().coerceAtLeast(1f)
            val nx = m.vx / len
            val ny = m.vy / len
            canvas.drawLine(m.x, m.y, m.x - nx * m.length, m.y - ny * m.length, meteorPaint)
            meteorPaint.strokeWidth = 7f
            meteorPaint.color = ColorUtils.withAlpha(0xFFFF8800.toInt(), a / 3)
            canvas.drawLine(m.x, m.y, m.x - nx * m.length * 0.5f, m.y - ny * m.length * 0.5f, meteorPaint)
            starPaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), a)
            canvas.drawCircle(m.x, m.y, 4f, starPaint)
        }

        if (shipVisible > 0.02f) {
            drawShip(canvas, shipX, shipY, shipVisible, time, friendly = true)
        }

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

    private fun drawPlanet(canvas: Canvas, x: Float, y: Float, r: Float, color: Int, ring: Boolean) {
        glowPaint.shader = RadialGradient(
            x - r * 0.25f, y - r * 0.25f, r * 1.4f,
            ColorUtils.withAlpha(color, 200),
            ColorUtils.withAlpha(color, 40),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, r, glowPaint)
        planetPaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 50)
        canvas.drawCircle(x - r * 0.3f, y - r * 0.3f, r * 0.25f, planetPaint)
        if (ring) {
            planetPaint.style = Paint.Style.STROKE
            planetPaint.strokeWidth = 3f
            planetPaint.color = ColorUtils.withAlpha(0xFFC8D8FF.toInt(), 140)
            canvas.drawOval(x - r * 1.6f, y - r * 0.35f, x + r * 1.6f, y + r * 0.35f, planetPaint)
            planetPaint.style = Paint.Style.FILL
        }
    }

    private fun drawNebula(canvas: Canvas, motion: MotionState) {
        val cx = w * 0.65f + motion.tiltX * 30f
        val cy = h * 0.35f + motion.tiltY * 20f
        nebulaPaint.shader = RadialGradient(
            cx, cy, w * 0.45f,
            ColorUtils.withAlpha(0xFF2A3A58.toInt(), 60),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, w * 0.45f, nebulaPaint)
        val cx2 = w * 0.25f - motion.tiltX * 20f
        val cy2 = h * 0.7f
        nebulaPaint.shader = RadialGradient(
            cx2, cy2, w * 0.35f,
            ColorUtils.withAlpha(0xFF204080.toInt(), 45),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx2, cy2, w * 0.35f, nebulaPaint)
    }

    private fun drawShip(canvas: Canvas, x: Float, y: Float, alphaF: Float, t: Float, friendly: Boolean) {
        val a = (alphaF * 255).toInt().coerceIn(0, 255)
        val bob = sin(t * 2.8f) * 5f
        canvas.save()
        canvas.translate(x, y + bob)
        canvas.scale(1.2f, 1.2f)

        // Engine glow pulse
        glowPaint.shader = RadialGradient(
            0f, 30f, 40f,
            ColorUtils.withAlpha(0xFF6AA8C8.toInt(), (a * 0.75f).toInt()),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 34f + sin(t * 16f) * 4f, 32f, glowPaint)

        // Friendlier rounded body
        shipPath.reset()
        shipPath.moveTo(0f, -38f)
        shipPath.cubicTo(18f, -20f, 26f, 8f, 20f, 20f)
        shipPath.lineTo(8f, 14f)
        shipPath.lineTo(0f, 24f)
        shipPath.lineTo(-8f, 14f)
        shipPath.lineTo(-20f, 20f)
        shipPath.cubicTo(-26f, 8f, -18f, -20f, 0f, -38f)
        shipPath.close()
        shipPaint.color = ColorUtils.withAlpha(0xFFE8EEFF.toInt(), a)
        shipPaint.style = Paint.Style.FILL
        canvas.drawPath(shipPath, shipPaint)

        // Cockpit face
        shipPaint.color = ColorUtils.withAlpha(0xFF6AB0FF.toInt(), a)
        canvas.drawCircle(0f, -6f, 9f, shipPaint)
        if (friendly) {
            shipPaint.color = ColorUtils.withAlpha(Color.WHITE, a)
            canvas.drawCircle(-3.5f, -8f, 2.8f, shipPaint)
            canvas.drawCircle(3.5f, -8f, 2.8f, shipPaint)
            shipPaint.color = ColorUtils.withAlpha(Color.BLACK, a)
            canvas.drawCircle(-3f, -8f, 1.3f, shipPaint)
            canvas.drawCircle(4f, -8f, 1.3f, shipPaint)
            shipPaint.style = Paint.Style.STROKE
            shipPaint.strokeWidth = 1.4f
            shipPaint.strokeCap = Paint.Cap.ROUND
            shipPath.reset()
            shipPath.moveTo(-3f, -2f)
            shipPath.quadTo(0f, 1f, 3f, -2f)
            canvas.drawPath(shipPath, shipPaint)
            shipPaint.style = Paint.Style.FILL
        }

        // Wing lights
        shipPaint.color = ColorUtils.withAlpha(0xFFC08090.toInt(), a)
        canvas.drawCircle(-16f, 10f, 3.5f, shipPaint)
        canvas.drawCircle(16f, 10f, 3.5f, shipPaint)
        canvas.restore()
    }
}
