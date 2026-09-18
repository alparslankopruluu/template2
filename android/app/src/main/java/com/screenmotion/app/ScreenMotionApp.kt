package com.screenmotion.app

import android.app.Application
import com.screenmotion.app.audio.SfxPlayer

class ScreenMotionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SfxPlayer.init(this)
    }

    override fun onTerminate() {
        SfxPlayer.release()
        super.onTerminate()
    }
}
