package tech.dotlab.dot.device

import tech.dotlab.dot.core.model.ToyType

/**
 * The single extension point for supporting new phones.
 *
 * Modules ask capability questions ("supportsAppMatrix?", "toyTypes.contains(AOD)?") instead of
 * "is this a 4a Pro?". Adding Phone (3) or a future model is one new [DeviceProfile] in
 * [DeviceRegistry] plus its [shapeMask] — module code stays untouched.
 */
data class DeviceProfile(
    /** Internal device id, e.g. "DEVICE_25111p". */
    val id: String,

    /** Value passed to the SDK `register()` call, e.g. "DEVICE_25111p". */
    val sdkTarget: String,

    /** Matrix edge length: 13 for 4a Pro, 25 for Phone 3. */
    val matrixSize: Int,

    /** Whether the device has Glyph Touch (false for 4a Pro). */
    val hasGlyphTouch: Boolean,

    /** True if the system supports `setAppMatrixFrame` (build >= 20250801). */
    val supportsAppMatrix: Boolean,

    val toyTypes: Set<ToyType>,

    val shapeMask: ShapeMask,

    /**
     * Identifiers used to recognise this device from [android.os.Build] fields.
     * For 4a Pro these are the real values: `Build.MODEL` = "A069P", `Build.DEVICE` = "FroggerPro".
     */
    val aliases: Set<String> = emptySet(),
) {
    init {
        require(shapeMask.size == matrixSize) {
            "shapeMask size (${shapeMask.size}) must match matrixSize ($matrixSize)"
        }
    }

    val supportsAod: Boolean get() = ToyType.AOD in toyTypes
}
