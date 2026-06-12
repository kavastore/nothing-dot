package tech.dotlab.dot.feature.key.action

import android.content.Context
import android.content.Intent
import tech.dotlab.dot.core.action.KeyAction

class OpenCameraAction : KeyAction {
    override val id: String = "open_camera"
    override val title: String = "Открыть камеру"
    override val icon: Int = android.R.drawable.ic_menu_camera
    override val needsPermissions: List<String> = emptyList()

    override fun run(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            action = "android.media.action.STILL_IMAGE_CAMERA"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
