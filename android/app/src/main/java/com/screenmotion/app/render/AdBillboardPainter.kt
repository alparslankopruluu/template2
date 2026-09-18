package com.screenmotion.app.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.screenmotion.app.R
import com.screenmotion.app.ScreenMotionApp
import com.screenmotion.app.data.ConfigRepository
import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.util.ColorUtils
import kotlin.math.sin

/**
 * Theme-embedded mock ad billboards — visible when !isPro, omitted when Pro.
 * Placed away from critical interactive UI (center / vehicle / mascots).
 */
object AdBillboardPainter {

    private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subtle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun drawIfFree(canvas: Canvas, w: Int, h: Int, theme: ThemeType, time: Float = 0f) {
        if (w <= 0 || h <= 0) return
        val ctx = try {
            ScreenMotionApp.instance
        } catch (_: Exception) {
            return
        }
        if (ConfigRepository.get(ctx).isPro) return

        val sponsored = ctx.getString(R.string.ad_sponsored)
        val brand = ctx.getString(R.string.ad_sample_brand)

        when (theme) {
            ThemeType.VEHICLE -> drawRoadside(canvas, w, h, sponsored, brand)
            ThemeType.SPACE -> drawSoftPanel(canvas, w, h, sponsored, brand, time)
            ThemeType.AQUARIUM -> drawFloatingSign(canvas, w, h, sponsored, brand, time)
            ThemeType.NATURE -> drawRoadSign(canvas, w, h, sponsored, brand)
        }
    }

    /** Roadside billboard — upper-right, above horizon / beside road. */
    private fun drawRoadside(canvas: Canvas, w: Int, h: Int, sponsored: String, brand: String) {
        val bw = w * 0.22f
        val bh = h * 0.11f
        val left = w * 0.72f
        val top = h * 0.28f

        // Pole
        subtle.color = ColorUtils.withAlpha(0xFF666677.toInt(), 200)
        subtle.strokeWidth = 4f
        canvas.drawLine(left + bw * 0.5f, top + bh, left + bw * 0.5f, h * 0.48f, subtle)

        panel.shader = LinearGradient(
            left, top, left, top + bh,
            0xFF3A4558.toInt(), 0xFF2A3344.toInt(),
            Shader.TileMode.CLAMP
        )
        rect.set(left, top, left + bw, top + bh)
        canvas.drawRoundRect(rect, 6f, 6f, panel)
        panel.shader = null
        panel.style = Paint.Style.STROKE
        panel.strokeWidth = 2f
        panel.color = ColorUtils.withAlpha(0xFF8899AA.toInt(), 160)
        canvas.drawRoundRect(rect, 6f, 6f, panel)
        panel.style = Paint.Style.FILL

        drawLabel(canvas, left + 8f, top + 14f, bw - 16f, sponsored, brand, 0xFFE8F0FF.toInt())
    }

    /** Soft translucent panel — upper-left corner of space. */
    private fun drawSoftPanel(canvas: Canvas, w: Int, h: Int, sponsored: String, brand: String, time: Float) {
        val bw = w * 0.28f
        val bh = h * 0.09f
        val left = w * 0.06f + sin(time * 0.4f) * 4f
        val top = h * 0.08f

        panel.shader = LinearGradient(
            left, top, left + bw, top + bh,
            ColorUtils.withAlpha(0xFF334466.toInt(), 140),
            ColorUtils.withAlpha(0xFF223355.toInt(), 110),
            Shader.TileMode.CLAMP
        )
        rect.set(left, top, left + bw, top + bh)
        canvas.drawRoundRect(rect, 14f, 14f, panel)
        panel.shader = null
        panel.style = Paint.Style.STROKE
        panel.strokeWidth = 1.5f
        panel.color = ColorUtils.withAlpha(0xFF88AADD.toInt(), 100)
        canvas.drawRoundRect(rect, 14f, 14f, panel)
        panel.style = Paint.Style.FILL

        drawLabel(canvas, left + 12f, top + 16f, bw - 24f, sponsored, brand, 0xFFCCDDFF.toInt())
    }

    /** Floating underwater sign — mid-right, bobbing. */
    private fun drawFloatingSign(canvas: Canvas, w: Int, h: Int, sponsored: String, brand: String, time: Float) {
        val bw = w * 0.24f
        val bh = h * 0.10f
        val left = w * 0.68f
        val top = h * 0.35f + sin(time * 1.1f) * 10f

        // Anchor line
        subtle.color = ColorUtils.withAlpha(0xFF88AACC.toInt(), 90)
        subtle.strokeWidth = 2f
        canvas.drawLine(left + bw * 0.5f, top + bh, left + bw * 0.5f, top + bh + h * 0.12f, subtle)

        panel.color = ColorUtils.withAlpha(0xFF1A4A55.toInt(), 170)
        rect.set(left, top, left + bw, top + bh)
        canvas.drawRoundRect(rect, 10f, 10f, panel)
        panel.style = Paint.Style.STROKE
        panel.strokeWidth = 2f
        panel.color = ColorUtils.withAlpha(0xFF66C8D8.toInt(), 140)
        canvas.drawRoundRect(rect, 10f, 10f, panel)
        panel.style = Paint.Style.FILL

        drawLabel(canvas, left + 10f, top + 16f, bw - 20f, sponsored, brand, 0xFFD0F4FF.toInt())
    }

    /** Nature road / trail sign — lower-left roadside. */
    private fun drawRoadSign(canvas: Canvas, w: Int, h: Int, sponsored: String, brand: String) {
        val bw = w * 0.20f
        val bh = h * 0.10f
        val left = w * 0.08f
        val top = h * 0.58f

        // Post
        subtle.color = ColorUtils.withAlpha(0xFF5A4A3A.toInt(), 220)
        subtle.strokeWidth = 5f
        canvas.drawLine(left + bw * 0.5f, top + bh, left + bw * 0.5f, top + bh + h * 0.14f, subtle)

        // Diamond-ish / rounded wood panel
        panel.shader = LinearGradient(
            left, top, left, top + bh,
            0xFF4A6A48.toInt(), 0xFF3A5438.toInt(),
            Shader.TileMode.CLAMP
        )
        rect.set(left, top, left + bw, top + bh)
        canvas.drawRoundRect(rect, 4f, 4f, panel)
        panel.shader = null
        panel.style = Paint.Style.STROKE
        panel.strokeWidth = 2.5f
        panel.color = ColorUtils.withAlpha(0xFFC8B080.toInt(), 180)
        canvas.drawRoundRect(rect, 4f, 4f, panel)
        panel.style = Paint.Style.FILL

        drawLabel(canvas, left + 8f, top + 14f, bw - 16f, sponsored, brand, 0xFFE8F0D8.toInt())
    }

    private fun drawLabel(
        canvas: Canvas,
        x: Float,
        y: Float,
        maxW: Float,
        sponsored: String,
        brand: String,
        color: Int
    ) {
        text.color = ColorUtils.withAlpha(color, 160)
        text.textSize = 11f.coerceAtMost(maxW * 0.12f)
        text.isFakeBoldText = false
        canvas.drawText(sponsored, x, y, text)

        text.color = ColorUtils.withAlpha(color, 220)
        text.textSize = 14f.coerceAtMost(maxW * 0.16f)
        text.isFakeBoldText = true
        canvas.drawText(brand, x, y + text.textSize + 4f, text)
    }
}
