package tech.dotlab.dot.feature.editor.toy

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.runBlocking
import tech.dotlab.dot.core.model.GlyphButtonEvent
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.feature.editor.data.EditorDatabase
import tech.dotlab.dot.feature.editor.data.PixelArtRepository
import tech.dotlab.dot.feature.editor.domain.EditorDocument
import tech.dotlab.dot.matrix.GlyphToyService

/**
 * Glyph Toy that shows the user's drawings on the matrix.
 *
 * - **AOD** (4a Pro and AOD-capable Phone 3): advances one frame per minute on `EVENT_AOD`.
 * - **Glyph Button** (Phone 3): a long press cycles through saved drawings; a press-and-hold
 *   toggles play/pause of the current drawing's animation.
 */
class PixelArtToyService : GlyphToyService() {

    private val main = Handler(Looper.getMainLooper())
    private var docs: List<EditorDocument> = emptyList()
    private var docIndex = 0
    private var frameIndex = 0
    private var playing = false

    private val animTick = object : Runnable {
        override fun run() {
            if (!playing) return
            advanceFrame()
            drawCurrent()
            val duration = docs.getOrNull(docIndex)?.frames?.getOrNull(frameIndex)?.durationMs ?: 500
            main.postDelayed(this, duration.coerceAtLeast(MIN_FRAME_MS).toLong())
        }
    }

    override fun onToyConnected(profile: DeviceProfile) {
        loadGallery(profile)
        drawCurrent()
    }

    override fun onAodTick() {
        advanceFrame()
        drawCurrent()
    }

    override fun onButton(event: GlyphButtonEvent) {
        when (event) {
            GlyphButtonEvent.LONG_PRESS -> nextDocument()
            GlyphButtonEvent.HOLD_DOWN -> togglePlay()
            GlyphButtonEvent.HOLD_UP -> Unit
        }
    }

    override fun onToyStopped() {
        playing = false
        main.removeCallbacks(animTick)
    }

    private fun loadGallery(profile: DeviceProfile) {
        val repo = PixelArtRepository(EditorDatabase.get(applicationContext).pixelArtDao())
        val all = runCatching { runBlocking { repo.allDocuments() } }.getOrNull().orEmpty()
        docs = all.ifEmpty {
            listOf(EditorDocument.blank("empty", profile.id, profile.matrixSize))
        }
        val activeId = runCatching { runBlocking { repo.activeAodDocument()?.id } }.getOrNull()
        docIndex = docs.indexOfFirst { it.id == activeId }.takeIf { it >= 0 } ?: 0
        frameIndex = 0
    }

    private fun drawCurrent() {
        val frame = docs.getOrNull(docIndex)?.frames?.getOrNull(frameIndex)?.frame ?: return
        render(frame)
    }

    private fun advanceFrame() {
        val frames = docs.getOrNull(docIndex)?.frames ?: return
        if (frames.isEmpty()) return
        frameIndex = (frameIndex + 1) % frames.size
    }

    private fun nextDocument() {
        if (docs.isEmpty()) return
        docIndex = (docIndex + 1) % docs.size
        frameIndex = 0
        drawCurrent()
    }

    private fun togglePlay() {
        playing = !playing
        main.removeCallbacks(animTick)
        if (playing) main.post(animTick)
    }

    private companion object {
        const val MIN_FRAME_MS = 60
    }
}
