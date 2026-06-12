package tech.dotlab.dot.feature.game

import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure-Kotlin Arkanoid for a square Glyph Matrix (tuned for 25×25, works for any size).
 *
 * No Android dependencies so it is unit-testable on the JVM. The toy service feeds it paddle
 * position (from tilt) and button presses, advances it with [tick], and renders [draw].
 *
 * Coordinates are in cells; velocities in cells/second. Brightness levels are monochrome 0..255.
 */
class ArkanoidGame(
    val size: Int,
    private val mask: ShapeMask,
) {
    enum class Status { READY, PLAYING, GAME_OVER, WON }

    var status: Status = Status.READY
        private set
    var score: Int = 0
        private set
    var lives: Int = START_LIVES
        private set

    private val paddleHalf: Double = (size / 8.0).coerceAtLeast(2.0)
    private val paddleY: Double = size - 2.0
    private var paddleX: Double = size / 2.0

    private var ballX = paddleX
    private var ballY = paddleY - 1.0
    private var ballVx = 0.0
    private var ballVy = 0.0
    private val speed: Double = size * 0.55

    /** Live bricks as packed indices y * size + x. */
    private val bricks = HashSet<Int>()

    /** Active laser bolts; each is [x, y] moving up. */
    private val lasers = ArrayList<DoubleArray>()
    private var laserCooldown = 0.0

    init {
        resetLevel()
    }

    /** Sets the paddle centre (e.g. from tilt), clamped to the field. */
    fun setPaddle(centerX: Double) {
        paddleX = centerX.coerceIn(paddleHalf, size - paddleHalf)
        if (status == Status.READY) ballX = paddleX
    }

    /** Glyph Button: launches the ball when idle, otherwise fires a laser. */
    fun primaryAction() {
        when (status) {
            Status.READY -> {
                status = Status.PLAYING
                ballVx = speed * LAUNCH_VX_RATIO
                ballVy = -speed
            }
            Status.PLAYING -> fireLaser()
            Status.GAME_OVER, Status.WON -> restart()
        }
    }

    fun restart() {
        score = 0
        lives = START_LIVES
        status = Status.READY
        resetLevel()
        resetBall()
    }

    fun tick(dtSeconds: Double) {
        if (status != Status.PLAYING) return
        updateLasers(dtSeconds)
        moveBall(dtSeconds)
        if (bricks.isEmpty()) status = Status.WON
    }

    private fun moveBall(dt: Double) {
        ballX += ballVx * dt
        ballY += ballVy * dt

        if (ballX < 0.5) { ballX = 0.5; ballVx = abs(ballVx) }
        if (ballX > size - 0.5) { ballX = size - 0.5; ballVx = -abs(ballVx) }
        if (ballY < 0.5) { ballY = 0.5; ballVy = abs(ballVy) }

        if (ballVy > 0 && ballY >= paddleY - 0.5 && ballY <= paddleY + 0.5 &&
            ballX >= paddleX - paddleHalf - 0.5 && ballX <= paddleX + paddleHalf + 0.5
        ) {
            ballY = paddleY - 0.5
            ballVy = -abs(ballVy)
            val offset = (ballX - paddleX) / paddleHalf // -1..1
            ballVx = (speed * offset).coerceIn(-speed, speed)
        }

        if (ballY > size - 0.5) {
            lives--
            if (lives <= 0) status = Status.GAME_OVER else { status = Status.READY; resetBall() }
            return
        }

        hitBrick(ballX.roundToInt(), ballY.roundToInt())?.let { ballVy = -ballVy }
    }

    private fun updateLasers(dt: Double) {
        if (laserCooldown > 0) laserCooldown -= dt
        val iterator = lasers.iterator()
        while (iterator.hasNext()) {
            val bolt = iterator.next()
            bolt[1] -= speed * LASER_SPEED_RATIO * dt
            if (bolt[1] < 0) { iterator.remove(); continue }
            if (hitBrick(bolt[0].roundToInt(), bolt[1].roundToInt()) != null) iterator.remove()
        }
    }

    private fun fireLaser() {
        if (laserCooldown > 0) return
        laserCooldown = LASER_COOLDOWN_S
        lasers.add(doubleArrayOf(paddleX, paddleY - 1.0))
    }

    /** Removes a brick at the cell if present and scores it; returns the index hit or null. */
    private fun hitBrick(x: Int, y: Int): Int? {
        if (x !in 0 until size || y !in 0 until size) return null
        val key = y * size + x
        if (bricks.remove(key)) {
            score += BRICK_SCORE
            return key
        }
        return null
    }

    private fun resetBall() {
        ballX = paddleX
        ballY = paddleY - 1.0
        ballVx = 0.0
        ballVy = 0.0
    }

    private fun resetLevel() {
        bricks.clear()
        lasers.clear()
        val top = (size * 0.12).roundToInt().coerceAtLeast(2)
        val rows = (size * 0.20).roundToInt().coerceAtLeast(2)
        for (y in top until top + rows) {
            for (x in 0 until size) {
                if (mask.isOn(x, y)) bricks.add(y * size + x)
            }
        }
    }

    /** Renders the current state to a [LogicalFrame], respecting the round [mask]. */
    fun draw(): LogicalFrame {
        val out = IntArray(size * size)
        for (key in bricks) out[key] = BRICK_BRIGHTNESS
        for (bolt in lasers) {
            val lx = bolt[0].roundToInt()
            val ly = bolt[1].roundToInt()
            if (lx in 0 until size && ly in 0 until size) out[ly * size + lx] = LASER_BRIGHTNESS
        }
        val py = paddleY.roundToInt()
        val from = (paddleX - paddleHalf).roundToInt().coerceAtLeast(0)
        val to = (paddleX + paddleHalf).roundToInt().coerceAtMost(size - 1)
        for (x in from..to) out[py * size + x] = FULL
        val bx = ballX.roundToInt().coerceIn(0, size - 1)
        val by = ballY.roundToInt().coerceIn(0, size - 1)
        out[by * size + bx] = FULL

        for (i in out.indices) {
            val x = i % size
            val y = i / size
            if (!mask.isOn(x, y)) out[i] = 0
        }
        return LogicalFrame(size, out)
    }

    private companion object {
        const val START_LIVES = 3
        const val BRICK_SCORE = 10
        const val FULL = 255
        const val BRICK_BRIGHTNESS = 110
        const val LASER_BRIGHTNESS = 200
        const val LAUNCH_VX_RATIO = 0.45
        const val LASER_SPEED_RATIO = 1.6
        const val LASER_COOLDOWN_S = 0.35
    }
}
