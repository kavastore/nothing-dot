package tech.dotlab.dot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import tech.dotlab.dot.R
import tech.dotlab.dot.device.DeviceOverride
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.device.ShapeMask

/**
 * Home-screen widget: a tile that previews the last drawing and, when tapped, opens
 * [QuickDrawActivity]. The widget can't host finger drawing itself (RemoteViews has no
 * Canvas/touch), so it acts purely as preview + entry point.
 */
class DrawWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {

        /** Re-renders every placed widget (called after a new drawing is committed). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DrawWidgetProvider::class.java),
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            DeviceOverride.init(context)
            val profile = (DeviceRegistry.resolveCurrent() as? DeviceSupport.Supported)?.profile
            val mask = profile?.shapeMask ?: ShapeMask.circle(13)
            val frame = WidgetDrawStore.load(context)?.takeIf { it.size == mask.size }
            val bitmap = WidgetPreview.render(frame, mask)

            val views = RemoteViews(context.packageName, R.layout.widget_draw)
            views.setImageViewBitmap(R.id.widget_preview, bitmap)
            views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
            manager.updateAppWidget(widgetId, views)
        }

        private fun launchIntent(context: Context): PendingIntent {
            val intent = Intent(context, QuickDrawActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
