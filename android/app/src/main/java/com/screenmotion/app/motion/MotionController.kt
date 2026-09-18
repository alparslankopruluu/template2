package com.screenmotion.app.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import com.screenmotion.app.util.MathUtils
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * TYPE_GAME_ROTATION_VECTOR preferred; accelerometer fallback.
 * Touch: drag, swipe velocity, long-press detection.
 */
class MotionController(context: Context) : SensorEventListener {

    val state = MotionState()

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val accelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val useRotation = rotationSensor != null
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTouchTime = 0L
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f

    private var targetTiltX = 0f
    private var targetTiltY = 0f

    fun start() {
        if (useRotation && rotationSensor != null) {
            sensorManager.registerListener(
                this, rotationSensor, SensorManager.SENSOR_DELAY_GAME
            )
        } else if (accelSensor != null) {
            sensorManager.registerListener(
                this, accelSensor, SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** Call each frame to ease tilt toward sensor targets. */
    fun update(dt: Float) {
        state.tiltX = MathUtils.smoothDamp(state.tiltX, targetTiltX, dt, 0.12f)
        state.tiltY = MathUtils.smoothDamp(state.tiltY, targetTiltY, dt, 0.12f)
        // Decay swipe
        state.swipeVx *= (1f - MathUtils.clamp(dt * 4f, 0f, 1f))
        state.swipeVy *= (1f - MathUtils.clamp(dt * 4f, 0f, 1f))
        if (abs(state.swipeVx) < 2f) state.swipeVx = 0f
        if (abs(state.swipeVy) < 2f) state.swipeVy = 0f
        // Long-press while holding still
        if (state.touching && !state.longPress) {
            val held = System.currentTimeMillis() - downTime
            val dx = state.touchX - downX
            val dy = state.touchY - downY
            if (held > 450 && dx * dx + dy * dy < 40f * 40f) {
                state.longPress = true
            }
        }
    }

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                downX = event.x
                downY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                lastTouchTime = System.currentTimeMillis()
                state.touchX = event.x
                state.touchY = event.y
                state.touching = true
                state.longPress = false
                state.boost = false
            }
            MotionEvent.ACTION_MOVE -> {
                val now = System.currentTimeMillis()
                val dtMs = (now - lastTouchTime).coerceAtLeast(1)
                val vx = (event.x - lastTouchX) / dtMs * 1000f
                val vy = (event.y - lastTouchY) / dtMs * 1000f
                state.touchX = event.x
                state.touchY = event.y
                // Fast swipe → boost flag for vehicle theme
                val speed = sqrt(vx * vx + vy * vy)
                if (speed > 1800f) {
                    state.boost = true
                    state.swipeVx = vx
                    state.swipeVy = vy
                } else if (speed > 600f) {
                    state.swipeVx = vx
                    state.swipeVy = vy
                }
                lastTouchX = event.x
                lastTouchY = event.y
                lastTouchTime = now
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val now = System.currentTimeMillis()
                val dtMs = (now - lastTouchTime).coerceAtLeast(1)
                val vx = (event.x - lastTouchX) / dtMs * 1000f
                val vy = (event.y - lastTouchY) / dtMs * 1000f
                val speed = sqrt(vx * vx + vy * vy)
                if (speed > 500f) {
                    state.swipeVx = vx
                    state.swipeVy = vy
                    if (speed > 1800f) state.boost = true
                }
                state.touching = false
                state.longPress = false
                state.touchX = -1f
                state.touchY = -1f
            }
        }
        return true
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (useRotation &&
            (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                event.sensor.type == Sensor.TYPE_ROTATION_VECTOR)
        ) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            // pitch / roll → normalized tilt
            targetTiltX = MathUtils.clamp(orientation[2] / 0.6f, -1f, 1f)
            targetTiltY = MathUtils.clamp(-orientation[1] / 0.6f, -1f, 1f)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            targetTiltX = MathUtils.clamp(-event.values[0] / 9.8f, -1f, 1f)
            targetTiltY = MathUtils.clamp(event.values[1] / 9.8f - 0.4f, -1f, 1f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
