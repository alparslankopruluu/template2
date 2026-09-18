package com.screenmotion.app.util

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object MathUtils {
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun clamp(v: Float, lo: Float, hi: Float): Float = max(lo, min(hi, v))

    fun smoothDamp(current: Float, target: Float, dt: Float, smoothTime: Float): Float {
        val t = 1f - exp(-dt / max(0.0001f, smoothTime))
        return lerp(current, target, t)
    }

    fun easeOutCubic(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}
