package tech.dotlab.dot.widget

import android.content.Context
import tech.dotlab.dot.core.model.LogicalFrame

/**
 * Persists the last drawing made through the home-screen widget, so its tile can show a preview.
 *
 * Uses plain [android.content.SharedPreferences] (synchronous): the widget provider's `onUpdate`
 * runs on the main thread and needs an immediate read, which a coroutine-based DataStore can't give
 * cleanly. The frame is encoded the same way as [tech.dotlab.dot.feature.key.data.KeyPrefs]:
 * a size plus a CSV of brightness values.
 */
object WidgetDrawStore {

    fun save(context: Context, frame: LogicalFrame) {
        prefs(context).edit()
            .putInt(KEY_SIZE, frame.size)
            .putString(KEY_FRAME, frame.brightness.joinToString(","))
            .apply()
    }

    fun load(context: Context): LogicalFrame? {
        val p = prefs(context)
        val size = p.getInt(KEY_SIZE, 0)
        val csv = p.getString(KEY_FRAME, null)
        if (size <= 0 || csv.isNullOrBlank()) return null
        val values = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (values.size != size * size) return null
        return LogicalFrame(size, values.toIntArray())
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "dot_widget_draw"
    private const val KEY_SIZE = "frame_size"
    private const val KEY_FRAME = "frame_csv"
}
