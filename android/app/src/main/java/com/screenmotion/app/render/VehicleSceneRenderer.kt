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
import kotlin.math.sin
import kotlin.random.Random

/**
 * Vehicle: tilt changes lane, finger follow, swipe boost with speed lines.
 */
class VehicleSceneRenderer : SceneRenderer {

    private var w = 0
    private var h = 0
    private val bgPaint = Paint()
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val carPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rng = Random(99)

    private var carLane = 0f // -1 .. 1
    private var carY = 0.72f
    private var speed = 1f
    private var boostTimer = 0f
    private var scroll = 0f
    private var time = 0f
    private var followX = 0.5f

    private data class Dash(var y: Float, var lane: Int)
    private data class Building(var x: Float, var width: Float, var height: Float, var shade: Int)
    private data class SpeedLine(var x: Float, var y: Float, var len: Float, var life: Float)

    private val dashes = mutableListOf<Dash>()
    private val buildings = mutableListOf<Building>()
    private val speedLines = mutableListOf<SpeedLine>()

    override fun onSizeChanged(width: Int, height: Int) {
        w = width
        h = height
        if (dashes.isEmpty()) reset()
    }

    override fun reset() {
        dashes.clear()
        buildings.clear()
        speedLines.clear()
        repeat(12) { dashes += Dash(y = it / 12f, lane = 0) }
        repeat(10) {
            buildings += Building(
                x = rng.nextFloat(),
                width = 0.06f + rng.nextFloat() * 0.1f,
                height = 0.15f + rng.nextFloat() * 0.35f,
                shade = 30 + rng.nextInt(50)
            )
        }
        carLane = 0f
        carY = 0.72f
        speed = 1f
        boostTimer = 0f
        scroll = 0f
        time = 0f
        followX = 0.5f
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
        // Tilt → lane
        val tiltTarget = MathUtils.clamp(motion.tiltX * 1.4f, -1f, 1f)
        // Finger follow overrides when touching
        if (motion.touching && motion.touchX >= 0) {
            followX = MathUtils.clamp((motion.touchX / w.coerceAtLeast(1)) * 2f - 1f, -1f, 1f)
            carLane = MathUtils.smoothDamp(carLane, followX, dt, 0.08f)
            carY = MathUtils.smoothDamp(carY, MathUtils.clamp(motion.touchY / h, 0.45f, 0.88f), dt, 0.1f)
        } else {
            carLane = MathUtils.smoothDamp(carLane, tiltTarget, dt, 0.14f)
            carY = MathUtils.smoothDamp(carY, 0.72f, dt, 0.2f)
        }

        if (motion.boost || kotlin.math.abs(motion.swipeVy) > 800f || kotlin.math.abs(motion.swipeVx) > 800f) {
            boostTimer = 1.2f
            motion.boost = false
            motion.clearSwipe()
            repeat(14) {
                speedLines += SpeedLine(
                    x = rng.nextFloat(),
                    y = rng.nextFloat() * 0.9f,
                    len = 40f + rng.nextFloat() * 80f,
                    life = 0.4f + rng.nextFloat() * 0.4f
                )
            }
        }
        boostTimer = (boostTimer - dt).coerceAtLeast(0f)
        val targetSpeed = if (boostTimer > 0f) 3.2f else 1f
        speed = MathUtils.smoothDamp(speed, targetSpeed, dt, 0.15f)
        scroll += speed * dt * 1.8f

        for (d in dashes) {
            d.y += speed * dt * 0.55f
            if (d.y > 1.1f) d.y -= 1.2f
        }
        val it = speedLines.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.y += speed * dt * 1.2f
            s.life -= dt
            if (s.life <= 0f) it.remove()
        }
    }

    override fun draw(canvas: Canvas, motion: MotionState, config: ThemeConfig) {
        if (w <= 0 || h <= 0) return
        // Night sky
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.45f,
            config.backgroundTop.toInt(), 0xFF2A1838.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, bgPaint)

        drawCity(canvas, motion)
        drawRoad(canvas)
        drawDashes(canvas)

        // Speed lines during boost
        for (s in speedLines) {
            val a = (s.life * 200).toInt().coerceIn(0, 200)
            linePaint.color = ColorUtils.withAlpha(0xFFAAEEFF.toInt(), a)
            linePaint.strokeWidth = 2.5f
            val sx = s.x * w
            val sy = s.y * h
            canvas.drawLine(sx, sy, sx, sy + s.len * speed, linePaint)
        }

        drawCar(canvas, carLane, carY, boostTimer > 0f)

        // Boost vignette
        if (boostTimer > 0f) {
            val pulse = boostTimer.coerceIn(0f, 1f)
            glowPaint.shader = RadialGradient(
                w * 0.5f, h * carY, w * 0.7f,
                Color.TRANSPARENT,
                ColorUtils.withAlpha(0xFFFF4400.toInt(), (40 * pulse).toInt()),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)
        }
    }

    private fun drawCity(canvas: Canvas, motion: MotionState) {
        val parallax = motion.tiltX * 25f
        for (b in buildings) {
            val left = b.x * w + parallax - b.width * w * 0.5f
            val top = h * 0.48f - b.height * h
            val right = left + b.width * w
            val bottom = h * 0.52f
            cityPaint.color = Color.rgb(b.shade, b.shade, b.shade + 10)
            canvas.drawRect(left, top, right, bottom, cityPaint)
            // Windows
            cityPaint.color = ColorUtils.withAlpha(0xFFFFE088.toInt(), 160)
            var wy = top + 10f
            while (wy < bottom - 10f) {
                var wx = left + 6f
                while (wx < right - 8f) {
                    if ((wx.toInt() + wy.toInt()) % 17 != 0) {
                        canvas.drawRect(wx, wy, wx + 5f, wy + 7f, cityPaint)
                    }
                    wx += 12f
                }
                wy += 16f
            }
        }
        // Horizon glow
        glowPaint.shader = LinearGradient(
            0f, h * 0.4f, 0f, h * 0.52f,
            ColorUtils.withAlpha(0xFFFF6688.toInt(), 40),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, h * 0.4f, w.toFloat(), h * 0.52f, glowPaint)
    }

    private fun drawRoad(canvas: Canvas) {
        roadPaint.shader = LinearGradient(
            0f, h * 0.48f, 0f, h.toFloat(),
            0xFF1A1A22.toInt(), 0xFF0E0E14.toInt(),
            Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(w * 0.05f, h.toFloat())
        path.lineTo(w * 0.35f, h * 0.48f)
        path.lineTo(w * 0.65f, h * 0.48f)
        path.lineTo(w * 0.95f, h.toFloat())
        path.close()
        canvas.drawPath(path, roadPaint)

        // Road edges
        linePaint.color = 0xFFCCAA44.toInt()
        linePaint.strokeWidth = 4f
        canvas.drawLine(w * 0.05f, h.toFloat(), w * 0.35f, h * 0.48f, linePaint)
        canvas.drawLine(w * 0.95f, h.toFloat(), w * 0.65f, h * 0.48f, linePaint)
    }

    private fun drawDashes(canvas: Canvas) {
        linePaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 180)
        linePaint.strokeWidth = 4f
        for (d in dashes) {
            val t = d.y.coerceIn(0f, 1f)
            val y = h * 0.48f + t * h * 0.52f
            val roadHalf = MathUtils.lerp(w * 0.15f, w * 0.45f, t)
            val cx = w * 0.5f
            val dashH = MathUtils.lerp(8f, 28f, t)
            canvas.drawLine(cx, y, cx, y + dashH, linePaint)
            // Side lane marks slight
            linePaint.color = ColorUtils.withAlpha(0xFF888899.toInt(), 100)
            canvas.drawLine(cx - roadHalf * 0.45f, y, cx - roadHalf * 0.45f, y + dashH * 0.6f, linePaint)
            canvas.drawLine(cx + roadHalf * 0.45f, y, cx + roadHalf * 0.45f, y + dashH * 0.6f, linePaint)
            linePaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 180)
        }
    }

    private fun drawCar(canvas: Canvas, lane: Float, yNorm: Float, boosting: Boolean) {
        val t = ((yNorm - 0.48f) / 0.52f).coerceIn(0f, 1f)
        val roadHalf = MathUtils.lerp(w * 0.12f, w * 0.42f, t)
        val cx = w * 0.5f + lane * roadHalf * 0.7f
        val cy = yNorm * h
        val scale = MathUtils.lerp(0.55f, 1.15f, t)
        val bob = sin(time * 14f * speed) * 1.5f

        canvas.save()
        canvas.translate(cx, cy + bob)
        canvas.scale(scale, scale)

        // Shadow
        carPaint.color = ColorUtils.withAlpha(Color.BLACK, 80)
        canvas.drawOval(-36f, 18f, 36f, 30f, carPaint)

        // Body
        path.reset()
        path.moveTo(-32f, 8f)
        path.lineTo(-28f, -6f)
        path.lineTo(-12f, -18f)
        path.lineTo(12f, -18f)
        path.lineTo(28f, -6f)
        path.lineTo(32f, 8f)
        path.lineTo(26f, 16f)
        path.lineTo(-26f, 16f)
        path.close()
        carPaint.color = if (boosting) 0xFFFF3355.toInt() else 0xFF3D8BFF.toInt()
        canvas.drawPath(path, carPaint)

        // Cabin
        carPaint.color = 0xFF1A2030.toInt()
        path.reset()
        path.moveTo(-10f, -6f)
        path.lineTo(-6f, -16f)
        path.lineTo(6f, -16f)
        path.lineTo(10f, -6f)
        path.close()
        canvas.drawPath(path, carPaint)

        // Headlights
        carPaint.color = 0xFFFFF0A0.toInt()
        canvas.drawCircle(-20f, -2f, 4f, carPaint)
        canvas.drawCircle(20f, -2f, 4f, carPaint)
        if (boosting) {
            glowPaint.shader = RadialGradient(
                0f, 20f, 40f,
                ColorUtils.withAlpha(0xFFFF6600.toInt(), 160),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(0f, 22f, 40f, glowPaint)
        }
        // Wheels
        carPaint.color = 0xFF222228.toInt()
        canvas.drawCircle(-22f, 14f, 7f, carPaint)
        canvas.drawCircle(22f, 14f, 7f, carPaint)
        carPaint.color = 0xFF888888.toInt()
        canvas.drawCircle(-22f, 14f, 3f, carPaint)
        canvas.drawCircle(22f, 14f, 3f, carPaint)

        canvas.restore()
    }
}
