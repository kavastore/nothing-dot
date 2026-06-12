package tech.dotlab.dot.feature.key.action

import tech.dotlab.dot.core.action.KeyAction

/**
 * Catalog of available [KeyAction]s — the second modularity seam. Adding an action = one new
 * implementation listed here; the service and UI discover it by id.
 */
object ActionRegistry {

    val all: List<KeyAction> = listOf(
        TorchAction(),
        OpenCameraAction(),
        ScreenshotAction(),
        SilentModeAction(),
        ShowGlyphArtAction(),
    )

    fun byId(id: String): KeyAction? = all.firstOrNull { it.id == id }
}
