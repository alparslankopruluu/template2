package com.screenmotion.app.data

/**
 * Mock Pro entitlement — SharedPreferences only.
 * No Play Billing / AdMob. Toggle via paywall mock purchase or debug switch.
 */
object ProEntitlement {
    const val PREFS_KEY = "is_pro"

    fun isPro(repo: ConfigRepository): Boolean = repo.isPro

    fun requiresUnlock(vehicle: VehicleType): Boolean = vehicle.requiresPro
}
