package tech.dotlab.dot.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens the system Glyph Interface settings screen.
 *
 * Real matrix output only works while Nothing OS "Glyph Interface" is enabled — a private system
 * toggle that third-party apps can neither read nor flip. When it is off the SDK still authorises
 * and accepts frames (no error), but the physical matrix stays dark, which looks like a broken app.
 * So we surface a one-tap shortcut to the toggle instead.
 *
 * Falls back through known Nothing settings activities, then to the app's own settings page.
 */
fun openGlyphInterfaceSettings(context: Context): Boolean {
    val targets = listOf(
        ComponentName("com.android.settings", "com.nothing.settings.NtSettings\$GlyphsSettingsActivity"),
        ComponentName("com.android.settings", "com.nothing.settings.NtSettings\$FlipToGlyphPreferenceActivity"),
    )
    for (target in targets) {
        val ok = runCatching {
            context.startActivity(Intent().setComponent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (ok) return true
    }
    return runCatching {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess
}
