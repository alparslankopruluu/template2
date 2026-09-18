package com.screenmotion.app

import android.app.Application
import com.screenmotion.app.audio.SfxPlayer

class ScreenMotionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SfxPlayer.init(this)
    }

    override fun onTerminate() {
        SfxPlayer.release()
        super.onTerminate()
    }

    companion object {
        @JvmStatic
        lateinit var instance: ScreenMotionApp
            private set
    }
}
