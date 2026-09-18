package com.screenmotion.app.data

/**
 * Selectable vehicle variants for the Vehicle theme.
 * Trail/body colors muted toward cinematic (no neon magenta/cyber).
 */
enum class VehicleType(
    val displayName: String,
    val displayNameTr: String,
    val emoji: String,
    val trailColor: Long,
    val bodyColor: Long,
    val boostMult: Float
) {
    SPORTS_CAR(
        displayName = "Sports",
        displayNameTr = "Spor",
        emoji = "🏎️",
        trailColor = 0xFF5BA3D9,
        bodyColor = 0xFF7BB8E0,
        boostMult = 3.4f
    ),
    TRUCK(
        displayName = "Truck",
        displayNameTr = "Kamyon",
        emoji = "🚛",
        trailColor = 0xFFD4A574,
        bodyColor = 0xFFC48A5A,
        boostMult = 2.4f
    ),
    MOTORCYCLE(
        displayName = "Bike",
        displayNameTr = "Motor",
        emoji = "🏍️",
        trailColor = 0xFFC47A8A,
        bodyColor = 0xFFA86890,
        boostMult = 4.0f
    ),
    HELICOPTER(
        displayName = "Heli",
        displayNameTr = "Helikopter",
        emoji = "🚁",
        trailColor = 0xFF7AAA8A,
        bodyColor = 0xFF6B9B7A,
        boostMult = 2.8f
    );

    companion object {
        fun fromName(name: String?): VehicleType =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SPORTS_CAR
    }
}
