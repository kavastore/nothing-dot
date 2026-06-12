package tech.dotlab.dot.feature.key.action

import android.content.Context
import android.hardware.camera2.CameraManager
import tech.dotlab.dot.core.action.KeyAction

/**
 * Toggles the rear flashlight. Uses [CameraManager.setTorchMode], which needs no CAMERA permission.
 */
class TorchAction : KeyAction {
    override val id: String = "torch"
    override val title: String = "Фонарик"
    override val icon: Int = android.R.drawable.ic_menu_camera
    override val needsPermissions: List<String> = emptyList()

    override fun run(context: Context) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        torchOn = !torchOn
        runCatching { manager.setTorchMode(cameraId, torchOn) }
    }

    private companion object {
        @Volatile
        var torchOn: Boolean = false
    }
}
