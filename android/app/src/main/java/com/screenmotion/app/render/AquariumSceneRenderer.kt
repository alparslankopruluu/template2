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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Aquarium: fish approach finger, tilt drives current, rising bubbles.
 */
class AquariumSceneRenderer : SceneRenderer {

    private var w = 0
    private var h = 0
    private val bgPaint = Paint()
    private val fishPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plantPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val causticPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rng = Random(7)

    private data class Fish(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var size: Float, var hue: Int, var phase: Float, var flap: Float
    )

    private data class Bubble(
        var x: Float, var y: Float, var r: Float, var speed: Float, var wobble: Float
    )

    private val fish = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    private var currentX = 0f
    private var time = 0f

    private val fishColors = intArrayOf(
        0xFFFF6B6B.toInt(), 0xFFFFD93D.toInt(), 0xFF6BCB77.toInt(),
        0xFF4D96FF.toInt(), 0xFFFF8C42.toInt(), 0xFFE056FD.toInt()
    )

    override fun onSizeChanged(width: Int, height: Int) {
        w = width
        h = height
        if (fish.isEmpty()) reset()
    }

    override fun reset() {
        fish.clear()
        bubbles.clear()
        repeat(8) {
            fish += Fish(
                x = rng.nextFloat(),
                y = 0.15f + rng.nextFloat() * 0.6f,
                vx = (rng.nextFloat() - 0.5f) * 80f,
                vy = (rng.nextFloat() - 0.5f) * 30f,
                size = 18f + rng.nextFloat() * 22f,
                hue = fishColors[it % fishColors.size],
                phase = rng.nextFloat() * 6.28f,
                flap = 0f
            )
        }
        repeat(35) {
            bubbles += Bubble(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = 3f + rng.nextFloat() * 10f,
                speed = 30f + rng.nextFloat() * 50f,
                wobble = rng.nextFloat() * 6.28f
            )
        }
        time = 0f
        currentX = 0f
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
        currentX = MathUtils.smoothDamp(currentX, motion.tiltX * 120f, dt, 0.2f)

        for (f in fish) {
            var tx = f.x * w
            var ty = f.y * h
            // Approach finger
            if (motion.touching && motion.touchX >= 0) {
                val dx = motion.touchX - tx
                val dy = motion.touchY - ty
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val pull = MathUtils.clamp(220f / dist, 0f, 180f)
                f.vx += (dx / dist) * pull * dt
                f.vy += (dy / dist) * pull * dt
            }
            // Current from tilt
            f.vx += currentX * dt * 0.8f
            f.vx += sin(time * 1.2f + f.phase) * 15f * dt
            f.vy += cos(time * 0.9f + f.phase) * 10f * dt
            // Damping
            f.vx *= (1f - 1.2f * dt).coerceAtLeast(0.85f)
            f.vy *= (1f - 1.2f * dt).coerceAtLeast(0.85f)
            tx += f.vx * dt
            ty += f.vy * dt
            // Wrap
            if (tx < -40f) tx = w + 40f
            if (tx > w + 40f) tx = -40f
            ty = MathUtils.clamp(ty, h * 0.08f, h * 0.82f)
            f.x = tx / w
            f.y = ty / h
            f.flap += dt * (4f + hypot(f.vx, f.vy) * 0.02f)
        }

        for (b in bubbles) {
            b.y -= (b.speed * dt) / h
            b.x += (sin(time * 2f + b.wobble) * 12f + currentX * 0.15f) * dt / w
            if (b.y < -0.05f) {
                b.y = 1.05f
                b.x = rng.nextFloat()
            }
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

        // Light caustics
        for (i in 0 until 5) {
            val cx = w * (0.15f + i * 0.18f) + sin(time * 0.7f + i) * 30f + motion.tiltX * 20f
            val cy = h * (0.2f + 0.1f * sin(time + i))
            causticPaint.shader = RadialGradient(
                cx, cy, 90f,
                ColorUtils.withAlpha(0xFF88EEFF.toInt(), 28),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, 90f, causticPaint)
        }

        drawPlants(canvas, motion)
        drawSand(canvas)

        // Bubbles behind fish
        for (b in bubbles) {
            val bx = b.x * w
            val by = b.y * h
            bubblePaint.style = Paint.Style.STROKE
            bubblePaint.strokeWidth = 1.5f
            bubblePaint.color = ColorUtils.withAlpha(0xFFAAEEFF.toInt(), 140)
            canvas.drawCircle(bx, by, b.r, bubblePaint)
            bubblePaint.style = Paint.Style.FILL
            bubblePaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 50)
            canvas.drawCircle(bx - b.r * 0.3f, by - b.r * 0.3f, b.r * 0.25f, bubblePaint)
        }

        for (f in fish) {
            drawFish(canvas, f.x * w, f.y * h, f.size, f.vx, f.vy, f.hue, f.flap)
        }

        // Finger lure glow
        if (motion.touching) {
            causticPaint.shader = RadialGradient(
                motion.touchX, motion.touchY, 70f,
                ColorUtils.withAlpha(0xFFFFEE88.toInt(), 90),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(motion.touchX, motion.touchY, 70f, causticPaint)
            fishPaint.color = ColorUtils.withAlpha(0xFFFFF8C0.toInt(), 200)
            canvas.drawCircle(motion.touchX, motion.touchY, 8f, fishPaint)
        }
    }

    private fun drawSand(canvas: Canvas) {
        sandPaint.shader = LinearGradient(
            0f, h * 0.85f, 0f, h.toFloat(),
            0xFF3A2E1A.toInt(), 0xFF5C4A28.toInt(),
            Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(0f, h.toFloat())
        path.lineTo(0f, h * 0.88f)
        var x = 0f
        while (x <= w) {
            val y = h * 0.88f + sin(x * 0.04f + time * 0.3f) * 8f
            path.lineTo(x, y)
            x += 12f
        }
        path.lineTo(w.toFloat(), h.toFloat())
        path.close()
        canvas.drawPath(path, sandPaint)
    }

    private fun drawPlants(canvas: Canvas, motion: MotionState) {
        val sway = motion.tiltX * 18f + sin(time * 1.5f) * 6f
        plantPaint.color = ColorUtils.withAlpha(0xFF1A6B4A.toInt(), 200)
        plantPaint.style = Paint.Style.STROKE
        plantPaint.strokeWidth = 6f
        plantPaint.strokeCap = Paint.Cap.ROUND
        val bases = floatArrayOf(0.12f, 0.35f, 0.55f, 0.78f, 0.92f)
        for (bx in bases) {
            val baseX = bx * w
            val baseY = h * 0.9f
            path.reset()
            path.moveTo(baseX, baseY)
            path.quadTo(baseX + sway, baseY - 80f, baseX + sway * 1.4f, baseY - 160f)
            canvas.drawPath(path, plantPaint)
            plantPaint.strokeWidth = 4f
            plantPaint.color = ColorUtils.withAlpha(0xFF2A9B5A.toInt(), 180)
            path.reset()
            path.moveTo(baseX + 8f, baseY)
            path.quadTo(baseX + sway * 0.7f + 10f, baseY - 50f, baseX + sway + 5f, baseY - 110f)
            canvas.drawPath(path, plantPaint)
            plantPaint.strokeWidth = 6f
            plantPaint.color = ColorUtils.withAlpha(0xFF1A6B4A.toInt(), 200)
        }
    }

    private fun drawFish(
        canvas: Canvas, x: Float, y: Float, size: Float,
        vx: Float, vy: Float, color: Int, flap: Float
    ) {
        val angle = atan2(vy, vx.coerceAtLeast(0.01f) * if (vx >= 0) 1f else -1f)
        val facing = if (vx >= -5f) 1f else -1f
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(facing, 1f)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() * 0.3f * facing)

        // Body
        fishPaint.style = Paint.Style.FILL
        fishPaint.color = color
        canvas.drawOval(-size, -size * 0.45f, size * 0.7f, size * 0.45f, fishPaint)
        // Tail
        val wag = sin(flap * 8f) * 12f
        path.reset()
        path.moveTo(-size * 0.85f, 0f)
        path.lineTo(-size * 1.5f, -size * 0.5f + wag)
        path.lineTo(-size * 1.5f, size * 0.5f - wag)
        path.close()
        fishPaint.color = ColorUtils.withAlpha(color, 220)
        canvas.drawPath(path, fishPaint)
        // Fin
        path.reset()
        path.moveTo(0f, -size * 0.35f)
        path.lineTo(size * 0.2f, -size * 0.85f)
        path.lineTo(size * 0.35f, -size * 0.3f)
        path.close()
        fishPaint.color = ColorUtils.withAlpha(color, 160)
        canvas.drawPath(path, fishPaint)
        // Eye
        fishPaint.color = Color.WHITE
        canvas.drawCircle(size * 0.35f, -size * 0.1f, size * 0.14f, fishPaint)
        fishPaint.color = Color.BLACK
        canvas.drawCircle(size * 0.4f, -size * 0.1f, size * 0.07f, fishPaint)
        // Shine stripe
        fishPaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 60)
        canvas.drawOval(-size * 0.3f, -size * 0.3f, size * 0.4f, -size * 0.05f, fishPaint)
        canvas.restore()
    }
}
