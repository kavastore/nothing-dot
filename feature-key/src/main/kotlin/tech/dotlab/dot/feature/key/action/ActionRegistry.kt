package tech.dotlab.dot.feature.key.action

import android.content.Context
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

    fun byId(id: String): KeyAction? =
        OpenAppAction.packageOf(id)?.let(::OpenAppAction) ?: all.firstOrNull { it.id == id }

    /** Human-readable title for a binding, resolving the app label for `open_app:` ids. */
    fun displayTitle(context: Context, id: String): String? {
        OpenAppAction.packageOf(id)?.let { pkg ->
            return runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
        }
        return byId(id)?.title
    }
}
