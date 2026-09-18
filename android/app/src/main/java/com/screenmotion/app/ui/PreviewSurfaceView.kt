package com.screenmotion.app.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.screenmotion.app.audio.SfxKind
import com.screenmotion.app.audio.SfxPlayer
import com.screenmotion.app.data.ConfigRepository
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.data.VehicleType
import com.screenmotion.app.motion.MotionController
import com.screenmotion.app.render.SceneFactory
import com.screenmotion.app.render.SceneRenderer
import com.screenmotion.app.render.VehicleSceneRenderer

/**
 * In-app interactive preview mirroring the live wallpaper scenes.
 */
class PreviewSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val repo = ConfigRepository.get(context)
    private var renderer: SceneRenderer = SceneFactory.create(ThemeType.SPACE)
    private var config: ThemeConfig = ThemeConfig.forTheme(ThemeType.SPACE)
    private val motion = MotionController(context)
    private var thread: Thread? = null
    @Volatile private var running = false
    private var touchDownPlayed = false

    init {
        holder.addCallback(this)
        isClickable = true
        isFocusable = true
        SfxPlayer.init(context)
    }

    fun setTheme(type: ThemeType) {
        config = ThemeConfig.forTheme(type)
        renderer = SceneFactory.create(type, repo.selectedVehicle)
        if (width > 0 && height > 0) {
            renderer.onSizeChanged(width, height)
            renderer.reset()
        }
    }

    fun setVehicle(type: VehicleType) {
        repo.selectedVehicle = type
        val v = renderer
        if (v is VehicleSceneRenderer) {
            v.setVehicle(type)
        } else if (repo.selectedTheme == ThemeType.VEHICLE) {
            setTheme(ThemeType.VEHICLE)
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownPlayed = false
                SfxPlayer.play(context, SfxKind.TOUCH_SPLASH)
                touchDownPlayed = true
            }
        }
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
