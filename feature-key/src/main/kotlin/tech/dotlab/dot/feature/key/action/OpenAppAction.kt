package tech.dotlab.dot.feature.key.action

import android.content.Context
import android.content.Intent
import tech.dotlab.dot.core.action.KeyAction

/**
 * Launches an installed app by its package. The package is encoded in the [id] as
 * `open_app:<package>`, so a gesture binding can point at any app without a separate stored map.
 */
class OpenAppAction(private val packageName: String) : KeyAction {
    override val id: String = "$ID_PREFIX$packageName"
    override val title: String = packageName
    override val icon: Int = android.R.drawable.sym_def_app_icon
    override val needsPermissions: List<String> = emptyList()

    override fun run(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    companion object {
        const val ID_PREFIX = "open_app:"

        fun packageOf(actionId: String): String? =
            if (actionId.startsWith(ID_PREFIX)) actionId.removePrefix(ID_PREFIX).ifBlank { null } else null
    }
}
