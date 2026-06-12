package tech.dotlab.dot.feature.key.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.core.model.UnlockMethod

/** Snapshot of everything the key remapper needs to act on a captured key event. */
data class KeyConfig(
    /** Recorded Essential Key keyCode, or null until the user records one. */
    val keyCode: Int?,
    /** Gesture -> actionId map (only gestures with a binding are present). */
    val bindings: Map<Gesture, String>,
    /** Active unlock depth. Lite always reports [UnlockMethod.None]. */
    val unlockMethod: UnlockMethod,
    /** Frame shown by the "show image" action (brightness already baked in), or null. */
    val showFrame: LogicalFrame? = null,
    /** How long the "show image" action keeps the frame on the matrix. */
    val showDurationSec: Int = 3,
) {
    val isRecorded: Boolean get() = keyCode != null
}

private val Context.keyDataStore: DataStore<Preferences> by preferencesDataStore(name = "dot_key_prefs")

/**
 * DataStore-backed storage for the key remapper: recorded keyCode, per-gesture action
 * bindings, unlock method, and the frame used by the "show image" action.
 */
class KeyPrefs(private val context: Context) {

    val config: Flow<KeyConfig> = context.keyDataStore.data.map { it.toConfig() }

    /** One-shot read for non-composable callers (e.g. the key action). */
    suspend fun current(): KeyConfig = config.first()

    suspend fun setKeyCode(keyCode: Int) {
        context.keyDataStore.edit { it[KEY_CODE] = keyCode }
    }

    /** Stores a frame (with brightness baked in) and a display duration for [ShowGlyphArtAction]. */
    suspend fun setShowImage(frame: LogicalFrame, durationSec: Int) {
        context.keyDataStore.edit {
            it[SHOW_FRAME] = frame.brightness.joinToString(",")
            it[SHOW_SIZE] = frame.size
            it[SHOW_DURATION] = durationSec.coerceIn(1, 30)
        }
    }

    suspend fun bind(gesture: Gesture, actionId: String) {
        context.keyDataStore.edit { it[bindingKey(gesture)] = actionId }
    }

    suspend fun clearBinding(gesture: Gesture) {
        context.keyDataStore.edit { it.remove(bindingKey(gesture)) }
    }

    suspend fun setUnlockMethod(method: UnlockMethod) {
        context.keyDataStore.edit { it[UNLOCK_METHOD] = method.id }
    }

    private fun Preferences.toConfig(): KeyConfig {
        val bindings = Gesture.entries.mapNotNull { gesture ->
            this[bindingKey(gesture)]?.let { gesture to it }
        }.toMap()
        return KeyConfig(
            keyCode = this[KEY_CODE],
            bindings = bindings,
            unlockMethod = UnlockMethod.fromId(this[UNLOCK_METHOD] ?: UnlockMethod.None.id),
            showFrame = decodeFrame(this[SHOW_FRAME], this[SHOW_SIZE]),
            showDurationSec = this[SHOW_DURATION] ?: 3,
        )
    }

    private fun decodeFrame(csv: String?, size: Int?): LogicalFrame? {
        if (csv.isNullOrBlank() || size == null || size <= 0) return null
        val values = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (values.size != size * size) return null
        return LogicalFrame(size, values.toIntArray())
    }

    private companion object {
        val KEY_CODE = intPreferencesKey("key_code")
        val UNLOCK_METHOD = stringPreferencesKey("unlock_method")
        val SHOW_FRAME = stringPreferencesKey("show_frame")
        val SHOW_SIZE = intPreferencesKey("show_size")
        val SHOW_DURATION = intPreferencesKey("show_duration")
        fun bindingKey(gesture: Gesture) = stringPreferencesKey("binding_${gesture.name}")
    }
}
