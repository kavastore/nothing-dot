package tech.dotlab.dot.device

/**
 * Runtime device signal fed from the `:matrix` layer.
 *
 * The GlyphMatrix SDK can report the matrix edge length (`Common.getDeviceMatrixLength()` → 25 for
 * Phone 3, 13 for 4a Pro) once its service connects. That is the most reliable, build-string-free
 * way to identify the hardware. `:device` can't depend on `:matrix`, so `:matrix` writes the value
 * here and [DeviceRegistry.resolveCurrent] reads it.
 */
object SdkSignal {

    /** Matrix edge length reported by the SDK, or null until a service has connected. */
    @Volatile
    var matrixLength: Int? = null
}
