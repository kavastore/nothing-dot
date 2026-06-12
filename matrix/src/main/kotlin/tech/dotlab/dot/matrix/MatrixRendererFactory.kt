package tech.dotlab.dot.matrix

import android.content.Context
import tech.dotlab.dot.device.DeviceProfile

/**
 * Builds the right [MatrixRenderer] for a device: SDK-backed when the device supports
 * `setAppMatrixFrame`, otherwise a no-op. Capability-driven, never branches on model name.
 */
object MatrixRendererFactory {
    fun create(context: Context, profile: DeviceProfile?): MatrixRenderer {
        if (profile == null || !profile.supportsAppMatrix) return NoopMatrixRenderer
        return GlyphMatrixRenderer(context, profile)
    }
}
