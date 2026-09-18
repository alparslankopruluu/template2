package com.screenmotion.app.data

import android.content.Context
import android.content.SharedPreferences

class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedTheme: ThemeType
        get() = ThemeType.fromName(prefs.getString(KEY_THEME, ThemeType.SPACE.name))
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var selectedVehicle: VehicleType
        get() = VehicleType.fromName(prefs.getString(KEY_VEHICLE, VehicleType.SPORTS_CAR.name))
        set(value) = prefs.edit().putString(KEY_VEHICLE, value.name).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var soundMuted: Boolean
        get() = prefs.getBoolean(KEY_SOUND_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_MUTED, value).apply()

    fun themeConfig(): ThemeConfig = ThemeConfig.forTheme(selectedTheme)

    companion object {
        private const val PREFS_NAME = "screenmotion_prefs"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_VEHICLE = "selected_vehicle"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_SOUND_MUTED = "sound_muted"

        @Volatile
        private var instance: ConfigRepository? = null

        fun get(context: Context): ConfigRepository =
            instance ?: synchronized(this) {
                instance ?: ConfigRepository(context.applicationContext).also { instance = it }
            }
    }
}
