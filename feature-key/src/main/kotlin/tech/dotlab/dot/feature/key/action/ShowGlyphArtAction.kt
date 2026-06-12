package tech.dotlab.dot.feature.key.action

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.runBlocking
import tech.dotlab.dot.core.action.KeyAction
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.feature.key.data.KeyPrefs
import tech.dotlab.dot.matrix.MatrixRendererFactory

/**
 * Shows the user-picked image (from [KeyPrefs]) on the Glyph Matrix for the configured duration,
 * falling back to a built-in heart when nothing was chosen yet.
 */
class ShowGlyphArtAction : KeyAction {
    override val id: String = "show_glyph_art"
    override val title: String = "Показать рисунок"
    override val icon: Int = android.R.drawable.ic_menu_gallery
    override val needsPermissions: List<String> = emptyList()

    override fun run(context: Context) {
        val profile = (DeviceRegistry.resolveCurrent() as? DeviceSupport.Supported)?.profile ?: return
        if (!profile.supportsAppMatrix) return

        val config = runBlocking { KeyPrefs(context).current() }
        val saved = config.showFrame?.takeIf { it.size == profile.matrixSize }
        val frame = saved ?: heartFrame(profile.matrixSize)
        val durationMs = config.showDurationSec.coerceIn(1, 30) * 1000L

        val renderer = MatrixRendererFactory.create(context, profile)
        renderer.showFrame(frame)
        Handler(Looper.getMainLooper()).postDelayed({ renderer.close() }, durationMs)
    }

    private fun heartFrame(size: Int): LogicalFrame {
        if (size != HEART.size) {
            // Unknown matrix size: light every cell as a safe fallback.
            return LogicalFrame(size, IntArray(size * size) { 255 })
        }
        val brightness = IntArray(size * size)
        HEART.forEachIndexed { y, row ->
            row.forEachIndexed { x, c -> if (c == '#') brightness[y * size + x] = 255 }
        }
        return LogicalFrame(size, brightness)
    }

    private companion object {
        val HEART = listOf(
            ".............",
            "..###..###...",
            ".###########.",
            ".###########.",
            "..#########..",
            "...#######...",
            "....#####....",
            ".....###.....",
            "......#......",
            ".............",
            ".............",
            ".............",
            ".............",
        )
    }
}
