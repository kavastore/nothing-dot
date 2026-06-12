package tech.dotlab.dot.feature.editor.ui

import android.content.Context
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.feature.editor.data.EditorDatabase
import tech.dotlab.dot.feature.editor.data.PixelArtRepository

/**
 * Tiny manual DI for the editor — avoids pulling a full DI framework into the module.
 */
object EditorGraph {

    fun repository(context: Context): PixelArtRepository =
        PixelArtRepository(EditorDatabase.get(context).pixelArtDao())

    /**
     * Profile to draw for. Uses the resolved device, or falls back to the 4a Pro profile so the
     * editor is usable on an emulator / unsupported device (just without live matrix output).
     */
    fun editingProfile(): DeviceProfile =
        when (val support = DeviceRegistry.resolveCurrent()) {
            is DeviceSupport.Supported -> support.profile
            is DeviceSupport.SystemTooOld -> support.profile
            DeviceSupport.UnsupportedDevice ->
                DeviceRegistry.byId("DEVICE_25111p")
                    ?: error("default profile DEVICE_25111p missing from registry")
        }
}
