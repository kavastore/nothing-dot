package tech.dotlab.dot.matrix

import android.util.Log

/**
 * Resolves the SDK device-code string that `GlyphMatrixManager.register()` expects.
 *
 * Device constants live in `com.nothing.ketchum.Glyph` as named String fields (e.g. `DEVICE_23112`
 * → "A024"). Their set grows with SDK releases — older AARs don't have `DEVICE_25111p`. We look the
 * field up reflectively by name (the value stored in [tech.dotlab.dot.device.DeviceProfile.sdkTarget])
 * so the app compiles against any SDK version and simply reports "unavailable" when a device's
 * constant is missing from the bundled AAR.
 */
object GlyphTarget {
    fun resolve(fieldName: String): String? =
        runCatching {
            Class.forName("com.nothing.ketchum.Glyph")
                .getField(fieldName)
                .get(null) as? String
        }.getOrElse {
            Log.w("GlyphTarget", "Glyph.$fieldName not found in bundled SDK", it)
            null
        }
}
