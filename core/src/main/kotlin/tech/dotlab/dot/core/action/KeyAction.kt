package tech.dotlab.dot.core.action

import android.content.Context

/**
 * A single thing the Essential Key can trigger (launch app, torch, screenshot, show a Glyph art…).
 *
 * Actions are plugins: adding one means a new implementation registered in `ActionRegistry`.
 * The core never knows about concrete actions — this is the second modularity seam.
 */
interface KeyAction {
    val id: String
    val title: String

    /** Drawable resource id used in the action catalog. */
    val icon: Int

    /** Android permissions that must be granted before [run] can work. */
    val needsPermissions: List<String>

    fun run(context: Context)
}
