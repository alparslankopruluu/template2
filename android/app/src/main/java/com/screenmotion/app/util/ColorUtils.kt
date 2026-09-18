package com.screenmotion.app.util

import android.graphics.Color

object ColorUtils {
    fun lerpColor(c1: Int, c2: Int, t: Float): Int {
        val a = Color.alpha(c1) + ((Color.alpha(c2) - Color.alpha(c1)) * t).toInt()
        val r = Color.red(c1) + ((Color.red(c2) - Color.red(c1)) * t).toInt()
        val g = Color.green(c1) + ((Color.green(c2) - Color.green(c1)) * t).toInt()
        val b = Color.blue(c1) + ((Color.blue(c2) - Color.blue(c1)) * t).toInt()
        return Color.argb(a, r, g, b)
    }

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
