package com.screenmotion.app.data

/**
 * Per-theme tuning knobs. Canvas-first; muted cinematic palette (Apple-inspired).
 */
data class ThemeConfig(
    val type: ThemeType,
    val parallaxStrength: Float = 1f,
    val particleCount: Int = 80,
    val touchSensitivity: Float = 1f,
    val backgroundTop: Long = 0xFF0A0E1A,
    val backgroundBottom: Long = 0xFF1A1030
) {
    companion object {
        fun forTheme(type: ThemeType): ThemeConfig = when (type) {
            ThemeType.SPACE -> ThemeConfig(
                type = type,
                parallaxStrength = 1.35f,
                particleCount = 160,
                backgroundTop = 0xFF050508,
                backgroundBottom = 0xFF12182A
            )
            ThemeType.AQUARIUM -> ThemeConfig(
                type = type,
                parallaxStrength = 0.9f,
                particleCount = 70,
                backgroundTop = 0xFF061418,
                backgroundBottom = 0xFF0E2A32
            )
            ThemeType.VEHICLE -> ThemeConfig(
                type = type,
                parallaxStrength = 1.1f,
                particleCount = 50,
                backgroundTop = 0xFF0A0A0C,
                backgroundBottom = 0xFF16161A
            )
            ThemeType.NATURE -> ThemeConfig(
                type = type,
                parallaxStrength = 1.25f,
                particleCount = 90,
                backgroundTop = 0xFF0A1014,
                backgroundBottom = 0xFF152018
            )
        }
    }
}
