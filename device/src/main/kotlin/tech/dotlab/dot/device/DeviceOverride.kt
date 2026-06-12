package tech.dotlab.dot.device

import android.content.Context

/**
 * Developer override for the active device profile.
 *
 * Lets us force a specific [DeviceProfile] regardless of the real hardware — essential for testing
 * Phone (3)'s 25×25 matrix, its interactive toy and the Arkanoid game on an emulator or a 4a Pro
 * when the actual phone isn't on hand. Persisted in SharedPreferences and mirrored in memory so
 * [DeviceRegistry.resolveCurrent] can read it without threading a Context everywhere.
 */
object DeviceOverride {

    @Volatile
    private var cachedId: String? = null

    /** Currently forced profile id, or null when detection runs normally. */
    val activeId: String? get() = cachedId

    /** Loads the persisted override into memory; call early (Application / service onCreate). */
    fun init(context: Context) {
        cachedId = prefs(context).getString(KEY, null)
    }

    /** Sets (or clears, when [id] is null) the override and persists it. */
    fun set(context: Context, id: String?) {
        cachedId = id
        prefs(context).edit().apply {
            if (id == null) remove(KEY) else putString(KEY, id)
        }.apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "dot_device_override"
    private const val KEY = "device_id"
}
