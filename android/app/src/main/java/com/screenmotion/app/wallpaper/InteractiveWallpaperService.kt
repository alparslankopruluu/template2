package com.screenmotion.app.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.screenmotion.app.data.ConfigRepository
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.motion.MotionController
import com.screenmotion.app.render.SceneFactory
import com.screenmotion.app.render.SceneRenderer

/**
 * Live wallpaper with ~60fps Canvas render loop on SurfaceHolder.
 */
class InteractiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = InteractiveEngine()

    inner class InteractiveEngine : Engine() {

        private var renderer: SceneRenderer? = null
        private var motion: MotionController? = null
        private var config: ThemeConfig = ThemeConfig.forTheme(
            ConfigRepository.get(this@InteractiveWallpaperService).selectedTheme
        )
        private var renderThread: Thread? = null
        @Volatile private var running = false
        private var width = 0
        private var height = 0

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            motion = MotionController(this@InteractiveWallpaperService).also { it.start() }
            reloadTheme()
        }

        override fun onDestroy() {
            stopLoop()
            motion?.stop()
            motion = null
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                // Reload theme in case user changed it in the app
                reloadTheme()
                motion?.start()
                startLoop()
            } else {
                stopLoop()
                motion?.stop()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
            renderer?.onSizeChanged(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopLoop()
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent) {
            motion?.onTouch(event)
            super.onTouchEvent(event)
        }

        private fun reloadTheme() {
            val repo = ConfigRepository.get(this@InteractiveWallpaperService)
            config = repo.themeConfig()
            renderer = SceneFactory.create(repo.selectedTheme, repo.selectedVehicle).also {
                if (width > 0 && height > 0) it.onSizeChanged(width, height)
                it.reset()
            }
        }

        private fun startLoop() {
            if (running) return
            running = true
            renderThread = Thread({
                var last = System.nanoTime()
                val frameNs = 1_000_000_000L / 60L
                while (running) {
                    val now = System.nanoTime()
                    val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    last = now
                    drawFrame(dt)
                    val sleep = frameNs - (System.nanoTime() - now)
                    if (sleep > 1_000_000L) {
                        try {
                            Thread.sleep(sleep / 1_000_000L)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }, "ScreenMotion-Render").also { it.start() }
        }

        private fun stopLoop() {
            running = false
            renderThread?.interrupt()
            try {
                renderThread?.join(500)
            } catch (_: InterruptedException) {
            }
            renderThread = null
        }

        private fun drawFrame(dt: Float) {
            val holder = surfaceHolder ?: return
            val m = motion ?: return
            val r = renderer ?: return
            m.update(dt)
            r.update(dt, m.state, config)
            var canvas = null as android.graphics.Canvas?
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        r.draw(canvas, m.state, config)
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
