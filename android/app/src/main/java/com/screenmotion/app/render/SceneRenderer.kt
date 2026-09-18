package com.screenmotion.app.render

import android.graphics.Canvas
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.motion.MotionState

/**
 * Canvas scene contract — swap for OpenGL later without changing wallpaper service.
 */
interface SceneRenderer {
    fun onSizeChanged(width: Int, height: Int)
    fun update(dt: Float, motion: MotionState, config: ThemeConfig)
    fun draw(canvas: Canvas, motion: MotionState, config: ThemeConfig)
    fun reset()
}
