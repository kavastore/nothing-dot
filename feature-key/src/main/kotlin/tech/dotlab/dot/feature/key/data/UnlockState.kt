package tech.dotlab.dot.feature.key.data

import android.content.Context
import android.content.pm.PackageManager

/** System packages that own the Essential Key until they are disabled. */
object EssentialKeyPackages {
    const val SPACE = "com.nothing.ntessentialspace"
    const val RECORDER = "com.nothing.ntessentialrecorder"
    val all = listOf(SPACE, RECORDER)
}

/** Remapper capability level, derived from real system state (not stored guesses). */
enum class KeyLevel {
    /** Accessibility service is off — nothing is intercepted. */
    OFF,

    /** Service on, but Essential Space still owns the key (single press = system). */
    LITE,

    /** Essential Space packages disabled — every press (incl. SINGLE) is ours. */
    FULL,
}

/**
 * Reads whether the Essential Key has been "freed" by checking if the owning packages are disabled.
 * Works regardless of how they were disabled (Shizuku or ADB), and requires only a `<queries>`
 * entry for the two packages.
 */
object UnlockState {

    /** A package no longer blocks the key when it is disabled or not installed. */
    private fun isPackageBlocking(context: Context, pkg: String): Boolean {
        return try {
            when (context.packageManager.getApplicationEnabledSetting(pkg)) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
                -> false
                else -> true
            }
        } catch (_: IllegalArgumentException) {
            false // not installed → not blocking
        }
    }

    fun isFreed(context: Context): Boolean =
        EssentialKeyPackages.all.none { isPackageBlocking(context, it) }

    fun level(context: Context, serviceConnected: Boolean): KeyLevel = when {
        isFreed(context) -> KeyLevel.FULL
        serviceConnected -> KeyLevel.LITE
        else -> KeyLevel.OFF
    }
}
