package com.screenmotion.app.data

/**
 * Per-theme tuning knobs. Canvas-first; OpenGL can map the same values later.
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
                backgroundTop = 0xFF050510,
                backgroundBottom = 0xFF1A0A2E
            )
            ThemeType.AQUARIUM -> ThemeConfig(
                type = type,
                parallaxStrength = 0.9f,
                particleCount = 70,
                backgroundTop = 0xFF021820,
                backgroundBottom = 0xFF0A3A4A
            )
            ThemeType.VEHICLE -> ThemeConfig(
                type = type,
                parallaxStrength = 1.1f,
                particleCount = 50,
                backgroundTop = 0xFF0C0C12,
                backgroundBottom = 0xFF1A1520
            )
            ThemeType.NATURE -> ThemeConfig(
                type = type,
                parallaxStrength = 1.25f,
                particleCount = 90,
                backgroundTop = 0xFF0A1218,
                backgroundBottom = 0xFF1A2E24
            )
        }
    }
}
