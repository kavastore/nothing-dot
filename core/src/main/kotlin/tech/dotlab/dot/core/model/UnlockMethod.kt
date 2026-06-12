package tech.dotlab.dot.core.model

/**
 * How deeply the app frees the Essential Key from the system.
 *
 * Modeled as a sealed hierarchy so future methods can carry method-specific data without
 * touching call sites that only branch on the type.
 */
sealed interface UnlockMethod {
    val id: String

    /** Work on top of the system; only multi-tap gestures are available. */
    data object None : UnlockMethod {
        override val id: String = "none"
    }

    /** Disable system packages via Shizuku — no PC, no manual commands. */
    data object Shizuku : UnlockMethod {
        override val id: String = "shizuku"
    }

    /** User wired an ADB config once via the in-app helper. */
    data object AdbConfig : UnlockMethod {
        override val id: String = "adb_config"
    }

    /** Direct control when root is available (optional, future). */
    data object Root : UnlockMethod {
        override val id: String = "root"
    }

    companion object {
        val all: List<UnlockMethod> = listOf(None, Shizuku, AdbConfig, Root)

        fun fromId(id: String): UnlockMethod =
            all.firstOrNull { it.id == id } ?: None
    }
}
