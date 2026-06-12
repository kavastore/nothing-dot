package tech.dotlab.dot.device

import android.os.Build
import tech.dotlab.dot.core.model.ToyType

/**
 * Registry of supported devices and the entry point for resolving the current one.
 *
 * Adding a new phone = one new [DeviceProfile] here. Nothing else changes.
 */
object DeviceRegistry {

    /** Minimum Nothing OS build (date code) that supports `setAppMatrixFrame`. */
    const val MIN_APP_MATRIX_BUILD: Long = 20_250_801L

    /** Build (date code) from which the ToysManagerActivity deep link is valid. */
    const val MIN_TOYS_MANAGER_BUILD: Long = 20_250_829L

    private val profiles: List<DeviceProfile> = listOf(
        nothingPhone4aPro(),
        nothingPhone3(),
    )

    fun all(): List<DeviceProfile> = profiles

    fun byId(id: String): DeviceProfile? = profiles.firstOrNull { it.id == id }

    /** Profile matching a runtime matrix edge length reported by the SDK (25→Phone 3, 13→4a Pro). */
    fun byMatrixSize(size: Int): DeviceProfile? = profiles.firstOrNull { it.matrixSize == size }

    /**
     * Resolves the device this code is running on into a support verdict for onboarding.
     *
     * Resolution priority:
     * 1. **Dev override** ([DeviceOverride]) — lets us force a profile for testing without the
     *    hardware on hand (e.g. preview Phone 3's 25×25 on an emulator or a 4a Pro).
     * 2. **Build aliases** — [DeviceProfile.aliases] vs [Build.MODEL]/[Build.DEVICE]. These are the
     *    confirmed identifiers of real hardware (4a Pro reports MODEL="A069P", DEVICE="FroggerPro"),
     *    so they take precedence: a single bad SDK reading must never flip a real 4a Pro to Phone 3
     *    and break all matrix output.
     * 3. **SDK signal** ([SdkSignal.matrixLength], fed from `:matrix` via `Common.getDeviceMatrixLength()`)
     *    — fallback for devices whose Build codenames aren't in our alias set yet.
     *
     * The system-build gate was removed: the real Nothing OS build string (e.g. "B4.1-260522-1414")
     * doesn't follow the YYYYMMDD scheme, so parsing it produced false negatives.
     *
     * @param model       defaults to [Build.MODEL]; injectable for tests.
     * @param device      defaults to [Build.DEVICE]; injectable for tests.
     * @param overrideId  forced profile id; defaults to [DeviceOverride.activeId].
     * @param sdkMatrixSize matrix edge length reported by the SDK; defaults to [SdkSignal.matrixLength].
     */
    fun resolveCurrent(
        model: String = Build.MODEL.orEmpty(),
        device: String = Build.DEVICE.orEmpty(),
        overrideId: String? = DeviceOverride.activeId,
        sdkMatrixSize: Int? = SdkSignal.matrixLength,
    ): DeviceSupport {
        overrideId?.let { id -> byId(id)?.let { return DeviceSupport.Supported(it) } }
        // Confirmed Build aliases of real hardware win over the SDK signal, so a stray
        // getDeviceMatrixLength() reading can't misidentify a real 4a Pro and kill matrix output.
        profiles.firstOrNull { it.matches(model, device) }?.let { return DeviceSupport.Supported(it) }
        sdkMatrixSize?.let { len -> byMatrixSize(len)?.let { return DeviceSupport.Supported(it) } }
        return DeviceSupport.UnsupportedDevice
    }

    private fun DeviceProfile.matches(model: String, device: String): Boolean {
        val fields = listOf(model.trim(), device.trim()).filter { it.isNotEmpty() }
        return aliases.any { alias ->
            fields.any { it.equals(alias, ignoreCase = true) || it.contains(alias, ignoreCase = true) }
        }
    }

    private fun nothingPhone4aPro(): DeviceProfile = DeviceProfile(
        id = "DEVICE_25111p",
        sdkTarget = "DEVICE_25111p",
        matrixSize = 13,
        hasGlyphTouch = false,
        supportsAppMatrix = true,
        toyTypes = setOf(ToyType.AOD),
        shapeMask = ShapeMask.circle(13),
        aliases = setOf("A069P", "FroggerPro", "25111p", "25111"),
    )

    private fun nothingPhone3(): DeviceProfile = DeviceProfile(
        id = "DEVICE_23112",
        sdkTarget = "DEVICE_23112",
        matrixSize = 25,
        hasGlyphTouch = true,
        supportsAppMatrix = true,
        toyTypes = setOf(ToyType.ALL, ToyType.AOD),
        // TODO(device): replace circle(25) with the exact 489-LED allocation from the official
        // Glyph Matrix spec SVG (ShapeMask.fromPattern) once verified on hardware.
        shapeMask = ShapeMask.circle(25),
        // Model number A024 is the stable offline signal; codenames are leak-based guesses and
        // must be confirmed on hardware. Reliable detection goes through the SDK (SdkSignal).
        aliases = setOf("A024", "23112", "Phone (3)", "Phone(3)", "Metroid", "Asteroid", "Arcanine"),
    )
}

/** Verdict for the current device, used by onboarding to show the right screen. */
sealed interface DeviceSupport {
    data class Supported(val profile: DeviceProfile) : DeviceSupport

    /** Device is known but the system build is older than [MIN_APP_MATRIX_BUILD]. */
    data class SystemTooOld(val profile: DeviceProfile, val currentBuild: Long) : DeviceSupport

    data object UnsupportedDevice : DeviceSupport
}
