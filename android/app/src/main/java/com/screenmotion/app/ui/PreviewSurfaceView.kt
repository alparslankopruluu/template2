package com.screenmotion.app.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.motion.MotionController
import com.screenmotion.app.render.SceneFactory
import com.screenmotion.app.render.SceneRenderer

/**
 * In-app interactive preview mirroring the live wallpaper scenes.
 */
class PreviewSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var renderer: SceneRenderer = SceneFactory.create(ThemeType.SPACE)
    private var config: ThemeConfig = ThemeConfig.forTheme(ThemeType.SPACE)
    private val motion = MotionController(context)
    private var thread: Thread? = null
    @Volatile private var running = false

    init {
        holder.addCallback(this)
        isClickable = true
        isFocusable = true
    }

    fun setTheme(type: ThemeType) {
        config = ThemeConfig.forTheme(type)
        renderer = SceneFactory.create(type)
        if (width > 0 && height > 0) {
            renderer.onSizeChanged(width, height)
            renderer.reset()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        motion.start()
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.onSizeChanged(width, height)
        renderer.reset()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
        motion.stop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motion.onTouch(event)
        return true
    }

    private fun startLoop() {
        if (running) return
        running = true
        thread = Thread({
            var last = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                last = now
                motion.update(dt)
                renderer.update(dt, motion.state, config)
                var canvas: Canvas? = null
                try {
                    canvas = getHolder().lockCanvas()
                    if (canvas != null) {
                        synchronized(getHolder()) {
                            renderer.draw(canvas, motion.state, config)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    if (canvas != null) {
                        try {
                            getHolder().unlockCanvasAndPost(canvas)
                        } catch (_: Exception) {
                        }
                    }
                }
                try {
                    Thread.sleep(16)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "Preview-Render").also { it.start() }
    }

    private fun stopLoop() {
        running = false
        thread?.interrupt()
        try {
            thread?.join(400)
        } catch (_: InterruptedException) {
        }
        thread = null
    }
}
