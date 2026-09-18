package com.screenmotion.app.data

/**
 * Selectable vehicle variants for the Vehicle theme.
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
        trailColor = 0xFF00F0FF,
        bodyColor = 0xFF2EE6FF,
        boostMult = 3.4f
    ),
    TRUCK(
        displayName = "Truck",
        displayNameTr = "Kamyon",
        emoji = "🚛",
        trailColor = 0xFFFFAA33,
        bodyColor = 0xFFFF8844,
        boostMult = 2.4f
    ),
    MOTORCYCLE(
        displayName = "Bike",
        displayNameTr = "Motor",
        emoji = "🏍️",
        trailColor = 0xFFFF2D95,
        bodyColor = 0xFFE040FB,
        boostMult = 4.0f
    ),
    HELICOPTER(
        displayName = "Heli",
        displayNameTr = "Helikopter",
        emoji = "🚁",
        trailColor = 0xFF88FFAA,
        bodyColor = 0xFF66EE99,
        boostMult = 2.8f
    );

    companion object {
        fun fromName(name: String?): VehicleType =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SPORTS_CAR
    }
}
