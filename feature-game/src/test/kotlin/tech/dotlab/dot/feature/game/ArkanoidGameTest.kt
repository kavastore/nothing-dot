package tech.dotlab.dot.feature.game

import tech.dotlab.dot.device.ShapeMask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArkanoidGameTest {

    private fun newGame(size: Int = 25) = ArkanoidGame(size, ShapeMask.circle(size))

    @Test
    fun `starts ready with full lives and no score`() {
        val game = newGame()
        assertEquals(ArkanoidGame.Status.READY, game.status)
        assertEquals(3, game.lives)
        assertEquals(0, game.score)
    }

    @Test
    fun `primary action launches the ball`() {
        val game = newGame()
        game.primaryAction()
        assertEquals(ArkanoidGame.Status.PLAYING, game.status)
    }

    @Test
    fun `tick is a no-op while ready`() {
        val game = newGame()
        game.tick(0.1)
        assertEquals(ArkanoidGame.Status.READY, game.status)
        assertEquals(0, game.score)
    }

    @Test
    fun `draw produces a masked frame of the right size`() {
        val size = 25
        val game = newGame(size)
        val frame = game.draw()
        assertEquals(size, frame.size)
        // Corner cells are outside the round mask and must stay dark.
        assertEquals(0, frame.brightnessAt(0, 0))
        assertTrue(frame.brightness.any { it > 0 })
    }

    @Test
    fun `restart resets score and lives`() {
        val game = newGame()
        game.primaryAction()
        repeat(300) { game.tick(0.066) }
        game.restart()
        assertEquals(3, game.lives)
        assertEquals(0, game.score)
    }

    @Test
    fun `simulated play does not crash and keeps a valid status`() {
        val game = newGame()
        game.primaryAction()
        repeat(500) {
            game.setPaddle(12.0)
            game.tick(0.066)
        }
        assertTrue(game.status in ArkanoidGame.Status.entries)
        assertTrue(game.score >= 0)
    }
}
