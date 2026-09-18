package com.screenmotion.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.screenmotion.app.data.VehicleType
import com.screenmotion.app.util.ColorUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Photorealistic / 3D-looking vehicle drawing via layered Canvas shading
 * (body gradient, specular highlight, ambient occlusion, chrome, glass).
 * Optionally composites PNG sprites from assets/themes/vehicles/ when present.
 */
class VehiclePainter(context: Context? = null) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private val sprites: Map<VehicleType, Bitmap?> = if (context != null) {
        VehicleType.entries.associateWith { loadSprite(context, it) }
    } else emptyMap()

    private fun loadSprite(context: Context, type: VehicleType): Bitmap? {
        val name = when (type) {
            VehicleType.SPORTS_CAR -> "themes/vehicles/sports_car.png"
            VehicleType.TRUCK -> "themes/vehicles/truck.png"
            VehicleType.MOTORCYCLE -> "themes/vehicles/motorcycle.png"
            VehicleType.HELICOPTER -> "themes/vehicles/helicopter.png"
        }
        return try {
            context.assets.open(name).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun draw(
        canvas: Canvas,
        type: VehicleType,
        boosting: Boolean,
        rotor: Float = 0f
    ) {
        val sprite = sprites[type]
        if (sprite != null && !sprite.isRecycled) {
            drawSprite(canvas, sprite, type, boosting, rotor)
            return
        }
        when (type) {
            VehicleType.SPORTS_CAR -> drawSportsCar(canvas, type, boosting)
            VehicleType.TRUCK -> drawTruck(canvas, type, boosting)
            VehicleType.MOTORCYCLE -> drawMotorcycle(canvas, type, boosting)
            VehicleType.HELICOPTER -> drawHelicopter(canvas, type, boosting, rotor)
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        bmp: Bitmap,
        type: VehicleType,
        boosting: Boolean,
        rotor: Float
    ) {
        // Soft ground contact shadow
        glow.shader = RadialGradient(
            0f, 26f, 56f,
            ColorUtils.withAlpha(Color.BLACK, 120),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(-48f, 18f, 48f, 34f, glow)

        val targetW = when (type) {
            VehicleType.TRUCK -> 110f
            VehicleType.HELICOPTER -> 100f
            VehicleType.MOTORCYCLE -> 78f
            else -> 96f
        }
        val scale = targetW / bmp.width
        val tw = bmp.width * scale
        val th = bmp.height * scale
        rect.set(-tw / 2f, -th * 0.72f, tw / 2f, th * 0.28f)
        paint.shader = null
        paint.alpha = 255
        canvas.drawBitmap(bmp, null, rect, paint)

        if (type == VehicleType.HELICOPTER) {
            drawRotorDisk(canvas, rotor, boosting)
        }
        if (boosting) {
            glow.shader = RadialGradient(
                0f, 28f, 52f,
                ColorUtils.withAlpha(type.trailColor.toInt(), 150),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(0f, 30f, 52f, glow)
        }
    }

    private fun bodyBase(type: VehicleType, boosting: Boolean): Int =
        if (boosting) ColorUtils.lerpColor(type.bodyColor.toInt(), 0xFFE8E8EC.toInt(), 0.15f)
        else type.bodyColor.toInt()

    private fun shade(c: Int, factor: Float): Int {
        val r = (Color.red(c) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(c) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(c) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun lighten(c: Int, amount: Int): Int {
        return Color.rgb(
            (Color.red(c) + amount).coerceIn(0, 255),
            (Color.green(c) + amount).coerceIn(0, 255),
            (Color.blue(c) + amount).coerceIn(0, 255)
        )
    }

    private fun drawGroundShadow(canvas: Canvas, w: Float, h: Float = 14f) {
        glow.shader = RadialGradient(
            0f, h * 0.3f, w * 0.55f,
            ColorUtils.withAlpha(Color.BLACK, 110),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(-w / 2f, 16f, w / 2f, 16f + h, glow)
    }

    private fun fillBodyGradient(canvas: Canvas, body: Path, base: Int) {
        val dark = shade(base, 0.45f)
        val mid = shade(base, 0.85f)
        val lit = lighten(base, 55)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, -28f, 0f, 22f,
            intArrayOf(lit, mid, dark),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(body, paint)
        // Specular streak along roof / upper panel
        paint.shader = LinearGradient(
            -30f, -18f, 30f, -6f,
            ColorUtils.withAlpha(Color.WHITE, 90),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(body, paint)
        // Soft rim light on leading edge
        stroke.shader = null
        stroke.color = ColorUtils.withAlpha(lighten(base, 80), 140)
        stroke.strokeWidth = 1.6f
        canvas.drawPath(body, stroke)
        paint.shader = null
    }

    private fun drawGlass(canvas: Canvas, glass: Path) {
        paint.shader = LinearGradient(
            0f, -22f, 0f, 4f,
            intArrayOf(0xFF9EB8D0.toInt(), 0xFF1A2430.toInt(), 0xFF0A1018.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(glass, paint)
        // Glass specular
        paint.shader = LinearGradient(
            -8f, -20f, 12f, -4f,
            ColorUtils.withAlpha(Color.WHITE, 70),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(glass, paint)
        paint.shader = null
        stroke.color = ColorUtils.withAlpha(0xFFD0E4F0.toInt(), 80)
        stroke.strokeWidth = 1f
        canvas.drawPath(glass, stroke)
    }

    private fun drawWheel(canvas: Canvas, cx: Float, cy: Float, r: Float, accent: Int) {
        // Tire
        paint.shader = RadialGradient(
            cx - r * 0.25f, cy - r * 0.25f, r * 1.1f,
            0xFF3A3A40.toInt(), 0xFF0C0C10.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, paint)
        // Rim
        paint.shader = RadialGradient(
            cx - r * 0.2f, cy - r * 0.3f, r * 0.7f,
            0xFFD8DCE4.toInt(), 0xFF4A4E58.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 0.58f, paint)
        // Hub
        paint.shader = null
        paint.color = shade(accent, 0.7f)
        canvas.drawCircle(cx, cy, r * 0.22f, paint)
        paint.color = ColorUtils.withAlpha(Color.WHITE, 50)
        canvas.drawCircle(cx - r * 0.15f, cy - r * 0.18f, r * 0.12f, paint)
    }

    private fun drawHeadlamp(canvas: Canvas, cx: Float, cy: Float, r: Float = 5.5f) {
        glow.shader = RadialGradient(
            cx, cy, r * 3.2f,
            ColorUtils.withAlpha(0xFFFFF4C8.toInt(), 160),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 3.2f, glow)
        paint.shader = RadialGradient(
            cx - 1.5f, cy - 1.5f, r,
            0xFFFFFFF0.toInt(), 0xFFE8D090.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
        paint.color = ColorUtils.withAlpha(Color.WHITE, 200)
        canvas.drawCircle(cx - 1.2f, cy - 1.4f, r * 0.28f, paint)
    }

    private fun drawSportsCar(canvas: Canvas, type: VehicleType, boosting: Boolean) {
        val base = bodyBase(type, boosting)
        drawGroundShadow(canvas, 86f)
        // Headlight beams
        glow.shader = LinearGradient(
            -22f, -4f, -22f, -95f,
            ColorUtils.withAlpha(0xFFFFF0C0.toInt(), 70),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(-26f, -2f); path.lineTo(-42f, -92f); path.lineTo(-6f, -92f); path.close()
        canvas.drawPath(path, glow)
        path.reset()
        path.moveTo(26f, -2f); path.lineTo(6f, -92f); path.lineTo(42f, -92f); path.close()
        canvas.drawPath(path, glow)

        // Lower chassis / side skirt (AO)
        paint.shader = null
        paint.color = shade(base, 0.28f)
        canvas.drawRoundRect(-32f, 8f, 32f, 20f, 4f, 4f, paint)

        // Main body
        path.reset()
        path.moveTo(-36f, 12f)
        path.cubicTo(-38f, 2f, -34f, -6f, -28f, -10f)
        path.cubicTo(-18f, -22f, -8f, -26f, 4f, -26f)
        path.cubicTo(18f, -26f, 28f, -16f, 34f, -6f)
        path.cubicTo(40f, 2f, 40f, 10f, 36f, 16f)
        path.cubicTo(28f, 22f, -28f, 22f, -36f, 12f)
        path.close()
        fillBodyGradient(canvas, path, base)

        // Hood crease / panel line
        stroke.color = ColorUtils.withAlpha(shade(base, 0.35f), 160)
        stroke.strokeWidth = 1.2f
        canvas.drawLine(-18f, 2f, 22f, 2f, stroke)

        // Cabin glass
        path.reset()
        path.moveTo(-12f, -2f)
        path.lineTo(-6f, -20f)
        path.cubicTo(0f, -24f, 8f, -24f, 14f, -18f)
        path.lineTo(18f, -2f)
        path.close()
        drawGlass(canvas, path)

        // Side mirror chrome
        paint.shader = LinearGradient(-30f, -8f, -22f, 0f, 0xFFE8ECF0.toInt(), 0xFF6A7078.toInt(), Shader.TileMode.CLAMP)
        canvas.drawOval(-32f, -10f, -22f, -2f, paint)
        canvas.drawOval(22f, -10f, 32f, -2f, paint)

        drawHeadlamp(canvas, -24f, 0f, 5f)
        drawHeadlamp(canvas, 24f, 0f, 5f)

        // Taillights
        paint.shader = RadialGradient(-30f, 10f, 6f, 0xFFFF6A6A.toInt(), 0xFF802020.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(-34f, 6f, -26f, 14f, 2f, 2f, paint)
        paint.shader = RadialGradient(30f, 10f, 6f, 0xFFFF6A6A.toInt(), 0xFF802020.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(26f, 6f, 34f, 14f, 2f, 2f, paint)

        drawWheel(canvas, -22f, 18f, 9f, type.trailColor.toInt())
        drawWheel(canvas, 22f, 18f, 9f, type.trailColor.toInt())

        if (boosting) drawBoostHeat(canvas, type)
    }

    private fun drawTruck(canvas: Canvas, type: VehicleType, boosting: Boolean) {
        val base = bodyBase(type, boosting)
        drawGroundShadow(canvas, 100f, 16f)

        // Trailer box — metallic panels
        paint.shader = LinearGradient(
            -46f, -12f, -46f, 20f,
            lighten(0xFF8A9098.toInt(), 30),
            shade(0xFF8A9098.toInt(), 0.55f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(-46f, -10f, -14f, 18f, 3f, 3f, paint)
        // Panel ribs
        stroke.color = ColorUtils.withAlpha(0xFF2A2E34.toInt(), 120)
        stroke.strokeWidth = 1.5f
        for (x in listOf(-40f, -32f, -24f)) {
            canvas.drawLine(x, -8f, x, 16f, stroke)
        }
        // Trailer specular
        paint.shader = LinearGradient(
            -46f, -10f, -14f, -10f,
            ColorUtils.withAlpha(Color.WHITE, 55),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(-46f, -10f, -14f, 0f, 3f, 3f, paint)

        // Cab body
        path.reset()
        path.moveTo(-16f, 14f)
        path.lineTo(-18f, -8f)
        path.cubicTo(-16f, -22f, -4f, -26f, 8f, -24f)
        path.lineTo(16f, -8f)
        path.lineTo(18f, 14f)
        path.close()
        fillBodyGradient(canvas, path, base)

        // Windshield
        path.reset()
        path.moveTo(-10f, -6f)
        path.lineTo(-6f, -20f)
        path.lineTo(10f, -18f)
        path.lineTo(12f, -4f)
        path.close()
        drawGlass(canvas, path)

        drawHeadlamp(canvas, 12f, 0f, 5.5f)

        // Chrome bumper
        paint.shader = LinearGradient(0f, 12f, 0f, 20f, 0xFFE8ECF0.toInt(), 0xFF5A6068.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(-8f, 12f, 20f, 20f, 2f, 2f, paint)

        drawWheel(canvas, -36f, 20f, 10f, type.trailColor.toInt())
        drawWheel(canvas, -22f, 20f, 10f, type.trailColor.toInt())
        drawWheel(canvas, 8f, 20f, 10f, type.trailColor.toInt())

        if (boosting) drawBoostHeat(canvas, type)
    }

    private fun drawMotorcycle(canvas: Canvas, type: VehicleType, boosting: Boolean) {
        val base = bodyBase(type, boosting)
        drawGroundShadow(canvas, 64f, 12f)

        drawWheel(canvas, -20f, 14f, 12f, type.trailColor.toInt())
        drawWheel(canvas, 20f, 14f, 12f, type.trailColor.toInt())

        // Swingarm / frame chrome
        stroke.shader = null
        stroke.color = 0xFFB0B8C0.toInt()
        stroke.strokeWidth = 3.5f
        canvas.drawLine(-16f, 10f, 14f, 6f, stroke)
        canvas.drawLine(8f, 4f, 18f, -8f, stroke)

        // Fuel tank (rounded 3D)
        path.reset()
        path.moveTo(-12f, 6f)
        path.cubicTo(-14f, -4f, -8f, -16f, 2f, -16f)
        path.cubicTo(12f, -16f, 16f, -6f, 14f, 4f)
        path.cubicTo(8f, 12f, -6f, 12f, -12f, 6f)
        path.close()
        fillBodyGradient(canvas, path, base)

        // Seat
        paint.shader = LinearGradient(0f, -2f, 0f, 10f, 0xFF2A2A30.toInt(), 0xFF0E0E12.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(-8f, 0f, 6f, 10f, 3f, 3f, paint)

        // Rider (helmet + jacket) with subtle shading
        paint.shader = RadialGradient(-1f, -20f, 9f, 0xFF4A5560.toInt(), 0xFF1A2028.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(0f, -18f, 8f, paint)
        paint.shader = LinearGradient(-6f, -12f, 8f, 4f, 0xFF2A3540.toInt(), 0xFF101820.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(-7f, -12f, 9f, 4f, 4f, 4f, paint)
        // Visor specular
        paint.color = ColorUtils.withAlpha(0xFF88AACC.toInt(), 180)
        paint.shader = null
        canvas.drawOval(-4f, -22f, 5f, -16f, paint)

        // Exhaust glow when boosting
        if (boosting) {
            glow.shader = RadialGradient(
                -24f, 12f, 40f,
                ColorUtils.withAlpha(0xFFD4A060.toInt(), 150),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(-24f, 12f, 40f, glow)
        }
    }

    private fun drawHelicopter(canvas: Canvas, type: VehicleType, boosting: Boolean, rotor: Float) {
        val base = bodyBase(type, boosting)
        drawGroundShadow(canvas, 78f, 12f)

        // Tail boom
        paint.shader = LinearGradient(-50f, 0f, -16f, 0f, shade(base, 0.55f), base, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(-50f, -5f, -16f, 5f, 3f, 3f, paint)
        // Tail fin
        path.reset()
        path.moveTo(-50f, -4f)
        path.lineTo(-56f, -18f)
        path.lineTo(-46f, -4f)
        path.close()
        fillBodyGradient(canvas, path, shade(base, 0.8f))
        // Tail rotor disc
        stroke.color = ColorUtils.withAlpha(Color.WHITE, if (boosting) 120 else 70)
        stroke.strokeWidth = 2f
        canvas.drawLine(-52f, -16f, -52f, 8f, stroke)

        // Cabin shell
        path.reset()
        path.moveTo(-24f, 10f)
        path.cubicTo(-28f, -4f, -22f, -22f, 0f, -26f)
        path.cubicTo(20f, -24f, 28f, -8f, 26f, 8f)
        path.cubicTo(18f, 18f, -16f, 18f, -24f, 10f)
        path.close()
        fillBodyGradient(canvas, path, base)

        // Bubble canopy glass
        path.reset()
        path.addOval(-12f, -22f, 14f, 2f, Path.Direction.CW)
        drawGlass(canvas, path)

        // Landing skids (chrome)
        stroke.color = 0xFFA8B0B8.toInt()
        stroke.strokeWidth = 3.2f
        canvas.drawLine(-20f, 18f, 22f, 18f, stroke)
        canvas.drawLine(-16f, 10f, -16f, 18f, stroke)
        canvas.drawLine(16f, 10f, 16f, 18f, stroke)

        drawRotorDisk(canvas, rotor, boosting)
        paint.shader = RadialGradient(0f, -26f, 5f, 0xFFD0D4D8.toInt(), 0xFF404448.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(0f, -26f, 4.5f, paint)

        if (boosting) {
            glow.shader = RadialGradient(
                0f, 22f, 56f,
                ColorUtils.withAlpha(type.trailColor.toInt(), 130),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(0f, 24f, 56f, glow)
        }
    }

    private fun drawRotorDisk(canvas: Canvas, rotor: Float, boosting: Boolean) {
        val r = if (boosting) 48f else 44f
        // Motion-blurred rotor disc
        glow.shader = RadialGradient(
            0f, -26f, r,
            ColorUtils.withAlpha(0xFFEEF2F6.toInt(), if (boosting) 55 else 35),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, -26f, r, glow)
        stroke.color = ColorUtils.withAlpha(0xFFF0F4F8.toInt(), if (boosting) 170 else 110)
        stroke.strokeWidth = 2.8f
        val a = rotor
        canvas.drawLine(
            cos(a.toDouble()).toFloat() * r,
            -26f + sin(a.toDouble()).toFloat() * 3f,
            -cos(a.toDouble()).toFloat() * r,
            -26f - sin(a.toDouble()).toFloat() * 3f,
            stroke
        )
        canvas.drawLine(
            cos(a + 1.57).toFloat() * r * 0.92f,
            -26f,
            -cos(a + 1.57).toFloat() * r * 0.92f,
            -26f,
            stroke
        )
    }

    private fun drawBoostHeat(canvas: Canvas, type: VehicleType) {
        glow.shader = RadialGradient(
            0f, 26f, 48f,
            ColorUtils.withAlpha(type.trailColor.toInt(), 150),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 28f, 48f, glow)
        glow.shader = RadialGradient(
            0f, 30f, 28f,
            ColorUtils.withAlpha(0xFFFFE8C0.toInt(), 100),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 30f, 28f, glow)
    }

    companion object {
        fun assetNameFor(type: VehicleType): String = when (type) {
            VehicleType.SPORTS_CAR -> "sports_car.png"
            VehicleType.TRUCK -> "truck.png"
            VehicleType.MOTORCYCLE -> "motorcycle.png"
            VehicleType.HELICOPTER -> "helicopter.png"
        }

        fun lockAssetNameFor(type: VehicleType): String = when (type) {
            VehicleType.SPORTS_CAR -> "themes/vehicles/sports_car_lock.png"
            VehicleType.TRUCK -> "themes/vehicles/truck_lock.png"
            VehicleType.MOTORCYCLE -> "themes/vehicles/motorcycle_lock.png"
            VehicleType.HELICOPTER -> "themes/vehicles/helicopter_lock.png"
        }
    }
}
