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
import com.screenmotion.app.data.VehicleType
import com.screenmotion.app.motion.MotionState
import com.screenmotion.app.util.ColorUtils
import com.screenmotion.app.util.MathUtils
import kotlin.math.sin
import kotlin.random.Random

/**
 * Vehicle: selectable sports car / truck / motorcycle / helicopter silhouettes,
 * distinct trail colors & boost behavior; tilt lanes + swipe boost.
 */
class VehicleSceneRenderer(
    private var vehicle: VehicleType = VehicleType.SPORTS_CAR
) : SceneRenderer {

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

    private var carLane = 0f
    private var carY = 0.72f
    private var speed = 1f
    private var boostTimer = 0f
    private var scroll = 0f
    private var time = 0f
    private var followX = 0.5f
    private var rotor = 0f
    private var wasBoosting = false

    private data class Dash(var y: Float, var lane: Int)
    private data class Building(var x: Float, var width: Float, var height: Float, var shade: Int)
    private data class SpeedLine(var x: Float, var y: Float, var len: Float, var life: Float)
    private data class TrailPoint(var x: Float, var y: Float, var life: Float, var scale: Float)

    private val dashes = mutableListOf<Dash>()
    private val buildings = mutableListOf<Building>()
    private val speedLines = mutableListOf<SpeedLine>()
    private val trail = mutableListOf<TrailPoint>()

    fun setVehicle(type: VehicleType) {
        if (vehicle != type) {
            vehicle = type
            trail.clear()
            speedLines.clear()
            boostTimer = 0f
        }
    }

    override fun onSizeChanged(width: Int, height: Int) {
        w = width
        h = height
        if (dashes.isEmpty()) reset()
    }

    override fun reset() {
        dashes.clear()
        buildings.clear()
        speedLines.clear()
        trail.clear()
        repeat(12) { dashes += Dash(y = it / 12f, lane = 0) }
        repeat(12) {
            buildings += Building(
                x = rng.nextFloat(),
                width = 0.06f + rng.nextFloat() * 0.1f,
                height = 0.15f + rng.nextFloat() * 0.38f,
                shade = 28 + rng.nextInt(55)
            )
        }
        carLane = 0f
        carY = if (vehicle == VehicleType.HELICOPTER) 0.55f else 0.72f
        speed = 1f
        boostTimer = 0f
        scroll = 0f
        time = 0f
        followX = 0.5f
        rotor = 0f
        wasBoosting = false
    }

    override fun update(dt: Float, motion: MotionState, config: ThemeConfig) {
        time += dt
        rotor += dt * (if (boostTimer > 0f) 28f else 14f)
        val tiltTarget = MathUtils.clamp(motion.tiltX * 1.4f, -1f, 1f)
        val idleY = if (vehicle == VehicleType.HELICOPTER) {
            0.52f + sin(time * 1.2f) * 0.03f
        } else {
            0.72f + sin(time * 0.8f) * 0.01f
        }
        if (motion.touching && motion.touchX >= 0) {
            followX = MathUtils.clamp((motion.touchX / w.coerceAtLeast(1)) * 2f - 1f, -1f, 1f)
            carLane = MathUtils.smoothDamp(carLane, followX, dt, 0.07f)
            val yMin = if (vehicle == VehicleType.HELICOPTER) 0.28f else 0.45f
            val yMax = if (vehicle == VehicleType.HELICOPTER) 0.72f else 0.88f
            carY = MathUtils.smoothDamp(carY, MathUtils.clamp(motion.touchY / h, yMin, yMax), dt, 0.09f)
        } else {
            carLane = MathUtils.smoothDamp(carLane, tiltTarget, dt, 0.14f)
            carY = MathUtils.smoothDamp(carY, idleY, dt, 0.2f)
        }

        val wantBoost = motion.boost ||
            kotlin.math.abs(motion.swipeVy) > 800f ||
            kotlin.math.abs(motion.swipeVx) > 800f
        if (wantBoost) {
            boostTimer = when (vehicle) {
                VehicleType.MOTORCYCLE -> 1.1f
                VehicleType.TRUCK -> 1.6f
                VehicleType.HELICOPTER -> 1.4f
                else -> 1.35f
            }
            motion.boost = false
            motion.clearSwipe()
            if (!wasBoosting) {
                SfxPlayer.play(SfxKind.BOOST)
            }
            wasBoosting = true
            val lineCount = when (vehicle) {
                VehicleType.TRUCK -> 12
                VehicleType.MOTORCYCLE -> 24
                else -> 18
            }
            repeat(lineCount) {
                speedLines += SpeedLine(
                    x = rng.nextFloat(),
                    y = rng.nextFloat() * 0.9f,
                    len = 40f + rng.nextFloat() * 90f,
                    life = 0.4f + rng.nextFloat() * 0.45f
                )
            }
        } else if (boostTimer <= 0f) {
            wasBoosting = false
        }
        boostTimer = (boostTimer - dt).coerceAtLeast(0f)
        val boostSpeed = vehicle.boostMult
        val targetSpeed = if (boostTimer > 0f) boostSpeed else 1f + if (!motion.touching) 0.15f else 0f
        speed = MathUtils.smoothDamp(speed, targetSpeed, dt, 0.14f)
        scroll += speed * dt * 1.8f

        val t = ((carY - 0.48f) / 0.52f).coerceIn(0f, 1f)
        val roadHalf = MathUtils.lerp(w * 0.12f, w * 0.42f, t)
        val cx = w * 0.5f + carLane * roadHalf * 0.7f
        val cy = carY * h
        trail += TrailPoint(cx, cy + 18f, 0.55f, MathUtils.lerp(0.55f, 1.15f, t))
        val tit = trail.iterator()
        while (tit.hasNext()) {
            val p = tit.next()
            p.life -= dt * 1.6f
            p.y += speed * 40f * dt
            if (p.life <= 0f) tit.remove()
        }
        while (trail.size > 40) trail.removeAt(0)

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
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.45f,
            config.backgroundTop.toInt(), 0xFF2A1838.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, bgPaint)

        drawCity(canvas, motion)
        drawRoad(canvas)
        drawDashes(canvas)
        drawNeonTrail(canvas)

        val trailCol = vehicle.trailColor.toInt()
        for (s in speedLines) {
            val a = (s.life * 200).toInt().coerceIn(0, 200)
            linePaint.color = ColorUtils.withAlpha(trailCol, a)
            linePaint.strokeWidth = 2.5f
            val sx = s.x * w
            val sy = s.y * h
            canvas.drawLine(sx, sy, sx, sy + s.len * speed, linePaint)
        }

        drawVehicle(canvas, carLane, carY, boostTimer > 0f)

        if (boostTimer > 0f) {
            val pulse = boostTimer.coerceIn(0f, 1f)
            glowPaint.shader = RadialGradient(
                w * 0.5f, h * carY, w * 0.7f,
                Color.TRANSPARENT,
                ColorUtils.withAlpha(trailCol, (45 * pulse).toInt()),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)
        }
    }

    private fun drawNeonTrail(canvas: Canvas) {
        val primary = vehicle.trailColor.toInt()
        val secondary = when (vehicle) {
            VehicleType.SPORTS_CAR -> 0xFFFF2D95.toInt()
            VehicleType.TRUCK -> 0xFFFF6600.toInt()
            VehicleType.MOTORCYCLE -> 0xFFFFEE55.toInt()
            VehicleType.HELICOPTER -> 0xFFAAFFDD.toInt()
        }
        for (p in trail) {
            val a = (p.life * 180).toInt().coerceIn(0, 180)
            glowPaint.shader = RadialGradient(
                p.x, p.y, 22f * p.scale,
                ColorUtils.withAlpha(primary, a),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(p.x, p.y, 22f * p.scale, glowPaint)
            carPaint.color = ColorUtils.withAlpha(secondary, a / 2)
            canvas.drawCircle(p.x, p.y, 5f * p.scale, carPaint)
        }
    }

    private fun drawCity(canvas: Canvas, motion: MotionState) {
        val parallaxFar = motion.tiltX * 14f
        for ((i, b) in buildings.withIndex()) {
            if (i % 2 == 0) continue
            val left = b.x * w + parallaxFar - b.width * w * 0.4f
            val top = h * 0.48f - b.height * h * 0.7f
            cityPaint.color = Color.rgb(b.shade - 10, b.shade - 10, b.shade)
            canvas.drawRect(left, top, left + b.width * w * 0.8f, h * 0.52f, cityPaint)
        }
        val parallax = motion.tiltX * 28f
        for (b in buildings) {
            val left = b.x * w + parallax - b.width * w * 0.5f
            val top = h * 0.48f - b.height * h
            val right = left + b.width * w
            val bottom = h * 0.52f
            cityPaint.color = Color.rgb(b.shade, b.shade, b.shade + 10)
            canvas.drawRect(left, top, right, bottom, cityPaint)
            cityPaint.color = ColorUtils.withAlpha(0xFFFFE088.toInt(), 170)
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
        glowPaint.shader = LinearGradient(
            0f, h * 0.4f, 0f, h * 0.52f,
            ColorUtils.withAlpha(0xFFFF6688.toInt(), 45),
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
            linePaint.color = ColorUtils.withAlpha(0xFF888899.toInt(), 100)
            canvas.drawLine(cx - roadHalf * 0.45f, y, cx - roadHalf * 0.45f, y + dashH * 0.6f, linePaint)
            canvas.drawLine(cx + roadHalf * 0.45f, y, cx + roadHalf * 0.45f, y + dashH * 0.6f, linePaint)
            linePaint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), 180)
        }
    }

    private fun drawVehicle(canvas: Canvas, lane: Float, yNorm: Float, boosting: Boolean) {
        val t = ((yNorm - 0.48f) / 0.52f).coerceIn(0f, 1f)
        val roadHalf = MathUtils.lerp(w * 0.12f, w * 0.42f, t)
        val cx = w * 0.5f + lane * roadHalf * 0.7f
        val cy = yNorm * h
        val scale = MathUtils.lerp(0.55f, 1.15f, t)
        val bob = sin(time * 14f * speed) * 1.8f

        canvas.save()
        canvas.translate(cx, cy + bob)
        canvas.scale(scale, scale)

        when (vehicle) {
            VehicleType.SPORTS_CAR -> drawSportsCar(canvas, boosting)
            VehicleType.TRUCK -> drawTruck(canvas, boosting)
            VehicleType.MOTORCYCLE -> drawMotorcycle(canvas, boosting)
            VehicleType.HELICOPTER -> drawHelicopter(canvas, boosting)
        }

        canvas.restore()
    }

    private fun drawSportsCar(canvas: Canvas, boosting: Boolean) {
        carPaint.color = ColorUtils.withAlpha(Color.BLACK, 90)
        canvas.drawOval(-40f, 20f, 40f, 32f, carPaint)
        drawHeadlightBeams(canvas)
        path.reset()
        path.moveTo(-34f, 10f)
        path.lineTo(-30f, -4f)
        path.lineTo(-14f, -20f)
        path.lineTo(8f, -22f)
        path.lineTo(30f, -6f)
        path.lineTo(36f, 10f)
        path.lineTo(28f, 18f)
        path.lineTo(-28f, 18f)
        path.close()
        carPaint.color = if (boosting) 0xFFFF2D6A.toInt() else vehicle.bodyColor.toInt()
        canvas.drawPath(path, carPaint)
        drawUnderglow(canvas, boosting)
        carPaint.color = 0xFF121820.toInt()
        path.reset()
        path.moveTo(-10f, -4f)
        path.lineTo(-4f, -18f)
        path.lineTo(10f, -18f)
        path.lineTo(14f, -4f)
        path.close()
        canvas.drawPath(path, carPaint)
        drawHeadlights(canvas)
        if (boosting) drawBoostFlame(canvas)
        carPaint.color = 0xFF1A1A22.toInt()
        canvas.drawCircle(-24f, 16f, 8f, carPaint)
        canvas.drawCircle(24f, 16f, 8f, carPaint)
        carPaint.color = vehicle.trailColor.toInt()
        canvas.drawCircle(-24f, 16f, 3f, carPaint)
        canvas.drawCircle(24f, 16f, 3f, carPaint)
    }

    private fun drawTruck(canvas: Canvas, boosting: Boolean) {
        carPaint.color = ColorUtils.withAlpha(Color.BLACK, 90)
        canvas.drawOval(-48f, 22f, 48f, 36f, carPaint)
        // Cab
        path.reset()
        path.moveTo(-20f, 12f)
        path.lineTo(-18f, -18f)
        path.lineTo(8f, -20f)
        path.lineTo(14f, 12f)
        path.close()
        carPaint.color = if (boosting) 0xFFFF6622.toInt() else vehicle.bodyColor.toInt()
        canvas.drawPath(path, carPaint)
        // Trailer box
        carPaint.color = if (boosting) 0xFFCC5522.toInt() else 0xFFDD7744.toInt()
        canvas.drawRoundRect(-44f, -8f, -16f, 18f, 4f, 4f, carPaint)
        carPaint.color = 0xFF1A2030.toInt()
        canvas.drawRect(-12f, -16f, 6f, -2f, carPaint)
        drawUnderglow(canvas, boosting)
        carPaint.color = 0xFFFFF8D0.toInt()
        canvas.drawCircle(10f, -2f, 5f, carPaint)
        if (boosting) drawBoostFlame(canvas)
        carPaint.color = 0xFF1A1A22.toInt()
        canvas.drawCircle(-36f, 18f, 9f, carPaint)
        canvas.drawCircle(-22f, 18f, 9f, carPaint)
        canvas.drawCircle(6f, 18f, 9f, carPaint)
        carPaint.color = vehicle.trailColor.toInt()
        canvas.drawCircle(-36f, 18f, 3f, carPaint)
        canvas.drawCircle(-22f, 18f, 3f, carPaint)
        canvas.drawCircle(6f, 18f, 3f, carPaint)
    }

    private fun drawMotorcycle(canvas: Canvas, boosting: Boolean) {
        carPaint.color = ColorUtils.withAlpha(Color.BLACK, 80)
        canvas.drawOval(-28f, 18f, 28f, 28f, carPaint)
        // Wheels
        carPaint.color = 0xFF222230.toInt()
        canvas.drawCircle(-18f, 14f, 11f, carPaint)
        canvas.drawCircle(18f, 14f, 11f, carPaint)
        carPaint.color = vehicle.trailColor.toInt()
        canvas.drawCircle(-18f, 14f, 4f, carPaint)
        canvas.drawCircle(18f, 14f, 4f, carPaint)
        // Slim body / tank
        path.reset()
        path.moveTo(-14f, 6f)
        path.lineTo(-6f, -14f)
        path.lineTo(10f, -12f)
        path.lineTo(16f, 4f)
        path.lineTo(4f, 10f)
        path.lineTo(-10f, 10f)
        path.close()
        carPaint.color = if (boosting) 0xFFFF44AA.toInt() else vehicle.bodyColor.toInt()
        canvas.drawPath(path, carPaint)
        // Rider silhouette
        carPaint.color = 0xFF1A1520.toInt()
        canvas.drawCircle(0f, -18f, 7f, carPaint)
        canvas.drawRoundRect(-6f, -12f, 8f, 4f, 3f, 3f, carPaint)
        // Handlebar
        linePaint.color = 0xFFCCCCDD.toInt()
        linePaint.strokeWidth = 3f
        canvas.drawLine(8f, -10f, 20f, -18f, linePaint)
        drawUnderglow(canvas, boosting)
        if (boosting) {
            glowPaint.shader = RadialGradient(
                -22f, 14f, 36f,
                ColorUtils.withAlpha(0xFFFFEE55.toInt(), 160),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(-22f, 14f, 36f, glowPaint)
        }
    }

    private fun drawHelicopter(canvas: Canvas, boosting: Boolean) {
        carPaint.color = ColorUtils.withAlpha(Color.BLACK, 70)
        canvas.drawOval(-36f, 16f, 36f, 28f, carPaint)
        // Cabin bubble
        carPaint.color = if (boosting) 0xFF88FFBB.toInt() else vehicle.bodyColor.toInt()
        path.reset()
        path.moveTo(-22f, 6f)
        path.quadTo(-24f, -18f, 0f, -22f)
        path.quadTo(18f, -16f, 22f, 4f)
        path.lineTo(14f, 14f)
        path.lineTo(-14f, 14f)
        path.close()
        canvas.drawPath(path, carPaint)
        carPaint.color = ColorUtils.withAlpha(0xFF102018.toInt(), 180)
        canvas.drawOval(-10f, -16f, 12f, 2f, carPaint)
        // Tail boom
        carPaint.color = vehicle.bodyColor.toInt()
        canvas.drawRoundRect(-48f, -4f, -18f, 4f, 3f, 3f, carPaint)
        canvas.drawRect(-50f, -12f, -44f, 10f, carPaint)
        // Main rotor
        linePaint.color = ColorUtils.withAlpha(0xFFEEFFEE.toInt(), if (boosting) 200 else 140)
        linePaint.strokeWidth = 3f
        val r = 42f + if (boosting) 6f else 0f
        val a = rotor
        canvas.drawLine(
            kotlin.math.cos(a.toDouble()).toFloat() * r,
            -24f + kotlin.math.sin(a.toDouble()).toFloat() * 4f,
            -kotlin.math.cos(a.toDouble()).toFloat() * r,
            -24f - kotlin.math.sin(a.toDouble()).toFloat() * 4f,
            linePaint
        )
        canvas.drawLine(
            kotlin.math.cos(a + 1.57).toFloat() * r * 0.9f,
            -24f,
            -kotlin.math.cos(a + 1.57).toFloat() * r * 0.9f,
            -24f,
            linePaint
        )
        carPaint.color = 0xFF334433.toInt()
        canvas.drawCircle(0f, -24f, 4f, carPaint)
        // Skids
        linePaint.color = 0xFF889988.toInt()
        linePaint.strokeWidth = 3f
        canvas.drawLine(-18f, 16f, 18f, 16f, linePaint)
        canvas.drawLine(-14f, 10f, -14f, 16f, linePaint)
        canvas.drawLine(14f, 10f, 14f, 16f, linePaint)
        drawUnderglow(canvas, boosting)
        if (boosting) {
            glowPaint.shader = RadialGradient(
                0f, 20f, 50f,
                ColorUtils.withAlpha(vehicle.trailColor.toInt(), 140),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(0f, 22f, 50f, glowPaint)
        }
    }

    private fun drawHeadlightBeams(canvas: Canvas) {
        glowPaint.shader = LinearGradient(
            -22f, -20f, -22f, -90f,
            ColorUtils.withAlpha(0xFFFFF0A0.toInt(), 90),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(-26f, -4f)
        path.lineTo(-40f, -85f)
        path.lineTo(-8f, -85f)
        path.close()
        canvas.drawPath(path, glowPaint)
        path.reset()
        path.moveTo(26f, -4f)
        path.lineTo(8f, -85f)
        path.lineTo(40f, -85f)
        path.close()
        canvas.drawPath(path, glowPaint)
    }

    private fun drawHeadlights(canvas: Canvas) {
        carPaint.color = 0xFFFFF8D0.toInt()
        canvas.drawCircle(-22f, -2f, 5f, carPaint)
        canvas.drawCircle(22f, -2f, 5f, carPaint)
        glowPaint.shader = RadialGradient(
            -22f, -2f, 16f,
            ColorUtils.withAlpha(0xFFFFF0A0.toInt(), 160),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(-22f, -2f, 16f, glowPaint)
        glowPaint.shader = RadialGradient(
            22f, -2f, 16f,
            ColorUtils.withAlpha(0xFFFFF0A0.toInt(), 160),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(22f, -2f, 16f, glowPaint)
    }

    private fun drawUnderglow(canvas: Canvas, boosting: Boolean) {
        val c = if (boosting) vehicle.trailColor.toInt() else vehicle.bodyColor.toInt()
        glowPaint.shader = RadialGradient(
            0f, 18f, 50f,
            ColorUtils.withAlpha(c, 120),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 20f, 48f, glowPaint)
    }

    private fun drawBoostFlame(canvas: Canvas) {
        glowPaint.shader = RadialGradient(
            0f, 24f, 44f,
            ColorUtils.withAlpha(0xFFFF6600.toInt(), 180),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 26f, 44f, glowPaint)
    }
}
