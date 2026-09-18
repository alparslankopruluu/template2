package com.screenmotion.app.data

enum class ThemeType(val displayName: String, val emoji: String, val subtitle: String) {
    SPACE("Space", "🚀", "Stars · meteors · friendly ship"),
    AQUARIUM("Aquarium", "🐠", "Curious fish · bubbles · current"),
    VEHICLE("Vehicle", "🏎️", "Neon drive · tilt lanes · boost"),
    NATURE("Nature", "🌿", "Clouds · birds · wind · hills");

    companion object {
        fun fromName(name: String?): ThemeType =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SPACE
    }
}
