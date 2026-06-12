package tech.dotlab.dot.matrix

import android.util.Log
import tech.dotlab.dot.device.SdkSignal

/**
 * Reads the device matrix length from the GlyphMatrix SDK (`com.nothing.ketchum.Common`) and
 * publishes it to [SdkSignal] so [tech.dotlab.dot.device.DeviceRegistry] can identify the hardware
 * without relying on `Build.*` strings (whose Phone 3 codename is unconfirmed).
 *
 * Resolved reflectively (like [GlyphTarget]) so the app compiles against any AAR version and simply
 * does nothing when the method is missing. Call once the Glyph service has connected.
 */
object SdkDeviceProbe {

    fun publishMatrixLength() {
        val length = runCatching {
            Class.forName("com.nothing.ketchum.Common")
                .getMethod("getDeviceMatrixLength")
                .invoke(null) as? Int
        }.getOrElse {
            Log.w(TAG, "Common.getDeviceMatrixLength() unavailable", it)
            null
        }
        if (length != null && length > 0) {
            SdkSignal.matrixLength = length
        }
    }

    private const val TAG = "SdkDeviceProbe"
}
