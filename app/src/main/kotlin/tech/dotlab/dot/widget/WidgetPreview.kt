package tech.dotlab.dot.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask

/**
 * Renders a [LogicalFrame] over a [ShapeMask] into a small monochrome dot bitmap for the widget's
 * `ImageView`. Kept intentionally small (default 256px) to stay well under the ~1MB Binder limit
 * when handed to the launcher via RemoteViews.
 */
object WidgetPreview {

    fun render(frame: LogicalFrame?, mask: ShapeMask, sizePx: Int = 256): Bitmap {
        val size = mask.size
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)

        val cell = sizePx.toFloat() / size
        val radius = cell * 0.42f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                val v = frame?.brightnessAt(x, y) ?: 0
                paint.color = if (v > 0) {
                    Color.argb(v.coerceIn(1, 255), 255, 255, 255)
                } else {
                    DIM_GREY
                }
                canvas.drawCircle(cell * x + cell / 2f, cell * y + cell / 2f, radius, paint)
            }
        }
        return bmp
    }

    private val DIM_GREY = Color.rgb(42, 42, 42)
}
