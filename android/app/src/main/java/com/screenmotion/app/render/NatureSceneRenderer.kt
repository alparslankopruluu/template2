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
 * Nature: dark-cinematic lush dusk — drifting clouds, birds that follow drag,
 * wind particles, soft parallax trees/hills (3 depths), idle leaf mascot flutter.
 */
class NatureSceneRenderer : SceneRenderer {

    private var w = 0
    private var h = 0
    private val bgPaint = Paint()
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val treePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val windPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rng = Random(21)

    private data class Cloud(
        var x: Float, var y: Float, var scale: Float, var speed: Float, var depth: Float
    )

    private data class Bird(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var wing: Float, var size: Float, var hue: Int
    )

    private data class WindDust(
        var x: Float, var y: Float, var vx: Float, var life: Float, var size: Float
    )

    private data class Tree(
        var x: Float, var height: Float, var lean: Float, var depth: Int
    )

    private val clouds = mutableListOf<Cloud>()
    private val birds = mutableListOf<Bird>()
    private val wind = mutableListOf<WindDust>()
    private val treesFar = mutableListOf<Tree>()
    private val treesNear = mutableListOf<Tree>()

    private var time = 0f
    private var wasTouching = false
    private var lastTiltAbs = 0f
    private var windForce = 0f
    private var leafX = 0.55f
    private var leafY = 0.42f
    private var leafRot = 0f
    private var leafVisible = 1f
    private var idleBob = 0f

    private val birdColors = intArrayOf(
        0xFFE8F0FF.toInt(), 0xFFFFD6A5.toInt(), 0xFFB8E0D2.toInt(), 0xFFFFB4C8.toInt()
    )

    override fun onSizeChanged(width: Int, height: Int) {
        w = width
        h = height
        if (clouds.isEmpty()) reset()
    }

    override fun reset() {
        clouds.clear()
        birds.clear()
        wind.clear()
        treesFar.clear()
        treesNear.clear()
        repeat(7) {
            clouds += Cloud(
                x = rng.nextFloat(),
                y = 0.08f + rng.nextFloat() * 0.28f,
                scale = 0.55f + rng.nextFloat() * 0.9f,
                speed = 0.012f + rng.nextFloat() * 0.028f,
                depth = 0.35f + rng.nextFloat() * 0.55f
            )
        }
        repeat(6) {
            birds += Bird(
                x = rng.nextFloat(),
                y = 0.18f + rng.nextFloat() * 0.35f,
                vx = 40f + rng.nextFloat() * 60f,
                vy = (rng.nextFloat() - 0.5f) * 20f,
                wing = rng.nextFloat() * 6.28f,
                size = 10f + rng.nextFloat() * 8f,
                hue = birdColors[it % birdColors.size]
            )
        }
        repeat(55) {
            wind += WindDust(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                vx = 40f + rng.nextFloat() * 90f,
                life = rng.nextFloat(),
                size = 1.2f + rng.nextFloat() * 2.8f
            )
        }
        repeat(9) {
            treesFar += Tree(
                x = 0.05f + it * 0.11f + rng.nextFloat() * 0.04f,
                height = 0.12f + rng.nextFloat() * 0.1f,
                lean = (rng.nextFloat() - 0.5f) * 0.15f,
                depth = 0
            )
        }
        repeat(6) {
            treesNear += Tree(
                x = 0.08f + it * 0.16f + rng.nextFloat() * 0.05f,
                height = 0.18f + rng.nextFloat() * 0.14f,
                lean = (rng.nextFloat() - 0.5f) * 0.2f,
                depth = 1
            )
        }
        time = 0f
        windForce = 0f
        leafX = 0.55f
        leafY = 0.42f
        leafRot = 0f
        leafVisible = 1f
        idleBob = 0f
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
        if (motion.touching && !wasTouching) {
            SfxPlayer.play(SfxKind.BIRD_CHIRP)
        }
        wasTouching = motion.touching
        val tiltAbs = kotlin.math.abs(motion.tiltX) + kotlin.math.abs(motion.tiltY)
        if (tiltAbs > 0.55f && tiltAbs > lastTiltAbs + 0.12f) {
            SfxPlayer.play(SfxKind.WIND_WHOOSH)
        }
        lastTiltAbs = tiltAbs
        idleBob = sin(time * 1.1f) * 0.5f + 0.5f
        val targetWind = motion.tiltX * 90f + if (motion.touching) motion.swipeVx * 0.04f else 0f
        windForce = MathUtils.smoothDamp(windForce, targetWind, dt, 0.22f)

        for (c in clouds) {
            c.x += c.speed * dt * (0.6f + c.depth) + windForce * 0.00015f * dt
            if (c.x > 1.35f) c.x = -0.35f
            if (c.x < -0.35f) c.x = 1.35f
        }

        for (b in birds) {
            var tx = b.x * w
            var ty = b.y * h
            if (motion.touching && motion.touchX >= 0) {
                val dx = motion.touchX - tx
                val dy = motion.touchY - ty
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val pull = MathUtils.clamp(200f / dist, 0f, 160f)
                b.vx += (dx / dist) * pull * dt
                b.vy += (dy / dist) * pull * dt * 0.85f
            } else {
                // Idle flocking drift
                b.vx += sin(time * 0.9f + b.wing) * 25f * dt
                b.vy += cos(time * 0.7f + b.wing * 0.5f) * 12f * dt
                b.vx += windForce * 0.15f * dt
            }
            b.vx *= (1f - 0.9f * dt).coerceAtLeast(0.88f)
            b.vy *= (1f - 0.9f * dt).coerceAtLeast(0.88f)
            // Soft speed floor so birds keep gliding
            if (hypot(b.vx, b.vy) < 25f) b.vx += 18f * dt
            tx += b.vx * dt
            ty += b.vy * dt
            if (tx < -50f) tx = w + 50f
            if (tx > w + 50f) tx = -50f
            ty = MathUtils.clamp(ty, h * 0.08f, h * 0.62f)
            b.x = tx / w
            b.y = ty / h
            b.wing += dt * (5f + hypot(b.vx, b.vy) * 0.03f)
        }

        for (d in wind) {
            d.x += (d.vx + windForce * 0.4f) * dt / w
            d.y += sin(time * 3f + d.life * 10f) * 18f * dt / h
            d.life -= dt * 0.15f
            if (d.x > 1.1f || d.life <= 0f) {
                d.x = -0.05f
                d.y = rng.nextFloat()
                d.life = 0.6f + rng.nextFloat()
                d.vx = 40f + rng.nextFloat() * 90f
            }
        }

        // Leaf mascot flutters near finger or idles mid-air
        val targetLeafX: Float
        val targetLeafY: Float
        if (motion.touching && motion.touchX >= 0) {
            targetLeafX = motion.touchX / w
            targetLeafY = motion.touchY / h
            leafVisible = MathUtils.smoothDamp(leafVisible, 1f, dt, 0.12f)
        } else {
            targetLeafX = 0.55f + sin(time * 0.55f) * 0.12f + motion.tiltX * 0.08f
            targetLeafY = 0.38f + cos(time * 0.4f) * 0.06f + idleBob * 0.02f
            leafVisible = MathUtils.smoothDamp(leafVisible, 0.85f, dt, 0.25f)
        }
        leafX = MathUtils.smoothDamp(leafX, targetLeafX, dt, 0.14f)
        leafY = MathUtils.smoothDamp(leafY, targetLeafY, dt, 0.14f)
        leafRot = MathUtils.smoothDamp(leafRot, windForce * 0.02f + sin(time * 2.2f) * 0.4f, dt, 0.18f)
    }

    override fun draw(canvas: Canvas, motion: MotionState, config: ThemeConfig) {
        if (w <= 0 || h <= 0) return

        // Lush dusk gradient
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            config.backgroundTop.toInt(),
            ColorUtils.lerpColor(config.backgroundBottom.toInt(), 0xFF2A4A38.toInt(), 0.35f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Soft sun/moon glow (far parallax)
        val sunX = w * 0.78f + motion.tiltX * 18f
        val sunY = h * 0.22f + motion.tiltY * 10f
        glowPaint.shader = RadialGradient(
            sunX, sunY, w * 0.35f,
            ColorUtils.withAlpha(0xFFFFC878.toInt(), 55),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sunX, sunY, w * 0.35f, glowPaint)
        glowPaint.shader = null
        birdPaint.color = ColorUtils.withAlpha(0xFFFFE0A0.toInt(), 200)
        canvas.drawCircle(sunX, sunY, 22f + idleBob * 3f, birdPaint)

        drawHills(canvas, motion, depth = 0) // far
        drawTrees(canvas, treesFar, motion, depthScale = 0.55f, alpha = 140)
        drawClouds(canvas, motion)
        drawHills(canvas, motion, depth = 1) // mid
        drawTrees(canvas, treesNear, motion, depthScale = 1f, alpha = 220)
        drawWind(canvas)
        drawHills(canvas, motion, depth = 2) // near grass

        for (b in birds) {
            drawBird(canvas, b.x * w, b.y * h, b.size, b.vx, b.vy, b.wing, b.hue)
        }

        if (leafVisible > 0.05f) {
            drawLeafMascot(canvas, leafX * w, leafY * h, leafRot, leafVisible)
        }

        if (motion.touching) {
            glowPaint.shader = RadialGradient(
                motion.touchX, motion.touchY, 70f,
                ColorUtils.withAlpha(0xFFA8E6CF.toInt(), 70),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(motion.touchX, motion.touchY, 70f, glowPaint)
        }

        AdBillboardPainter.drawIfFree(canvas, w, h, com.screenmotion.app.data.ThemeType.NATURE, time)
    }

    private fun drawHills(canvas: Canvas, motion: MotionState, depth: Int) {
        val parallax = when (depth) {
            0 -> motion.tiltX * 12f
            1 -> motion.tiltX * 28f
            else -> motion.tiltX * 48f
        }
        val baseY = when (depth) {
            0 -> h * 0.58f
            1 -> h * 0.68f
            else -> h * 0.82f
        }
        val color = when (depth) {
            0 -> 0xFF1A2E28.toInt()
            1 -> 0xFF243F34.toInt()
            else -> 0xFF2F5540.toInt()
        }
        hillPaint.color = color
        hillPaint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(-40f + parallax, h.toFloat())
        path.lineTo(-40f + parallax, baseY)
        var x = -40f
        val amp = when (depth) {
            0 -> 28f
            1 -> 40f
            else -> 22f
        }
        val freq = when (depth) {
            0 -> 0.008f
            1 -> 0.012f
            else -> 0.018f
        }
        while (x <= w + 40f) {
            val y = baseY + sin((x + parallax) * freq + time * 0.15f + depth) * amp
            path.lineTo(x + parallax, y)
            x += 16f
        }
        path.lineTo(w + 40f + parallax, h.toFloat())
        path.close()
        canvas.drawPath(path, hillPaint)

        // Soft grass highlights near layer
        if (depth == 2) {
            hillPaint.color = ColorUtils.withAlpha(0xFF5CFF9A.toInt(), 28)
            for (i in 0 until 18) {
                val gx = (i / 18f) * w + parallax * 0.3f + sin(time + i) * 4f
                val gy = h * 0.86f + sin(time * 2f + i) * 3f
                canvas.drawCircle(gx, gy, 3f, hillPaint)
            }
        }
    }

    private fun drawTrees(
        canvas: Canvas,
        list: List<Tree>,
        motion: MotionState,
        depthScale: Float,
        alpha: Int
    ) {
        val px = motion.tiltX * (20f * depthScale)
        for (t in list) {
            val baseX = t.x * w + px
            val baseY = h * (if (t.depth == 0) 0.62f else 0.78f)
            val th = t.height * h * depthScale
            val sway = windForce * 0.08f * depthScale + sin(time * 1.8f + t.x * 10f) * 4f * depthScale
            // Trunk
            treePaint.style = Paint.Style.STROKE
            treePaint.strokeWidth = 5f * depthScale
            treePaint.strokeCap = Paint.Cap.ROUND
            treePaint.color = ColorUtils.withAlpha(0xFF3A2818.toInt(), alpha)
            path.reset()
            path.moveTo(baseX, baseY)
            path.quadTo(baseX + sway * 0.4f + t.lean * 20f, baseY - th * 0.5f, baseX + sway + t.lean * 30f, baseY - th)
            canvas.drawPath(path, treePaint)
            // Canopy
            treePaint.style = Paint.Style.FILL
            treePaint.color = ColorUtils.withAlpha(0xFF1E6B45.toInt(), alpha)
            val cx = baseX + sway + t.lean * 30f
            val cy = baseY - th
            canvas.drawCircle(cx, cy, 22f * depthScale, treePaint)
            treePaint.color = ColorUtils.withAlpha(0xFF2A8F58.toInt(), (alpha * 0.85f).toInt())
            canvas.drawCircle(cx - 12f * depthScale, cy + 6f * depthScale, 16f * depthScale, treePaint)
            canvas.drawCircle(cx + 14f * depthScale, cy + 4f * depthScale, 15f * depthScale, treePaint)
        }
    }

    private fun drawClouds(canvas: Canvas, motion: MotionState) {
        for (c in clouds) {
            val px = motion.tiltX * (1.1f - c.depth) * 35f
            val py = motion.tiltY * (1.1f - c.depth) * 18f
            val cx = c.x * w + px
            val cy = c.y * h + py
            val s = c.scale * 40f
            val a = (90 + c.depth * 80).toInt().coerceIn(50, 180)
            cloudPaint.color = ColorUtils.withAlpha(0xFFD8E8F0.toInt(), a)
            canvas.drawCircle(cx, cy, s * 0.7f, cloudPaint)
            canvas.drawCircle(cx - s * 0.55f, cy + 4f, s * 0.5f, cloudPaint)
            canvas.drawCircle(cx + s * 0.5f, cy + 2f, s * 0.55f, cloudPaint)
            canvas.drawCircle(cx + s * 0.1f, cy - s * 0.25f, s * 0.45f, cloudPaint)
        }
    }

    private fun drawWind(canvas: Canvas) {
        for (d in wind) {
            val a = (d.life * 140).toInt().coerceIn(0, 140)
            windPaint.color = ColorUtils.withAlpha(0xFFC8FFE0.toInt(), a)
            windPaint.strokeWidth = 1.2f
            windPaint.style = Paint.Style.STROKE
            val x = d.x * w
            val y = d.y * h
            canvas.drawLine(x, y, x + d.size * 6f, y + sin(time * 4f + d.life) * 2f, windPaint)
            windPaint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, d.size * 0.6f, windPaint)
        }
    }

    private fun drawBird(
        canvas: Canvas, x: Float, y: Float, size: Float,
        vx: Float, vy: Float, wing: Float, color: Int
    ) {
        val facing = if (vx >= 0f) 1f else -1f
        val angle = atan2(vy, kotlin.math.abs(vx).coerceAtLeast(0.01f))
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(facing, 1f)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() * 0.4f)

        val flap = sin(wing * 2.4f)
        // Wings
        birdPaint.style = Paint.Style.STROKE
        birdPaint.strokeWidth = 2.5f
        birdPaint.strokeCap = Paint.Cap.ROUND
        birdPaint.color = ColorUtils.withAlpha(color, 230)
        path.reset()
        path.moveTo(0f, 0f)
        path.quadTo(-size * 0.9f, -size * (0.7f + flap * 0.55f), -size * 1.6f, -size * 0.15f * flap)
        canvas.drawPath(path, birdPaint)
        path.reset()
        path.moveTo(0f, 0f)
        path.quadTo(size * 0.9f, -size * (0.7f + flap * 0.55f), size * 1.6f, -size * 0.15f * flap)
        canvas.drawPath(path, birdPaint)

        // Body
        birdPaint.style = Paint.Style.FILL
        birdPaint.color = color
        canvas.drawOval(-size * 0.55f, -size * 0.28f, size * 0.65f, size * 0.28f, birdPaint)
        // Beak
        birdPaint.color = 0xFFFFB070.toInt()
        path.reset()
        path.moveTo(size * 0.6f, 0f)
        path.lineTo(size * 1.05f, -size * 0.08f)
        path.lineTo(size * 0.6f, size * 0.1f)
        path.close()
        canvas.drawPath(path, birdPaint)
        // Eye
        birdPaint.color = Color.WHITE
        canvas.drawCircle(size * 0.28f, -size * 0.08f, size * 0.12f, birdPaint)
        birdPaint.color = Color.BLACK
        canvas.drawCircle(size * 0.32f, -size * 0.08f, size * 0.06f, birdPaint)
        canvas.restore()
    }

    private fun drawLeafMascot(canvas: Canvas, x: Float, y: Float, rot: Float, alphaF: Float) {
        val a = (alphaF * 255).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(Math.toDegrees(rot.toDouble()).toFloat())
        canvas.scale(1.1f + idleBob * 0.05f, 1.1f + idleBob * 0.05f)

        // Soft glow
        glowPaint.shader = RadialGradient(
            0f, 0f, 36f,
            ColorUtils.withAlpha(0xFF7CFFB2.toInt(), (a * 0.35f).toInt()),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, 36f, glowPaint)

        // Leaf body
        leafPaint.style = Paint.Style.FILL
        leafPaint.color = ColorUtils.withAlpha(0xFF3DDC84.toInt(), a)
        path.reset()
        path.moveTo(0f, -28f)
        path.cubicTo(22f, -18f, 26f, 10f, 0f, 28f)
        path.cubicTo(-26f, 10f, -22f, -18f, 0f, -28f)
        path.close()
        canvas.drawPath(path, leafPaint)

        // Vein
        leafPaint.style = Paint.Style.STROKE
        leafPaint.strokeWidth = 2f
        leafPaint.color = ColorUtils.withAlpha(0xFF1A6B40.toInt(), a)
        canvas.drawLine(0f, -22f, 0f, 22f, leafPaint)
        canvas.drawLine(0f, -4f, 10f, 4f, leafPaint)
        canvas.drawLine(0f, -4f, -10f, 4f, leafPaint)

        // Cute face
        leafPaint.style = Paint.Style.FILL
        leafPaint.color = ColorUtils.withAlpha(Color.WHITE, a)
        canvas.drawCircle(-7f, -4f, 4.5f, leafPaint)
        canvas.drawCircle(7f, -4f, 4.5f, leafPaint)
        leafPaint.color = ColorUtils.withAlpha(Color.BLACK, a)
        canvas.drawCircle(-6f, -4f, 2.2f, leafPaint)
        canvas.drawCircle(8f, -4f, 2.2f, leafPaint)
        // Smile
        leafPaint.style = Paint.Style.STROKE
        leafPaint.strokeWidth = 1.8f
        leafPaint.strokeCap = Paint.Cap.ROUND
        path.reset()
        path.moveTo(-5f, 6f)
        path.quadTo(0f, 11f, 5f, 6f)
        canvas.drawPath(path, leafPaint)

        canvas.restore()
    }
}
