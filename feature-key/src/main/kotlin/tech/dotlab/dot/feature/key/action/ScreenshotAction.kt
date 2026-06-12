package tech.dotlab.dot.feature.key.action

import android.accessibilityservice.AccessibilityService
import android.content.Context
import tech.dotlab.dot.core.action.KeyAction

/**
 * Takes a screenshot via [AccessibilityService.performGlobalAction]. Works because the service
 * passes itself as the [Context] when running an action.
 */
class ScreenshotAction : KeyAction {
    override val id: String = "screenshot"
    override val title: String = "Скриншот"
    override val icon: Int = android.R.drawable.ic_menu_save
    override val needsPermissions: List<String> = emptyList()

    override fun run(context: Context) {
        (context as? AccessibilityService)
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}
