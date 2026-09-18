package com.screenmotion.app.motion

/**
 * Normalized tilt in roughly [-1, 1] plus touch state shared with renderers.
 */
data class MotionState(
    var tiltX: Float = 0f,
    var tiltY: Float = 0f,
    var touchX: Float = -1f,
    var touchY: Float = -1f,
    var touching: Boolean = false,
    var swipeVx: Float = 0f,
    var swipeVy: Float = 0f,
    var longPress: Boolean = false,
    var boost: Boolean = false
) {
    fun clearSwipe() {
        swipeVx = 0f
        swipeVy = 0f
    }
}
