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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Aquarium: cute expressive fish (eyes/fins react), schooling + approach finger,
 * tilt current, bubbles, idle wiggle when alone.
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
        var size: Float, var hue: Int, var phase: Float, var flap: Float,
        var blink: Float, var cheek: Float, var schoolRole: Int
    )

    private data class Bubble(
        var x: Float, var y: Float, var r: Float, var speed: Float, var wobble: Float
    )

    private val fish = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    private var currentX = 0f
    private var time = 0f
    private var idlePulse = 0f

    private val fishColors = intArrayOf(
        0xFFFF6B6B.toInt(), 0xFFFFD93D.toInt(), 0xFF6BCB77.toInt(),
        0xFF4D96FF.toInt(), 0xFFFF8C42.toInt(), 0xFFE056FD.toInt(),
        0xFF7FDBDA.toInt(), 0xFFFF9FF3.toInt()
    )

    override fun onSizeChanged(width: Int, height: Int) {
        w = width
        h = height
        if (fish.isEmpty()) reset()
    }

    override fun reset() {
        fish.clear()
        bubbles.clear()
        repeat(10) {
            fish += Fish(
                x = rng.nextFloat(),
                y = 0.15f + rng.nextFloat() * 0.6f,
                vx = (rng.nextFloat() - 0.5f) * 80f,
                vy = (rng.nextFloat() - 0.5f) * 30f,
                size = 16f + rng.nextFloat() * 26f,
                hue = fishColors[it % fishColors.size],
                phase = rng.nextFloat() * 6.28f,
                flap = 0f,
                blink = rng.nextFloat() * 8f,
                cheek = 0f,
                schoolRole = it
            )
        }
        repeat(48) {
            bubbles += Bubble(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = 2.5f + rng.nextFloat() * 11f,
                speed = 28f + rng.nextFloat() * 55f,
                wobble = rng.nextFloat() * 6.28f
            )
        }
        time = 0f
        currentX = 0f
        idlePulse = 0f
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
        idlePulse = sin(time * 1.3f) * 0.5f + 0.5f
        currentX = MathUtils.smoothDamp(currentX, motion.tiltX * 140f, dt, 0.18f)

        // School centroid for flocking
        var cx = 0f
        var cy = 0f
        for (f in fish) {
            cx += f.x
            cy += f.y
        }
        cx /= fish.size.coerceAtLeast(1)
        cy /= fish.size.coerceAtLeast(1)

        for (f in fish) {
            var tx = f.x * w
            var ty = f.y * h
            val excited = motion.touching && motion.touchX >= 0
            if (excited) {
                val dx = motion.touchX - tx
                val dy = motion.touchY - ty
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val pull = MathUtils.clamp(260f / dist, 0f, 200f)
                f.vx += (dx / dist) * pull * dt
                f.vy += (dy / dist) * pull * dt
                f.cheek = MathUtils.smoothDamp(f.cheek, 1f, dt, 0.1f)
            } else {
                // Soft schooling toward centroid + idle swim
                f.vx += (cx - f.x) * w * 0.15f * dt
                f.vy += (cy - f.y) * h * 0.1f * dt
                f.vx += sin(time * 1.15f + f.phase) * 22f * dt
                f.vy += cos(time * 0.85f + f.phase) * 14f * dt
                f.cheek = MathUtils.smoothDamp(f.cheek, idlePulse * 0.35f, dt, 0.25f)
            }
            f.vx += currentX * dt * 0.85f
            f.vx *= (1f - 1.15f * dt).coerceAtLeast(0.86f)
            f.vy *= (1f - 1.15f * dt).coerceAtLeast(0.86f)
            tx += f.vx * dt
            ty += f.vy * dt
            if (tx < -40f) tx = w + 40f
            if (tx > w + 40f) tx = -40f
            ty = MathUtils.clamp(ty, h * 0.08f, h * 0.82f)
            f.x = tx / w
            f.y = ty / h
            f.flap += dt * (4.5f + hypot(f.vx, f.vy) * 0.025f)
            f.blink += dt
            if (f.blink > 4f + f.phase) f.blink = 0f
        }

        for (b in bubbles) {
            b.y -= (b.speed * dt) / h
            b.x += (sin(time * 2f + b.wobble) * 14f + currentX * 0.18f) * dt / w
            if (b.y < -0.05f) {
                b.y = 1.05f
                b.x = rng.nextFloat()
                SfxPlayer.play(SfxKind.BUBBLE_POP)
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

        for (i in 0 until 6) {
            val cx = w * (0.12f + i * 0.16f) + sin(time * 0.7f + i) * 34f + motion.tiltX * 22f
            val cy = h * (0.18f + 0.1f * sin(time + i))
            causticPaint.shader = RadialGradient(
                cx, cy, 100f,
                ColorUtils.withAlpha(0xFF88EEFF.toInt(), 32),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, 100f, causticPaint)
        }

        drawPlants(canvas, motion)
        drawSand(canvas)

        for (b in bubbles) {
            val bx = b.x * w
            val by = b.y * h
            bubblePaint.style = Paint.Style.STROKE
            bubblePaint.strokeWidth = 1.5f
            bubblePaint.color = ColorUtils.withAlpha(0xFFAAEEFF.toInt(), 150)
            canvas.drawCircle(bx, by, b.r, bubblePaint)
            bubblePaint.style = Paint.Style.FILL
            bubblePaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 55)
            canvas.drawCircle(bx - b.r * 0.3f, by - b.r * 0.3f, b.r * 0.25f, bubblePaint)
        }

        for (f in fish) {
            drawFish(canvas, f)
        }

        if (motion.touching) {
            causticPaint.shader = RadialGradient(
                motion.touchX, motion.touchY, 80f,
                ColorUtils.withAlpha(0xFFFFEE88.toInt(), 100),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(motion.touchX, motion.touchY, 80f, causticPaint)
            fishPaint.color = ColorUtils.withAlpha(0xFFFFF8C0.toInt(), 210)
            canvas.drawCircle(motion.touchX, motion.touchY, 9f, fishPaint)
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
        val sway = motion.tiltX * 20f + sin(time * 1.5f) * 7f + currentX * 0.05f
        plantPaint.color = ColorUtils.withAlpha(0xFF1A6B4A.toInt(), 200)
        plantPaint.style = Paint.Style.STROKE
        plantPaint.strokeWidth = 6f
        plantPaint.strokeCap = Paint.Cap.ROUND
        val bases = floatArrayOf(0.1f, 0.28f, 0.45f, 0.62f, 0.78f, 0.92f)
        for (bx in bases) {
            val baseX = bx * w
            val baseY = h * 0.9f
            path.reset()
            path.moveTo(baseX, baseY)
            path.quadTo(baseX + sway, baseY - 90f, baseX + sway * 1.5f, baseY - 175f)
            canvas.drawPath(path, plantPaint)
            plantPaint.strokeWidth = 4f
            plantPaint.color = ColorUtils.withAlpha(0xFF2A9B5A.toInt(), 185)
            path.reset()
            path.moveTo(baseX + 8f, baseY)
            path.quadTo(baseX + sway * 0.7f + 10f, baseY - 55f, baseX + sway + 5f, baseY - 120f)
            canvas.drawPath(path, plantPaint)
            plantPaint.strokeWidth = 6f
            plantPaint.color = ColorUtils.withAlpha(0xFF1A6B4A.toInt(), 200)
        }
    }

    private fun drawFish(canvas: Canvas, f: Fish) {
        val x = f.x * w
        val y = f.y * h
        val size = f.size
        val vx = f.vx
        val vy = f.vy
        val color = f.hue
        val flap = f.flap
        val angle = atan2(vy, vx.coerceAtLeast(0.01f) * if (vx >= 0) 1f else -1f)
        val facing = if (vx >= -5f) 1f else -1f
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(facing, 1f)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() * 0.3f * facing)

        // Soft body glow
        causticPaint.shader = RadialGradient(
            0f, 0f, size * 1.4f,
            ColorUtils.withAlpha(color, 40),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, size * 1.3f, causticPaint)

        // Body (rounder / cuter)
        fishPaint.style = Paint.Style.FILL
        fishPaint.color = color
        canvas.drawOval(-size * 0.95f, -size * 0.52f, size * 0.75f, size * 0.52f, fishPaint)
        // Belly highlight
        fishPaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 55)
        canvas.drawOval(-size * 0.35f, size * 0.05f, size * 0.45f, size * 0.42f, fishPaint)

        // Tail — reactive flap
        val wag = sin(flap * 9f) * (14f + f.cheek * 6f)
        path.reset()
        path.moveTo(-size * 0.85f, 0f)
        path.lineTo(-size * 1.55f, -size * 0.55f + wag)
        path.lineTo(-size * 1.35f, 0f)
        path.lineTo(-size * 1.55f, size * 0.55f - wag)
        path.close()
        fishPaint.color = ColorUtils.withAlpha(color, 230)
        canvas.drawPath(path, fishPaint)

        // Dorsal fin
        val finLift = sin(flap * 6f) * 4f - f.cheek * 3f
        path.reset()
        path.moveTo(-size * 0.1f, -size * 0.4f)
        path.lineTo(size * 0.15f, -size * 0.95f + finLift)
        path.lineTo(size * 0.4f, -size * 0.35f)
        path.close()
        fishPaint.color = ColorUtils.withAlpha(color, 170)
        canvas.drawPath(path, fishPaint)

        // Pectoral fin flutter
        path.reset()
        path.moveTo(size * 0.05f, size * 0.1f)
        path.lineTo(size * 0.35f, size * 0.55f + sin(flap * 10f) * 5f)
        path.lineTo(size * 0.2f, size * 0.15f)
        path.close()
        canvas.drawPath(path, fishPaint)

        // Cheeks when excited
        if (f.cheek > 0.1f) {
            fishPaint.color = ColorUtils.withAlpha(0xFFFF8A9A.toInt(), (f.cheek * 160).toInt())
            canvas.drawCircle(size * 0.15f, size * 0.18f, size * 0.14f * f.cheek, fishPaint)
        }

        // Eyes — blink + look toward velocity / finger energy
        val eyeOpen = if (f.blink in 0f..0.12f) 0.15f else 1f
        val pupilShift = MathUtils.clamp(vx * 0.01f, -size * 0.04f, size * 0.04f)
        fishPaint.color = Color.WHITE
        canvas.drawOval(
            size * 0.28f, -size * 0.22f * eyeOpen,
            size * 0.55f, size * 0.08f * eyeOpen,
            fishPaint
        )
        if (eyeOpen > 0.4f) {
            fishPaint.color = Color.BLACK
            canvas.drawCircle(size * 0.42f + pupilShift, -size * 0.06f, size * 0.09f, fishPaint)
            fishPaint.color = Color.WHITE
            canvas.drawCircle(size * 0.45f + pupilShift, -size * 0.1f, size * 0.035f, fishPaint)
        }

        // Smile
        fishPaint.style = Paint.Style.STROKE
        fishPaint.strokeWidth = 1.6f
        fishPaint.color = ColorUtils.withAlpha(0xFF442222.toInt(), 160)
        fishPaint.strokeCap = Paint.Cap.ROUND
        path.reset()
        path.moveTo(size * 0.55f, size * 0.12f)
        path.quadTo(size * 0.68f, size * (0.18f + f.cheek * 0.08f), size * 0.72f, size * 0.06f)
        canvas.drawPath(path, fishPaint)
        fishPaint.style = Paint.Style.FILL

        canvas.restore()
    }
}
