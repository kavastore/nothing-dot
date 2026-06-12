package tech.dotlab.dot.feature.game.toy

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import tech.dotlab.dot.core.model.GlyphButtonEvent
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.feature.game.ArkanoidGame
import tech.dotlab.dot.matrix.GlyphToyService

/**
 * Arkanoid played entirely on the rear Glyph Matrix (Phone 3).
 *
 * - Tilt (accelerometer) moves the paddle left/right.
 * - Glyph Button (long press → [GlyphButtonEvent.LONG_PRESS]) launches the ball, then fires lasers.
 *
 * The game model is pure Kotlin; this service only wires sensors, the Glyph Button and a render loop
 * driving [GlyphToyService.render] (which uses `setMatrixFrame`).
 */
class ArkanoidToyService : GlyphToyService(), SensorEventListener {

    private val loop = Handler(Looper.getMainLooper())
    private var game: ArkanoidGame? = null
    private var sensorManager: SensorManager? = null
    private var running = false

    private val frameTick = object : Runnable {
        override fun run() {
            if (!running) return
            game?.let {
                it.tick(DT_SECONDS)
                render(it.draw())
            }
            loop.postDelayed(this, FRAME_MS)
        }
    }

    override fun onToyConnected(profile: DeviceProfile) {
        game = ArkanoidGame(profile.matrixSize, profile.shapeMask)
        startSensors()
        running = true
        loop.post(frameTick)
    }

    override fun onButton(event: GlyphButtonEvent) {
        if (event == GlyphButtonEvent.LONG_PRESS) game?.primaryAction()
    }

    override fun onToyStopped() {
        running = false
        loop.removeCallbacks(frameTick)
        stopSensors()
        game = null
    }

    private fun startSensors() {
        val manager = getSystemService(SENSOR_SERVICE) as? SensorManager ?: return
        sensorManager = manager
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, loop)
        }
    }

    private fun stopSensors() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val g = game ?: return
        val center = g.size / 2.0
        // Tilt on the X axis maps to paddle position; normalised by gravity and scaled.
        val target = center + (event.values[0] / SensorManager.GRAVITY_EARTH) * center * SENSITIVITY
        g.setPaddle(target)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val FRAME_MS = 66L
        const val DT_SECONDS = 0.066
        const val SENSITIVITY = 1.6
    }
}
