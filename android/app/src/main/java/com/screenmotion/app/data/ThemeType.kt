package com.screenmotion.app.data

enum class ThemeType(val displayName: String, val emoji: String) {
    SPACE("Space", "🚀"),
    AQUARIUM("Aquarium", "🐠"),
    VEHICLE("Vehicle", "🏎️");

    companion object {
        fun fromName(name: String?): ThemeType =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SPACE
    }
}
